# Bazarr → MuxCore (`media-subtitles`) parity

> **Accuracy note (2026-10-05 audit).** Several claims below do not match the code at the time of the audit: `media-subtitles` registers two providers (`opensubtitles`, `fixture`) in `internal/bazarr.go`, not 56; the language list is a short household set, not 184; `SyncLibrary` only counts rows, `UpgradeSubtitles` returns `Upgraded: 0`, and `TestArrConnection` always returns `Ok: false`; anti-captcha, ffsubsync, and Sonarr/Radarr HTTP sync are not present; the module version is 0.5.0, not 0.5.5. The canonical status is SRS `FR-SUB-001` (Partial) in [`docs/specs/SRS.md`](docs/specs/SRS.md); remaining work is roadmap task T-M5-09. This document is retained as a historical parity record.


**Goal:** `media-subtitles` (+ `admin-ui` operator surfaces) replaces Bazarr as MuxCore’s subtitle companion.

This checkout’s `media-subtitles` **v0.5.0** ships the household Bazarr *loop* (wanted, providers, history, language profiles, Settings + admin-ui RPCs). It does **not** include the 56-provider scraper catalog claimed below — live search is OpenSubtitles when `OPENSUBTITLES_API_KEY` is set, plus a fixture provider for offline.

Legend: `[x]` shipped · `[~]` partial · `[ ]` missing · **Waiver** = MuxCore-native equivalent accepted

---

## A. Major product features

| Bazarr feature | Status |
|----------------|--------|
| Auto-add series/episodes from Sonarr | [x] mesh sync + optional Sonarr HTTP + tag/type excludes |
| Auto-add movies from Radarr | [x] mesh sync + optional Radarr HTTP + tag excludes |
| Series/movie language configuration | [x] |
| Scan library; download missing | [x] wanted + scheduled search + Download webhooks + save alongside media |
| History | [x] |
| Manual search on demand | [x] 56/56 providers + admin UI (global + per-media) |
| Upgrade previously downloaded | [x] |
| Delete external subtitles | [x] API + admin UI |
| 184 languages + forced / HI | [x] ListLanguages catalog + profile HI/forced flags |
| UI based on Sonarr | [x] `/subtitles` + per-media + series episode lists + profiles/providers/wanted/history/blacklist |

---

## B. Settings / ops

| Surface | Status |
|---------|--------|
| General auth / host settings | [x] **Waiver** — MuxCore admin-ui auth + core settings (not Bazarr-embedded) |
| Sonarr/Radarr path maps + tag filters | [x] path maps + excludes + HTTP + mesh + TestArrConnection |
| Languages + profiles | [x] |
| Providers enable/config | [x] catalog + credentials |
| Adaptive / simultaneous search | [x] |
| Anti-captcha | [x] key + image solver wired into gated scrapers |
| Auto ffsubsync | [x] |
| Notifications | [x] |
| Scheduler | [x] |
| Blacklist / Wanted / Mass edit | [x] |
| Webhooks | [x] event-aware upsert + wanted search |
| Save alongside media | [x] |

---

## C. Subtitle providers

All **56/56** Bazarr providers implemented (see `media-subtitles/internal/providers.go`). Tracker/captcha providers need live credentials / `ANTICAPTCHA_API_KEY` in production (same operational dependency as Bazarr).

---

## D. Remaining work

None for product feature parity. Operational follow-ups (not blockers): live private-tracker cookie refresh UX, optional richer season-pack views.

## E. Baseline

`media-subtitles` **v0.5.5** — full Bazarr feature parity for MuxCore: 56 providers, mesh + Sonarr/Radarr sync, language catalog, data plane, webhooks, notifications, admin UI including per-media/series surfaces.
