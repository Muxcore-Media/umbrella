# MuxCore — Software Requirements Specification (SRS)

| | |
|---|---|
| **Status** | Active — v1.0 (2026-10-05) |
| **Owns** | The **canonical, uniquely identified, verifiable** functional (`FR-*`) and non-functional (`NFR-*`) requirements, including interface, data, security, reliability, performance, and operational constraints |
| **Rule** | Every other document references requirements **by ID**; none restates them with different wording. To change a requirement, edit it here and bump the doc version note. Never reuse a retired ID — mark it `Retired` with a pointer. |
| **Traces** | `CAP-*` ([PRD](PRD.md)) → `FR/NFR-*` (here) → design ([HLD](../architecture/HLD.md), [SDD](../architecture/SDD.md), [TDD](../architecture/TDD.md), [LLD](../architecture/lld/README.md), [ADRs](../adr/README.md)) → milestone/task ([roadmap](../../MASTER-ROADMAP.md)) → verification (column below) |

**Column legend**

- **Pri** — `M` Must, `S` Should, `C` Could, `W` Won't-now.
- **Status** — `Impl` implemented with evidence · `Part` partial · `Scaf` scaffold · `Plan` planned · `Def` deferred · `Blk` blocked. Evidence is from the 2026-10-05 audit; it describes code at the umbrella-pinned SHAs, **which did not all build** at audit time (see `NFR-MNT-001`).
- **Verify** — `E:` names a test or check that **exists in the repo** (not necessarily run or passing); `P:` is a **proposed** check that a delivery task must create. Only checks listed in the roadmap's "Validation log" have actually been run.
- **MS** — roadmap milestone that delivers or closes the requirement (`—` = already met; `D` = deferred bucket).

---

## 1. Functional requirements

### 1.1 Install, configure, upgrade — CAP-01

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-INS-001 | The system shall be installable on a single Linux host from documented steps using only artefacts reachable from GitHub (`Muxcore-Media/*`) or a LAN OCI registry populated from them. | M | Part — root `docker-compose.yml` now includes `_mvp/docker-compose.registry.yml` (config validated); clean-room install job pending | P: clean-room container install script runs smoke | M2 |
| FR-INS-002 | The canonical module set for a working household shall be defined in one manifest consumed by every install path. | M | Impl — `_mvp/household-manifest.yaml` used by `run-host.sh`, registry compose, `publish-module-images.sh` | E: `_mvp/scripts/check-household-manifest.sh` | — |
| FR-INS-003 | Every install path shall default to fixture acquisition (`DOWNLOADER_ENGINE=fixture`, `INDEXER_FIXTURE=1`) and require explicit operator action to enable live acquisition. | M | Part — defaults in compose/run-host; native torrent engine defaults to live when unset | P: compose lint asserts fixture env; unit test that engine defaults to fixture when env unset in household profile | M3 |
| FR-INS-004 | No install path shall ship a fixed default credential; first-admin bootstrap shall use an operator-supplied or randomly generated one-time secret. | M | Impl — run-host/setup generate a one-time admin secret (printed once), registry compose requires `MVP_ADMIN_PASSWORD`, installer wizard generates one | P: `check-compose-*` test greps for known defaults; installer test | M3 |
| FR-INS-005 | Upgrading shall be a single version-pin change (`MUXCORE_IMAGE_TAG` or installer pin) with a documented rollback to the previous pin. | M | Part — `scripts/deploy-atomic.sh` rollback for binaries; no container data-migration/downgrade doc | E: `_mvp/scripts/deploy-atomic_test.sh`; P: upgrade/rollback drill script | M2 |
| FR-INS-006 | All install paths shall pin a single, consistent core version per release train. | M | Impl — `release-train.env` (train-2026.10.1: core and images v0.6.7, spool catalog 2.5.0) consumed by `_mvp` compose/Helm/kustomize/manifest, installer v0.3.6, operator v0.1.2, starter v0.1.2; `scripts/check-pins.sh` ok (2026-10-05) and in umbrella CI | P: `check-pins` script compares pins across `_mvp`, installer, operator, starter | M2 |
| FR-INS-007 | The dev host path (`_mvp/run-host.sh`) shall support start, stop-one, restart, status for every module. | S | Impl — `run-host.sh` subcommands | E: `_mvp/scripts/run-script-tests.sh` (partial) | — |

