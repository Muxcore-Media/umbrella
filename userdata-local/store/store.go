// Package store provides a durable, scoped userdata blob store for the
// household loop (progress / favorites / prefs / playlists / queue).
//
// When TENANT_MODE=1, blobs live under tenants/{id}/{user}.json (per-tenant
// data subdirs). Otherwise files are {user}.json under the store root.
// Merge is entry-level for progress/favorites (newer updatedAt wins) so
// multi-device clients can PUT without clobbering fresher server rows.
package store

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"sync"
	"time"
)

// Blob is the wire/file shape shared by media-ui BFF and userdata-local.
type Blob struct {
	Progress  map[string]json.RawMessage `json:"progress"`
	Favorites map[string]json.RawMessage `json:"favorites"`
	Prefs     json.RawMessage            `json:"prefs,omitempty"`
	Playlists json.RawMessage            `json:"playlists,omitempty"`
	Queue     json.RawMessage            `json:"queue,omitempty"`
	UpdatedAt string                     `json:"updated_at"`
	UserID    string                     `json:"user_id,omitempty"`
	TenantID  string                     `json:"tenant_id,omitempty"`
}

// Scope identifies a userdata partition.
type Scope struct {
	TenantID string
	UserID   string
}

// Store is a file-backed userdata map keyed by ScopeKey.
type Store struct {
	mu  sync.Mutex
	dir string
}

var safeID = regexp.MustCompile(`[^a-zA-Z0-9._-]+`)

// New creates a store rooted at dir (created with 0700 if missing).
func New(dir string) (*Store, error) {
	if dir == "" {
		dir = filepath.Join(os.TempDir(), "muxcore-userdata-local")
	}
	if err := os.MkdirAll(dir, 0o700); err != nil {
		return nil, err
	}
	return &Store{dir: dir}, nil
}

// Dir returns the root directory.
func (s *Store) Dir() string { return s.dir }

// ScopeKey builds a logical partition key (tests / diagnostics).
func ScopeKey(tenantID, userID string) string {
	userID = strings.TrimSpace(userID)
	if userID == "" {
		userID = "anonymous"
	}
	userID = safeID.ReplaceAllString(userID, "_")
	tenantID = strings.TrimSpace(tenantID)
	if tenantID == "" {
		return userID
	}
	tenantID = safeID.ReplaceAllString(tenantID, "_")
	return tenantID + "__" + userID
}

// PartitionDir returns the on-disk directory for a tenant scope.
// With tenant: {root}/tenants/{id}; without: {root}.
func PartitionDir(root string, scope Scope) string {
	tenantID := strings.TrimSpace(scope.TenantID)
	if tenantID == "" {
		return root
	}
	tenantID = safeID.ReplaceAllString(tenantID, "_")
	if tenantID == "" || tenantID == "." || tenantID == ".." {
		tenantID = "default"
	}
	return filepath.Join(root, "tenants", tenantID)
}

func (s *Store) pathFor(scope Scope) string {
	userID := strings.TrimSpace(scope.UserID)
	if userID == "" {
		userID = "anonymous"
	}
	userID = safeID.ReplaceAllString(userID, "_")
	dir := PartitionDir(s.dir, scope)
	return filepath.Join(dir, userID+".json")
}

// Get loads the blob for scope (empty maps if missing).
func (s *Store) Get(scope Scope) Blob {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.loadLocked(scope)
}

func (s *Store) loadLocked(scope Scope) Blob {
	raw, err := os.ReadFile(s.pathFor(scope))
	if err != nil {
		return emptyBlob(scope)
	}
	var blob Blob
	if json.Unmarshal(raw, &blob) != nil {
		return emptyBlob(scope)
	}
	normalize(&blob, scope)
	return blob
}

// Put replaces the blob after merging with any existing server state.
func (s *Store) Put(scope Scope, incoming Blob) (Blob, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	base := s.loadLocked(scope)
	merged := Merge(base, incoming)
	normalize(&merged, scope)
	merged.UpdatedAt = time.Now().UTC().Format(time.RFC3339)
	if err := s.saveLocked(scope, merged); err != nil {
		return Blob{}, err
	}
	return merged, nil
}

