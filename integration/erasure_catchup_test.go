//go:build integration

package integration

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"path/filepath"
	"testing"
	"time"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
	_ "modernc.org/sqlite" // database/sql driver "sqlite", used to inspect the consumer's store

	authv1 "github.com/Muxcore-Media/core/proto/gen/muxcore/auth/v1"
	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
)

// ADR-0035 section 5 gate: "a cross-module integration/ test passes in which
// one consumer is stopped during deletion and catches up after it starts".
//
// The identity provider is the published auth-local and the personal-data
// consumer is the published playback-monitor, joined by a real muxcored on
// the household profile. Every call that matters travels over mesh mTLS:
//
//   - ListUserErasures / AckUserErasure are admitted only for a verified
//     client certificate whose CN is on AUTH_ERASURE_CONSUMERS. playback-monitor
//     reaches the ledger through the SDK reconciler with its enrolled
//     certificate (CN playback-monitor).
//   - DeleteUser needs an end-user admin bearer in x-auth-token.
//
// This test is ONE of the NFR-DATA-003 gates (ADR-0035 section 5). It does not
// close the requirement: the household clean-room journey and the restore
// drill (roadmap T-M4-07 slice E12) are separate gates, and NFR-DATA-003
// stays Plan.

const (
	monitorModuleID = "playback-monitor"
	nativeServerID  = "muxcore-native" // the BFF's server id for native MuxCore players

	userPassword = "e11-user-Passw0rd-long-enough"

	// catchUpTimeout bounds one startup sweep plus its acknowledgement. The
	// reconciler sweeps immediately at start, so this is generous.
	catchUpTimeout = 90 * time.Second
)

