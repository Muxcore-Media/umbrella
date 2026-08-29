package playbackv1

import (
	"encoding/json"
	"testing"
	"time"
)

func TestMarshalSessionEvent(t *testing.T) {
	at := time.Date(2026, 8, 20, 12, 0, 0, 0, time.UTC)
	data, err := MarshalSessionEvent(SessionInput{
		EventType:         "playback.started",
		SourceModule:      "jellyfin",
		ServerID:          "jf1",
		ServerType:        "jellyfin",
		ExternalSessionID: "sess-1",
		UserID:            "u1",
		UserName:          "alice",
		ItemID:            "item-1",
		Title:             "Movie",
		PositionSeconds:   10,
		DurationSeconds:   6000,
		IsTranscode:       true,
		OccurredAt:        at,
	})
	if err != nil {
		t.Fatal(err)
	}
	var raw map[string]any
	if err := json.Unmarshal(data, &raw); err != nil {
		t.Fatal(err)
	}
	if raw["event_type"] != "playback.started" || raw["external_session_id"] != "sess-1" {
		t.Fatalf("raw: %v", raw)
	}
	msg, err := UnmarshalSessionEvent(data)
	if err != nil || msg.GetItemId() != "item-1" || msg.GetUserName() != "alice" {
		t.Fatalf("roundtrip: msg=%+v err=%v", msg, err)
	}
}
