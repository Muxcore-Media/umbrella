package internal

import (
	"context"
	"os"
	"path/filepath"
	"testing"
)

func TestModuleInfo(t *testing.T) {
	m := NewModule(Config{})
	info := m.Info()
	if info.ID != "media-dlna" {
		t.Fatalf("ID = %q", info.ID)
	}
	if info.Version == "" {
		t.Fatal("version required")
	}
	for _, cap := range []string{"media.dlna", "settings"} {
		found := false
		for _, got := range info.Capabilities {
			if got == cap {
				found = true
				break
			}
		}
		if !found {
			t.Fatalf("missing capability %q: %v", cap, info.Capabilities)
		}
	}
}

func TestResolveMediaPaths(t *testing.T) {
	dir := t.TempDir()
	child := filepath.Join(dir, "movies")
	if err := os.MkdirAll(child, 0755); err != nil {
		t.Fatal(err)
	}
	paths, err := resolveMediaPaths([]string{child})
	if err != nil {
		t.Fatal(err)
	}
	if len(paths) != 1 || paths[0] != child {
		t.Fatalf("paths = %v", paths)
	}
	root, err := browseRootForPaths(paths)
	if err != nil || root != child {
		t.Fatalf("root = %q err = %v", root, err)
	}
}

func TestBrowseRootCommonParent(t *testing.T) {
	base := t.TempDir()
	a := filepath.Join(base, "movies")
	b := filepath.Join(base, "tv")
	for _, p := range []string{a, b} {
		if err := os.MkdirAll(p, 0755); err != nil {
			t.Fatal(err)
		}
	}
	root, err := browseRootForPaths([]string{a, b})
	if err != nil {
		t.Fatal(err)
	}
	if root != base {
		t.Fatalf("root = %q want %q", root, base)
	}
}

func TestModuleLifecycle(t *testing.T) {
	root := t.TempDir()
	if err := os.WriteFile(filepath.Join(root, "sample.mkv"), []byte("demo"), 0644); err != nil {
		t.Fatal(err)
	}

	m := NewModule(Config{
		MediaPaths:     []string{root},
		BrowseRoot:     root,
		GRPCAddr:       "127.0.0.1:0",
		HealthHTTPAddr: "127.0.0.1:0",
		DlnaHTTPAddr:   "127.0.0.1:0",
		FriendlyName:   "Test DLNA",
		NoTranscode:    true,
		NoProbe:        true,
	})
	ctx := context.Background()
	if err := m.Init(ctx); err != nil {
		t.Fatalf("Init: %v", err)
	}
	if err := m.Start(ctx); err != nil {
		t.Fatalf("Start: %v", err)
	}
	if err := m.Health(ctx); err != nil {
		t.Fatalf("Health: %v", err)
	}
	status, err := m.Status(ctx, nil)
	if err != nil {
		t.Fatalf("Status: %v", err)
	}
	if !status.GetConfigured() || !status.GetRunning() || status.GetBrowseRoot() != root {
		t.Fatalf("status: %+v", status)
	}
	if err := m.Stop(ctx); err != nil {
		t.Fatalf("Stop: %v", err)
	}
}

func TestSettingsUpdateMediaPaths(t *testing.T) {
	root := t.TempDir()
	m := NewModule(Config{MediaPaths: []string{root}, BrowseRoot: root})
	if err := m.UpdateSetting("media_paths", root); err != nil {
		t.Fatal(err)
	}
	defs := m.Settings()
	if len(defs) == 0 {
		t.Fatal("expected settings")
	}
}
