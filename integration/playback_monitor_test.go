//go:build integration

package integration

import (
	"context"
	"testing"

	monitorv1 "github.com/Muxcore-Media/playback-monitor/proto/monitorv1"
	monitorinteg "github.com/Muxcore-Media/playback-monitor/integsupport"
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

func TestPlaybackMonitor_AnalyticsRollupAndAnomalies(t *testing.T) {
	ctx := context.Background()
	m := monitorinteg.NewTestModule(t, monitorinteg.Config{})

	seedStoppedPlayback(t, m, ctx, monitorinteg.SessionEvent{
		ServerID:          "srv-integ",
		ExternalSessionID: "rollup-1",
		UserID:            "alice",
		UserName:          "Alice",
		ItemID:            "item-a",
		Title:             "Movie A",
		Platform:          "Android",
		IsTranscode:       true,
		PositionSeconds:   3600,
	})

	if err := monitorinteg.RefreshAnalyticsRollups(ctx, m, 1); err != nil {
		t.Fatalf("refresh rollups: %v", err)
	}

	analytics, err := m.GetStreamAnalytics(ctx, &monitorv1.GetStreamAnalyticsRequest{Days: 7})
	if err != nil {
		t.Fatalf("stream analytics: %v", err)
	}
	if len(analytics.GetPlatforms()) == 0 && len(analytics.GetTranscodes()) == 0 {
		t.Fatal("expected stream analytics breakdown rows")
	}

	seedStoppedPlayback(t, m, ctx, monitorinteg.SessionEvent{
		ServerID:          "srv-integ",
		ExternalSessionID: "anomaly-long",
		UserID:            "binger",
		UserName:          "Binger",
		ItemID:            "item-long",
		Title:             "Long Movie",
		PositionSeconds:   4 * 3600,
	})
	types, err := monitorinteg.DetectViewingAnomalies(ctx, m, 7, 10)
	if err != nil {
		t.Fatalf("detect anomalies: %v", err)
	}
	found := false
	for _, typ := range types {
		if typ == "long_session" {
			found = true
		}
	}
	if !found {
		t.Fatalf("expected long_session anomaly, got %v", types)
	}
}

func TestPlaybackMonitor_Health(t *testing.T) {
	ctx := context.Background()
	m := monitorinteg.NewTestModule(t, monitorinteg.Config{})
	if err := m.Health(ctx); err != nil {
		t.Fatalf("health: %v", err)
	}
}
