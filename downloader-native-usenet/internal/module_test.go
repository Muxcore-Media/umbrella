package internal_test

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	"github.com/Muxcore-Media/core/pkg/contracts"
	"github.com/Muxcore-Media/downloader-native-usenet/internal"
)

const minimalNZB = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE nzb PUBLIC "-//newzBin//DTD NZB 1.1//EN" "http://www.newzbin.com/DTD/nzb/nzb-1.1.dtd">
<nzb xmlns="http://www.newzbin.com/DTD/nzb/nzb-1.1.dtd">
  <file poster="poster@example" date="1700000000" subject="Fixture.Movie.mkv">
    <groups><group>alt.binaries.test</group></groups>
    <segments>
      <segment bytes="8192" number="1">&lt;test-segment-1@example&gt;</segment>
    </segments>
  </file>
</nzb>`

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
	t.Fatalf("timeout waiting for %s", typ)
	return recEvt{}
}

func nzbFixtureServer(t *testing.T) *httptest.Server {
	t.Helper()
	return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/x-nzb")
		_, _ = w.Write([]byte(minimalNZB))
	}))
}

func TestOfflineDispatchFixture(t *testing.T) {
	srv := nzbFixtureServer(t)
	defer srv.Close()

	pub := &recPub{}
	m := internal.NewModule(internal.Config{
		Engine:     "fixture",
		DestDir:    t.TempDir(),
		GRPCAddr:   "127.0.0.1:0",
		HTTPAddr:   "127.0.0.1:0",
		Publish:    pub.Publish,
		HTTPClient: srv.Client(),
	})

	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	jobID, err := m.OfflineDispatch(ctx, srv.URL+"/fixture.nzb", "Fixture.Movie", "movies")
	if err != nil {
		t.Fatal(err)
	}
	if jobID == "" {
		t.Fatal("expected job id")
	}
	started := pub.wait(t, contracts.EventDownloadStarted, time.Second)
	if started.Payload.Name != "Fixture.Movie" {
		t.Fatalf("started name: %+v", started.Payload)
	}
	completed := pub.wait(t, contracts.EventDownloadCompleted, time.Second)
	if completed.Payload.SavePath == "" {
		t.Fatalf("expected save path: %+v", completed.Payload)
	}
}

func TestModuleInfo(t *testing.T) {
	m := internal.NewModule(internal.Config{})
	info := m.Info()
	if info.ID != "downloader-native-usenet" {
		t.Fatalf("id: %s", info.ID)
	}
	found := false
	for _, c := range info.Capabilities {
		if c == "downloader.native.usenet" {
			found = true
		}
	}
	if !found {
		t.Fatal("missing downloader.native.usenet capability")
	}
}

func TestGetCapabilitiesBackend(t *testing.T) {
	m := internal.NewModule(internal.Config{Engine: "fixture"})
	if got := m.CapabilitiesBackend(); got != "native-usenet" {
		t.Fatalf("backend: %s", got)
	}
}
