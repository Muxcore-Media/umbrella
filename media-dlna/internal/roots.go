package internal

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

func resolveMediaPaths(raw []string) ([]string, error) {
	if len(raw) == 0 {
		if v := strings.TrimSpace(os.Getenv("DLNA_MEDIA_PATHS")); v != "" {
			raw = splitPaths(v)
		} else if v := strings.TrimSpace(os.Getenv("DLNA_MEDIA_PATH")); v != "" {
			raw = []string{v}
		} else if v := strings.TrimSpace(os.Getenv("SCANNER_LIBRARY_ROOT")); v != "" {
			raw = []string{v}
		} else if v := strings.TrimSpace(os.Getenv("MVP_LIBRARY_ROOT")); v != "" {
			raw = []string{v}
		}
	}
	if len(raw) == 0 {
		return nil, fmt.Errorf("no media paths configured (set DLNA_MEDIA_PATH or DLNA_MEDIA_PATHS)")
	}

	seen := make(map[string]struct{}, len(raw))
	out := make([]string, 0, len(raw))
	for _, p := range raw {
		p = strings.TrimSpace(p)
		if p == "" {
			continue
		}
		abs, err := filepath.Abs(p)
		if err != nil {
			return nil, fmt.Errorf("resolve path %q: %w", p, err)
		}
		abs = filepath.Clean(abs)
		if _, ok := seen[abs]; ok {
			continue
		}
		info, err := os.Stat(abs)
		if err != nil {
			return nil, fmt.Errorf("media path %q: %w", abs, err)
		}
		if !info.IsDir() {
			return nil, fmt.Errorf("media path %q is not a directory", abs)
		}
		seen[abs] = struct{}{}
		out = append(out, abs)
	}
	if len(out) == 0 {
		return nil, fmt.Errorf("no valid media paths configured")
	}
	return out, nil
}

func browseRootForPaths(paths []string) (string, error) {
	if len(paths) == 1 {
		return paths[0], nil
	}
	parent, err := commonParent(paths)
	if err != nil {
		return "", err
	}
	if parent != "" {
		if info, err := os.Stat(parent); err == nil && info.IsDir() {
			return parent, nil
		}
	}
	return "", fmt.Errorf("multiple media paths without a shared parent; set DLNA_MEDIA_PATH to one directory")
}

func commonParent(paths []string) (string, error) {
	if len(paths) == 0 {
		return "", fmt.Errorf("no paths")
	}
	parent := filepath.Clean(paths[0])
	for _, p := range paths[1:] {
		p = filepath.Clean(p)
		for {
			if strings.HasPrefix(p, parent+string(os.PathSeparator)) || p == parent {
				break
			}
			next := filepath.Dir(parent)
			if next == parent {
				return "", fmt.Errorf("paths share no common parent: %v", paths)
			}
			parent = next
		}
	}
	return parent, nil
}

func splitPaths(s string) []string {
	parts := strings.Split(s, ",")
	out := make([]string, 0, len(parts))
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}
