//go:build integration

package integration

import (
	"context"
	"os"
	"path/filepath"
	"testing"
	"time"

	coreinteg "github.com/Muxcore-Media/core/integsupport"
	moviesinteg "github.com/Muxcore-Media/media-movies/integsupport"
	transcoderinteg "github.com/Muxcore-Media/media-transcoder/integsupport"
	transcodev1 "github.com/Muxcore-Media/media-transcoder/proto/transcodev1"
	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
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
	transcoder := transcoderinteg.NewTestModule(t, transcoderConfig(t))
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

// transcoderConfig returns a hermetic media-transcoder config. Health probes the
// ffmpeg binary on PATH, so point it at a stub: the tests never transcode real media
// and must not depend on the host having ffmpeg installed.
func transcoderConfig(t *testing.T) transcoderinteg.Config {
	t.Helper()
	stub := filepath.Join(t.TempDir(), "ffmpeg")
	if err := os.WriteFile(stub, []byte("#!/bin/sh\nexit 1\n"), 0o700); err != nil { //nolint:gosec // test stub must be executable
		t.Fatalf("write ffmpeg stub: %v", err)
	}
	return transcoderinteg.Config{FFmpegBin: stub}
}

// waitForPipelineRun waits until the on_import pipeline, triggered by the movie file
// event travelling through the core mesh, has recorded a pipeline run.
func waitForPipelineRun(t *testing.T, tr *transcoderinteg.Module) {
	t.Helper()
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	for ctx.Err() == nil {
		runs, err := tr.ListPipelineRuns(ctx, &transcodev1.ListPipelineRunsRequest{Page: 1, PageSize: 10})
		if err == nil && runs.GetTotal() > 0 {
			return
		}
		time.Sleep(100 * time.Millisecond)
	}
	t.Fatal("expected pipeline run after movie file added event")
}
