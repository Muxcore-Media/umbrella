package playbackv1

import (
	"fmt"
	"time"

	"google.golang.org/protobuf/encoding/protojson"
)

// SessionInput is the bridge-neutral input for a normalized playback mesh event.
type SessionInput struct {
	EventType         string
	SourceModule      string
	ServerID          string
	ServerType        string
	ExternalSessionID string
	UserID            string
	UserName          string
	ItemID            string
	MuxcoreID         string
	Title             string
	MediaType         string
	PositionSeconds   int64
	DurationSeconds   int64
	IsPaused          bool
	IsTranscode       bool
	Platform          string
	Device            string
	Player            string
	IPAddress         string
	MediaPath         string
	LibraryName       string
	StreamResolution  string
	PlayMethod        string
	OccurredAt        time.Time
}

var sessionJSONOpts = protojson.MarshalOptions{UseProtoNames: true}

// MarshalSessionEvent encodes a normalized PlaybackSessionEvent as JSON for mesh publish.
func MarshalSessionEvent(in SessionInput) ([]byte, error) {
	if in.OccurredAt.IsZero() {
		in.OccurredAt = time.Now().UTC()
	}
	msg := &PlaybackSessionEvent{
		EventType:         in.EventType,
		SourceModule:      in.SourceModule,
		ServerId:          in.ServerID,
		ServerType:        in.ServerType,
		ExternalSessionId: in.ExternalSessionID,
		UserId:            in.UserID,
		UserName:          in.UserName,
		ItemId:            in.ItemID,
		MuxcoreId:         in.MuxcoreID,
		Title:             in.Title,
		MediaType:         in.MediaType,
		PositionSeconds:   in.PositionSeconds,
		DurationSeconds:   in.DurationSeconds,
		IsPaused:          in.IsPaused,
		IsTranscode:       in.IsTranscode,
		Platform:          in.Platform,
		Device:            in.Device,
		Player:            in.Player,
		IpAddress:         in.IPAddress,
		MediaPath:         in.MediaPath,
		LibraryName:       in.LibraryName,
		StreamResolution:  in.StreamResolution,
		PlayMethod:        in.PlayMethod,
		OccurredAtUnix:    in.OccurredAt.UTC().Unix(),
	}
	data, err := sessionJSONOpts.Marshal(msg)
	if err != nil {
		return nil, fmt.Errorf("marshal playback session: %w", err)
	}
	return data, nil
}

// UnmarshalSessionEvent decodes mesh JSON into PlaybackSessionEvent.
func UnmarshalSessionEvent(data []byte) (*PlaybackSessionEvent, error) {
	msg := &PlaybackSessionEvent{}
	opts := protojson.UnmarshalOptions{DiscardUnknown: true}
	if err := opts.Unmarshal(data, msg); err != nil {
		return nil, err
	}
	return msg, nil
}