### 1.2 Identity and access — CAP-02

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-AUTH-001 | Users shall authenticate with password (bcrypt) and optionally TOTP and WebAuthn passkeys. | M | Impl — `auth-local/internal` | E: auth-local tests (128 `Test*`) | — |
| FR-AUTH-002 | Authorisation shall be role-based with roles `admin`, `manager`, `user`, `viewer`, `module` defined in a reloadable policy file that fails closed when empty. | M | Impl — `auth-local/policies.yaml`, SIGHUP reload | E: auth-local policy tests | — |
| FR-AUTH-003 | Admins shall create, list, and revoke invites carrying role, max uses, and expiry; tokens stored hashed. | M | Impl — `internal/store/invites.go`, admin-ui `/invites` | E: auth-local invite tests; admin-ui `handler/invites` tests | — |
| FR-AUTH-004 | Invite redemption shall create an account with the invite's role and reject revoked, expired, or exhausted invites without revealing which. | M | Impl — `/invite/redeem`, BFF `/api/invite/*` | E: auth-local tests; P: fixture e2e J-02 | M2 |
| FR-AUTH-005 | Web apps shall establish sessions via one-time code exchange with auth-local and issue their own HttpOnly cookie. | M | Impl — BFF `auth_exchange.go`, admin-ui `/auth/callback` | E: BFF + admin-ui handler tests | — |
| FR-AUTH-006 | Admins shall create, rotate, and revoke scoped API tokens; only token hashes are stored. | M | Impl — `CreateAPIToken`, admin-ui `/keys` | E: auth-local tests | — |
| FR-AUTH-007 | Admins shall view and revoke **all** active sessions/devices for any user (not only admin-ui sessions). | S | Part — admin-ui `/devices` lists own sessions only | P: admin-ui handler test against auth-local session list RPC | M5 |
| FR-AUTH-008 | OIDC login (Keycloak/Authentik/Dex) shall be supported with role mapping from claims. | S | Impl — `auth-oidc` PKCE + `AUTH_OIDC_ROLE_MAP` | E: auth-oidc tests incl. `keycloak_compose_test.go` (Docker-gated) | — |
| FR-AUTH-009 | When the identity provider does not support an operation (user CRUD, TOTP, tokens, invites under `auth-oidc`), admin-ui shall hide or disable the page with an explanation instead of failing. | S | Plan | P: admin-ui test with OIDC stub returning `Unimplemented` | M5 |
| FR-AUTH-010 | Household profiles: one account may hold multiple viewer profiles (incl. kids profiles with PIN) with separate userdata and parental settings. | S | Plan — no profile switcher in `media-ui-app` | P: BFF + SPA tests | M4 |

