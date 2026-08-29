package internal

import (
	"path/filepath"
	"testing"
)

func TestSplitPaths(t *testing.T) {
	got := splitPaths(" /a , /b , ")
	if len(got) != 2 || got[0] != "/a" || got[1] != "/b" {
		t.Fatalf("splitPaths = %#v", got)
	}
}

func TestResolveMediaPathsMissing(t *testing.T) {
	t.Setenv("DLNA_MEDIA_PATH", "")
	t.Setenv("DLNA_MEDIA_PATHS", "")
	t.Setenv("SCANNER_LIBRARY_ROOT", "")
	t.Setenv("MVP_LIBRARY_ROOT", "")
	if _, err := resolveMediaPaths(nil); err == nil {
		t.Fatal("expected error")
	}
}

func TestResolveMediaPathsFromEnv(t *testing.T) {
	dir := t.TempDir()
	t.Setenv("DLNA_MEDIA_PATH", dir)
	paths, err := resolveMediaPaths(nil)
	if err != nil {
		t.Fatal(err)
	}
	if len(paths) != 1 {
		t.Fatalf("paths = %v", paths)
	}
	if filepath.Clean(paths[0]) != filepath.Clean(dir) {
		t.Fatalf("path = %q", paths[0])
	}
}
