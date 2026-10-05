# ADR-0005: `media-ui-app` replaces Jellyfin web; Jellyfin handoff near-term

- **Status:** Accepted
- **Date:** recorded 2026-10-05; decision from `MASTER-ROADMAP.md` §1 "Playback product decision" (pre-2026-10)
- **Decided by:** Owner

## Context

Households need to watch media on day one; a native player with full parity takes time. Jellyfin already plays everything.

## Decision (verbatim intent)

- **End state:** `media-ui-app` replaces Jellyfin's consumer web client **including production playback** (OSD, tracks, resume, transcoder wiring). Agents must keep shipping native player parity.
- **Near-term:** households may play via the `jellyfin` bridge (deep link / Jellyfin clients); MuxCore owns browse, request, automation, and userdata sync.
- The bridge module remains an optional peer, not the UI end state.
- `contracts-playback` as a second-backend gRPC contract stays deferred until a second playback backend is committed (NG-6).

## Consequences

- Requirements FR-PLAY-003…009 are Must/Should; FR-PLAY-008 (handoff) is Should.
- `admin-ui` replaces the Jellyfin dashboard for operators.
- The empty `playback-contract` repo is folded into `contracts-playback` as a Go package (shared types/events), which does not contradict the deferral of a playback *service* contract ([TDD §2.1](../architecture/TDD.md#21-archived-and-empty-repositories)).