func TestErasureCatchUp_ConsumerStoppedDuringUserDeletion(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 8*time.Minute)
	defer cancel()

	// The process-level view of the deployment configuration. auth-local
	// receives the same values in its own environment (the test process is
	// not auth-local); playback-monitor, which runs in this process, reads
	// the mesh identity and core address below.
	t.Setenv("AUTH_ERASURE_CONSUMERS", monitorModuleID)
	t.Setenv("AUTH_ERASURE_REQUIRED", monitorModuleID)
	for _, k := range []string{"MUXCORE_INSECURE_DISABLE_TLS", "MUXCORE_DEV_TLS_SKIP", "MUXCORE_GRPC_INSECURE"} {
		t.Setenv(k, "") // plaintext is never evidence for this gate
	}

	stack := newMeshStack(t, []string{monitorModuleID}, []string{monitorModuleID})

	// --- Mesh identity of the consumer (CN = playback-monitor). ---
	id := stack.Enroll(t, monitorModuleID)
	t.Setenv("MUXCORE_PROFILE", "household")
	t.Setenv("MUXCORE_GRPC_ADDR", stack.CoreAddr)
	t.Setenv("MUXCORE_MODULE_ID", monitorModuleID)
	t.Setenv("MUXCORE_TLS_CERT", id.Cert)
	t.Setenv("MUXCORE_TLS_KEY", id.Key)
	t.Setenv("MUXCORE_TLS_CA", id.CA)

	// --- End users, over TLS, with no module certificate. ---
	userCl := stack.AuthClient(t, "", "")
	adminToken := waitLogin(t, userCl, meshAdminUser, meshAdminPassword)
	adminCtx := withBearer(ctx, adminToken)

	victim := createUser(t, adminCtx, userCl, "e11-victim")
	victim2 := createUser(t, adminCtx, userCl, "e11-victim-two")
	control := createUser(t, adminCtx, userCl, "e11-control")
	victimToken := login(t, userCl, "e11-victim", userPassword)
	victim2Token := login(t, userCl, "e11-victim-two", userPassword)
	controlToken := login(t, userCl, "e11-control", userPassword)
	for name, tok := range map[string]string{"victim": victimToken, "victim2": victim2Token, "control": controlToken} {
		if v, err := userCl.Validate(ctx, &authv1.ValidateRequest{Token: tok}); err != nil || !v.GetValid() {
			t.Fatalf("setup: %s bearer must validate before deletion: %v, %v", name, v, err)
		}
	}

	// --- playback-monitor state keyed by those user ids. ---
	dbPath := filepath.Join(t.TempDir(), "playback-monitor.db")
	mon := startMonitor(t, ctx, dbPath)
	for _, u := range []struct{ id, name string }{{victim, "e11-victim"}, {victim2, "e11-victim-two"}, {control, "e11-control"}} {
		seedSession(t, ctx, mon, nativeServerID, u.id, u.name)
	}
	// An external media-server account that nothing links to a deleted user.
	// ADR-0035 section 3 retains it: it lives in another id space.
	const externalUser = "jellyfin-account-e11"
	seedSession(t, ctx, mon, "jellyfin-e11", externalUser, "ext-viewer")
	stopMonitor(t, ctx, mon)

	victimRows := monitorRows(t, dbPath, nativeServerID, victim)
	victim2Rows := monitorRows(t, dbPath, nativeServerID, victim2)
	controlRows := monitorRows(t, dbPath, nativeServerID, control)
	externalRows := monitorRows(t, dbPath, "jellyfin-e11", externalUser)
	for name, r := range map[string]rowCounts{"victim": victimRows, "victim2": victim2Rows, "control": controlRows, "external": externalRows} {
		if r.total() == 0 || r.Sessions == 0 || r.ServerUsers == 0 || r.Identities == 0 {
			t.Fatalf("setup: playback-monitor must hold sessions, a server_users link and an identity for %s, got %+v", name, r)
		}
	}

	// --- Access control over the real transport. ---
	// x-caller-id is only a hint: a TLS caller without a certificate cannot
	// borrow playback-monitor's identity with it.
	spoof := metadata.AppendToOutgoingContext(ctx, "x-caller-id", monitorModuleID)
	if _, err := userCl.ListUserErasures(spoof, &authv1.ListUserErasuresRequest{}); status.Code(err) != codes.Unauthenticated {
		t.Fatalf("ListUserErasures with x-caller-id and no certificate = %v; want Unauthenticated", err)
	}
	moduleCl := stack.AuthClient(t, id.Cert, id.Key)
	// A module certificate alone cannot erase a user.
	if _, err := moduleCl.DeleteUser(ctx, &authv1.DeleteUserRequest{UserId: victim}); err == nil {
		t.Fatal("DeleteUser with only a module certificate succeeded; ADR-0035 section 1 requires an admin bearer")
	} else if c := status.Code(err); c != codes.Unauthenticated && c != codes.PermissionDenied {
		t.Fatalf("DeleteUser with only a module certificate = %v; want Unauthenticated or PermissionDenied", err)
	}
	// A non-admin end user cannot erase anyone.
	if _, err := userCl.DeleteUser(withBearer(ctx, controlToken), &authv1.DeleteUserRequest{UserId: victim}); status.Code(err) != codes.PermissionDenied {
		t.Fatalf("DeleteUser with a non-admin bearer = %v; want PermissionDenied", err)
	}
	// An end-user bearer is not accepted on the ledger either.
	if _, err := moduleCl.ListUserErasures(withBearer(ctx, adminToken), &authv1.ListUserErasuresRequest{}); status.Code(err) != codes.PermissionDenied {
		t.Fatalf("ListUserErasures with a user bearer = %v; want PermissionDenied", err)
	}
	if got := monitorRows(t, dbPath, nativeServerID, victim); got != victimRows {
		t.Fatalf("rejected calls changed the consumer's store: %+v -> %+v", victimRows, got)
	}
	if v, err := userCl.Validate(ctx, &authv1.ValidateRequest{Token: victimToken}); err != nil || !v.GetValid() {
		t.Fatalf("rejected DeleteUser calls must not revoke the victim: %v, %v", v, err)
	}

	// --- Delete the first user while playback-monitor is stopped. ---
	erasure1 := deleteUser(t, adminCtx, userCl, victim)

	if v, err := userCl.Validate(ctx, &authv1.ValidateRequest{Token: victimToken}); err != nil || v.GetValid() {
		t.Fatalf("Validate for the deleted user = %v, %v; want invalid", v, err)
	}
	if v, err := userCl.Validate(ctx, &authv1.ValidateRequest{Token: controlToken}); err != nil || !v.GetValid() {
		t.Fatalf("Validate for the control user = %v, %v; want valid", v, err)
	}
	// Tombstone exists: the ledger lists it for an allowlisted consumer cert.
	if tomb := findTombstone(t, ctx, moduleCl, erasure1); tomb.GetUserId() != victim || tomb.GetAcknowledgedByCaller() {
		t.Fatalf("tombstone = %v; want user %s, not acknowledged by %s", tomb, victim, monitorModuleID)
	}
	// ...and the admin status shows playback-monitor has not acknowledged.
	st := erasureStatus(t, adminCtx, userCl, erasure1)
	if st.GetComplete() {
		t.Fatalf("erasure %s is complete while %s was stopped: %v", erasure1, monitorModuleID, st)
	}
	if ms := moduleStatus(t, st, monitorModuleID); !ms.GetRequired() ||
		ms.GetOutcome() != authv1.ErasureOutcome_ERASURE_OUTCOME_UNSPECIFIED || ms.GetAckedAt() != "" {
		t.Fatalf("%s status before it starts = %v; want required and pending", monitorModuleID, ms)
	}
	// The stopped consumer's rows are still there: nothing but the ledger
	// (and nothing at all, while it is down) erases them.
	if got := monitorRows(t, dbPath, nativeServerID, victim); got != victimRows {
		t.Fatalf("victim rows changed while the consumer was stopped: %+v -> %+v", victimRows, got)
	}

	// --- Start playback-monitor: it catches up on its own. ---
	mon = startMonitor(t, ctx, dbPath)
	waitErasureComplete(t, adminCtx, userCl, erasure1)
	if ms := moduleStatus(t, erasureStatus(t, adminCtx, userCl, erasure1), monitorModuleID); ms.GetOutcome() != authv1.ErasureOutcome_ERASURE_OUTCOME_OK || ms.GetAckedAt() == "" {
		t.Fatalf("%s acknowledgement = %v; want OK with a time", monitorModuleID, ms)
	}
	if got := monitorRows(t, dbPath, nativeServerID, victim); got.total() != 0 {
		t.Fatalf("playback-monitor still holds rows for the deleted user after OK: %+v", got)
	}
	assertApplied(t, dbPath, erasure1, victim)
	assertUntouched(t, dbPath, "after the first catch-up", map[string]rowCounts{
		"victim2": victim2Rows, "control": controlRows, "external": externalRows,
	}, map[string][2]string{
		"victim2": {nativeServerID, victim2}, "control": {nativeServerID, control}, "external": {"jellyfin-e11", externalUser},
	})
	// A native session for the erased user is refused (ADR-0035 section 3).
	if _, _, err := monitorinteg.IngestSession(ctx, mon, monitorinteg.SessionEvent{
		EventType: "playback.started", ServerID: nativeServerID, ExternalSessionID: "e11-late",
		UserID: victim, UserName: "e11-victim", ItemID: "mv-late", Title: "Late write",
	}); err == nil {
		t.Fatal("playback-monitor accepted a session for an erased user")
	}
	if got := monitorRows(t, dbPath, nativeServerID, victim); got.total() != 0 {
		t.Fatalf("a refused session left rows for the erased user: %+v", got)
	}

	// --- Second start: a replayed tombstone is harmless, a new one is applied. ---
	stopMonitor(t, ctx, mon)
	erasure2 := deleteUser(t, adminCtx, userCl, victim2)
	if st := erasureStatus(t, adminCtx, userCl, erasure2); st.GetComplete() {
		t.Fatalf("erasure %s is complete while %s was stopped", erasure2, monitorModuleID)
	}
	if got := monitorRows(t, dbPath, nativeServerID, victim2); got != victim2Rows {
		t.Fatalf("victim2 rows changed while the consumer was stopped: %+v -> %+v", victim2Rows, got)
	}
	mon = startMonitor(t, ctx, dbPath)
	waitErasureComplete(t, adminCtx, userCl, erasure2)
	_ = mon // stopped by t.Cleanup

	if got := monitorRows(t, dbPath, nativeServerID, victim2); got.total() != 0 {
		t.Fatalf("playback-monitor still holds rows for the second deleted user: %+v", got)
	}
	assertApplied(t, dbPath, erasure1, victim)
	assertApplied(t, dbPath, erasure2, victim2)
	assertAppliedCount(t, dbPath, 2) // the replay of the first tombstone was not re-applied
	assertUntouched(t, dbPath, "after the second start", map[string]rowCounts{
		"control": controlRows, "external": externalRows,
	}, map[string][2]string{
		"control": {nativeServerID, control}, "external": {"jellyfin-e11", externalUser},
	})
	// The control user is a fully working account at the provider.
	if v, err := userCl.Validate(ctx, &authv1.ValidateRequest{Token: controlToken}); err != nil || !v.GetValid() || v.GetUserId() != control {
		t.Fatalf("control user bearer after both erasures = %v, %v; want valid", v, err)
	}
}

