//go:build integration

package integration

import (
	"context"
	"testing"

	coreinteg "github.com/Muxcore-Media/core/integsupport"
	moviesinteg "github.com/Muxcore-Media/media-movies/integsupport"
	transcoderinteg "github.com/Muxcore-Media/media-transcoder/integsupport"
	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
)

func TestFullStack_AllModulesInit(t *testing.T) {
	stack := NewStack(t)
	if stack.Core == nil {
		t.Fatal("core not initialized")
	}
	if stack.Movies == nil {
		t.Fatal("movies not initialized")
	}
	if stack.Transcoder == nil {
		t.Fatal("transcoder not initialized")
	}
	if stack.Monitor == nil {
		t.Fatal("monitor not initialized")
	}
}

func TestFullStack_ModuleIndependence(t *testing.T) {
	// Each module can be initialized independently.
	ctx := context.Background()

	m := moviesinteg.NewTestModule(t, moviesinteg.Config{})
	if err := m.Health(ctx); err != nil {
		t.Fatalf("movies health: %v", err)
	}

	tr := transcoderinteg.NewTestModule(t, transcoderConfig(t))
	if err := tr.Health(ctx); err != nil {
		t.Fatalf("transcoder health: %v", err)
	}

	mon := monitorinteg.NewTestModule(t, monitorinteg.Config{})
	if err := mon.Health(ctx); err != nil {
		t.Fatalf("monitor health: %v", err)
	}

	_ = coreinteg.NewCoreHarness(t)
}
