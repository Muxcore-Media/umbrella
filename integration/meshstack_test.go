//go:build integration

package integration

import (
	"bytes"
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"syscall"
	"testing"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	authv1 "github.com/Muxcore-Media/core/proto/gen/muxcore/auth/v1"
	"github.com/Muxcore-Media/core/sdk/go/module/meshid"
)

// This file starts the identity side of an ADR-0035 test the way a household
// deployment does: a real muxcored and a real auth-local, both built from the
// published module versions pinned in go.mod, on the household security
// profile. There is no plaintext listener and no MUXCORE_INSECURE_DISABLE_TLS
// anywhere: every module reaches core through an enrolled certificate whose CN
// is the module id (ADR-0017), and auth-local identifies erasure-ledger
// callers by that verified CN only (ADR-0035 section 2).
//
// core's integsupport harness cannot be used for this: it serves plaintext
// and trusts x-caller-id, which the ledger RPCs must not accept.

const (
	meshEnrollSecret = "integration-enroll-secret-0123456789"

	// meshAdminUser/meshAdminPassword is the bootstrap admin auth-local
	// creates from AUTH_BOOTSTRAP_USER/AUTH_BOOTSTRAP_PASSWORD.
	meshAdminUser     = "e11-admin"
	meshAdminPassword = "e11-admin-Passw0rd-long-enough"

	authModuleID = "auth-local"
)

// meshStack is a running muxcored + auth-local pair.
type meshStack struct {
	root string
	// CoreAddr is core's gRPC address (mesh TLS, household profile).
	CoreAddr string
	// AuthAddr is auth-local's gRPC address, as advertised through discovery.
	AuthAddr string
	// CAFile is the exported core CA certificate.
	CAFile string
	// CADir is core's CA directory (holds the enrollment ledger).
	CADir string

	coreBin string
}

// newMeshStack builds and starts core and auth-local. consumers and required
// become AUTH_ERASURE_CONSUMERS and AUTH_ERASURE_REQUIRED (ADR-0035 section 2).
func newMeshStack(t *testing.T, consumers, required []string) *meshStack {
	t.Helper()
	root := t.TempDir()
	binDir := filepath.Join(root, "bin")
	if err := os.MkdirAll(binDir, 0o700); err != nil {
		t.Fatal(err)
	}
	coreBin := goBuild(t, binDir, "github.com/Muxcore-Media/core/cmd/muxcored", "muxcored")
	authBin := goBuild(t, binDir, "github.com/Muxcore-Media/auth-local/cmd/module", "auth-local")

	s := &meshStack{
		root:     root,
		CoreAddr: loopbackAddr(t),
		AuthAddr: loopbackAddr(t),
		CAFile:   filepath.Join(root, "export", "ca.crt"),
		CADir:    filepath.Join(root, "ca"),
		coreBin:  coreBin,
	}
	coreHTTP := loopbackAddr(t)
	authHTTP := loopbackAddr(t)

	cfg, err := json.Marshal(map[string]any{
		"server": map[string]any{"addr": coreHTTP},
		"grpc":   map[string]any{"addr": s.CoreAddr, "ca_cert_dir": s.CADir},
		"log":    map[string]any{"level": "info", "format": "text"},
	})
	if err != nil {
		t.Fatal(err)
	}
	cfgPath := filepath.Join(root, "muxcore.json")
	if err := os.WriteFile(cfgPath, cfg, 0o600); err != nil {
		t.Fatal(err)
	}

	startProc(t, "muxcored", coreBin, nil, root, filepath.Join(root, "muxcored.log"), []string{
		"MUXCORE_PROFILE=household",
		"MUXCORE_CONFIG=" + cfgPath,
		"MUXCORE_DATA_DIR=" + filepath.Join(root, "core-data"),
		"MUXCORE_CA_EXPORT_DIR=" + filepath.Dir(s.CAFile),
		"MUXCORE_ENROLL_SECRET=" + meshEnrollSecret,
	})
	waitFor(t, 30*time.Second, "core to export its CA and listen", func() error {
		if _, err := os.Stat(s.CAFile); err != nil {
			return err
		}
		c, err := net.DialTimeout("tcp", s.CoreAddr, time.Second)
		if err != nil {
			return err
		}
		return c.Close()
	})

	authDir := filepath.Join(root, "auth-local")
	startProc(t, authModuleID, authBin, nil, authDir, filepath.Join(root, "auth-local.log"), []string{
		"MUXCORE_PROFILE=household",
		"MUXCORE_GRPC_ADDR=" + s.CoreAddr,
		"MUXCORE_MODULE_ID=" + authModuleID,
		"MUXCORE_TLS_CA=" + s.CAFile,
		"MUXCORE_TLS_DIR=" + filepath.Join(authDir, "mesh-id"),
		"MUXCORE_BOOTSTRAP_TOKEN=" + s.EnrollToken(t, authModuleID),
		"AUTH_DB_PATH=" + filepath.Join(authDir, "auth.db"),
		"AUTH_GRPC_ADDR=" + s.AuthAddr,
		"AUTH_HTTP_ADDR=" + authHTTP,
		"AUTH_BOOTSTRAP_USER=" + meshAdminUser,
		"AUTH_BOOTSTRAP_PASSWORD=" + meshAdminPassword,
		"AUTH_ERASURE_CONSUMERS=" + strings.Join(consumers, ","),
		"AUTH_ERASURE_REQUIRED=" + strings.Join(required, ","),
	})
	return s
}

