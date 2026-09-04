//go:build integration

package integration

import (
	"context"
	"os"
	"testing"
	"time"

	coreinteg "github.com/Muxcore-Media/core/integsupport"
	moviesinteg "github.com/Muxcore-Media/media-movies/integsupport"
	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
	transcoderinteg "github.com/Muxcore-Media/media-transcoder/integsupport"
)

// Stack wires the media-movies → media-transcoder → playback-monitor pipeline with core mesh.
type Stack struct {
	Core       *coreinteg.CoreHarness
	Movies     *moviesinteg.Module
	Transcoder *transcoderinteg.Module
	Monitor    *monitorinteg.Module
}

// NewStack starts core and all three modules on isolated in-memory SQLite databases.
func NewStack(t *testing.T) *Stack {
	t.Helper()

	core := coreinteg.NewCoreHarness(t)
	t.Setenv("MUXCORE_GRPC_ADDR", core.Addr)
	t.Setenv("MUXCORE_INSECURE_DISABLE_TLS", "true")

	movies := moviesinteg.NewTestModule(t, moviesinteg.Config{})
	transcoder := transcoderinteg.NewTestModule(t, transcoderinteg.Config{})
	monitor := monitorinteg.NewTestModule(t, monitorinteg.Config{})

	ctx := context.Background()
	t.Setenv("MUXCORE_MODULE_ID", "media-movies")
	if err := moviesinteg.Start(ctx, movies); err != nil {
		t.Fatalf("start media-movies: %v", err)
	}
	t.Setenv("MUXCORE_MODULE_ID", "media-transcoder")
	if err := transcoderinteg.Start(ctx, transcoder); err != nil {
		t.Fatalf("start media-transcoder: %v", err)
	}
	t.Setenv("MUXCORE_MODULE_ID", "playback-monitor")
	if err := monitorinteg.Start(ctx, monitor); err != nil {
		t.Fatalf("start playback-monitor: %v", err)
	}

	// Allow async core dial + event subscriptions to settle.
	deadline := time.Now().Add(2 * time.Second)
	for time.Now().Before(deadline) {
		if os.Getenv("MUXCORE_GRPC_ADDR") != "" {
			break
		}
		time.Sleep(10 * time.Millisecond)
	}
	time.Sleep(200 * time.Millisecond)

	return &Stack{
		Core:       core,
		Movies:     movies,
		Transcoder: transcoder,
		Monitor:    monitor,
	}
}