### 1.3 Discover and request — CAP-03

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-REQ-001 | Users with `media.request` shall search library and metadata (TMDB) in one query and request unavailable titles. | M | Impl — `Search.tsx`, `lib/unified-search.ts`, BFF `/api/search`, `/api/request` | E: `media-ui-app` page tests; request-media tests | — |
| FR-REQ-002 | Requests shall follow the state machine in [FRD WF-04](FRD.md#wf-04-request-lifecycle-j-03). | M | Impl — `request-media/internal/approval.go` | E: request-media tests (41) | — |
| FR-REQ-003 | Approval/denial shall require a privileged role; configured users may be auto-approved. | M | Impl — `approval.go`, `REQUEST_AUTO_APPROVE_USERS` | E: request-media tests | — |
| FR-REQ-004 | Per-user quotas (max pending, max per week) shall be enforced at request creation. | S | Impl — `reqquota` | E: request-media tests | — |
| FR-REQ-005 | Requesters shall be notified once when a requested item becomes available. | M | Impl — `request_ready_notified` | E: request-media tests | — |
| FR-REQ-006 | Requests for music, books, comics, and audiobooks shall enter the same lifecycle and reach `media-automation`. | S | Plan — `media.music.requested` has no subscriber | P: automation subscriber tests | M6 |

### 1.4 Acquisition — CAP-04

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-ACQ-001 | Wanted items shall be created from approved requests and library sync, unique per `(item_type, item_id)`. | M | Impl — `wanted_items` | E: media-automation tests (140) | — |
| FR-ACQ-002 | Search shall fan out to all registered `indexer` providers in parallel, merge, and de-duplicate by GUID; rate-limited indexers are held 15 min. | M | Impl — `parallelIndexerSearch`, `holdIndexer` | E: media-automation tests | — |
| FR-ACQ-003 | Releases shall be scored by `media-custom-formats` (`ScoreRelease`) with a deterministic local fallback when the scorer is unavailable. | M | Impl — `scoreWithFormatsFallback` | E: tests in both modules | — |
| FR-ACQ-004 | Operators shall sync TRaSH-style custom-format packs from a vendored fixture offline, and optionally from the official upstream repository. | M | Impl — `SyncTrashGuides`, `internal/guides-fixture`, `trash_official.go` (opt-in) | E: media-custom-formats tests | — |
| FR-ACQ-005 | Grab decisions shall follow RULE-ACQ-1 (min score, cutoff score, upgrade delay). | M | Impl — `decideGrab` | E: media-automation tests; P: decision-corpus test (SM-3) | M5 |
| FR-ACQ-006 | Quality profiles shall support a quality-tier cutoff ("upgrade until") in addition to score cutoff. | S | Plan | P: decision-corpus cases | M5 |
| FR-ACQ-007 | Per-protocol delay profiles shall defer grabs (RULE-ACQ-2). | S | Impl — `delay.go` | E: tests | — |
| FR-ACQ-008 | Failed and stalled downloads shall blocklist the release for the current attempt loop and return the item to search (RULE-ACQ-3, -4). | M | Impl — `release_blacklist`, `stall.go` | E: tests | — |
| FR-ACQ-009 | Import failures shall be recorded (`import_failed`), surfaced to operators, and retryable manually and once at boot. | M | Impl — `RetryImport`, `retryImportFailed` | E: tests | — |
| FR-ACQ-010 | Operators shall manually blocklist, clear blocklist, and trigger search for an item. | M | Impl — `operator_rpc.go`, admin-ui `/queue` | E: tests | — |
| FR-ACQ-011 | Torrent and Usenet shall be equal acquisition paths, each with a native engine and an external-client peer (qBittorrent, SABnzbd). | S | Part — all four modules exist; SAB/debrid described as scaffolds; `contracts-downloader` is torrent-only, Usenet uses a module-local proto | P: contract unification + fixture tests per downloader | M5 |
| FR-ACQ-012 | The fixture gate shall reject any non-fixture grab whenever fixture mode is active (RULE-ACQ-6). | M | Impl — `dispatch_guard.go` | E: tests | — |

### 1.5 Import and library — CAP-05

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-LIB-001 | `ImportPath` shall parse, probe, rename (via `media-rename` templates), place into a configured root folder, record the import, and publish `media.file.imported`. | M | Impl — `media-scanner` | E: media-scanner tests (76) | — |
| FR-LIB-002 | Movie and TV libraries shall record files on `media.file.imported` and publish file-added events. | M | Impl | E: media-movies, media-tvshows tests | — |
| FR-LIB-003 | Metadata for movies/TV shall come from TMDB, with an offline fixture corpus (`TMDB_FIXTURE=1`). | M | Impl — `metadata-tmdb` (local proto lacks 3 RPCs of `contracts-metadata`) | E: metadata-tmdb tests | — |
| FR-LIB-004 | Operators shall manage root folders, naming templates, and tags. | M | Impl — `media-root-folders`, `media-rename`, `media-tagging` | E: tests | — |
| FR-LIB-005 | Library scans shall detect new, changed, and removed files in watched roots. | M | Impl — `Scan`, `ScanLibraryRoots`, watch dirs | E: tests | — |

### 1.6 Browse and play — CAP-06

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-PLAY-001 | The consumer UI shall provide Home rails (continue watching, next up, recently added, favorites, collections, per-media shelves). | M | Impl — `pages/Home.tsx` | E: vitest page tests | — |
| FR-PLAY-002 | Users shall browse each enabled media type with filters, and open detail pages with cast, trailer, and related items. | M | Impl — `Movies.tsx`, `TVShows.tsx`, detail pages | E: vitest | — |
| FR-PLAY-003 | Playback shall resolve to direct play or HLS transcode server-side and expose audio/subtitle tracks, chapters, and intro/outro segments. | M | Impl — BFF `/api/playback/resolve`, `VideoPlayer.tsx` | E: BFF tests, vitest | — |
| FR-PLAY-004 | Progress, watched state, favorites, playlists, and queue shall persist server-side per user and survive device changes. | M | Impl — `/api/userdata`, `userdata-local` | E: tests | — |
| FR-PLAY-005 | The player shall offer resume, up-next, skip intro/outro (with optional auto-skip), keyboard shortcuts, and gestures. | M | Impl — `components/player/*` | E: vitest | — |
| FR-PLAY-006 | Parental controls (max rating, blocked/allowed tags, PIN, unrated handling) shall be configurable per account/profile. | M | Impl (settings) — `lib/parental.ts` | E: vitest | — |
| FR-PLAY-007 | Parental restrictions shall be enforced **server-side** on every list, search, detail, and playback API for the restricted principal. | M | Part — movie/TV list, library list, collection, and detail use catalog genres plus `content_rating` (`_mvp` parental.go). Playback still trusts caller query tags. Remote `/api/search` has no certification. | P: BFF table test across all library/search routes | M4 |
| FR-PLAY-008 | Near-term, the UI shall offer Jellyfin playback handoff when the bridge is enabled ([ADR-0005](../adr/0005-consumer-ui-replaces-jellyfin-web.md)). | S | Impl — `/api/jellyfin/play` | E: BFF tests; smoke soft check | — |
| FR-PLAY-009 | Cast/AirPlay controls shall either work (Remote Playback API / Cast SDK) or not be shown. | S | Part — buttons shown; only browser Remote Playback API | P: vitest asserting control hidden when unsupported | M4 |
| FR-PLAY-010 | Music, book (PDF/EPUB), comic, and audiobook items shall be consumable in-app. | S | Part — `AudioPlayerBar`, inline readers, `audiobookPlayback.ts` | E: vitest | M6 |
| FR-PLAY-011 | The consumer UI shall support localisation (string catalogue, locale switch); English is the only required locale. | C | Plan — no i18n | P: lint for hard-coded strings in new code | D |

### 1.7 Operator control center — CAP-07

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-ADM-001 | admin-ui shall show module list/health, cluster, events, and a health dashboard. | M | Impl — `/modules`, `/dashboard/health`, `/events` | E: admin-ui tests (349) | — |
| FR-ADM-002 | admin-ui shall show **one** wanted/missing view across all enabled media types. | M | Part — movie/TV only in `/wanted`; separate `/music/wanted` | P: handler test with all six media capabilities | M5 |
| FR-ADM-003 | admin-ui shall show a unified calendar of upcoming releases. | M | Impl — `/calendar` | E: tests | — |
| FR-ADM-004 | admin-ui shall list queue, activity, and failed/stuck downloads with remove, retry-import, and blocklist actions. | M | Impl — `/queue`, `/activity`, `/failed-downloads` | E: tests | — |
| FR-ADM-005 | admin-ui shall manage users, roles, invites, API keys, TOTP, passkeys, and parental settings. | M | Impl — `/users`, `/invites`, `/keys` | E: tests | — |
| FR-ADM-006 | admin-ui shall provide indexer and download-client management pages (add, edit, test, enable/disable). | M | Plan — read-only peer lists in `/automation` | P: handler tests against `IndexerService` CRUD + downloader capabilities | M5 |
| FR-ADM-007 | admin-ui shall manage module settings generically via the `settings` capability. | M | Impl — `/settings` | E: tests; `docs/SETTINGS-COVERAGE.md` | — |
| FR-ADM-008 | admin-ui shall provide a transcoder queue and profile management page (issue #121). | S | Part — `/transcode` profiles/review; pool profile RPCs `Unimplemented` | P: handler tests; pool RPC tests | M5 |
| FR-ADM-009 | admin-ui authorisation shall be per page/action by permission so `manager` can use request and library pages. | S | Plan | P: handler table test per role | M5 |
| FR-ADM-010 | Operator-only settings shall live in admin-ui; the consumer Settings page shall contain only personal settings. | S | Part — `media-ui-app/Settings.tsx` (7,035 lines) includes operator sections | P: vitest asserting operator sections gated/removed | M4 |
| FR-ADM-011 | admin-ui shall show logs from module log files and the event ring. | S | Part — file tail viewer | E: tests | — |

### 1.8 Multi-media — CAP-08

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-MM-001 | Music shall have a metadata source (MusicBrainz) via a buildable `metadata-musicbrainz` module. | S | Impl — `metadata-musicbrainz` v0.1.0 (WS/2 client, `MUSICBRAINZ_FIXTURE=1` corpus, gRPC fixture e2e test); `media-music` v0.3.0 builds standalone against it | P: module restored + tests | M1 (restore) / M6 |
| FR-MM-002 | `media-automation` shall consume requests and wanted items for music, books, comics, audiobooks. | S | Part — wanted sync exists for music/books; no `media.music.requested` subscriber | P: subscriber tests | M6 |
| FR-MM-003 | Indexer queries shall be shaped per media type (categories, author/title, issue numbers). | S | Part — only movie/tv/music search types | P: automation + indexer tests | M6 |
| FR-MM-004 | Library-plus imports shall move/rename into root folders like video imports, not register in place. | S | Plan | P: library-plus import tests | M6 |
| FR-MM-005 | Books, comics, audiobooks shall have metadata providers that work offline with fixtures (e.g. Open Library, ComicVine — operator keys optional). | C | Plan | P: provider fixture tests | M6 |

### 1.9 Migration — CAP-11

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-MIG-001 | Import library catalogues from Sonarr, Radarr, and Lidarr via their APIs. | M | Impl — `admin-ui/arrmigrate` | E: arrmigrate tests | — |
| FR-MIG-002 | Import quality profiles **with** scores/custom formats, root folders, tags, indexers, and download clients. | M | Part — profile names only | P: fixture Arr API tests (SM-5) | M5 |
| FR-MIG-003 | Migration shall offer a dry-run preview and be idempotent on re-run. | S | Plan | P: tests | M5 |
| FR-MIG-004 | Import lists (Trakt, IMDb, Plex, Jellyfin, Radarr, Sonarr) shall sync on a schedule. | S | Impl — `media-list-sync` | E: media-list-sync tests | — |

### 1.10 Backup and restore — CAP-13

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-BAK-001 | Scheduled archives with checksum and retention shall be created of configured state. | M | Impl — `backup-local` | E: backup-local tests | — |
| FR-BAK-002 | Every stateful module in the household manifest shall be covered by backup (via `ExportState` or a declared data directory). | M | Impl — manifest `state:` model + `check-state-coverage.sh` in mvp CI (ADR-0013); drill restores 18 modules | P: manifest-driven coverage check | M2 |
| FR-BAK-003 | Restore shall only target an allow-listed restore directory from every UI/API (RULE-VAL-1). | M | Impl — admin-ui v0.1.15 (`handler/restore_path_test.go`) | P: admin-ui handler test | M3 |
| FR-BAK-004 | A restore drill (backup → fresh install → restore → smoke) shall be documented and automated on fixtures. | M | Impl — `_mvp/scripts/restore-drill.sh` + `docs/RESTORE.md`; host mode passed 2026-10-05; compose mode passed 2026-10-05 in `scripts/clean-room.sh --restore-drill` (16 modules restored); CI run (T-M2-10) pending B-1 | P: drill script in CI | M2 |

### 1.11 Extensibility — CAP-12

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-EXT-001 | Modules shall register capabilities with core and be discoverable by capability and role. | M | Impl — `registry.go`, `Discovery` | E: core grpcmesh tests | — |
| FR-EXT-002 | Marketplace deploy shall verify repo host allow-list, checksum, publisher, and (when required) signature before running a module. | M | Impl — `SpoolService.DeployTag`, `spool/signature.go`; checksums reproducible since core v0.6.5 ([ADR-0012](../adr/0012-reproducible-module-builds-for-spool-checksums.md)); spool catalog 2.5.0 recomputed and re-checked byte-identical | E: core spool tests + fuzz, `mgr` reproducibility test, `_mvp/scripts/populate-spool-checksums_test.sh` | — |
| FR-EXT-003 | The household profile shall require signed modules for marketplace deploys. | S | Impl — core v0.6.10: household requires signatures for marketplace DeployTag and orphan resurrection | P: compose/installer lint | M3 |
| FR-EXT-004 | A module starter template shall generate a buildable module with CI against the current core. | S | Impl — `muxcore-module-starter` | E: `scripts/check-cookiecutter.sh` | — |

### 1.12 Bridges, analytics, subtitles, transcoding, native — CAP-09, 10, 14, 15

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| FR-BRG-001 | Jellyfin, Plex, and Emby bridges shall sync library links and act as playback session sources. | S | Impl — jellyfin v0.3.1, plex v0.1.1, emby v0.1.1 build and test green (contracts-playback/playbackv1) | E: module tests once buildable | M1 |
| FR-ANA-001 | Playback sessions, history, and statistics shall be recorded and viewable (Tautulli-style). | C | Impl — `playback-monitor` | E: tests (73) | — |
| FR-ANA-002 | Account-sharing rules shall flag violations and optionally terminate streams. | C | Impl — `playback-guard` | E: tests | — |
| FR-SUB-001 | Subtitles shall be searched and downloaded from configured providers per language profile; `fixture` provider works offline. | S | Part — `opensubtitles`, `fixture` only | E: media-subtitles tests | M5 |
| FR-SUB-002 | `BAZARR-PARITY.md` claims shall match code. | S | Impl — BAZARR-PARITY.md carries an accuracy note | P: doc corrected (M0 doc task) | M0 |
| FR-TRN-001 | Transcode pipelines (setups, filters, review) shall run locally or on a worker pool. | S | Part — `media-transcoder`; pool profile RPCs `Unimplemented` | E: transcoder tests (64) | M5 |
| FR-NAT-001 | iOS and tvOS apps shall sign in via BFF (Quick Connect, password+TOTP) and browse/play/resume. | S | Part — thin tests | E: `MuxCoreAPITests.swift`, `muxcore-ios/Tests` | M8 |
| FR-NAT-002 | tvOS app shall be validated on physical Apple TV hardware. | S | Blk — needs device | E: `media-tvos-app/docs/HARDWARE-VALIDATION.md` checklist | M8 |
| FR-NAT-003 | An Android client shall exist with parity to iOS for browse/play/resume. | C | Blk — GitHub repo empty | P: — | M8 |
| FR-AI-001 | AI modules (recommend, librarian, subtitles, tickets, filter) shall use `ai-runtime` providers. | W | Scaf | — | D |

## 2. Non-functional requirements

### 2.1 Security — NFR-SEC

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| NFR-SEC-001 | No shipped configuration contains a usable default password or token (admin bootstrap, `PLAYBACK_MONITOR_HTTP_TOKEN`, etc.). | M | Impl — `scripts/check-default-creds.sh` (+test) ok over umbrella and active modules, in umbrella CI | P: repo-wide lint in CI for known default literals | M3 |
| NFR-SEC-002 | Registration of security-critical capabilities (`call.policy`, `publish.policy`, `authorizer`, `identity`, `auth`) shall require an authenticated module identity (mTLS client cert bound to module ID, or single-use bootstrap token). | M | Impl — verified-certificate module principals, CN-checked Register/Unregister, exclusive security capabilities, CSR enrollment with single-use tokens (core v0.6.9–v0.6.15; ADR-0017, ADR-0022); household compose run registers all 26 services with certificates | P: core grpcmesh tests | M3 |
| NFR-SEC-003 | Mesh traffic shall use TLS (mTLS where module identity matters) in every non-dev profile; `MUXCORE_INSECURE_DISABLE_TLS` is allowed only in the dev profile and logs a warning. | M | Impl — household is the default for the registry compose and the installer (TLS everywhere, automatic core CA, insecure flags only in the dev override / `--dev`); verified by the household clean-room run and installer runs | P: compose lint; admin-ui dial helper test | M3 |
| NFR-SEC-004 | Browser-facing state-changing endpoints shall be protected against CSRF (token or strict Origin check) and use POST/PUT/PATCH/DELETE only. | M | Impl — BFF Origin/Referer CSRF for cookie sessions + `POST /logout` (`_mvp` 831491f, `http_hardening_test.go`); admin-ui double-submit CSRF | P: BFF middleware tests | M3 |
| NFR-SEC-005 | Session and secret material at rest (session files, TOTP secrets) shall be stored hashed or encrypted, with file mode 0600, outside temp directories by default. | M | Impl — auth-local v0.1.19 encrypts TOTP secrets (AES-256-GCM) and stores session tokens as SHA-256 hashes, with in-place migration; admin-ui and BFF sessions hashed/encrypted 0600 | P: tests | M3 |
| NFR-SEC-006 | HTTP servers shall set read/write/idle timeouts and security headers (CSP, frame options, nosniff). | M | Impl — BFF timeouts (streams lifted per request), CSP, nosniff, frame-ancestors, Referrer-Policy (`_mvp` 831491f); admin-ui headers | P: tests | M3 |
| NFR-SEC-007 | Services shall not trust caller identity headers (`X-Caller-Id`, `X-Tenant-ID`) from unauthenticated peers. | M | Impl — BFF/admin-ui forward bearer and strip identity headers; request-media v0.3.4 and jellyfin v0.3.2 resolve the bearer; core `pkg/tenant` ignores tenant headers unless `TENANT_TRUST_HEADERS=1` (ADR-0019/0020) | P: tests | M3 |
| NFR-SEC-008 | User-supplied paths shall be confined to allow-listed roots (RULE-VAL-1). | M | Impl — core sdk/go/module v0.6.6 `pathguard`; confinement shipped in media-transcoder v0.3.11, media-subtitles v0.5.5, media-root-folders v0.1.12, media-scanner v0.1.34, media-rename v0.2.12, media-list-sync v0.1.13, media-movies v0.1.21, media-tvshows v0.1.20, downloader-native-torrent v0.3.16, downloader-qbittorrent v0.1.5, admin-ui restore; media-books (`7ce670a`), media-comics (`3a43e09`), media-audiobooks (`709d315`), library-maintainer move path (`28a6826`), backup-local (`953c14b`), media-music (`ab1507c`), media-ffprobe (`f23a1f6`), media-custom-formats (`5abf997`); household compose and run-host set the allow-lists (`_mvp` `1b95f43`, earlier `c6e8829`). Compose media-ffprobe has no library mount, so it stays fail-closed until roots are mounted and set | P: tests | M3 |
| NFR-SEC-009 | Outbound requests to user-configured URLs shall be SSRF-guarded (RULE-VAL-2). | M | Impl — core sdk/go/module v0.6.6 `netguard` (dial-time + redirect checks); shipped in notification-default/apprise, playback-monitor, scheduler-cron, media-list-sync, media-subtitles, media-movies/tvshows artwork, torrent/qbittorrent/sabnzbd; BFF playback `src` is confined to `/stream/movies/` and `/stream/tv/` and transcoder HTTPS presents the mesh client cert (`_mvp` 66a1118); BFF and admin-ui arr migrate (`admin-ui` e80fd24); jellyfin (`d0030b1`), plex (`13251c1`), emby (`a3ee7e4`), metadata-tmdb (`5d9c0a1`), ai-runtime (`8feb268`), auth-oidc (`8d26400`), media-library-maintainer (`28a6826`), indexer-torznab (`5e365b6`), indexer-piratebay (`e6c6cb8`), downloader-native-usenet (`7f232c9`); core spool fetcher (`c94f972`) | P: audit + tests | M3 |
| NFR-SEC-010 | Live acquisition shall be impossible unless the operator explicitly enables it and provides VPN config (fail-closed). | M | Impl — downloader-native-torrent v0.3.11 (fixture by default, live needs `WG_CONF`, fail closed) + media-automation v0.1.48 guard fails closed | P: engine default test | M3 |
| NFR-SEC-011 | Unauthenticated HTTP control endpoints (scheduler-cron `:9200`, health-monitor `/status`, core `/metrics` when enabled) shall require auth or bind to loopback by default. | M | Impl — scheduler-cron v0.1.7, health-monitor v0.1.7 (loopback default, bearer token off-loopback) and core v0.6.8 (`/metrics` on `127.0.0.1:9464`) | P: tests | M3 |
| NFR-SEC-012 | Secrets shall never be committed; CI shall scan for secrets. | M | Impl — gitleaks job in umbrella CI | P: secret-scan workflow | M0 |
| NFR-SEC-013 | Dependencies shall be monitored for vulnerabilities (Dependabot + `govulncheck`). | S | Part — Dependabot active on GitHub; `govulncheck` in core CI | E: Dependabot runs; P: umbrella vuln job | M1 |

### 2.2 Reliability and data — NFR-REL / NFR-DATA

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| NFR-REL-001 | Optional peers shall degrade softly; absence of an optional module never breaks required journeys (RULE-VAL-4). | M | Impl (BFF library-plus); unverified elsewhere | P: smoke with optional modules off | M2 |
| NFR-REL-002 | A required module crash shall be detected by core health checks and reported as `degraded` within one health interval. | M | Impl — `StartHealthCheckLoop`, `module.degraded` | E: core tests; smoke `ReportHealth` check | — |
| NFR-REL-003 | Consumer sessions shall survive a BFF restart. | S | Impl — BFF sessions persisted in `MEDIA_UI_USERDATA_DIR/sessions.json` (`session_store_test.go` restart test) | P: BFF test with persisted store | M3 |
| NFR-REL-004 | Acquisition state transitions (dispatch, import, retry) shall be idempotent under event redelivery. | M | Part — history/status guards exist; not systematically tested | P: redelivery tests | M5 |
| NFR-DATA-001 | Each module owns its data store; no cross-module database access or foreign keys ([ADR-0009](../adr/0009-per-module-data-ownership.md)). | M | Impl | E: code structure | — |
| NFR-DATA-002 | Schema changes shall be forward-only, idempotent at startup, and covered by an upgrade-from-previous-release test for the household modules. | M | Part — inline `CREATE IF NOT EXISTS` / guarded `ALTER`; no upgrade tests | P: per-module migration test | M2 |
| NFR-DATA-003 | Deleting a user shall delete or anonymise their userdata, requests, sessions, and playback history. | S | Plan — unverified | P: cross-module deletion test | M4 |

### 2.3 Performance and capacity — NFR-PERF

No performance requirement has been approved by the owner. The following are **[Proposed]** design targets for a single-household reference host (≈ 10 concurrent users, ≤ 20 k library items); they are not commitments.

| ID | Target | Pri | Status | Verify | MS |
|----|--------|-----|--------|--------|----|
| NFR-PERF-001 | BFF library list APIs respond in < 500 ms p95 at 20 k items on the reference host. | C | Unmeasured | P: benchmark with fixture library | D |
| NFR-PERF-002 | Core mesh call overhead < 5 ms p95 on loopback. | C | Unmeasured — benchmarks exist (`#17`) | E: core benchmark suite | D |
| NFR-PERF-003 | A full stack (household manifest) runs within 2 GiB RAM idle. | C | Part — measured 2026-10-05 by `scripts/clean-room.sh`: 469.9 MiB summed process RSS over 27 containers (required + recommended set, fixture acquisition peers, backup-local), idle 20 s after smoke; report-only by default (`--enforce-rss`) | P: clean-room RSS step (`--enforce-rss` in `.github/workflows/clean-room.yml`) | M2 |

### 2.4 Operability and observability — NFR-OPS

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| NFR-OPS-001 | Every module shall expose a health signal consumed by core and visible in admin-ui. | M | Impl | E: core + admin-ui tests | — |
| NFR-OPS-002 | Logs shall be structured (slog JSON) with configurable level and redaction. | S | Impl — shared slog contract (#15), `logging-file` | E: tests | — |
| NFR-OPS-003 | Metrics (Prometheus) and traces (OTLP) shall be available when enabled; a starter Grafana dashboard ships in-repo. | C | Part — no dashboards | P: dashboard JSON lint | D |
| NFR-OPS-004 | Core audit log shall be tamper-evident (hash chain; HMAC when a key is configured). | S | Impl — core v0.6.8: HMAC-SHA256 over previous hash + entry when `MUXCORE_AUDIT_HMAC_KEY[_FILE]` is set; `audit.VerifyEntries` | P: core test (`internal/audit`) | M3 |
| NFR-OPS-005 | Configuration shall be documented in one reference generated or checked against code. | S | Impl — `scripts/check-env-docs.mjs` (Go getenv-style reads in active modules + `_mvp` `MVP_*`/`SMOKE_*`) ok: 898 documented / 902 read, 4 allowlisted; 44 phantoms removed incl. `BACKUP_EXCLUDE`; generated rows need a human description pass | P: env-var drift check script | M2 |

### 2.5 Accessibility and UX — NFR-A11Y

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| NFR-A11Y-001 | `media-ui-app` and `admin-ui` shall meet WCAG 2.1 AA **[Proposed]** for core journeys (keyboard, focus, contrast, labels, reduced motion). | S | Part — focus-visible, reduced motion, aria in ~109 files; no automated checks | P: axe checks in vitest (`vitest-axe`) and admin-ui `templ/a11y_test.go` | M4 |
| NFR-A11Y-002 | Layouts shall work from 375 px to 1920 px widths. | S | Part — jsdom breakpoint test only | E: `breakpoints.test.tsx`; P: Playwright viewport snapshots | M4 |

### 2.6 Maintainability, build, and delivery — NFR-MNT

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| NFR-MNT-001 | Every active (non-archived) Go module shall `go build`, `go vet`, and `go test` green both **standalone** (published dependency versions) and in the **umbrella workspace** at the pinned SHA. | M | Impl (standalone) — every active Go module green standalone (build/vet/test −race, lint) at its 2026-10-05 tag, incl. the M1 tail and `_mvp`; workspace leg blocked by B-1 (umbrella workspace job skips) | E: validation log in roadmap; P: umbrella workspace CI job | M1 |
| NFR-MNT-002 | `media-ui-app` typecheck, tests, and build shall pass. | M | Impl — typecheck/1,108 tests/build green (2026-10-05) | E: `npm run typecheck`, `npm test`, `npm run build` | M1 |
| NFR-MNT-003 | Umbrella scripts tests and lint shall pass. | M | Impl — 60/60 node tests, eslint, shellcheck green | E: `node --test scripts/*.test.mjs`, `npx eslint` | M1 |
| NFR-MNT-004 | The root `integration/` suite shall compile and run in CI with the `integration` tag. | M | Part — compiles against published core v0.6.4 and sibling `integsupport` packages (C-15); suite runs locally with `-tags integration -race`; CI job blocked by B-1 | P: CI job | M1 |
| NFR-MNT-005 | Each repository shall have GitHub Actions CI on GitHub-hosted runners that builds, vets, tests, and lints on PR and push to `main` ([ADR-0003](../adr/0003-ci-on-github-hosted-runners.md)). | M | Part — GitHub-hosted template CI in every active repo (`scripts/sync-ci.sh --check` ok); ~60 repos lack `MUXCORE_CI_TOKEN` so their runs fail at the private fetch (roadmap B-1) | P: workflow present + green run | M0 |
| NFR-MNT-006 | No repository content shall reference the retired self-hosted origin (its host, runners, or mirrors) except historical records under `docs/history/` and decision records under `docs/adr/` ([ADR-0002](../adr/0002-github-sole-origin.md)). | M | Impl — `scripts/check-retired-origin.sh` ok over all active content (2026-10-05) | P: `rg` check in umbrella CI | M0 |
| NFR-MNT-007 | Module `go.mod` files shall not contain filesystem `replace` directives; local multi-module development uses the umbrella `go.work` ([ADR-0011](../adr/0011-go-workspace-and-published-versions.md)). | M | Impl — `scripts/check-no-replace.sh` ok (2026-10-05) | P: CI grep check | M1 |
| NFR-MNT-008 | Commits on `main` shall follow Conventional Commits. | S | Impl — `scripts/lint-commits.sh` | E: `lint-commits_test.sh` | — |
| NFR-MNT-009 | Every umbrella submodule URL shall resolve to a non-empty GitHub repository containing the pinned SHA. | M | Impl — `scripts/check-submodules.sh` green; empty repos removed; `metadata-musicbrainz` rebuilt on `main` (v0.1.0) | P: `scripts/check-submodules.sh` in CI | M0 |

### 2.7 Cost — NFR-COST

| ID | Requirement | Pri | Status | Verify | MS |
|----|-------------|-----|--------|--------|----|
| NFR-COST-001 | CI shall fit the GitHub Free plan for private repos: path filters, `concurrency` cancel-in-progress, no scheduled jobs except weekly dependency/vuln checks, macOS jobs manual-only. | M | Plan | P: workflow lint | M0 |
| NFR-COST-002 | No feature shall require a paid third-party service (BC-1). | M | Impl (policy) | review | — |

## 3. External interfaces (summary)

Interface **contracts** are owned by code (proto files and Go contract packages); this table records which interfaces are normative. Details: [LLD](../architecture/lld/README.md).

| Interface | Normative source | Consumers |
|-----------|------------------|-----------|
| Core gRPC (`ModuleRegistration`, `Discovery`, `ModuleMesh`, `Events`, `Health`, `Storage`, `Spool`, `Lifecycle`, `Audit`) on `:9090` | `core/proto/muxcore/**` | All modules, `muxcorectl-cli`, `api-rest` |
| Domain gRPC contracts | `contracts-{automation,downloader,indexer,media-admin,metadata,notification,scanner}` protos (`*.v1`) | See [SDD §3](../architecture/SDD.md#3-interface-responsibilities) |
| Domain events | `contracts-media/events`, `contracts-playback`, `contracts-ai` | Publishers/subscribers per SDD |
| Core REST facade | `api-rest` routes (`/api/v1/*`) | Operators, scripts |
| Consumer BFF HTTP | `_mvp/cmd/mediauiprox` routes; documented subset in `_mvp/BFF-API.md` | `media-ui-app`, iOS, tvOS |
| auth-local HTTP (`/login`, `/login/exchange`, `/invite/*`, `/api/invites`) | `auth-local` | BFF, admin-ui, auth-oidc (same contract) |
