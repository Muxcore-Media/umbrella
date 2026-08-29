package playbackv1

import "testing"

func TestNormalizeStreamResolution(t *testing.T) {
	tests := []struct {
		height, width int
		label         string
		want          string
	}{
		{1080, 0, "", "1080"},
		{0, 0, "4k", "4k"},
		{0, 0, "1080p", "1080"},
		{720, 0, "", "720"},
		{0, 1920, "", "1080"},
		{4000, 0, "", "unknown"},
	}
	for _, tc := range tests {
		if got := NormalizeStreamResolution(tc.height, tc.width, tc.label); got != tc.want {
			t.Fatalf("NormalizeStreamResolution(%d,%d,%q)=%q want %q", tc.height, tc.width, tc.label, got, tc.want)
		}
	}
}
