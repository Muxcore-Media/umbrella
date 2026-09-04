//go:build integration

package integration

import (
	"context"
	"os"
	"path/filepath"
	"testing"

	backupv1 "github.com/Muxcore-Media/backup-local/muxcore/backup/v1"
	backupinteg "github.com/Muxcore-Media/backup-local/integsupport"
	moviesinteg "github.com/Muxcore-Media/media-movies/integsupport"
	mgmntv1 "github.com/Muxcore-Media/media-movies/proto/mgmntv1"
)

func TestBackupMedia_BackupAndVerify(t *testing.T) {
	ctx := context.Background()

	movies := moviesinteg.NewTestModule(t, moviesinteg.Config{})
	bk := backupinteg.NewTestModule(t, backupinteg.Config{})

	if err := moviesinteg.Start(ctx, movies); err != nil {
		t.Fatalf("start media-movies: %v", err)
	}
	if err := backupinteg.Start(ctx, bk); err != nil {
		t.Fatalf("start backup-local: %v", err)
	}

	// Create a movie and add files.
	addResp, err := movies.AddMovie(ctx, &mgmntv1.AddMovieRequest{
		TmdbId: 550,
		Title:  "Fight Club",
		Year:   1999,
	})
	if err != nil {
		t.Fatalf("add movie: %v", err)
	}

	dir := t.TempDir()
	mediaFile := filepath.Join(dir, "fight-club.mkv")
	if err := os.WriteFile(mediaFile, []byte("fake-mkv-content-for-backup"), 0o600); err != nil {
		t.Fatal(err)
	}
	_, err = movies.AddFile(ctx, &mgmntv1.AddFileRequest{
		MovieId:  addResp.GetMovieId(),
		FilePath: mediaFile,
		Quality:  "1080p",
	})
	if err != nil {
		t.Fatalf("add file: %v", err)
	}

	// Create a backup.
	createResp, err := bk.CreateBackup(ctx, &backupv1.CreateBackupRequest{
		ModuleIds: []string{"media-movies"},
	})
	if err != nil {
		t.Fatalf("create backup: %v", err)
	}
	backupInfo := createResp.GetBackup()
	if backupInfo == nil {
		t.Fatal("expected backup info in response")
	}
	if backupInfo.GetId() == "" {
		t.Fatal("expected backup id")
	}

	// List backups.
	listResp, err := bk.ListBackups(ctx, &backupv1.ListBackupsRequest{})
	if err != nil {
		t.Fatalf("list backups: %v", err)
	}
	backups := listResp.GetBackups()
	if len(backups) < 1 {
		t.Fatal("expected at least 1 backup")
	}

	// Verify backup info.
	found := false
	for _, b := range backups {
		if b.GetId() == backupInfo.GetId() {
			found = true
			if b.GetSizeBytes() == 0 {
				t.Log("backup size is 0 (expected for in-memory test)")
			}
			break
		}
	}
	if !found {
		t.Fatalf("backup %s not found in list", backupInfo.GetId())
	}

	// Verify backup.
	_, err = bk.VerifyBackup(ctx, &backupv1.VerifyBackupRequest{
		BackupId: backupInfo.GetId(),
	})
	if err != nil {
		t.Fatalf("verify backup: %v", err)
	}

	// Remove the movie.
	_, err = movies.RemoveMovie(ctx, &mgmntv1.RemoveMovieRequest{MovieId: addResp.GetMovieId()})
	if err != nil {
		t.Fatalf("remove movie: %v", err)
	}
}

func TestBackupMedia_EmptyList(t *testing.T) {
	ctx := context.Background()
	bk := backupinteg.NewTestModule(t, backupinteg.Config{})

	listResp, err := bk.ListBackups(ctx, &backupv1.ListBackupsRequest{})
	if err != nil {
		t.Fatalf("list backups: %v", err)
	}
	if len(listResp.GetBackups()) != 0 {
		t.Fatalf("expected 0 backups, got %d", len(listResp.GetBackups()))
	}
}
