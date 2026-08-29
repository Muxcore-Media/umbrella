package qbit_test

import (
	"context"
	"testing"

	"github.com/Muxcore-Media/downloader-qbittorrent/internal/qbit"
)

func TestClientAddPauseResumeDeleteOffline(t *testing.T) {
	mock := qbit.NewMockServer("admin", "secret")
	defer mock.Close()

	c := mock.Client()
	magnet := "magnet:?xt=urn:btih:0123456789ABCDEF0123456789ABCDEF01234567&dn=Fixture.Movie"
	if err := c.AddTorrent(context.Background(), magnet, "/downloads", "movies", false); err != nil {
		t.Fatal(err)
	}
	items, err := c.ListTorrents(context.Background(), "movies", "")
	if err != nil {
		t.Fatal(err)
	}
	if len(items) != 1 {
		t.Fatalf("want 1 torrent, got %d", len(items))
	}
	hash := items[0].Hash
	if err := c.Pause(context.Background(), hash); err != nil {
		t.Fatal(err)
	}
	got, err := c.GetTorrent(context.Background(), hash)
	if err != nil || got == nil || qbit.MapState(got.State) != "paused" {
		t.Fatalf("after pause: %+v err=%v", got, err)
	}
	if err := c.Resume(context.Background(), hash); err != nil {
		t.Fatal(err)
	}
	if !mock.Complete(hash) {
		t.Fatal("complete failed")
	}
	got, _ = c.GetTorrent(context.Background(), hash)
	if qbit.MapState(got.State) != "completed" {
		t.Fatalf("want completed, got %s", got.State)
	}
	if err := c.Delete(context.Background(), hash, true); err != nil {
		t.Fatal(err)
	}
	items, _ = c.ListTorrents(context.Background(), "", "")
	if len(items) != 0 {
		t.Fatalf("after delete: %+v", items)
	}
}

func TestClientRejectsBadPassword(t *testing.T) {
	mock := qbit.NewMockServer("admin", "secret")
	defer mock.Close()
	c := &qbit.Client{
		BaseURL: mock.URL(), Username: "admin", Password: "wrong",
		HTTPClient: mock.Server.Client(),
	}
	_, err := c.Version(context.Background())
	if err == nil {
		t.Fatal("expected auth error")
	}
}

func TestFixtureClientCompletes(t *testing.T) {
	f := qbit.NewFixtureClient()
	hash, tor, err := f.AddTorrent(context.Background(), "magnet:?xt=urn:btih:abc&dn=X", "/dl", "tv", false)
	if err != nil {
		t.Fatal(err)
	}
	if hash == "" || tor == nil || qbit.MapState(tor.State) != "completed" {
		t.Fatalf("fixture should complete: hash=%q tor=%+v", hash, tor)
	}
}
