package playbackcontract

import playbackv1 "github.com/Muxcore-Media/playback-contract/proto/playbackv1"

// SessionInput is an alias for bridge publishers.
type SessionInput = playbackv1.SessionInput

// MarshalSessionEvent encodes a normalized playback event for mesh publish.
var MarshalSessionEvent = playbackv1.MarshalSessionEvent

// UnmarshalSessionEvent decodes mesh JSON into PlaybackSessionEvent.
var UnmarshalSessionEvent = playbackv1.UnmarshalSessionEvent