// EnrollToken returns the single-use enrollment token for moduleID, using
// muxcored's own `enroll token` command.
func (s *meshStack) EnrollToken(t *testing.T, moduleID string) string {
	t.Helper()
	cmd := exec.Command(s.coreBin, "enroll", "token", moduleID, "--ca-dir", s.CADir) //nolint:gosec // test binary built from the pinned core module
	cmd.Env = []string{"MUXCORE_ENROLL_SECRET=" + meshEnrollSecret, "PATH=" + os.Getenv("PATH")}
	var stdout, stderr bytes.Buffer
	cmd.Stdout, cmd.Stderr = &stdout, &stderr
	if err := cmd.Run(); err != nil {
		t.Fatalf("muxcored enroll token %s: %v: %s", moduleID, err, stderr.String())
	}
	tok := strings.TrimSpace(stdout.String())
	if tok == "" {
		t.Fatalf("muxcored enroll token %s: empty token", moduleID)
	}
	return tok
}

// Enroll enrolls moduleID with core in this process and returns its mesh
// identity files (certificate CN = moduleID, signed by the core CA).
func (s *meshStack) Enroll(t *testing.T, moduleID string) meshid.Paths {
	t.Helper()
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	noEnv := func(string) string { return "" }
	p, err := meshid.Ensure(ctx, meshid.Config{
		Getenv:   noEnv,
		Setenv:   func(string, string) error { return nil },
		ModuleID: moduleID,
		GRPCAddr: s.CoreAddr,
		CAFile:   s.CAFile,
		Dir:      filepath.Join(s.root, "identities", moduleID),
		Token:    s.EnrollToken(t, moduleID),
		Timeout:  30 * time.Second,
	})
	if err != nil {
		t.Fatalf("enroll %s: %v", moduleID, err)
	}
	if !p.Enrolled || p.Cert == "" || p.Key == "" || p.CA == "" {
		t.Fatalf("enroll %s: got %+v, want a freshly enrolled identity", moduleID, p)
	}
	return p
}

// AuthClient dials auth-local over TLS. The server certificate must chain to
// the core CA and carry CN auth-local. When certFile is non-empty the client
// presents that certificate; otherwise it presents none (an end-user caller).
func (s *meshStack) AuthClient(t *testing.T, certFile, keyFile string) authv1.AuthServiceClient {
	t.Helper()
	caPEM, err := os.ReadFile(s.CAFile)
	if err != nil {
		t.Fatal(err)
	}
	roots := x509.NewCertPool()
	if !roots.AppendCertsFromPEM(caPEM) {
		t.Fatal("no certificates in the exported core CA")
	}
	cfg := &tls.Config{
		MinVersion: tls.VersionTLS12,
		RootCAs:    roots,
		ServerName: authModuleID,
		VerifyConnection: func(cs tls.ConnectionState) error {
			if len(cs.VerifiedChains) == 0 || cs.VerifiedChains[0][0].Subject.CommonName != authModuleID {
				return fmt.Errorf("server certificate CN is not %q", authModuleID)
			}
			return nil
		},
	}
	if certFile != "" {
		pair, err := tls.LoadX509KeyPair(certFile, keyFile)
		if err != nil {
			t.Fatal(err)
		}
		cfg.Certificates = []tls.Certificate{pair}
	}
	conn, err := grpc.NewClient(s.AuthAddr, grpc.WithTransportCredentials(credentials.NewTLS(cfg)))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = conn.Close() })
	return authv1.NewAuthServiceClient(conn)
}

