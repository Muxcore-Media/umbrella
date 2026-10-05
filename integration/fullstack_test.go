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
	mgmntv1 "github.com/Muxcore-Media/media-movies/proto/mgmntv1"
	transcoderinteg "github.com/Muxcore-Media/media-transcoder/integsupport"
	transcodev1 "github.com/Muxcore-Media/media-transcoder/proto/transcodev1"
	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
	monitorv1 "github.com/Muxcore-Media/playback-monitor/proto/monitorv1"
)

// FullStack wires core + movies + transcoder + monitor for cross-module tests.
type FullStack struct {
	Core       *coreinteg.CoreHarness
	Movies     *moviesinteg.Module
	Transcoder *transcoderinteg.Module
	Monitor    *monitorinteg.Module
}

func NewFullStack(t *testing.T) *FullStack {
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
		t.Fatalf("start movies: %v", err)
	}
	t.Setenv("MUXCORE_MODULE_ID", "media-transcoder")
	if err := transcoderinteg.Start(ctx, transcoder); err != nil {
		t.Fatalf("start transcoder: %v", err)
	}
	t.Setenv("MUXCORE_MODULE_ID", "playback-monitor")
	if err := monitorinteg.Start(ctx, monitor); err != nil {
		t.Fatalf("start monitor: %v", err)
	}

	time.Sleep(200 * time.Millisecond)

	return &FullStack{
		Core:       core,
		Movies:     movies,
		Transcoder: transcoder,
		Monitor:    monitor,
	}
}

func TestFullStack_MovieTranscodePlayback(t *testing.T) {
	stack := NewFullStack(t)
	ctx := context.Background()

	// 1. Set up transcoder pipeline for on_import.
	libRoot := t.TempDir()
	_, err := stack.Transcoder.UpsertSetup(ctx, &transcodev1.UpsertSetupRequest{
		Setup: &transcodev1.TranscodeSetup{
			Name:              "fullstack-integ",
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
		},
	})
	if err != nil {
		t.Fatalf("upsert setup: %v", err)
	}

	// 2. Add a movie.
	addResp, err := stack.Movies.AddMovie(ctx, &mgmntv1.AddMovieRequest{
		TmdbId: 42,
		Title:  "Full Stack Test",
		Year:   2026,
	})
	if err != nil {
		t.Fatalf("add movie: %v", err)
	}
	movieID := addResp.GetMovieId()

	// 3. Add a file — triggers transcode pipeline.
	mediaFile := filepath.Join(libRoot, "fullstack.mkv")
	if err := os.WriteFile(mediaFile, []byte("fake-fullstack-video"), 0o600); err != nil {
		t.Fatal(err)
	}
	_, err = stack.Movies.AddFile(ctx, &mgmntv1.AddFileRequest{
		MovieId:  movieID,
		FilePath: mediaFile,
		Quality:  "4K",
	})
	if err != nil {
		t.Fatalf("add file: %v", err)
	}

	// 4. Wait for transcoder to pick up the pipeline run.
	waitForPipelineRun(t, stack.Transcoder)

	// 5. Simulate playback via monitor.
	_, created, err := monitorinteg.IngestSession(ctx, stack.Monitor, monitorinteg.SessionEvent{
		EventType:         "playback.started",
		ExternalSessionID: "fullstack-sess-1",
		UserID:            "tester",
		UserName:          "Tester",
		ItemID:            movieID,
		Title:             "Full Stack Test",
		Platform:          "Web",
	})
	if err != nil {
		t.Fatalf("playback start: %v", err)
	}
	if !created {
		t.Fatal("expected new session")
	}

	_, _, err = monitorinteg.IngestSession(ctx, stack.Monitor, monitorinteg.SessionEvent{
		EventType:         "playback.stopped",
		ExternalSessionID: "fullstack-sess-1",
		UserID:            "tester",
		UserName:          "Tester",
		ItemID:            movieID,
		Title:             "Full Stack Test",
		PositionSeconds:   1800,
	})
	if err != nil {
		t.Fatalf("playback stop: %v", err)
	}

	// 6. Verify history.
	history, err := stack.Monitor.ListHistory(ctx, &monitorv1.ListHistoryRequest{Limit: 10})
	if err != nil {
		t.Fatalf("list history: %v", err)
	}
	if history.GetTotal() < 1 {
		t.Fatal("expected history row")
	}

	// 7. Cleanup.
	_, err = stack.Movies.RemoveMovie(ctx, &mgmntv1.RemoveMovieRequest{MovieId: movieID})
	if err != nil {
		t.Fatalf("remove movie: %v", err)
	}
}

