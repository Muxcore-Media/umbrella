//go:build integration

package integration

import (
	"context"
	"os"
	"path/filepath"
	"testing"

	mgmntv1 "github.com/Muxcore-Media/media-movies/proto/mgmntv1"
	moviesinteg "github.com/Muxcore-Media/media-movies/integsupport"
)

func TestMediaMovies_CRUDAndViewState(t *testing.T) {
	ctx := context.Background()
	m := moviesinteg.NewTestModule(t, moviesinteg.Config{})

	add, err := m.AddMovie(ctx, &mgmntv1.AddMovieRequest{
		TmdbId:   550,
		Title:    "Fight Club",
		Year:     1999,
		Overview: "Integration test movie",
		Genres:   []string{"Drama"},
	})
	if err != nil {
		t.Fatalf("add movie: %v", err)
	}
	if add.GetMovieId() == "" {
		t.Fatal("expected movie id")
	}

	get, err := m.GetMovie(ctx, &mgmntv1.GetMovieRequest{MovieId: add.GetMovieId()})
	if err != nil {
		t.Fatalf("get movie: %v", err)
	}
	if get.GetMovie().GetTitle() != "Fight Club" {
		t.Fatalf("title=%q", get.GetMovie().GetTitle())
	}

	monitored := false
	_, err = m.UpdateMovie(ctx, &mgmntv1.UpdateMovieRequest{
		MovieId:   add.GetMovieId(),
		Monitored: &monitored,
	})
	if err != nil {
		t.Fatalf("update movie: %v", err)
	}

	_, err = m.SetViewState(ctx, &mgmntv1.SetViewStateRequest{
		UserId:  "user-1",
		MovieId: add.GetMovieId(),
		Watched: true,
	})
	if err != nil {
		t.Fatalf("set view state: %v", err)
	}
	vs, err := m.GetViewState(ctx, &mgmntv1.GetViewStateRequest{
		UserId:  "user-1",
		MovieId: add.GetMovieId(),
	})
	if err != nil {
		t.Fatalf("get view state: %v", err)
	}
	if !vs.GetState().GetWatched() {
		t.Fatal("expected watched=true")
	}

	dir := t.TempDir()
	mediaFile := filepath.Join(dir, "fight-club.mkv")
	if err := os.WriteFile(mediaFile, []byte("fake-mkv"), 0o600); err != nil {
		t.Fatal(err)
	}
	_, err = m.AddFile(ctx, &mgmntv1.AddFileRequest{
		MovieId:  add.GetMovieId(),
		FilePath: mediaFile,
		Quality:  "1080p",
	})
	if err != nil {
		t.Fatalf("add file: %v", err)
	}

	_, err = m.RefreshMetadata(ctx, &mgmntv1.RefreshMetadataRequest{MovieId: add.GetMovieId()})
	if err == nil {
		t.Log("metadata refresh succeeded with live metadata-tmdb (optional)")
	} else {
		t.Logf("metadata refresh without metadata-tmdb (expected in CI): %v", err)
	}

	_, err = m.RemoveMovie(ctx, &mgmntv1.RemoveMovieRequest{MovieId: add.GetMovieId()})
	if err != nil {
		t.Fatalf("remove movie: %v", err)
	}
	_, err = m.GetMovie(ctx, &mgmntv1.GetMovieRequest{MovieId: add.GetMovieId()})
	if err == nil {
		t.Fatal("expected movie to be deleted")
	}
}

func TestMediaMovies_BatchRefreshValidation(t *testing.T) {
	ctx := context.Background()
	m := moviesinteg.NewTestModule(t, moviesinteg.Config{})

	_, err := m.BatchRefreshMetadata(ctx, &mgmntv1.BatchRefreshMetadataRequest{
		MovieIds: make([]string, 101),
	})
	if err == nil {
		t.Fatal("expected batch size validation error")
	}
}

func TestMediaMovies_Health(t *testing.T) {
	ctx := context.Background()
	m := moviesinteg.NewTestModule(t, moviesinteg.Config{})
	if err := m.Health(ctx); err != nil {
		t.Fatalf("health: %v", err)
	}
}
