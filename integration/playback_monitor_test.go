//go:build integration

package integration

import (
	"context"
	"testing"

	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
	monitorv1 "github.com/Muxcore-Media/playback-monitor/proto/monitorv1"
)

func seedStoppedPlayback(t *testing.T, m *monitorinteg.Module, ctx context.Context, ev monitorinteg.SessionEvent) {
	t.Helper()
	ev.EventType = "playback.started"
	if _, _, err := monitorinteg.IngestSession(ctx, m, ev); err != nil {
		t.Fatalf("playback start: %v", err)
	}
	ev.EventType = "playback.stopped"
	if _, _, err := monitorinteg.IngestSession(ctx, m, ev); err != nil {
		t.Fatalf("playback stop: %v", err)
	}
}

func TestPlaybackMonitor_SessionIngestAndHistory(t *testing.T) {
	ctx := context.Background()
	m := monitorinteg.NewTestModule(t, monitorinteg.Config{})

	_, created, err := monitorinteg.IngestSession(ctx, m, monitorinteg.SessionEvent{
		EventType:         "playback.started",
		ExternalSessionID: "sess-integ-1",
		UserID:            "alice",
		UserName:          "Alice",
		ItemID:            "mv-integ-1",
		Title:             "Integration Movie",
		Platform:          "Web",
	})
	if err != nil {
		t.Fatalf("ingest start: %v", err)
	}
	if !created {
		t.Fatal("expected created session")
	}

	_, _, err = monitorinteg.IngestSession(ctx, m, monitorinteg.SessionEvent{
		EventType:         "playback.stopped",
		ExternalSessionID: "sess-integ-1",
		UserID:            "alice",
		UserName:          "Alice",
		ItemID:            "mv-integ-1",
		Title:             "Integration Movie",
		PositionSeconds:   5400,
	})
	if err != nil {
		t.Fatalf("ingest stop: %v", err)
	}

	history, err := m.ListHistory(ctx, &monitorv1.ListHistoryRequest{Limit: 10})
	if err != nil {
		t.Fatalf("list history: %v", err)
	}
	if history.GetTotal() < 1 {
		t.Fatal("expected history row")
	}

	stats, err := m.GetHomeStats(ctx, &monitorv1.GetHomeStatsRequest{Days: 7})
	if err != nil {
		t.Fatalf("home stats: %v", err)
	}
	if len(stats.GetStats()) == 0 {
		t.Fatal("expected home stats")
	}
}

// TestPlaybackMonitor_LiveAnalytics targets the published contract: analytics are
// computed live from ingested sessions (no rollup refresh or anomaly detection API).
func TestPlaybackMonitor_LiveAnalytics(t *testing.T) {
	ctx := context.Background()
	m := monitorinteg.NewTestModule(t, monitorinteg.Config{})

	seedStoppedPlayback(t, m, ctx, monitorinteg.SessionEvent{
		ServerID:          "srv-integ",
		ExternalSessionID: "live-1",
		UserID:            "alice",
		UserName:          "Alice",
		ItemID:            "item-a",
		Title:             "Movie A",
		Platform:          "Android",
		IsTranscode:       true,
		PositionSeconds:   3600,
	})

	history, err := m.ListHistory(ctx, &monitorv1.ListHistoryRequest{Limit: 10})
	if err != nil {
		t.Fatalf("list history: %v", err)
	}
	if history.GetTotal() < 1 {
		t.Fatal("expected ingested session in history")
	}

	stats, err := m.GetHomeStats(ctx, &monitorv1.GetHomeStatsRequest{Days: 7})
	if err != nil {
		t.Fatalf("home stats: %v", err)
	}
	if len(stats.GetStats()) == 0 {
		t.Fatal("expected home stats to reflect ingested session")
	}

	analytics, err := m.GetStreamAnalytics(ctx, &monitorv1.GetStreamAnalyticsRequest{Days: 7})
	if err != nil {
		t.Fatalf("stream analytics: %v", err)
	}
	if len(analytics.GetPlatforms()) == 0 && len(analytics.GetTranscodes()) == 0 {
		t.Fatal("expected stream analytics breakdown rows")
	}
}

func TestPlaybackMonitor_Health(t *testing.T) {
	ctx := context.Background()
	m := monitorinteg.NewTestModule(t, monitorinteg.Config{})
	if err := m.Health(ctx); err != nil {
		t.Fatalf("health: %v", err)
	}
}