func TestFullStack_CoreMeshLifecycle(t *testing.T) {
	stack := NewFullStack(t)
	if stack.Core.Addr == "" {
		t.Fatal("expected non-empty core address")
	}
	stack.Core.Close()
}

func TestFullStack_MoviesCRUD(t *testing.T) {
	stack := NewFullStack(t)
	ctx := context.Background()

	// Add.
	addResp, err := stack.Movies.AddMovie(ctx, &mgmntv1.AddMovieRequest{
		TmdbId: 100,
		Title:  "CRUD Test",
		Year:   2025,
	})
	if err != nil {
		t.Fatalf("add: %v", err)
	}

	// Get.
	getResp, err := stack.Movies.GetMovie(ctx, &mgmntv1.GetMovieRequest{MovieId: addResp.GetMovieId()})
	if err != nil {
		t.Fatalf("get: %v", err)
	}
	if getResp.GetMovie().GetTitle() != "CRUD Test" {
		t.Fatalf("title=%q", getResp.GetMovie().GetTitle())
	}

	// List.
	listResp, err := stack.Movies.ListMovies(ctx, &mgmntv1.ListMoviesRequest{Page: 1, PageSize: 10})
	if err != nil {
		t.Fatalf("list: %v", err)
	}
	if listResp.GetTotal() < 1 {
		t.Fatal("expected at least 1 movie")
	}

	// Update.
	monitored := false
	_, err = stack.Movies.UpdateMovie(ctx, &mgmntv1.UpdateMovieRequest{
		MovieId:   addResp.GetMovieId(),
		Monitored: &monitored,
	})
	if err != nil {
		t.Fatalf("update: %v", err)
	}

	// Remove.
	_, err = stack.Movies.RemoveMovie(ctx, &mgmntv1.RemoveMovieRequest{MovieId: addResp.GetMovieId()})
	if err != nil {
		t.Fatalf("remove: %v", err)
	}
}

func TestFullStack_TranscoderJobLifecycle(t *testing.T) {
	stack := NewFullStack(t)
	ctx := context.Background()

	dir := t.TempDir()
	input := filepath.Join(dir, "input.mkv")
	output := filepath.Join(dir, "output.mp4")
	if err := os.WriteFile(input, []byte("video-data"), 0o600); err != nil {
		t.Fatal(err)
	}

	// Enqueue.
	enqResp, err := stack.Transcoder.Enqueue(ctx, &transcodev1.EnqueueRequest{
		InputPath:  input,
		OutputPath: output,
		ProfileId:  "h264_fast",
	})
	if err != nil {
		t.Fatalf("enqueue: %v", err)
	}

	// Get job.
	jobResp, err := stack.Transcoder.GetJob(ctx, &transcodev1.GetJobRequest{JobId: enqResp.GetJobId()})
	if err != nil {
		t.Fatalf("get job: %v", err)
	}
	if jobResp.GetJob().GetId() != enqResp.GetJobId() {
		t.Fatalf("job id mismatch: %s != %s", jobResp.GetJob().GetId(), enqResp.GetJobId())
	}

	// Cancel.
	_, err = stack.Transcoder.CancelJob(ctx, &transcodev1.CancelJobRequest{JobId: enqResp.GetJobId()})
	if err != nil {
		t.Fatalf("cancel: %v", err)
	}

	// Verify cancelled.
	deadline := time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) {
		j, _ := stack.Transcoder.GetJob(ctx, &transcodev1.GetJobRequest{JobId: enqResp.GetJobId()})
		if j != nil && j.GetJob().GetStatus() == "cancelled" {
			return
		}
		time.Sleep(50 * time.Millisecond)
	}
	t.Fatal("job was not cancelled before timeout")
}
