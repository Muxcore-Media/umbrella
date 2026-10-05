//go:build integration

package integration

import (
	"context"
	"os"
	"path/filepath"
	"sync"
	"testing"

	mgmntv1 "github.com/Muxcore-Media/media-movies/proto/mgmntv1"
	transcodev1 "github.com/Muxcore-Media/media-transcoder/proto/transcodev1"
	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
	monitorv1 "github.com/Muxcore-Media/playback-monitor/proto/monitorv1"
)

func TestPipeline_MovieFileTranscodePlayback(t *testing.T) {
	stack := NewStack(t)
	ctx := context.Background()

	libRoot := t.TempDir()
	_, err := stack.Transcoder.UpsertSetup(ctx, &transcodev1.UpsertSetupRequest{Setup: &transcodev1.TranscodeSetup{
		Name:              "Integration on_import",
		Enabled:           true,
		LibraryPaths:      []string{libRoot},
		Trigger:           "on_import",
		SourceDisposition: "keep",
		Outputs: []*transcodev1.SetupOutput{
			{ProfileId: "h264_fast", Suffix: "-integ", ReplaceExtension: true, Enabled: true},
		},
		Steps: []*transcodev1.PipelineStep{
			{StepType: "filter.extension", ConfigJson: `{"extensions":["mkv"]}`, Enabled: true},
		},
	}})
	if err != nil {
		t.Fatalf("upsert setup: %v", err)
	}

	add, err := stack.Movies.AddMovie(ctx, &mgmntv1.AddMovieRequest{
		TmdbId: 42,
		Title:  "Pipeline Test",
		Year:   2026,
	})
	if err != nil {
		t.Fatalf("add movie: %v", err)
	}

	mediaFile := filepath.Join(libRoot, "pipeline-test.mkv")
	if err := os.WriteFile(mediaFile, []byte("fake-video"), 0o600); err != nil {
		t.Fatal(err)
	}
	_, err = stack.Movies.AddFile(ctx, &mgmntv1.AddFileRequest{
		MovieId:  add.GetMovieId(),
		FilePath: mediaFile,
		Quality:  "1080p",
	})
	if err != nil {
		t.Fatalf("add file: %v", err)
	}

	waitForPipelineRun(t, stack.Transcoder)

	jobs, err := stack.Transcoder.ListJobs(ctx, &transcodev1.ListJobsRequest{Page: 1, PageSize: 10})
	if err != nil {
		t.Fatalf("list jobs: %v", err)
	}
	if jobs.GetTotal() < 1 {
		t.Fatal("expected transcoding job from pipeline")
	}

	_, _, err = monitorinteg.IngestSession(ctx, stack.Monitor, monitorinteg.SessionEvent{
		EventType:         "playback.started",
		ExternalSessionID: "pipeline-playback",
		UserID:            "viewer",
		UserName:          "Viewer",
		ItemID:            add.GetMovieId(),
		Title:             "Pipeline Test",
		IsTranscode:       true,
	})
	if err != nil {
		t.Fatalf("ingest playback: %v", err)
	}
	_, _, err = monitorinteg.IngestSession(ctx, stack.Monitor, monitorinteg.SessionEvent{
		EventType:         "playback.stopped",
		ExternalSessionID: "pipeline-playback",
		UserID:            "viewer",
		UserName:          "Viewer",
		ItemID:            add.GetMovieId(),
		Title:             "Pipeline Test",
		PositionSeconds:   600,
	})
	if err != nil {
		t.Fatalf("ingest playback stop: %v", err)
	}

	history, err := stack.Monitor.ListHistory(ctx, &monitorv1.ListHistoryRequest{Limit: 5})
	if err != nil {
		t.Fatalf("playback history: %v", err)
	}
	if history.GetTotal() < 1 {
		t.Fatal("expected playback history for pipeline movie")
	}
}

func TestPipeline_ConcurrentModuleDatabaseAccess(t *testing.T) {
	stack := NewStack(t)
	ctx := context.Background()

	const workers = 8
	var wg sync.WaitGroup
	errCh := make(chan error, workers*3)

	work := func(label string, fn func() error) {
		defer wg.Done()
		if err := fn(); err != nil {
			errCh <- err
			t.Logf("%s: %v", label, err)
		}
	}

	for i := 0; i < workers; i++ {
		wg.Add(1)
		idx := i
		go work("movies", func() error {
			_, err := stack.Movies.ListMovies(ctx, &mgmntv1.ListMoviesRequest{Page: 1, PageSize: 5})
			return err
		})

		wg.Add(1)
		go work("transcoder", func() error {
			_, err := stack.Transcoder.ListProfiles(ctx, &transcodev1.ListProfilesRequest{})
			return err
		})

		wg.Add(1)
		go work("monitor", func() error {
			_, err := stack.Monitor.ListHistory(ctx, &monitorv1.ListHistoryRequest{Limit: int32(5 + idx)}) //nolint:gosec // test-only bounded limit
			return err
		})
	}

	wg.Wait()
	close(errCh)
	for err := range errCh {
		if err != nil {
			t.Fatalf("concurrent access error: %v", err)
		}
	}
}

func TestPipeline_HealthWhenPeerDegraded(t *testing.T) {
	stack := NewStack(t)
	ctx := context.Background()

	if err := stack.Movies.Stop(ctx); err != nil {
		t.Fatalf("stop movies: %v", err)
	}
	if err := stack.Movies.Health(ctx); err == nil {
		t.Fatal("expected degraded media-movies health after stop")
	}

	if err := stack.Transcoder.Health(ctx); err != nil {
		t.Fatalf("transcoder health should remain ok when movies degraded: %v", err)
	}
	if err := stack.Monitor.Health(ctx); err != nil {
		t.Fatalf("playback-monitor health should remain ok when movies degraded: %v", err)
	}
}