// --- helpers: auth-local --------------------------------------------------

func withBearer(ctx context.Context, token string) context.Context {
	return metadata.AppendToOutgoingContext(ctx, "x-auth-token", token)
}

// waitLogin retries until auth-local (still starting) accepts the password.
func waitLogin(t *testing.T, c authv1.AuthServiceClient, username, password string) string {
	t.Helper()
	var token string
	waitFor(t, 60*time.Second, "auth-local to accept the bootstrap admin", func() error {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		r, err := c.Authenticate(ctx, passwordRequest(t, username, password))
		if err != nil {
			return err
		}
		if !r.GetAuthenticated() {
			return errors.New(r.GetError())
		}
		token = r.GetSessionToken()
		return nil
	})
	return token
}

func login(t *testing.T, c authv1.AuthServiceClient, username, password string) string {
	t.Helper()
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	r, err := c.Authenticate(ctx, passwordRequest(t, username, password))
	if err != nil || !r.GetAuthenticated() || r.GetSessionToken() == "" {
		t.Fatalf("login %s: %v, %v", username, r, err)
	}
	return r.GetSessionToken()
}

func passwordRequest(t *testing.T, username, password string) *authv1.AuthenticateRequest {
	t.Helper()
	data, err := json.Marshal(map[string]string{"username": username, "password": password})
	if err != nil {
		t.Fatal(err)
	}
	return &authv1.AuthenticateRequest{CredentialType: "password", CredentialData: data}
}

