package internal_test

import (
	"context"
	"encoding/json"
	"sync"
	"testing"
	"time"

	cdlv1 "github.com/Muxcore-Media/contracts-downloader/muxcore/downloader/v1"
	"github.com/Muxcore-Media/core/pkg/contracts"
	"github.com/Muxcore-Media/downloader-qbittorrent/internal"
	"github.com/Muxcore-Media/downloader-qbittorrent/internal/qbit"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

type recPub struct {
	mu   sync.Mutex
	evts []recEvt
}

type recEvt struct {
	Type    string
	Payload contracts.DownloadEventPayload
}

func (r *recPub) Publish(_ context.Context, eventType string, payload []byte) error {
	var p contracts.DownloadEventPayload
	_ = json.Unmarshal(payload, &p)
	r.mu.Lock()
	r.evts = append(r.evts, recEvt{Type: eventType, Payload: p})
	r.mu.Unlock()
	return nil
}

func (r *recPub) wait(t *testing.T, typ string, d time.Duration) recEvt {
	t.Helper()
	deadline := time.Now().Add(d)
	for time.Now().Before(deadline) {
		r.mu.Lock()
		for _, e := range r.evts {
			if e.Type == typ {
				r.mu.Unlock()
				return e
			}
		}
		r.mu.Unlock()
		time.Sleep(5 * time.Millisecond)
	}
	t.Fatalf("timeout waiting for %s; got %+v", typ, r.snapshot())
	return recEvt{}
}

func (r *recPub) snapshot() []recEvt {
	r.mu.Lock()
	defer r.mu.Unlock()
	out := make([]recEvt, len(r.evts))
	copy(out, r.evts)
	return out
}

func dialDownloader(t *testing.T, addr string) cdlv1.DownloaderServiceClient {
	t.Helper()
	conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = conn.Close() })
	return cdlv1.NewDownloaderServiceClient(conn)
}

func TestFixtureAddTorrentCompletesViaRPC(t *testing.T) {
	pub := &recPub{}
	m := internal.NewModule(internal.Config{
		Fixture:  true,
		GRPCAddr: "127.0.0.1:0",
		HTTPAddr: "127.0.0.1:0",
		Publish:  pub.Publish,
	})
	ctx := context.Background()
	if err := m.Start(ctx); err != nil {
		t.Fatal(err)
	}
	defer m.Stop(ctx)

	client := dialDownloader(t, m.ListenAddr())
	resp, err := client.AddTorrent(ctx, &cdlv1.AddTorrentRequest{
		TorrentUrl: "magnet:?xt=urn:btih:0123456789ABCDEF0123456789ABCDEF01234567&dn=Fixture.Movie",
		SavePath:   "/downloads",
		Category:   "movies",
	})
	if err != nil {
		t.Fatal(err)
	}
	if resp.GetTorrentId() == "" {
		t.Fatal("empty torrent id")
	}
	started := pub.wait(t, contracts.EventDownloadStarted, time.Second)
	if started.Payload.ID != resp.GetTorrentId() {
		t.Fatalf("started id=%q want %q", started.Payload.ID, resp.GetTorrentId())
	}
	completed := pub.wait(t, contracts.EventDownloadCompleted, time.Second)
	if completed.Payload.SavePath == "" {
		t.Fatalf("completed missing save_path: %+v", completed.Payload)
	}

	got, err := client.GetTorrent(ctx, &cdlv1.GetTorrentRequest{TorrentId: resp.GetTorrentId()})
	if err != nil {
		t.Fatal(err)
	}
	if got.GetTorrent().GetStatus() != "completed" {
		t.Fatalf("status=%q", got.GetTorrent().GetStatus())
	}

	if _, err := client.PauseTorrent(ctx, &cdlv1.PauseTorrentRequest{TorrentId: resp.GetTorrentId()}); err != nil {
		t.Fatal(err)
	}
	if _, err := client.ResumeTorrent(ctx, &cdlv1.ResumeTorrentRequest{TorrentId: resp.GetTorrentId()}); err != nil {
		t.Fatal(err)
	}
	if _, err := client.RemoveTorrent(ctx, &cdlv1.RemoveTorrentRequest{TorrentId: resp.GetTorrentId(), DeleteFiles: true}); err != nil {
		t.Fatal(err)
	}
}

func TestMockWebUIHealthAndList(t *testing.T) {
	mock := qbit.NewMockServer("admin", "ci")
	defer mock.Close()

	m := internal.NewModule(internal.Config{
		BaseURL:    mock.URL(),
		Username:   "admin",
		Password:   "ci",
		GRPCAddr:   "127.0.0.1:0",
		HTTPAddr:   "127.0.0.1:0",
		HTTPClient: mock.Server.Client(),
	})
	ctx := context.Background()
	if err := m.Start(ctx); err != nil {
		t.Fatal(err)
	}
	defer m.Stop(ctx)

	if err := m.Health(ctx); err != nil {
		t.Fatalf("health: %v", err)
	}

	client := dialDownloader(t, m.ListenAddr())
	_, err := client.AddTorrent(ctx, &cdlv1.AddTorrentRequest{
		TorrentUrl: "magnet:?xt=urn:btih:abcdef0123456789abcdef0123456789abcdef01&dn=LiveShape",
		Category:   "tv",
	})
	if err != nil {
		t.Fatal(err)
	}
	list, err := client.ListTorrents(ctx, &cdlv1.ListTorrentsRequest{Category: "tv"})
	if err != nil {
		t.Fatal(err)
	}
	if len(list.GetTorrents()) != 1 {
		t.Fatalf("want 1, got %d", len(list.GetTorrents()))
	}
}

func TestUnconfiguredSoftEmpty(t *testing.T) {
	t.Setenv("DOWNLOADER_ENGINE", "")
	t.Setenv("QBIT_FIXTURE", "")
	m := internal.NewModule(internal.Config{
		GRPCAddr: "127.0.0.1:0",
		HTTPAddr: "127.0.0.1:0",
	})
	if err := m.Health(context.Background()); err != nil {
		t.Fatalf("unconfigured health should be soft-ok: %v", err)
	}
}

func TestQBITFixtureEnv(t *testing.T) {
	t.Setenv("QBIT_FIXTURE", "1")
	m := internal.NewModule(internal.Config{GRPCAddr: "127.0.0.1:0", HTTPAddr: "127.0.0.1:0"})
	if err := m.Health(context.Background()); err != nil {
		t.Fatal(err)
	}
}