// goBuild builds pkg (a main package in the integration module's build list,
// i.e. a pinned published version) into dir/name and returns its path.
func goBuild(t *testing.T, dir, pkg, name string) string {
	t.Helper()
	out := filepath.Join(dir, name)
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Minute)
	defer cancel()
	cmd := exec.CommandContext(ctx, "go", "build", "-o", out, pkg) //nolint:gosec // fixed package paths pinned in go.mod
	var combined bytes.Buffer
	cmd.Stdout, cmd.Stderr = &combined, &combined
	if err := cmd.Run(); err != nil {
		t.Fatalf("go build %s: %v\n%s", pkg, err, combined.String())
	}
	return out
}

// loopbackAddr reserves a free 127.0.0.1 port. The port is released before it
// is handed to the process that will listen on it.
func loopbackAddr(t *testing.T) string {
	t.Helper()
	var lc net.ListenConfig
	lis, err := lc.Listen(context.Background(), "tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("reserve loopback port: %v", err)
	}
	addr := lis.Addr().String()
	if err := lis.Close(); err != nil {
		t.Fatal(err)
	}
	return addr
}

// startProc runs bin with exactly env (plus PATH and HOME), so nothing from
// the test process, notably an insecure-TLS flag, leaks into the child. It is
// stopped by t.Cleanup; its log is replayed when the test failed.
func startProc(t *testing.T, name, bin string, args []string, dir, logPath string, env []string) {
	t.Helper()
	if err := os.MkdirAll(dir, 0o700); err != nil {
		t.Fatal(err)
	}
	logf, err := os.Create(logPath) //nolint:gosec // test-owned temp path
	if err != nil {
		t.Fatal(err)
	}
	cmd := exec.Command(bin, args...) //nolint:gosec // test binary built from a pinned module
	cmd.Dir = dir
	cmd.Env = append([]string{"PATH=" + os.Getenv("PATH"), "HOME=" + dir}, env...)
	cmd.Stdout, cmd.Stderr = logf, logf
	if err := cmd.Start(); err != nil {
		_ = logf.Close()
		t.Fatalf("start %s: %v", name, err)
	}
	done := make(chan struct{})
	go func() { _ = cmd.Wait(); close(done) }()
	t.Cleanup(func() {
		_ = cmd.Process.Signal(syscall.SIGTERM)
		select {
		case <-done:
		case <-time.After(10 * time.Second):
			_ = cmd.Process.Kill()
			<-done
		}
		_ = logf.Close()
		if t.Failed() {
			if b, err := os.ReadFile(logPath); err == nil { //nolint:gosec // test-owned temp path
				t.Logf("--- %s log (tail) ---\n%s", name, tail(string(b), 60))
			}
		}
	})
}

func tail(s string, lines int) string {
	parts := strings.Split(strings.TrimRight(s, "\n"), "\n")
	if len(parts) > lines {
		parts = parts[len(parts)-lines:]
	}
	return strings.Join(parts, "\n")
}

// waitFor polls fn until it returns nil or timeout elapses.
func waitFor(t *testing.T, timeout time.Duration, what string, fn func() error) {
	t.Helper()
	deadline := time.Now().Add(timeout)
	var last error
	for {
		if last = fn(); last == nil {
			return
		}
		if time.Now().After(deadline) {
			t.Fatalf("timed out after %s waiting for %s: %v", timeout, what, last)
		}
		time.Sleep(200 * time.Millisecond)
	}
}