func createUser(t *testing.T, adminCtx context.Context, c authv1.AuthServiceClient, username string) string {
	t.Helper()
	r, err := c.CreateUser(adminCtx, &authv1.CreateUserRequest{Username: username, Password: userPassword})
	if err != nil || r.GetError() != "" || r.GetUserId() == "" {
		t.Fatalf("create user %s: %v, %v", username, r, err)
	}
	return r.GetUserId()
}

func deleteUser(t *testing.T, adminCtx context.Context, c authv1.AuthServiceClient, userID string) string {
	t.Helper()
	r, err := c.DeleteUser(adminCtx, &authv1.DeleteUserRequest{UserId: userID})
	if err != nil || r.GetErasureId() == "" {
		t.Fatalf("DeleteUser %s: %v, %v", userID, r, err)
	}
	return r.GetErasureId()
}

func findTombstone(t *testing.T, ctx context.Context, moduleCl authv1.AuthServiceClient, erasureID string) *authv1.UserErasure {
	t.Helper()
	token := ""
	for {
		r, err := moduleCl.ListUserErasures(ctx, &authv1.ListUserErasuresRequest{PageToken: token})
		if err != nil {
			t.Fatalf("ListUserErasures as %s: %v", monitorModuleID, err)
		}
		for _, e := range r.GetErasures() {
			if e.GetErasureId() == erasureID {
				return e
			}
		}
		if token = r.GetNextPageToken(); token == "" {
			t.Fatalf("tombstone %s is not in the ledger", erasureID)
		}
	}
}

func erasureStatus(t *testing.T, adminCtx context.Context, c authv1.AuthServiceClient, erasureID string) *authv1.ErasureStatus {
	t.Helper()
	r, err := c.GetUserErasureStatus(adminCtx, &authv1.GetUserErasureStatusRequest{ErasureId: erasureID})
	if err != nil || len(r.GetErasures()) != 1 {
		t.Fatalf("GetUserErasureStatus(%s) = %v, %v; want exactly one erasure", erasureID, r, err)
	}
	return r.GetErasures()[0]
}

func moduleStatus(t *testing.T, st *authv1.ErasureStatus, moduleID string) *authv1.ErasureModuleStatus {
	t.Helper()
	for _, m := range st.GetModules() {
		if m.GetModuleId() == moduleID {
			return m
		}
	}
	t.Fatalf("erasure status has no entry for %s: %v", moduleID, st)
	return nil
}

func waitErasureComplete(t *testing.T, adminCtx context.Context, c authv1.AuthServiceClient, erasureID string) {
	t.Helper()
	waitFor(t, catchUpTimeout, "playback-monitor to acknowledge erasure "+erasureID, func() error {
		r, err := c.GetUserErasureStatus(adminCtx, &authv1.GetUserErasureStatusRequest{ErasureId: erasureID})
		if err != nil {
			return err
		}
		if len(r.GetErasures()) != 1 {
			return fmt.Errorf("got %d erasures", len(r.GetErasures()))
		}
		if st := r.GetErasures()[0]; !st.GetComplete() {
			var ms *authv1.ErasureModuleStatus
			for _, m := range st.GetModules() {
				if m.GetModuleId() == monitorModuleID {
					ms = m
				}
			}
			return fmt.Errorf("not complete yet: %v", ms)
		}
		return nil
	})
}

// --- helpers: playback-monitor --------------------------------------------

// startMonitor starts a playback-monitor over dbPath. Its mesh identity and
// core address come from the process environment set by the test, exactly as
// the module reads them in a deployment. Stopping it and starting another
// over the same database is a module restart.
func startMonitor(t *testing.T, ctx context.Context, dbPath string) *monitorinteg.Module {
	t.Helper()
	m := monitorinteg.NewTestModule(t, monitorinteg.Config{DBPath: dbPath})
	if err := monitorinteg.Start(ctx, m); err != nil {
		t.Fatalf("start playback-monitor: %v", err)
	}
	return m
}

