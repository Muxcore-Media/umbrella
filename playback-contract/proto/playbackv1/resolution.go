package playbackv1

import (
	"strconv"
	"strings"
)

// NormalizeStreamResolution maps height or a source label to Tautulli-style buckets.
func NormalizeStreamResolution(height, width int, label string) string {
	if s := normalizeResolutionLabel(label); s != "" {
		return s
	}
	h := height
	if h <= 0 && width > 0 {
		h = width * 9 / 16
	}
	if h <= 0 {
		return ""
	}
	switch {
	case h <= 360:
		return "SD"
	case h <= 480:
		return "480"
	case h <= 576:
		return "576"
	case h <= 720:
		return "720"
	case h <= 1080:
		return "1080"
	case h <= 1440:
		return "QHD"
	case h <= 2160:
		return "4k"
	default:
		return "unknown"
	}
}

func normalizeResolutionLabel(label string) string {
	s := strings.ToLower(strings.TrimSpace(label))
	s = strings.TrimSuffix(s, "p")
	s = strings.TrimSuffix(s, "i")
	switch s {
	case "", "unknown", "na", "n/a":
		return ""
	case "sd", "360", "240":
		return "SD"
	case "480", "576":
		return s
	case "720", "hd":
		return "720"
	case "1080", "full hd", "fhd":
		return "1080"
	case "1440", "qhd", "2k":
		return "QHD"
	case "2160", "4k", "uhd", "ultra hd":
		return "4k"
	}
	if n, err := strconv.Atoi(s); err == nil && n > 0 {
		return NormalizeStreamResolution(n, 0, "")
	}
	return label
}
