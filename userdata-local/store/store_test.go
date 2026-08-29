package store

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
)

func TestScopeKeyTenantIsolation(t *testing.T) {
	a := ScopeKey("tenant-a", "alice")
	b := ScopeKey("tenant-b", "alice")
	if a == b {
		t.Fatalf("expected distinct keys for different tenants, got %q", a)
	}
	c := ScopeKey("", "alice")
	if c != "alice" {
		t.Fatalf("empty tenant should be user-only, got %q", c)
	}
}

func TestMergeProgressPrefersNewer(t *testing.T) {
	base := Blob{
		Progress: map[string]json.RawMessage{
			"m1": json.RawMessage(`{"id":"m1","positionSec":10,"updatedAt":"2026-01-01T00:00:00Z"}`),
		},
		Favorites: map[string]json.RawMessage{},
	}
	incoming := Blob{
		Progress: map[string]json.RawMessage{
			"m1": json.RawMessage(`{"id":"m1","positionSec":40,"updatedAt":"2026-01-02T00:00:00Z"}`),
			"m2": json.RawMessage(`{"id":"m2","positionSec":5,"updatedAt":"2026-01-02T00:00:00Z"}`),
		},
		Favorites: map[string]json.RawMessage{
			"m1": json.RawMessage(`{"id":"m1","title":"One"}`),
		},
		Prefs: json.RawMessage(`{"display":{"theme":"light"}}`),
	}
	out := Merge(base, incoming)
	var p struct {
		PositionSec int `json:"positionSec"`
	}
	if err := json.Unmarshal(out.Progress["m1"], &p); err != nil {
		t.Fatal(err)
	}
	if p.PositionSec != 40 {
		t.Fatalf("want newer progress 40, got %d", p.PositionSec)
	}
	if _, ok := out.Progress["m2"]; !ok {
		t.Fatal("expected m2 from incoming")
	}
	if len(out.Favorites) != 1 {
		t.Fatalf("favorites=%d", len(out.Favorites))
	}
	if string(out.Prefs) != `{"display":{"theme":"light"}}` {
		t.Fatalf("prefs=%s", out.Prefs)
	}
}

func TestMergeKeepsFresherServerProgress(t *testing.T) {
	base := Blob{
		Progress: map[string]json.RawMessage{
			"m1": json.RawMessage(`{"id":"m1","positionSec":90,"updatedAt":"2026-06-01T00:00:00Z"}`),
		},
		Favorites: map[string]json.RawMessage{},
	}
	incoming := Blob{
		Progress: map[string]json.RawMessage{
			"m1": json.RawMessage(`{"id":"m1","positionSec":10,"updatedAt":"2026-01-01T00:00:00Z"}`),
		},
		Favorites: map[string]json.RawMessage{},
	}
	out := Merge(base, incoming)
	var p struct {
		PositionSec int `json:"positionSec"`
	}
	_ = json.Unmarshal(out.Progress["m1"], &p)
	if p.PositionSec != 90 {
		t.Fatalf("server fresher should win, got %d", p.PositionSec)
	}
}

func TestStoreRoundTripScoped(t *testing.T) {
	dir := t.TempDir()
	st, err := New(dir)
	if err != nil {
		t.Fatal(err)
	}
	s1 := Scope{TenantID: "t1", UserID: "u1"}
	s2 := Scope{TenantID: "t2", UserID: "u1"}
	_, err = st.Put(s1, Blob{
		Progress: map[string]json.RawMessage{
			"a": json.RawMessage(`{"id":"a","updatedAt":"2026-01-01T00:00:00Z"}`),
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	_, err = st.Put(s2, Blob{
		Progress: map[string]json.RawMessage{
			"b": json.RawMessage(`{"id":"b","updatedAt":"2026-01-01T00:00:00Z"}`),
		},
	})
	if err != nil {
		t.Fatal(err)
	}
	g1 := st.Get(s1)
	g2 := st.Get(s2)
	if len(g1.Progress) != 1 || g1.Progress["a"] == nil {
		t.Fatalf("tenant1 blob=%v", g1.Progress)
	}
	if len(g2.Progress) != 1 || g2.Progress["b"] == nil {
		t.Fatalf("tenant2 blob=%v", g2.Progress)
	}
	if g1.TenantID != "t1" || g1.UserID != "u1" {
		t.Fatalf("scope fields: %+v", g1)
	}
	dirA := PartitionDir(dir, s1)
	dirB := PartitionDir(dir, s2)
	if dirA == dirB {
		t.Fatalf("expected distinct partition dirs")
	}
	if _, err := os.Stat(filepath.Join(dirA, "u1.json")); err != nil {
		t.Fatalf("tenant-a file: %v", err)
	}
	if _, err := os.Stat(filepath.Join(dirB, "u1.json")); err != nil {
		t.Fatalf("tenant-b file: %v", err)
	}
	// Cross-tenant Get is empty (different partition).
	cross := st.Get(Scope{TenantID: "t2", UserID: "u1"})
	if cross.Progress["a"] != nil {
		t.Fatal("cross-tenant must not see other tenant progress via wrong expectation — t2 has b only")
	}
	if len(cross.Progress) != 1 || cross.Progress["b"] == nil {
		t.Fatalf("t2 should only have b: %v", cross.Progress)
	}
	empty := st.Get(Scope{TenantID: "t1", UserID: "nobody"})
	if len(empty.Progress) != 0 {
		t.Fatalf("other user empty: %v", empty.Progress)
	}
}
