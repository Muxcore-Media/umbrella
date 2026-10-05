# MuxCore — Product Requirements Document (PRD)

| | |
|---|---|
| **Status** | Active — v1.0 (2026-10-05) |
| **Owns** | Personas (`P-*`), product capabilities (`CAP-*`), user journeys (`J-*`), capability priority, scope and non-goals, product-level acceptance criteria |
| **Does not own** | Detailed behaviour (→ [FRD](FRD.md)), individual requirements (→ [SRS](SRS.md)), delivery sequencing (→ [roadmap](../../MASTER-ROADMAP.md)) |
| **Traces from** | Business outcomes `BO-*` in [BRD](BRD.md) |

Status vocabulary (used everywhere in this doc set): **Implemented** (code + tests in repo), **Partial** (some paths implemented, gaps listed), **Scaffold** (module exists, core behaviour stubbed), **Planned** (approved intent, no code), **Deferred** (explicitly out of current scope), **Blocked** (cannot proceed without an external change). Status claims cite repository evidence from the 2026-10-05 audit.

---

## 1. Personas

| ID | Persona | Goals | Primary surface |
|----|---------|-------|-----------------|
| **P-1** | Household viewer (adult or child) | Find something to watch/read/listen to; resume where they left off; never see admin | `media-ui-app` (web), `muxcore-ios`, `media-tvos-app` |
| **P-2** | Household requester | Ask for a title and know when it is ready | `media-ui-app` search + requests |
| **P-3** | Household operator / admin | Install, configure, approve, triage, back up, upgrade | `admin-ui`, `muxcorectl-cli`, installer |
| **P-4** | Module developer | Write a module against stable contracts and publish it | `muxcore-module-starter`, `core` SDK, spool |
| **P-5** | Delivery agent | Execute roadmap tasks with clear specs and checks | This doc set + `AGENTS.md` |

## 2. Product scope

### 2.1 In scope (product)

MuxCore is a **self-hosted, single-household media platform** composed of a core runtime and capability modules, with two first-party web UIs (consumer `media-ui-app`, operator `admin-ui`), first-party native clients (iOS, tvOS; Android planned), and optional bridges to Jellyfin/Plex/Emby.

### 2.2 Non-goals

| ID | Non-goal | Provenance |
|----|----------|------------|
| NG-1 | Hosted/SaaS offering, multi-household tenancy as a product feature. `core/pkg/tenant` remains scaffolding; tenant isolation is **Deferred**. | [Derived] — `tenant` package not used end-to-end (audit, core §1) |
| NG-2 | Start-page / dashboard-over-Arr products (Homarr, Dashy, Prismarr-style UIs over existing Arr backends) | [Owner] Appendix A |
| NG-3 | Bundling or recommending specific piracy indexers; live indexer/swarm testing | [Owner] Appendix E ([ADR-0008](../adr/0008-fixture-only-acquisition-gate.md)) |
| NG-4 | Paid services as hard dependencies (debrid, Usenet providers, paid metadata) | [Owner] Appendix E |
| NG-5 | Full Tautulli API compatibility (97 handlers), Tailscale, ntfy/gotify integrations | [Owner] `TAUTULLI-TRACEARR-PARITY.md` non-goals |
| NG-6 | A second playback *backend* contract (`contracts-playback` as a gRPC service) until a second backend is committed | [Owner] `MASTER-ROADMAP.md` §1 |
| NG-7 | Developing archived dump directories (`media-ui`, `media-jellyfin`, `cache-memory`, `custom-scripts`, `muxcorectl`) | [Owner] Appendix H |
| NG-8 | Telemetry/phone-home from installs | [Derived] BC-5 |

## 3. Capabilities

Priority: **Must** (required for BO-1…BO-4 / v1.0), **Should** (required for the winning claim), **Could** (valuable, not required), **Won't-now** (deferred).

