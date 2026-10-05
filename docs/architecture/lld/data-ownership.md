# LLD — Data ownership, lifecycle, and retention

| | |
|---|---|
| **Status** | Active — v1.0 (2026-10-05) |
| **Requirements** | NFR-DATA-001…003, FR-BAK-001…004, NFR-SEC-005 |
| **Decision** | [ADR-0009](../../adr/0009-per-module-data-ownership.md) |

## 1. Rules

1. Each stateful module owns exactly one store (SQLite file, WAL mode, `SetMaxOpenConns(1)`) under its data directory (`<MODULE>_DATA_DIR` / `*_DB_PATH`). No other module opens it.
2. Cross-module references are opaque string IDs (`item_id`, `tmdb_id`, `wanted_item_id`, user IDs). Readers tolerate dangling references.
3. Schema evolution is forward-only and idempotent at startup; each household module gets an upgrade-from-previous-release test (TDD §6).
4. Every household-manifest module that holds state either implements `Backupable.ExportState/ImportState` or declares its data directory in the manifest (`state_dirs:`) so `backup-local` covers it (FR-BAK-002).

## 2. Store inventory (household-relevant)

| Module | Store / main tables | Personal data | Retention |
|--------|---------------------|---------------|-----------|
| `auth-local` | `users`, `sessions`, `api_tokens`, `totp`, `webauthn_*`, `invites` | **Yes** (usernames, credentials) | Sessions expire 24 h (rows pruned); users until deleted |
| `userdata-local` | `user_blobs` (progress, favorites, prefs, playlists, queue) | **Yes** (viewing behaviour) | Until user deleted |
| `request-media` | `requests`, `request_ready_notified` | **Yes** (who requested what) | Until deleted |
| `playback-monitor` | `sessions`, `library_items`, `user_identities`, storage-daily, notification rules | **Yes** (viewing history, IPs/devices) | **[Proposed]** configurable history retention (default 365 days) — not implemented |
| `playback-guard` | `guard_rules`, `violations`, `trust_scores`, `user_aliases`, `module_settings` | **Yes** | Until deleted |
| `media-automation` | `wanted_items`, `download_history`, `release_blacklist`, `delay_profiles`, `release_seen`, `series_overrides` | No | History unbounded **[Proposed]** prune completed > 180 days |
| `media-scanner` | `watch_dirs`, `imported_files`, `scan_log` | No | `scan_log` unbounded **[Proposed]** prune |
| `media-movies` / `media-tvshows` | `movies`/`series`, `seasons`, `episodes`, `*_files`, `tags`, `history` | No | Library lifetime |
| `media-music`, `media-books`, `media-comics`, `media-audiobooks` | domain tables + `history` | No | Library lifetime |
| `media-custom-formats` | `custom_formats`, `quality_profiles`, `release_profile_groups` | No | Config |
| `media-list-sync`, `media-rename`, `media-root-folders`, `media-tagging`, `media-subtitles`, `media-library-maintainer`, `media-graph`, `media-ffprobe` (cache) | module tables | No | Config / cache |
| BFF | in-memory sessions; JSON-file userdata fallback | Yes (sessions) | Process lifetime (target: SQLite, NFR-REL-003) |
| admin-ui | session JSON file | Yes (bearer tokens) | 30 min TTL |
| core | audit JSONL, event WAL, CA keys, module cache | Audit contains actor IDs | Size-rotated (100 MB × 5) |

## 3. User deletion (NFR-DATA-003, Planned)

Deleting a user in auth-local publishes `identity.user.deleted {user_id}` (new event in `contracts-media/events` → move to a `contracts-identity` package if more identity events follow). Subscribers: `userdata-local` (delete blob), `request-media` (anonymise `requested_by` → `deleted-user`), `playback-monitor` / `playback-guard` (delete or anonymise per-user rows), BFF/admin-ui (drop sessions). Verification: cross-module fixture test in `integration/`.