// stopMonitor stops the module completely: its servers, its mesh connection
// and its erasure reconciler. Nothing of it keeps running.
func stopMonitor(t *testing.T, ctx context.Context, m *monitorinteg.Module) {
	t.Helper()
	if err := m.Stop(ctx); err != nil {
		t.Fatalf("stop playback-monitor: %v", err)
	}
}

func seedSession(t *testing.T, ctx context.Context, m *monitorinteg.Module, serverID, userID, userName string) {
	t.Helper()
	ev := monitorinteg.SessionEvent{
		ServerID: serverID, ExternalSessionID: "e11-" + userName, UserID: userID, UserName: userName,
		ItemID: "mv-" + userName, Title: "Movie for " + userName, Platform: "Web",
		IPAddress: "192.0.2.10", PositionSeconds: 60,
	}
	ev.EventType = "playback.started"
	if _, created, err := monitorinteg.IngestSession(ctx, m, ev); err != nil || !created {
		t.Fatalf("seed session for %s: created=%v err=%v", userName, created, err)
	}
	ev.EventType = "playback.stopped"
	if _, _, err := monitorinteg.IngestSession(ctx, m, ev); err != nil {
		t.Fatalf("stop seeded session for %s: %v", userName, err)
	}
}

// rowCounts is what playback-monitor stores about one (server, user) pair.
type rowCounts struct {
	Sessions    int
	ServerUsers int
	Identities  int
}

func (r rowCounts) total() int { return r.Sessions + r.ServerUsers + r.Identities }

// monitorRows reads playback-monitor's SQLite store directly: the sessions,
// the server_users link and the identity of (serverID, userID). The identity
// is followed through sessions.identity_id so that a link deleted without its
// identity is still found.
func monitorRows(t *testing.T, dbPath, serverID, userID string) rowCounts {
	t.Helper()
	db := openStore(t, dbPath)
	var r rowCounts
	scan := func(dst *int, query string, args ...any) {
		t.Helper()
		if err := db.QueryRow(query, args...).Scan(dst); err != nil {
			t.Fatalf("query %q: %v", query, err)
		}
	}
	scan(&r.Sessions, `SELECT COUNT(1) FROM sessions WHERE server_id = ? AND user_id = ?`, serverID, userID)
	scan(&r.ServerUsers, `SELECT COUNT(1) FROM server_users WHERE server_id = ? AND external_user_id = ?`, serverID, userID)
	scan(&r.Identities, `SELECT COUNT(1) FROM user_identities WHERE id IN (
			SELECT identity_id FROM server_users WHERE server_id = ? AND external_user_id = ?
			UNION
			SELECT identity_id FROM sessions WHERE server_id = ? AND user_id = ? AND identity_id != '')`,
		serverID, userID, serverID, userID)
	return r
}

func assertApplied(t *testing.T, dbPath, erasureID, userID string) {
	t.Helper()
	var got string
	err := openStore(t, dbPath).QueryRow(`SELECT user_id FROM erasure_applied WHERE erasure_id = ?`, erasureID).Scan(&got)
	if err != nil || got != userID {
		t.Fatalf("erasure_applied(%s) = %q, %v; want the erased user id", erasureID, got, err)
	}
}

func assertAppliedCount(t *testing.T, dbPath string, want int) {
	t.Helper()
	var n int
	if err := openStore(t, dbPath).QueryRow(`SELECT COUNT(1) FROM erasure_applied`).Scan(&n); err != nil || n != want {
		t.Fatalf("erasure_applied rows = %d, %v; want %d", n, err, want)
	}
}

// assertUntouched checks that every named bystander still has exactly the
// rows it had before any deletion.
func assertUntouched(t *testing.T, dbPath, when string, want map[string]rowCounts, who map[string][2]string) {
	t.Helper()
	for name, w := range want {
		if got := monitorRows(t, dbPath, who[name][0], who[name][1]); got != w {
			t.Fatalf("%s: rows of %s changed: %+v -> %+v", when, name, w, got)
		}
	}
}

func openStore(t *testing.T, dbPath string) *sql.DB {
	t.Helper()
	db, err := sql.Open("sqlite", "file:"+dbPath+"?_pragma=busy_timeout(10000)")
	if err != nil {
		t.Fatalf("open playback-monitor store: %v", err)
	}
	t.Cleanup(func() { _ = db.Close() })
	return db
}
