package internal

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"

	"github.com/Muxcore-Media/userdata-local/store"
)

func TestHTTPUserdataScoped(t *testing.T) {
	t.Setenv("TENANT_MODE", "1")
	dir := t.TempDir()
	st, err := store.New(dir)
	if err != nil {
		t.Fatal(err)
	}
	m := &Module{store: st, dataDir: dir}

	put := httptest.NewRequest(http.MethodPut, "/userdata?user_id=alice&tenant_id=t1",
		strings.NewReader(`{"progress":{"x":{"id":"x","updatedAt":"2026-01-02T00:00:00Z","positionSec":3}}}`))
	pw := httptest.NewRecorder()
	m.handlePut(pw, put)
	if pw.Code != http.StatusOK {
		t.Fatalf("put status %d body %s", pw.Code, pw.Body.String())
	}

	getOther := httptest.NewRequest(http.MethodGet, "/userdata?user_id=alice&tenant_id=t2", nil)
	gw := httptest.NewRecorder()
	m.handleGet(gw, getOther)
	var blob store.Blob
	if err := json.NewDecoder(gw.Body).Decode(&blob); err != nil {
		t.Fatal(err)
	}
	if len(blob.Progress) != 0 {
		t.Fatalf("tenant isolation failed: %#v", blob.Progress)
	}

	getSame := httptest.NewRequest(http.MethodGet, "/userdata?user_id=alice&tenant_id=t1", nil)
	gw2 := httptest.NewRecorder()
	m.handleGet(gw2, getSame)
	if err := json.NewDecoder(gw2.Body).Decode(&blob); err != nil {
		t.Fatal(err)
	}
	if len(blob.Progress) != 1 {
		t.Fatalf("expected progress for t1, got %#v", blob)
	}
}

func TestTenantModeOffIgnoresTenantHeader(t *testing.T) {
	_ = os.Unsetenv("TENANT_MODE")
	dir := t.TempDir()
	st, err := store.New(dir)
	if err != nil {
		t.Fatal(err)
	}
	m := &Module{store: st}
	r := httptest.NewRequest(http.MethodGet, "/userdata?user_id=bob", nil)
	r.Header.Set("X-Tenant-ID", "should-ignore")
	scope := m.scopeFrom(r)
	if scope.TenantID != "" {
		t.Fatalf("expected empty tenant when TENANT_MODE off, got %q", scope.TenantID)
	}
	if scope.UserID != "bob" {
		t.Fatalf("user=%q", scope.UserID)
	}
}

func TestTenantModeDefaultsTenant(t *testing.T) {
	t.Setenv("TENANT_MODE", "1")
	dir := t.TempDir()
	st, err := store.New(dir)
	if err != nil {
		t.Fatal(err)
	}
	m := &Module{store: st}
	r := httptest.NewRequest(http.MethodGet, "/userdata?user_id=bob", nil)
	scope := m.scopeFrom(r)
	if scope.TenantID != "default" {
		t.Fatalf("expected default tenant, got %q", scope.TenantID)
	}
}