// PutReplace writes without merge (tests / admin repair).
func (s *Store) PutReplace(scope Scope, blob Blob) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	normalize(&blob, scope)
	blob.UpdatedAt = time.Now().UTC().Format(time.RFC3339)
	return s.saveLocked(scope, blob)
}

func (s *Store) saveLocked(scope Scope, blob Blob) error {
	raw, err := json.MarshalIndent(blob, "", "  ")
	if err != nil {
		return err
	}
	path := s.pathFor(scope)
	if err := os.MkdirAll(filepath.Dir(path), 0o700); err != nil {
		return err
	}
	tmp := path + ".tmp"
	if err := os.WriteFile(tmp, raw, 0o600); err != nil {
		return err
	}
	return os.Rename(tmp, path)
}

func emptyBlob(scope Scope) Blob {
	b := Blob{
		Progress:  map[string]json.RawMessage{},
		Favorites: map[string]json.RawMessage{},
	}
	normalize(&b, scope)
	return b
}

func normalize(b *Blob, scope Scope) {
	if b.Progress == nil {
		b.Progress = map[string]json.RawMessage{}
	}
	if b.Favorites == nil {
		b.Favorites = map[string]json.RawMessage{}
	}
	if scope.UserID != "" {
		b.UserID = scope.UserID
	}
	if scope.TenantID != "" {
		b.TenantID = scope.TenantID
	}
}

// Merge combines base (server) and incoming (client). Newer progress/favorite
// entries win by updatedAt; prefs/playlists/queue take incoming when present.
func Merge(base, incoming Blob) Blob {
	out := Blob{
		Progress:  map[string]json.RawMessage{},
		Favorites: map[string]json.RawMessage{},
		Prefs:     base.Prefs,
		Playlists: base.Playlists,
		Queue:     base.Queue,
		UserID:    firstNonEmpty(incoming.UserID, base.UserID),
		TenantID:  firstNonEmpty(incoming.TenantID, base.TenantID),
	}
	for k, v := range base.Progress {
		out.Progress[k] = v
	}
	for k, v := range incoming.Progress {
		if cur, ok := out.Progress[k]; !ok || entryNewer(v, cur) {
			out.Progress[k] = v
		}
	}
	for k, v := range base.Favorites {
		out.Favorites[k] = v
	}
	for k, v := range incoming.Favorites {
		out.Favorites[k] = v
	}
	if len(incoming.Prefs) > 0 && string(incoming.Prefs) != "null" {
		out.Prefs = incoming.Prefs
	}
	if len(incoming.Playlists) > 0 && string(incoming.Playlists) != "null" {
		out.Playlists = incoming.Playlists
	}
	if len(incoming.Queue) > 0 && string(incoming.Queue) != "null" {
		out.Queue = incoming.Queue
	}
	return out
}

type stamped struct {
	UpdatedAt string `json:"updatedAt"`
}

func entryNewer(a, b json.RawMessage) bool {
	var sa, sb stamped
	_ = json.Unmarshal(a, &sa)
	_ = json.Unmarshal(b, &sb)
	if sa.UpdatedAt == "" {
		return false
	}
	if sb.UpdatedAt == "" {
		return true
	}
	return sa.UpdatedAt > sb.UpdatedAt
}

func firstNonEmpty(vals ...string) string {
	for _, v := range vals {
		if strings.TrimSpace(v) != "" {
			return v
		}
	}
	return ""
}

// EnsureDistinctScopes returns an error if two scopes collide on disk key.
func EnsureDistinctScopes(a, b Scope) error {
	if ScopeKey(a.TenantID, a.UserID) == ScopeKey(b.TenantID, b.UserID) &&
		(a.TenantID != b.TenantID || a.UserID != b.UserID) {
		return fmt.Errorf("scope key collision")
	}
	return nil
}