| ID | Capability | Outcomes | Priority | Current status (evidence) | Primary modules |
|----|-----------|----------|----------|---------------------------|-----------------|
| **CAP-01** | Install, configure, upgrade, roll back on one machine | BO-1, BO-4 | Must | **Partial** — `_mvp/run-host.sh` (dev host) and `_mvp/docker-compose.registry.yml` (Path A) exist; root `docker-compose.yml` includes a non-existent `_mvp/docker-compose.public.yml`; GHCR compose incomplete (13 services); installer pins core v0.5.0 vs stack v0.5.7 | `_mvp`, `muxcore-installer`, `muxcorectl-cli` |
| **CAP-02** | Identity & household access: login, invites, roles, 2FA/passkeys, API tokens, optional OIDC | BO-2, BO-4 | Must | **Implemented** in `auth-local` (bcrypt, sessions, invites, TOTP, WebAuthn, `policies.yaml` roles); `auth-oidc` **Partial** (user/TOTP/token RPCs `Unimplemented`) | `auth-local`, `auth-oidc`, `admin-ui`, BFF |
| **CAP-03** | Discover & request with approval and quotas | BO-2 | Must | **Implemented** — `request-media` statuses, BFF `/api/request*`, `media-ui-app` `Search.tsx`, `InProgress.tsx`, approval gated by role | `request-media`, `metadata-tmdb`, `media-ui-app` |
| **CAP-04** | Acquisition automation: wanted, indexer search, release scoring (custom formats, TRaSH packs), grab, upgrade-until, blocklist, stall handling | BO-3 | Must | **Implemented for movies/TV** (fixture-proven): `media-automation` (`decideGrab`, `release_blacklist`, `stall.go`), `media-custom-formats` (`ScoreRelease`, `SyncTrashGuides`). Quality-tier cutoff absent (score-only) | `media-automation`, `media-custom-formats`, `indexer-*`, `downloader-*` |
| **CAP-05** | Import & library management: scan, rename, root folders, metadata, tags | BO-1, BO-3 | Must | **Implemented** for movies/TV (`media-scanner` `ImportPath`, `media-rename`, `media-root-folders`, `metadata-tmdb`, `media-tagging`) | `media-scanner`, `media-movies`, `media-tvshows` |
| **CAP-06** | Browse & play in the consumer UI: home rails, search, detail, player (OSD, tracks, subtitles, resume, intro skip, transcode), userdata, parental controls | BO-2, BO-6 | Must | **Partial** — rich SPA (`media-ui-app`, ~1,100 tests) and player (`VideoPlayer.tsx`, HLS); gaps: multi-profile/kids profiles missing, parental filtering client-side except `/api/playback/resolve`, Cast/AirPlay stubs, no i18n, no automated a11y checks | `media-ui-app`, `_mvp/cmd/mediauiprox`, `userdata-local`, `media-transcoder` |
| **CAP-07** | Operator control center: modules/health, queue, activity, calendar, wanted/missing, failed downloads, users/invites/keys, settings, logs | BO-5 | Must | **Partial** — `admin-ui` has most pages (`handler/handler.go`); gaps: no indexer/downloader management pages, unified wanted covers movie/TV only, devices page lists admin-ui sessions only, no per-page RBAC, transcoder queue (issue #121) | `admin-ui`, `health-monitor` |
| **CAP-08** | Multi-media libraries: music, books, comics, audiobooks with acquire→import | BO-7 | Should | Music **Partial** (depends on empty `metadata-musicbrainz` mirror); books **Partial**; comics/audiobooks **Scaffold** (no metadata source; register-in-place import) | `media-music`, `media-books`, `media-comics`, `media-audiobooks` |
| **CAP-09** | Playback analytics & account-sharing guard (Tautulli/Tracearr-style) | BO-5 | Could | **Implemented** per `TAUTULLI-TRACEARR-PARITY.md` (26 phases) — `playback-monitor`, `playback-guard` | `playback-monitor`, `playback-guard` |
| **CAP-10** | Subtitles (Bazarr-style) and transcoding (Tdarr-style) | BO-3, BO-6 | Should | Subtitles **Partial** (2 providers: `opensubtitles`, `fixture`; `BAZARR-PARITY.md` overclaims); transcoder **Partial** (`media-transcoder` pipelines/setups; pool profile RPCs `Unimplemented`) | `media-subtitles`, `media-transcoder`, `media-transcoder-pool` |
| **CAP-11** | Migration from Sonarr/Radarr/Lidarr | BO-4 | Must | **Partial** — `admin-ui/arrmigrate` imports library catalogues and profile names only; not indexers, download clients, history, custom formats | `admin-ui`, `media-list-sync` |
| **CAP-12** | Extensibility: module SDK, starter template, spool/marketplace with checksum & signature trust | BO-4 | Should | **Implemented** (opt-in signing): `core/internal/spool`, `SpoolService.DeployTag`, `muxcore-module-starter` | `core`, `spool`, `admin-ui` |
| **CAP-13** | Backup & restore of household state | BO-4 | Must | **Partial** — `backup-local` real; `ExportState` only in a few modules; restore drill undocumented | `backup-local` |
| **CAP-14** | Native clients: iOS, tvOS, Android | BO-2, BO-6 | Should | iOS/tvOS **Partial** (SwiftUI, thin tests, unsigned IPA; tvOS hardware validation **Blocked** on device); Android **Blocked** — GitHub repo is empty | `muxcore-ios`, `media-tvos-app`, `muxcore-android` |
| **CAP-15** | Bridges to Jellyfin/Plex/Emby (library sync, play handoff, session source) | BO-2 (near-term) | Should | **Implemented** in code (`jellyfin`, `plex`, `emby`) but **not buildable** at current pins (depend on empty `playback-contract`) | `jellyfin`, `plex`, `emby` |
| **CAP-16** | AI assistants (recommendations, librarian QA, subtitle generation, ticket triage, content filter) | — | Won't-now | **Scaffold** — six `ai-*` modules, heuristic-only, not wired to `ai-runtime` | `ai-*`, `contracts-ai` |
| **CAP-17** | Observability: health, metrics, tracing, logs, audit | BO-4, BO-5 | Should | **Partial** — modules exist (`health-monitor`, `metrics-prometheus`, `tracing-otlp`, `logging-file`, core audit hash chain); no shipped dashboards/alerts; HMAC audit signing never wired | platform modules |
| **CAP-18** | DLNA serving and live TV (file-backed) | BO-2 | Could | **Partial** — `media-dlna` (~730 LOC), `LiveTV.tsx` | `media-dlna`, `media-ui-app` |

## 4. User journeys

Each journey is the product-level acceptance scenario for the listed capabilities. Detailed paths (alternate/failure) are in the [FRD](FRD.md).

| ID | Journey | Persona | Capabilities | Product acceptance criterion |
|----|---------|---------|--------------|------------------------------|
| **J-01** | First install: operator follows the documented install, opens admin-ui, creates the first admin without a default password, sees all required modules healthy | P-3 | CAP-01, CAP-02, CAP-07 | Clean-room scripted install passes smoke; no credential is shipped in defaults (SRS `FR-INS-*`, `NFR-SEC-001`) |
| **J-02** | Invite & first login: operator creates an invite; family member redeems it, sets a password (optionally a passkey), lands on Home | P-3, P-1 | CAP-02, CAP-06 | Fixture e2e: invite → redeem → session → Home renders (SRS `FR-AUTH-003..005`) |
| **J-03** | Request → watch: member searches a title not in the library, requests it, request is approved (auto or by admin), fixture acquisition grabs and imports it, member is notified and plays it, progress resumes later | P-2, P-1, P-3 | CAP-03, CAP-04, CAP-05, CAP-06 | Fixture end-to-end test green (SM-2) |
| **J-04** | Triage: a download stalls/fails or an import fails; operator sees it on one page, blocklists/retries, and the item re-enters search | P-3 | CAP-04, CAP-07 | Fixture failure injection produces an actionable row; retry path covered (SRS `FR-ACQ-008..010`, `FR-ADM-004`) |
| **J-05** | Migrate: operator points the migrate wizard at Sonarr/Radarr; library, profiles, root folders and indexers appear in MuxCore | P-3 | CAP-11 | Fixture Arr API import covers listed entities (SM-5) |
| **J-06** | Backup & restore: operator schedules backups, restores onto a fresh install, and the household state (users, libraries, requests, userdata) is intact | P-3 | CAP-13 | Restore drill script passes on fixtures (SRS `FR-BAK-*`) |
| **J-07** | Extend: operator deploys an optional module (e.g. `media-music`) from the marketplace with checksum/signature verification | P-3, P-4 | CAP-12, CAP-08 | `DeployTag` with signature required succeeds for signed module, rejects unsigned (SRS `FR-EXT-*`) |
| **J-08** | Parent sets restrictions: max rating / blocked tags / PIN on a child account; restricted titles never appear in any list or API for that account | P-3, P-1 | CAP-02, CAP-06 | Server-side filtering verified on list, search, detail, and playback APIs (SRS `FR-PLAY-007`) |
| **J-09** | Daily ops: operator opens the dashboard, sees health, calendar, queue, and wanted across media types | P-3 | CAP-07, CAP-17 | admin-ui pages render with fixture data for all enabled media types (SRS `FR-ADM-*`) |
| **J-10** | Native client: member signs in on iOS/tvOS via Quick Connect and resumes playback started on web | P-1 | CAP-14, CAP-06 | Simulator/XCTest coverage of auth + resume; physical-device validation is Blocked (hardware) |

## 5. Product-level acceptance (v1.0 "household-ready")

v1.0 is reached when **all Must capabilities are Implemented** per the SRS, journeys **J-01 … J-06 and J-08** pass as automated fixture tests in GitHub CI, and success measures SM-1, SM-2, SM-4, SM-8 are met. This is a **[Proposed]** definition set under delegation (2026-10-05); it operationalises the owner's Appendix E "Definition of done (product)".

## 6. Platform constraints affecting the product

- Web UIs must work on current evergreen browsers at phone (≥ 375 px) through desktop widths; `media-ui-app/src/layout/breakpoints.test.tsx` exercises 375/768/1280/1920 (jsdom only).
- tvOS/iOS builds need macOS runners or Xcode; GitHub-hosted macOS minutes count 10× on the Free plan (BC-4) — native CI must be manual/dispatch-triggered ([TDD](../architecture/TDD.md) §4).
- Accessibility target: WCAG 2.1 AA for `media-ui-app` and `admin-ui` (**[Proposed]**; current a11y support is partial and unverified by tooling).
