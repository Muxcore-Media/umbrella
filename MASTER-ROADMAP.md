# MuxCore — Master Roadmap (Source of Truth)

**Scope:** Entire workspace (`/home/ender/Projects/muxcore`) — core, MVP stack, all modules, CI/GHCR, ops, docs.
**Rule:** This is the **only** living workspace notes document. Open work only — delete items when done; do not keep checked-off or historical narratives.

**How to use**
- Track open work with `[ ]` (missing) or `[~]` (partial).
- When an item is finished, **delete it** from this file (do not leave `[x]`).
- Priority: **P2** (parity stretch) → **P3** (long-term).

---

## 1. Open platform work

### Playback product decision (agents: read carefully)

**End state (do not abandon):** `media-ui-app` **replaces** Jellyfin’s consumer web client — including production playback (OSD, tracks, resume, transcoder wiring). Appendix C player/parity rows stay in scope until that is true. Do **not** treat the bridge as the permanent product UI.

**Near-term production posture (until native player is household-ready):** households may play via the **`jellyfin` bridge** (deep link / JF clients) with MuxCore owning browse, request, automation, and userdata sync — so day-1 installs work without blocking on full player parity.

| Layer | Now | Destination |
|-------|-----|-------------|
| Browse / request / userdata | `media-ui-app` | `media-ui-app` |
| Production play | Jellyfin handoff OK | **`media-ui-app` player** |
| Bridge module | Keep for JF library sync / optional clients | Optional peer, not the UI end state |

`contracts-playback` stays deferred until a second playback *backend* (e.g. Plex) is committed — that is unrelated to replacing Jellyfin *web* with `media-ui-app`.

### Install product decision (locked)

**Origin / household install** is Forgejo or LAN OCI registry: `docker-compose.registry.yml` + `publish-muxcored-local.sh` + `local-registry.sh` + [`PUBLIC-INSTALL.md`](_mvp/docs/PUBLIC-INSTALL.md) (Appendix E). Fixture smoke remains the gate (`DOWNLOADER_ENGINE=fixture`).

**Public GHCR consumer mirror** stays deferred until GitHub `write:packages` exists on the public org — not an origin install blocker (same posture as deferred `contracts-playback`).

### P0 — Distribution & CI

- [ ] **Public GHCR consumer mirror** — scripts + `Dockerfile.monorepo` + `core/scripts/publish-muxcored-vault-ghcr.sh` (vault BUILD_ONLY verified 2026-08-22); **push** needs `gh auth refresh -s write:packages,read:packages,repo` then vault script or `publish-muxcored-ghcr.sh`

### P1 — Platform engineering

- [x] **Per-module backlog** — 0 open todos from the 2026-08-28 fanout (**97 of 97** modules marked complete). Canonical list: [Appendix I](#appendix-i-per-module-todos-2026-08-28-fanout).
- [x] **Wave1 closeout (2026-09-01)** — all fanout commits merged to `main` on Forgejo (including `core` tracing parent-span proto); vault soak redeployed (**69/69** running modules + `muxcorectl`); `smoke-vault-all.sh` PASS (vault HTTP + public edge + mesh).

### P2 — Consumer UI (`media-ui-app/AGENTS.md` §12)

### P2 — Native clients (documented intentional gaps)

- [ ] **`media-tvos-app`** — live validation on physical Apple TV ([`media-tvos-app/docs/HARDWARE-VALIDATION.md`](media-tvos-app/docs/HARDWARE-VALIDATION.md); [`bff-soak.sh`](media-tvos-app/scripts/bff-soak.sh) authenticated walk PASS on mux.zem.systems 2026-08-22; CI ships unsigned IPA only)

### P3 — Phase 3 / long-term

(No other open P3 rows — multi-region placement, cross-cluster storage sync, distro rootfs / vsock Firecracker microVM config, native VideoPlayer OSD + transcoder wiring, **storage-ceph** native RADOS/CephFS, and **muxcore-operator** LAN registry publish shipped 2026-08-22.)

---

## 2. Maintenance rules

1. **One file** — Update this document (roadmap + appendices); do not recreate parallel root note files or status novels.
2. **Delete when done** — Prefer small atomic items; remove the line when the PR/tag lands.
3. **Smoke is sacred** — No “Deployable” claim without `mvp/smoke.sh` PASS on fixture path.
4. **Dumps are dead** — Never open feature work in polluted archive directories (Appendix H).
5. **Wiki follows code** — After material milestones, sync Getting-Started / Deployment / Security; keep wiki Roadmap as a stub to this file.

---

## Appendices

- [Appendix A: Market research — competitive gaps](#appendix-a-market-research-competitive-gaps)
- [Appendix B: Radarr / Sonarr operator parity](#appendix-b-radarr-sonarr-operator-parity)
- [Appendix C: Jellyfin web → native UI parity](#appendix-c-jellyfin-web-native-ui-parity)
- [Appendix D: Bazarr → media-subtitles parity](#appendix-d-bazarr-media-subtitles-parity)
- [Appendix E: Working constraints](#appendix-e-working-constraints)
- [Appendix F: Vault loop improvements](#appendix-f-vault-loop-improvements)
- [Appendix G: Indexer / torrent VPN gates](#appendix-g-indexer-torrent-vpn-gates)
- [Appendix H: Archived / polluted workspace dumps](#appendix-h-archived-polluted-workspace-dumps)
- [Appendix I: Per-module todos (2026-08-28 fanout)](#appendix-i-per-module-todos-2026-08-28-fanout)

---

## Appendix A: Market research — competitive gaps

## MuxCore Market Research — Competitive Gaps

**Date:** 2026-08-20  
**Purpose:** Determine what MuxCore is lacking to become the perfect competitor to existing options.

**Verdict:** MuxCore’s thesis is right — replace the multi-app Arr stack with one modular platform. What’s missing isn’t more architecture; it’s the **household-ready product surface**, **Arr-grade decision quality**, and **trust/distribution** that make people leave Sonarr/Radarr/Seerr/Jellyfin.

---

### Competitive map

| Camp | Examples | What they optimize | Threat to MuxCore |
|------|----------|--------------------|-------------------|
| **Fragmented stack (incumbent)** | Sonarr, Radarr, Prowlarr, Lidarr, Bazarr + qBit/SAB + Seerr + Jellyfin/Plex | Depth per job, huge community, TRaSH/Recyclarr | Default choice; “good enough + guides” |
| **Unified control planes** | Prismarr, Panelarr, Streamarr | One UI over existing Arrs | Steal “single dashboard” narrative without replacing backends |
| **Unified automation (closest peer)** | Riven | Debrid + plugins + VFS → Plex/Jellyfin | Same “one system” pitch, debrid-first |
| **Request / invite layers** | Seerr, Wizarr | Family request + onboarding UX | Own the non-admin experience MuxCore must match |
| **MuxCore** | Loom + modules | One platform, multi-media, mesh, multi-quality | Wins only if day-2 ops feel *better*, not just different |

Dashboards are **not** your competition for “replace the stack.” **Servarr maturity + Seerr UX + Jellyfin playback** are. Riven is the nearest architectural peer.

---

### Gaps that block “perfect competitor” status

#### 1. Household product (vs Seerr + Wizarr + Jellyfin)

Homelabbers don’t leave Arr for architecture. They leave when **family can request, watch, and stay out of admin**.

Appendix C (Jellyfin web parity) shows the hole:

- No durable **continue watching / favorites / watched / resume**
- Weak **global search**, home rows, collections, playlists, queue
- Player is basic `<video>` (no mature OSD, tracks, intro skip wired to UX)
- No **invite / onboarding** (Wizarr’s job)
- Request flow exists but lacks Seerr-class **approval, permissions, discovery, watchlists**

**Must-have to compete:** Seerr-grade request/discovery + Jellyfin-grade userdata/playback *or* a rock-solid “use Jellyfin for play, MuxCore for everything else” story with zero friction.

#### 2. Arr decision quality (vs Sonarr/Radarr + TRaSH)

The automation loop for movies/TV is real and hardening. What’s still below market bar:

- **TRaSH/Recyclarr-level profile ecosystem** — community-scored custom formats people trust day one
- **Decade of release-name edge cases** (scene quirks, anime, foreign, packs) — live fixes continue; Arrs already “just work”
- **Upgrade until quality/score**, interactive search UX, blocklists, history depth operators live in
- **Usenet + torrent as equal paths** — SAB/debrid modules exist as scaffolds; not “install and forget” like Arr + qBit + SAB
- **External clients** — power users want **qBittorrent/Transmission** as peers, not only native torrent
- **Music/books/comics/audiobooks** — managers scaffolded; not a full Lidarr/Readarr-grade acquire→import loop

Without TRaSH-class scoring + battle-tested parsing, MuxCore feels like a clever beta next to Arr.

#### 3. Playback identity (vs Jellyfin / Plex / Riven)

Today MuxCore is mostly **automation + bridge**, not a complete media *experience*.

- Native UI far from Jellyfin web
- No mobile clients (Plex/JF win households here)
- No Riven-style **debrid VFS** path for “request → appear instantly”
- Transcoder / intro-outro / graph modules are early; not productized in the player

**Chosen story:**

- **Near-term:** deep Jellyfin sync/handoff so households can play today (bridge + userdata).
- **End state:** `media-ui-app` **replaces** Jellyfin web for browse **and** playback (Appendix C). Agents must keep shipping native player parity — do not freeze on “Jellyfin is forever the player.”

#### 4. Trust, distribution, migration (vs linuxserver + Servearr wiki)

This is where Arr stacks still crush newcomers:

| Need | Market standard | MuxCore today |
|------|-----------------|---------------|
| Install | `docker compose` + known images | Forgejo/LAN registry compose ready; public GHCR mirror still gated |
| Migrate | Export/API from Sonarr/Radarr | No first-class migrate story |
| Guides | TRaSH, Servearr wiki, Discord | Strong internal wiki; little public gravity |
| Version trust | Years of `v3/v4` production | Many `v0.1.x` modules; beta disclaimer correct |
| Marketplace | Arr plugins + Recyclarr | Runtime DeployTag + checksum/signature trust + `/marketplace/trust` |

**Must-have:** one public compose path, **Sonarr/Radarr import**, and “Recyclarr for MuxCore” (sync curated format packs).

#### 5. Day-2 ops polish operators feel immediately

Market pain Arr users hate (and dashboards try to paper over):

- Unified calendar / upcoming
- One queue across media types with clear stuck/warning actions
- Activity / failed-import triage
- Devices, sessions, API keys, backups as first-class admin (see Jellyfin-admin parity list)

Pieces exist (automation, events, health). The **daily-driver control center** people open 20×/day is not there yet.

---

### Priority: what to build to actually win

Ordered by competitive impact, not roadmap nostalgia:

1. **Household loop** — userdata + continue watching + resume player + Seerr-like request/approval + invite links
2. **Quality moat** — import/sync TRaSH-style format packs; upgrade-until; blocklist; interactive search parity
3. **One install + migrate** — public images, Arr DB/API import, “bring your library in an afternoon”
4. **Acquisition completeness** — qBit peer + production Usenet + production debrid (or explicitly cede debrid to Riven and own local)
5. **Playback commitment** — near-term JF handoff OK; **end state** is `media-ui-app` replacing Jellyfin web (Appendix C player milestones)
6. **Calendar + unified queue + failure UX** — beat Panelarr/Prismarr at their own game *without* needing Arr backends
7. **Library-plus automation** — only after movies/TV feel better than Arr for normal users

---

### Positioning that markets will believe

**Winning claim (when gaps close):**  
“One install replaces Sonarr + Radarr + Prowlarr + Seerr + download client — and multi-quality without dual instances — with Arr-grade picks and a family UI.”

**Losing claim (today):**  
“Modular mesh loom with 80 modules.” That’s architecture marketing; Arr users buy outcomes.

**Don’t compete with:** Homarr/Dashy (start pages).  
**Do outcompete:** Arr complexity + Seerr UX + (optionally) Jellyfin playback.  
**Watch closely:** Riven on debrid/VFS; Panelarr/Prismarr on “one screen” habits.

---

### Bottom line

MuxCore isn’t lacking “more modules.” It’s lacking the **incumbent experience contract**:

- Arr-level **release decisions** (especially TRaSH-class scoring)
- Seerr/Wizarr-level **household UX**
- Jellyfin-level **watch experience** *or* a perfect JF handoff
- linuxserver-level **trust: install, migrate, stay updated**

Close those four, and the loom architecture becomes the reason people stay. Leave them open, and unified dashboards + Arr will keep the market no matter how elegant the mesh is.
---

## Appendix B: Radarr / Sonarr operator parity

## Radarr / Sonarr → MuxCore operator parity

**Goal:** MuxCore’s media stack (`media-movies`, `media-tvshows`, `media-automation`, leaf modules) plus `admin-ui` are a full operator replacement for **Radarr** and **Sonarr**.

**Canonical modules:** `media-movies`, `media-tvshows`, `media-automation`, `media-custom-formats`, `media-rename`, `media-root-folders`, `media-scanner`, `media-list-sync`, `indexer-*`, `downloader-*`, `admin-ui`. Do not develop archived dumps.

Legend: `[~]` partial · `[ ]` missing · `N/A` not applicable · **Waiver** = accepted MuxCore-native equivalent. Delete rows when fully done.

---

### A. Shared (*Arr) operator surfaces

| *Arr surface | MuxCore target | Status |
|--------------|----------------|--------|
| Import Lists | `media-list-sync` + `/list-sync` | [x] [`LISTSYNC-PARITY.md`](./LISTSYNC-PARITY.md) |
| Full library migrate | `admin-ui` `/migrate` | [x] One-shot Radarr/Sonarr import |

---

### B. Radarr (movies) — `media-movies` + admin

| Radarr surface | MuxCore target | Status |
|----------------|----------------|--------|
| Calendar | N/A (movies) | N/A |
| Import Lists | `media-list-sync` | [x] [`LISTSYNC-PARITY.md`](./LISTSYNC-PARITY.md) |

---

### C. Sonarr (TV) — `media-tvshows` + admin

| Sonarr surface | MuxCore target | Status |
|----------------|----------------|--------|
| Import Lists | `media-list-sync` | [x] [`LISTSYNC-PARITY.md`](./LISTSYNC-PARITY.md) |

---
---

## Appendix C: Jellyfin web → native UI parity

## Jellyfin web → MuxCore native UI parity

**Goal:** `media-ui-app` replaces Jellyfin’s **non-admin** web client; `admin-ui` replaces Jellyfin’s **dashboard** web client.

**Sources:** jellyfin-web `src/apps/modern`, `src/apps/legacy` (user), `src/apps/dashboard` (admin) routes (master, 2026-08).

**Canonical modules:** `media-ui-app` (consumer), `admin-ui` (operator). Do not develop archived `media-ui` / `media-jellyfin` dumps.

Legend: `[~]` partial · `[ ]` missing · `N/A` not applicable · **Waiver** = accepted MuxCore-native equivalent. Delete rows when fully done.

---

### A. Consumer (`media-ui-app`) ↔ Jellyfin user web

**End state:** `media-ui-app` replaces Jellyfin web (including playback). **Near-term:** JF handoff is OK so installs work; that must not stop native player work.

Delete rows when companion *or* player parity is production-grade for that surface.

**Shipped (removed from open checklist):** home/next-up, global search, movies/TV browse+detail, genres/studios/collections, upcoming, music+lyrics, books library section, mixed, favorites, playlists, queue, settings/profile/prefs, quick connect, login/forgot-password path, music videos / home videos (`?library=` + path-prefix config), live TV guide/channels/recordings/timers (**Waiver** — file-backed companion; no physical tuners / EPG grabbers), **Video OSD player** (`/player` + `VideoPlayer` — custom overlay OSD, resume, tracks, skip intro, keyboard shortcuts, BFF `/api/playback/resolve` + transcoder path wiring).

---

### B. Admin (`admin-ui`) ↔ Jellyfin dashboard

**Shipped operator surfaces (removed):** dashboard, users (+ password-reset queue), devices, libraries/roots, metadata, playback→transcoder, tasks, modules/marketplace/plugins, logs (file viewer), branding, networking, API keys, backups, live TV admin (file-backed), auth/SSO.

No open Jellyfin-dashboard checklist rows for near-term ops. MuxCore-native extras (keep): Automation, Queue, Calendar, Subtitles, Jellyfin bridge, List Sync, Migrate, Invites, Formats, Profiles, Naming, Cluster, Events, Storage, Audit, Request.

---
---

## Appendix D: Bazarr → media-subtitles parity

**Done** — product parity shipped in `media-subtitles` **v0.5.5**. Canonical record: [`BAZARR-PARITY.md`](./BAZARR-PARITY.md). No open checklist rows.

---
---

## Appendix E: Working constraints

#### Constraints (non-negotiable)

| Allowed | Not allowed as task drivers |
|---------|------------------------------|
| This laptop (Go, local Docker/Podman if present) | User creating paid accounts or entering payment info |
| **Forgejo origin** (`git.zem.systems/muxcore`, vault `forgejo-runner` `native`) | Treating `.github/workflows` as origin CI (use `.forgejo/workflows` only) |
| GitHub later as **public** consumer (Releases/GHCR for homelabbers) | Treating GHCR/`write:packages` as a current origin blocker |
| Fixture / httptest / mock / offline paths | Live pirate indexers, real torrent swarms, Apibay/VPN live grabs |
| Local MinIO, Postgres, Redis, Vault/OpenBao, Keycloak, Jellyfin in Docker | Real-Debrid / AllDebrid / paid Usenet / paid TMDB as hard deps |
| Forgejo or LAN image registry | Assuming paid GHCR packages scope |

**Acquisition rule:** keep `DOWNLOADER_ENGINE=fixture` / indexer soft-empty / httptest fixtures as the gate. Do **not** add tasks that require browsing or hitting pirate websites.

**Definition of done (product):** a non-developer can install MuxCore on one machine, add movies/TV via fixture or mock acquisition, browse/play in consumer UI, manage via admin-ui, and expand into music/books/etc. without a monorepo of sibling clones.
---

## Appendix F: Vault loop improvements

**Done** — all 14 vault-loop follow-ups are implemented and covered by module tests (`media-automation`, `media-scanner`, `request-media`, `_mvp/run-host.sh`). Source: live host (vault) acquisition watch, 2026-08-18. Re-open only if a regression appears on vault.
---

## Appendix G: Indexer / torrent VPN gates

### Non-negotiable VPN wiring (live indexer / torrent)

| Knob | Required value on gringotts |
|------|-----------------------------|
| `WG_CONF` | `/home/ender/Projects/muxcore/wg-mux.conf` (or rotated peer under `mvp/secrets/`; keep mode `600`) |
| `WG_USE_WG_QUICK` | `0` |
| `WG_KILL_SWITCH` | `false` |
| Interface | Source-routed iface from conf basename (typically `wg-mux`) |
| Gateway / DNS in conf | Proton `10.2.0.1` (do not change without rotation drill) |

Docs: [`_mvp/tls/README.md`](_mvp/tls/README.md), [`_mvp/tls/SECRET-ROTATION.md`](_mvp/tls/SECRET-ROTATION.md).

**Prove VPN before any live grab** on gringotts: downloader must source-route via `WG_CONF`; confirm public egress IP is Proton, not residential/mesh egress.


---

## Appendix H: Archived / polluted workspace dumps

These directories under the muxcore workspace are **not** modules. Do not open feature work, CI PRs, or spool pins against them.

| Dump path | Canonical replacement |
|-----------|----------------------|
| `media-ui/` | [`media-ui-app`](https://github.com/Muxcore-Media/media-ui-app) |
| `media-jellyfin/` | [`jellyfin`](https://github.com/Muxcore-Media/jellyfin) |
| `cache-memory/` | [`cache-local`](https://github.com/Muxcore-Media/cache-local) (process-local) or [`cache-redis`](https://github.com/Muxcore-Media/cache-redis) |
| `custom-scripts/` | None — ignore; use real module repos |
| `muxcorectl/` | [`muxcorectl-cli`](https://github.com/Muxcore-Media/muxcorectl-cli) |

Each dump directory has `ARCHIVED.md` + a stub `README.md`.


## Appendix I: Per-module todos (2026-08-28 fanout)

One wave of read-only audits: **10 implementable todos per enrolled module**, or `COMPLETE` if the folder was picture-perfect. **97 of 97** were marked complete (see index; latest: `media-scanner`).

Skip Appendix H dumps (`media-ui`, `media-jellyfin`, `cache-memory`, `custom-scripts`, `muxcorectl`) and scratch dirs (`branding`, `core.wiki`, `scripts`, `_wave1`).

Source files: `_wave1/module-todos/<module>.md`. Delete a row here when the work lands.

### Index

- [`api-rest`](#module-api-rest)
- [`auth-local`](#module-auth-local)
- [`auth-oidc`](#module-auth-oidc) — **COMPLETE**
- [`backup-local`](#module-backup-local)
- [`cache-local`](#module-cache-local)
- [`cache-redis`](#module-cache-redis) — **COMPLETE**
- [`call-policy-default`](#module-call-policy-default)
- [`circuitbreaker-simple`](#module-circuitbreaker-simple)
- [`config-watcher`](#module-config-watcher)
- [`contracts-automation`](#module-contracts-automation)
- [`contracts-downloader`](#module-contracts-downloader)
- [`contracts-indexer`](#module-contracts-indexer)
- [`contracts-media`](#module-contracts-media)
- [`contracts-media-admin`](#module-contracts-media-admin)
- [`contracts-metadata`](#module-contracts-metadata)
- [`contracts-notification`](#module-contracts-notification)
- [`contracts-playback`](#module-contracts-playback)
- [`contracts-reconciler`](#module-contracts-reconciler)
- [`contracts-scanner`](#module-contracts-scanner)
- [`core`](#module-core)
- [`database-postgres`](#module-database-postgres)
- [`database-sqlite`](#module-database-sqlite)
- [`data-redaction-pattern`](#module-data-redaction-pattern)
- [`distributed-lock-sqlite`](#module-distributed-lock-sqlite)
- [`downloader-debrid`](#module-downloader-debrid)
- [`downloader-native-torrent`](#module-downloader-native-torrent) — **COMPLETE**
- [`downloader-native-usenet`](#module-downloader-native-usenet) — **COMPLETE**
- [`downloader-qbittorrent`](#module-downloader-qbittorrent)
- [`downloader-sabnzbd`](#module-downloader-sabnzbd) — **COMPLETE**
- [`emby`](#module-emby)
- [`encryption-aesgcm`](#module-encryption-aesgcm)
- [`executor-shell`](#module-executor-shell)
- [`feature-flags-file`](#module-feature-flags-file)
- [`health-monitor`](#module-health-monitor)
- [`indexer-piratebay`](#module-indexer-piratebay)
- [`indexer-torznab`](#module-indexer-torznab) — **COMPLETE**
- [`input-validate-jsonschema`](#module-input-validate-jsonschema)
- [`jellyfin`](#module-jellyfin) — **COMPLETE**
- [`logging-file`](#module-logging-file)
- [`media-audiobooks`](#module-media-audiobooks)
- [`media-automation`](#module-media-automation)
- [`media-books`](#module-media-books) — **COMPLETE**
- [`media-comics`](#module-media-comics) — **COMPLETE**
- [`media-custom-formats`](#module-media-custom-formats) — **COMPLETE**
- [`media-dlna`](#module-media-dlna)
- [`media-ffprobe`](#module-media-ffprobe)
- [`media-graph`](#module-media-graph) — **COMPLETE**
- [`media-intro-outro`](#module-media-intro-outro) — **COMPLETE**
- [`media-library-maintainer`](#module-media-library-maintainer) — **COMPLETE**
- [`media-list-sync`](#module-media-list-sync) — **COMPLETE**
- [`media-movies`](#module-media-movies) — **COMPLETE**
- [`media-music`](#module-media-music) — **COMPLETE**
- [`media-rename`](#module-media-rename)
- [`media-root-folders`](#module-media-root-folders)
- [`media-scanner`](#module-media-scanner) — **COMPLETE**
- [`media-subtitles`](#module-media-subtitles)
- [`media-tagging`](#module-media-tagging) — **COMPLETE**
- [`media-transcoder`](#module-media-transcoder)
- [`media-transcoder-pool`](#module-media-transcoder-pool)
- [`media-tvos-app`](#module-media-tvos-app) — **COMPLETE**
- [`media-tvshows`](#module-media-tvshows)
- [`media-ui-app`](#module-media-ui-app)
- [`media-ui-tv`](#module-media-ui-tv) — **COMPLETE**
- [`metadata-musicbrainz`](#module-metadata-musicbrainz)
- [`metadata-tmdb`](#module-metadata-tmdb) — **COMPLETE**
- [`metrics-prometheus`](#module-metrics-prometheus)
- [`muxcore-android`](#module-muxcore-android)
- [`muxcorectl-cli`](#module-muxcorectl-cli) — **COMPLETE**
- [`muxcore-docs`](#module-muxcore-docs)
- [`muxcore-installer`](#module-muxcore-installer)
- [`muxcore-ios`](#module-muxcore-ios)
- [`muxcore-module-starter`](#module-muxcore-module-starter)
- [`muxcore-operator`](#module-muxcore-operator)
- [`notification-apprise`](#module-notification-apprise)
- [`notification-default`](#module-notification-default)
- [`playback-contract`](#module-playback-contract)
- [`playback-guard`](#module-playback-guard) — **COMPLETE**
- [`playback-monitor`](#module-playback-monitor) — **COMPLETE**
- [`plex`](#module-plex) — **COMPLETE**
- [`publish-policy-default`](#module-publish-policy-default)
- [`ratelimit-tokenbucket`](#module-ratelimit-tokenbucket)
- [`request-media`](#module-request-media) — **COMPLETE**
- [`scheduler-cron`](#module-scheduler-cron)
- [`secrets-file`](#module-secrets-file)
- [`secrets-vault`](#module-secrets-vault) — **COMPLETE**
- [`serialization-safe`](#module-serialization-safe)
- [`spool`](#module-spool) — **COMPLETE**
- [`spool-resolver-http`](#module-spool-resolver-http)
- [`storage-ceph`](#module-storage-ceph)
- [`storage-overlay`](#module-storage-overlay) — **COMPLETE**
- [`storage-s3`](#module-storage-s3)
- [`tracing-otlp`](#module-tracing-otlp) — **COMPLETE**
- [`userdata-local`](#module-userdata-local)
- [`worker-pool-memory`](#module-worker-pool-memory)
- [`workflow-tapestry`](#module-workflow-tapestry)

### Module: `api-rest`

**Status:** OPEN

1. [ ] Enforce `ValidateResponse.roles`/`permissions` in `internal/handler/auth.go` on mutating admin routes (`POST /modules/{id}/stop|restart`, `PUT|DELETE /storage`, `POST /events`, `POST /spools/.../deploy`, `GET /audit/export`, `POST /audit/verify`). Any valid household bearer can administer the mesh today; add a `gateway_test.go` case with a non-admin session.
2. [ ] Stop hardcoding `"0.1.0"` in `handleVersion`/`handleNode` (`internal/handler/handler.go`). Declare `var version` in `cmd/module/main.go` so Makefile/release `-X main.version` works, and have `/node` return core Health/Members identity instead of a static service blob.
3. [ ] Document the full `/api/v1` route table plus `API_REST_CORS_ORIGINS` and the `settings` capability in `README.md` and `.env.example`. Fix `COMPATIBILITY.md` (says core@v0.5.2; `go.mod` is v0.5.8).
4. [ ] Return the spool RPC error from `handleListTags` (`handler.go`) as 502/503 instead of empty `200` so admin-ui marketplace is not silently empty when ListTags fails.
5. [ ] Map storage Get/Stat/Delete not-found (`codes.NotFound`) to HTTP 404 in `handleGetStorage`/`handleStatStorage`/`handleDeleteStorage`; missing keys currently become 500.
6. [ ] Bound `PUT /api/v1/storage/{key}` and `POST /api/v1/events` with `http.MaxBytesReader` and `413` in `handler.go` (no upload/JSON size cap on the public listener).
7. [ ] Reuse a single auth gRPC client in `coreAuthValidator` (`auth.go`) instead of `grpc.NewClient`+Close on every request; keep the 5s Validate timeout.
8. [ ] Drop the parallel extra-client dial in `Handler.initClients` for Audit/Events (SDK `client.Client` already reconnects those). Dial lifecycle/spool on the same core conn or a closed-on-Stop conn; if `client.Dial` fails in `Module.Start`, retry so those clients are not nil for the process lifetime.
9. [ ] Add httptest coverage in `gateway_test.go`/`fakes_test.go` for audit query/export/verify, `POST /events`, spool list/tags/deploy, and cluster members/leader (only modules/storage/discovery/auth are exercised end-to-end).
10. [ ] Add `POST /api/v1/modules/spawn` proxying `Lifecycle.SpawnModule` (`lifecycle.proto` already has it; REST only exposes stop/restart) with a fake-mesh test.

### Module: `auth-local`

**Status:** OPEN

1. [ ] Gate `GET|POST /api/invites` and `DELETE /api/invites/{id}` in `internal/webapp/invites.go` behind an admin session; allowlist invite roles (`user`/`viewer`/`manager`); add CSRF to `POST /invite/redeem` (create/list/revoke are currently unauthenticated, including `role=admin`).
2. [ ] Apply `mustAuth` plus admin-or-owner checks in `internal/webauthn/handler.go` on `beginAdminRegistration`, `completeAdminRegistration`, `listCredentials`, and `deleteCredential` (any client can register or delete passkeys by `user_id` today).
3. [ ] Call `requireAdmin` (or self-or-admin) in `internal/server/server.go` on `CreateAPIToken`, `ListAPITokens`, `DeleteAPIToken`, `EnableTOTP`, `DisableTOTP`, `VerifyTOTPSetup`, `ListWebAuthnCredentials`, `DeleteWebAuthnCredential`, and `BeginAdminRegistration`/`CompleteAdminRegistration`.
4. [ ] Make the first `CreateUser` on an empty store (`internal/store/store.go` / `server.CreateUser`) receive role `admin`; add a `server_test.go` case. First user is currently `user`, so `authctl addrole` cannot succeed (`requireAdmin` deadlock).
5. [ ] Attach `-token` / `AUTHCTL_TOKEN` as gRPC metadata `x-auth-token` on every RPC in `cmd/authctl/main.go` (flag is parsed; `cmdListUsers` takes it unused; other commands never send it).
6. [ ] Add `module: ["*"]` to shipped `policies.yaml` and the README role table so the default file matches `policy.Builtin` (CHANGELOG 0.1.6). A present `policies.yaml` without `module` deny-alls mesh `x-caller-id` callers.
7. [ ] Document `/invite`, `/login/device`, `AUTH_HTTP_URL`, `tenant_id`, and the `settings` capability in `README.md` and `.env.example`. Fix `AGENTS.md` ("Contracts: none") and `COMPATIBILITY.md` (missing `settings`; false "Requires MuxCore v1.0.0").
8. [ ] Call `DeleteUserSessions` from `SetPassword` and `DisableTOTP` in store/server so password reset and 2FA disable invalidate live sessions. Treat TOTP as login-required only after `VerifyTOTPSetup` (`SetTOTPSecret` sets `enabled=1` immediately and can lock the user out).
9. [ ] Hash session tokens at rest in `sessions` the way `api_tokens`/`invites` already hash secrets (`internal/store/store.go`), and run `CleanupExpiredSessions` on a ticker from `Module.Start` (the helper exists and is never scheduled).
10. [ ] Set `Module.Info().HTTPAddr` to the HTTP listen address (it copies `grpcAddr` today). Rate-limit gRPC `Authenticate` in `internal/server/server.go` to the documented 6-attempt / 1-minute backoff (HTTP UI has it; gRPC does not).

### Module: `backup-local`

**Status:** OPEN

1. [ ] Wire production Backupable peers: `cmd/module/main.go` currently calls `NewModule(Config{})` so `peers` is always empty and README's `ExportState`/`ImportState` path never runs. Add a Backupable gRPC service to `proto/muxcore/backup/v1`, discover capability `backupable` via core, dial those modules from `internal/`, and cover with a bufconn mock peer (no live mesh). Add the grpcurl examples `database-postgres/MIGRATION-SQLITE.md` already points at.
2. [ ] Add `BACKUP_EXCLUDE` / settings `exclude_globs` in `internal/archive.go` `collectDirEntries` and `internal/settings.go`. Default-skip `.incomplete`, `*.parts`, `*.part`, `*.!qb`, `*.tmp`. `_mvp/run-host.sh` sets `BACKUP_SOURCE_DIRS=$DATA` and ACQUISITION.md already documents this hole.
3. [ ] Stream files into `tar.Writer` in `internal/archive.go` instead of `os.ReadFile` + `[]tarEntry` holding the whole tree. Honor `ctx` on walk/write. A real library tree under `BACKUP_SOURCE_DIRS` will OOM today.
4. [ ] Allowlist `CreateBackupRequest.source_paths` and `RestoreBackupRequest.target_path` against configured sources / `BACKUP_ALLOWED_ROOTS` in `internal/module.go`. Unauthenticated `BackupService` on `:9302` can otherwise archive or overwrite any path the process can reach.
5. [ ] Make index durable in `internal/module.go`: `saveIndex` errors must fail Create/Delete; `DeleteBackup` must fail if `os.Remove` fails; `Init` must reconcile `*.tar.gz` vs `index.json` and must not replace the catalog with `{}` when JSON is corrupt. Add persist-across-restart + missing-archive tests in `internal/module_test.go`.
6. [ ] Implement real `Health()` and HTTP `/health` (dir exists + writable, index readable) in `internal/module.go`. Add `--health-check` in `cmd/module/main.go` so `Dockerfile` `HEALTHCHECK CMD ["/module", "--health-check"]` works instead of starting the module.
7. [ ] Add retention settings `max_backups` / `max_age_days` and prune oldest archives after a successful `CreateBackup`. Archives grow forever today. Tests for prune-by-count and prune-by-age.
8. [ ] Encrypt archives at rest (env `BACKUP_ENCRYPT_KEY` or optional `encryption-aesgcm` capability; fixture key in tests). Restore decrypts before extract. Library SQLite in `modules/*/state.bin` is household data sitting as plaintext `.tar.gz`.
9. [ ] Make `RestoreBackup` atomic: extract to a sibling staging dir, verify checksum, then rename into `target_path`. Add `TestRestoreChecksumMismatch` in `internal/module_test.go` (checksum path is untested). Do not leave a half-applied tree if `ImportState` fails.
10. [ ] Add settings `schedule_cron` (or a ticker from `BACKUP_SCHEDULE`) in `Module.Start` that calls `CreateBackup` so backups happen without admin-ui/cli. Replace `test/integration_test.go` (dummy `test-module` register against live core, `//go:build integration`) with an in-process create/list/restore test that origin CI runs. Run `go test -race` and `golangci-lint` in `.forgejo/workflows/ci.yml` (Makefile already does; Forgejo does not).

### Module: `cache-local`

**Status:** OPEN

1. [ ] Add `pkg/client` implementing `contracts.CacheLayer` over `CacheLayerService` (bufconn/in-process tests). README's `DialCacheLayer` does not exist; core `DiscoverCache` only type-asserts in-process modules, so the sidecar never serves storage.
2. [ ] Cap memory in `internal/cache/cache.go` (`max_bytes`, `max_entry_bytes`, optional `max_entries`) with env + `Settings` keys in `internal/settings.go`. Unbounded map; storage orchestrator can `Set` full objects (core `MaxObjectSize` is 64GiB).
3. [ ] Copy slices on `Set`/`Get` and re-check the map entry under the write lock before expire-delete in `internal/cache/cache.go`. Current `Get` returns a shared buffer and can delete a concurrent `Set` of the same key.
4. [ ] Add tests in `internal/cache/cache_test.go`: sweeper removes expired keys without `Get`; `SetDefaultTTL` does not rewrite existing expiry; expire-delete does not clobber a concurrent `Set`.
5. [ ] In `internal/server/server.go` reject empty keys (`InvalidArgument`) and oversize values with a documented max; raise gRPC `MaxRecvMsgSize` to match. Add `server_test.go` cases (empty key, empty-prefix invalidate, oversize). Default 4MiB recv makes large storage `Set`s fail as opaque `ResourceExhausted`.
6. [ ] Fix origin CI in `.forgejo/workflows/ci.yml`: strip `replace => ../core` (or check out sibling `core`) so `go test` works without a monorepo layout; run `go test -race` (Makefile already does) and `golangci-lint`.
7. [ ] Docs drift: add `CACHE_LOCAL_TTL` to the README config table; replace the invented `DialCacheLayer` diagram with the real gRPC/`HTTPAddr` dial path; point the CI badge at Forgejo; update `COMPATIBILITY.md` from v0.1.0/core v0.4.0 to v0.1.1/core v0.5.8+; warn default listen is `:9602` (installer still defaults `:9600`, which is `cache-redis`).
8. [ ] Make `Health` in `internal/module.go` a Get/Set/Invalidate round-trip, not a nil check. Log or fail `Init` on invalid `CACHE_LOCAL_TTL` (today it is silently ignored).
9. [ ] Replace `test/integration_test.go` (registers a dummy `test-module` against live core, `//go:build integration`) with an in-process CacheLayer Get/Set/Invalidate test that CI actually runs.
10. [ ] Wire a single version: declare `var version` in `cmd/module/main.go` so Makefile `-X main.version` works; stop hardcoding `"0.1.1"` in `internal/module.go` / `Info()`; set `MinCoreVersion` to match `COMPATIBILITY.md`.

### Module: `cache-redis`

**Status:** COMPLETE — distributed Lock/Unlock tokens, Subscribe ctx cleanup, miniredis server/module tests, CAS TTL + binary payloads, DEL count, REDIS_USERNAME/TLS/CACHE_KEY_PREFIX, docker-compose Redis + healthcheck + systemd, CI redis:7-alpine + golangci-lint, empty-key validation, HTTP /metrics + version ldflags + 30s lock TTL docs.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/cache-redis.md -->

### Module: `call-policy-default`

**Status:** OPEN

1. [ ] Rewrite shipped `policies.yaml` to match core storage ACL (`AllowCall(caller, "storage", "read"|"write")` in `core/internal/grpcmesh/storage_server.go`): drop `storage-local` / `Put`/`Get`/`Stat`/`Delete`/`Stream` rules for `downloader-qbittorrent` and `transcoder-ffmpeg`; use `media-tvshows` (not `media-tv`); add `jellyfin` next to `media-jellyfin`; add least-privilege rules for `media-automation`, `request-media`, and `admin-ui`.
2. [ ] Add a fixture test that `policy.Load`s repo `policies.yaml` and `LoadWithOverlay`s `policies-dev.yaml`, asserting scanner/native-torrent/indexers → `storage` `read`/`write`, `_public` denied on prod file and allowed only via overlay, and unknown caller/target/method denied.
3. [ ] Merge overlay `groups` into the base policy in `internal/policy/policy.go` `LoadWithOverlay` (today only `rules` are appended, so overlay `caller_group` members are dropped at runtime). Cover with a unit test.
4. [ ] Add `internal` tests that drive `HandleGrantForTest` / `applyRevoke` for valid grant JSON, missing caller/target/methods, revoke-by-id, and revoke-by-caller/target. The hook exists; nothing calls it.
5. [ ] Make `subscribePolicyEvents` in `internal/events.go` retry subscribe/dial until `Stop` (context-cancel the 5s sleep and stream). A single failed subscribe currently disables `call.policy.grant`/`revoke` for the process lifetime.
6. [ ] Reject dynamic grants/revokes unless `Event.Source` is on an allowlist (`grantors` in YAML or a setting). `applyGrant` currently trusts any payload; `Event.Source` is ignored.
7. [ ] Expand `internal/settings.go` beyond `policy_file`: expose overlay path (`CALL_POLICY_DEV_FILE`), `DynamicCount()`, allow/deny counters from `PolicyServer.Metrics()`, and a reload action that does not require changing the file path.
8. [ ] Document `CALL_POLICY_DEV_FILE` and `CALL_POLICY_EVENT_SUBSCRIBE_DELAY` in `README.md` and `.env.example`. Fix `AGENTS.md` ("Contracts: none") and `COMPATIBILITY.md` (missing `settings` and v0.3 grants). Replace README `media-tv` examples with `media-tvshows`.
9. [ ] Fix deploy artifacts: `Dockerfile` `EXPOSE 9101` (not 9300) and include `policies.yaml` in `.dockerignore`; set systemd `WorkingDirectory` + `Environment=CALL_POLICY_FILE=…` in `deploy/systemd/muxcore-module.service`; pass `CALL_POLICY_FILE` in `deploy/docker-compose.yml`.
10. [ ] Implement a real `grpc_health_v1` `Watch` in `internal/module.go` (stream SERVING until the RPC context ends) and stop the SIGHUP notifier / close the mesh client in `Stop`.

### Module: `circuitbreaker-simple`

**Status:** OPEN

1. [ ] Add `pkg/client` implementing `contracts.CircuitBreaker` (`Execute` + `State`) over existing `CircuitBreakerService` RPCs (bufconn tests). `core/pkg/contracts/circuitbreaker.go` documents Mesh.Call wrapping; no workspace type implements it, and core's `CircuitBreakerSet` is never wired.
2. [ ] Latch HALF_OPEN in `internal/module.go` `effectiveState` (write back, do not only compute on read) and honor unused `halfOpenMax` as a probe budget. Expose setting/env `half_open_max` / `CB_HALF_OPEN_MAX`. Tests: `State` after cooldown is HALF_OPEN; extra probes re-open or reject.
3. [ ] Idle-TTL eviction of `Module.circuits` in `internal/module.go` — `State`/`getOrCreate` allocates per key forever. Test expiry of unused keys.
4. [ ] Reject empty keys with `codes.InvalidArgument` on `State`/`RecordSuccess`/`RecordFailure`/`Reset` in `internal/module.go`. `getOrCreate("")` currently pollutes the map. Tests for all four RPCs.
5. [ ] Origin CI: sibling-checkout `core` and `contracts-media` in `.forgejo/workflows/ci.yml` (match `.github/workflows/ci.yml` / `_wave1/ci-siblings.json`). Isolated Forgejo `go test` fails on `replace => ../core`. Run `go test -race` (Makefile already does; Forgejo does not).
6. [ ] Replace `fmt.Sscanf` env parse in `NewModule` with `strconv` like `internal/settings.go`; log and ignore invalid `CB_FAILURE_THRESHOLD`/`CB_COOLDOWN`. Tests: env override, `UpdateSetting` rejects `0` / bad duration / unknown key.
7. [ ] Make `Health` fail when gRPC is not serving (nil `lis` / after `Stop`) in `internal/module.go`. `TestHealth`/`TestLifecycle` currently pass a never-started and a stopped module.
8. [ ] Log open / half-open / close transitions in `RecordFailure`/`RecordSuccess`/`Reset` (key + old→new). Circuits trip with no slog today.
9. [ ] Add `List` (or `ListOpen`) to `core/proto/muxcore/circuitbreaker/v1/circuitbreaker.proto` and implement in `internal/module.go` so admin-ui/muxcorectl can show tripped keys. Unit-test list of open vs closed.
10. [ ] Docs drift: `COMPATIBILITY.md` still lists v0.1.1 as Current and omits `settings`; README missing live settings, process-local state, `MVP_ENABLE_CIRCUITBREAKER_SIMPLE`; `AGENTS.md` says "Contracts: none"; CHANGELOG 0.1.4 sits above Unreleased and still says core v0.5.2; `muxcore.json`/`Info()` contract `v0.4.0` vs `go.mod` `v0.5.8`.

### Module: `config-watcher`

**Status:** OPEN

1. [ ] Subscribe to core `module.registered` / `module.unregistered` in `internal/module.go` Start() via `core/sdk/go/client` Events, resolve capabilities from the registry, and fan out internally so the sidecar works without an external `ReportModuleChange` caller (none exists outside tests).
2. [ ] Add a Go client in this module (e.g. `client/` or `pkg/watch`) that implements `contracts.ConfigWatcher.OnChange` over `WatchChanges` gRPC so other modules can use the documented contract without hand-rolling streams.
3. [ ] On `WatchChanges` subscribe, emit a snapshot of currently registered modules matching the capability (registry lookup) so late joiners do not miss already-up services; cover in `internal/module_test.go`.
4. [ ] Authenticate `ReportModuleChange` in `internal/module.go` — `grpc.NewServer()` has no interceptors, so any client that can reach `:9614` can inject fake register/unregister events. Require mesh identity / admin capability.
5. [ ] Align listen port to `:9614` in README.md, `.env.example`, COMPATIBILITY.md, `deploy/systemd/muxcore-config-watcher.service`, and `deploy/docker-compose.yml` (set `CONFIG_WATCHER_GRPC_ADDR`). Docs still say `:9611`, which is storage-s3 health in `_mvp/PORTS.md`.
6. [ ] Fix Docker so `make docker` and `docker compose -f deploy/docker-compose.yml up` actually run: Dockerfile copies sibling `core/` + `config-watcher/` from a parent context, but Makefile/compose use module-root context and compose publishes 9611 while the process listens 9614.
7. [ ] Fix origin CI in `.forgejo/workflows/ci.yml`: checkout `muxcore/core` as `../core` (GitHub workflow already does this) so `replace => ../core` resolves, and add a golangci-lint step matching `make lint`.
8. [ ] Expand `internal/module_test.go`: wildcard `""` fan-out, invalid `ReportModuleChange` (empty capability/event_type), slow-watcher drop path, `CONFIG_WATCHER_STREAM_BUFFER` / unknown setting key, and stream-cancel removing the watcher map entry.
9. [ ] Stop holding `m.mu.RLock` across 100ms send timeouts in `ReportModuleChange` (copy the channel set, send outside the lock). Make `Health()` fail when `lis`/`grpcSrv` is not serving (today always returns nil).
10. [ ] Fix identity/docs drift: AGENTS.md says "Contracts: none declared" but `muxcore.json` and `Info()` declare ConfigWatcher v0.4.0; `Info().Roles` is empty vs muxcore.json `["config"]`; COMPATIBILITY.md still lists v0.1.0 as current.

### Module: `contracts-automation`

**Status:** OPEN

1. [ ] proto/muxcore/automation/v1/automation.proto `QueueItem`: add `series_id`, `absolute_number`, `series_type` (already on `AddToQueueRequest`) so `GetQueue` can return TV grouping and anime absolute.
2. [ ] proto `AutomationService`: add `UpdateQueueItem` (`queue_id`, `monitored`, `quality_profile_id`) and `AddToQueueRequest.monitored` — `QueueItem.monitored` is read-only and add always defaults monitored.
3. [ ] proto `SearchNowRequest`: add optional `queue_id` / `item_type`+`item_id` so clients can force one wanted-item search (empty request stays a full-queue pass).
4. [ ] proto `GetHistoryRequest`: add `status` and `wanted_item_id` filters so admin-ui/muxcorectl can page `import_failed`/`stalled` without pulling all history.
5. [ ] proto: add `SeriesOverride` plus `ListSeriesOverrides` / `UpsertSeriesOverride` / `DeleteSeriesOverride` (`series_id`, `delay_minutes`, `preferred_groups`, `ignored_groups`) — media-automation only exposes this as `series_overrides_json`.
6. [ ] proto comments on `SearchItemRequest.item_type`, `AddToQueueRequest.item_type`, `GetQueueRequest.filter`: document `movie|tv|music|book|comic|audiobook` (implementer already searches music and syncs other libraries; comments still say movie/tv only).
7. [ ] proto `ReleaseMatch`: add `rejected` + `rejection_reason` (optional `format_score`) so interactive `SearchItem` can show why a release lost quality/title/cutoff.
8. [ ] proto: add `GetCapabilities` (`supported_item_types`, `supports_delay_profiles`, `supports_cutoff`, `supports_blocklist`, `supports_anime_absolute`, `supported_protocols`) to match contracts-downloader/indexer.
9. [ ] proto `GetQueueRequest`: add `missing` and `monitored` filters (`filter` is undocumented and item_type-only).
10. [ ] `.forgejo/workflows/ci.yml` + `Makefile`: install protoc/plugins in CI, run `make proto` and fail on `git diff` under `muxcore/automation/v1/`; stop `clean` from deleting checked-in generated stubs.

### Module: `contracts-downloader`

**Status:** OPEN

1. [ ] Align `proto/muxcore/downloader/v1/downloader.proto` `go_package` with checked-in stubs (`github.com/Muxcore-Media/contracts-downloader/muxcore/downloader/v1;downloaderv1`, same pattern as `contracts-automation`) and fix `Makefile` `clean` (it deletes `gen/muxcore/downloader/v1/*.go`, which does not exist).
2. [ ] Add a Forgejo proto-check job in `.forgejo/workflows/ci.yml` (`make proto` + fail on dirty tree). Origin CI is Forgejo (Appendix E); only `.github/workflows/ci.yml` has this gate today.
3. [ ] Add a `TorrentStatus` enum in `downloader.proto` and document the vocabulary in README (`downloading`, `paused`, `completed`, `failed`, `stalled`, `removed`, `unknown`). `TorrentInfo.status` / `ListTorrentsRequest.status` are free strings; qBit maps via `MapState`, native uses a different set, so list filters are not portable.
4. [ ] Add `string error` on `TorrentInfo` in `downloader.proto`. Native private proto already has it and `contracts-media/events.DownloadEventPayload` has `Error`; `GetTorrent` cannot surface a failure reason to automation/admin.
5. [ ] Add `SetTorrentFiles` (path + `wanted` / priority) RPC so `GetCapabilities.supports_file_selection` and `TorrentFile.wanted` are actually writable. Both native and qBit adapters currently report `SupportsFileSelection=false`; native file priority is settings-only.
6. [ ] Add `bytes torrent_bytes` and optional `name` on `AddTorrentRequest` for fixture/offline `.torrent` add (no live HTTP). Document that `torrent_url` accepts `magnet:` and `http(s)` (field name is magnet-hostile; automation already passes magnets there).
7. [ ] Fix discovery docs: COMPATIBILITY.md Capability ID is `contracts.downloader` only; callers (`media-automation`) and implementer `muxcore.json` use `downloader`. List both IDs (indexer pattern) and state the proto interface is `DownloaderService` (implementers declare `"Downloader"`).
8. [ ] Document required `contracts-media/events` emissions in COMPATIBILITY.md / README: `download.started`, `download.completed`, `download.failed` with `DownloadEventPayload` (`id`, `save_path`, `files`, `error`). Automation import is event-driven; the contract never says implementers must publish.
9. [ ] Add `muxcore/downloader/v1/contract_test.go` (fixture/offline only): assert RPC names, status vocabulary, import path `.../muxcore/downloader/v1`, and capability ID `downloader`. `go test ./...` currently compiles generated stubs only — no `*_test.go`.
10. [ ] Update README: list `downloader-qbittorrent` as an implementer; add proto comments that `ListTorrents` category and status are AND filters (native `contracts_server.go` ORs them into one `filter`). Add `RecheckTorrent` RPC so verify-data is not settings-only (`downloader-native-torrent` `recheck_torrent`).

### Module: `contracts-indexer`

**Status:** OPEN

1. [ ] Align `proto/muxcore/indexer/v1/indexer.proto` `go_package` with checked-in stubs: change `.../gen/muxcore/indexer/v1` to `github.com/Muxcore-Media/contracts-indexer/muxcore/indexer/v1;indexerv1`. Point `Makefile` `clean` at `muxcore/indexer/v1/*.go` (it currently deletes nonexistent `gen/`).
2. [ ] Add a proto regen+diff job to `.forgejo/workflows/ci.yml` (origin CI): `make proto` then fail if `git status --porcelain` is dirty. `.github/workflows/ci.yml` already has `proto-check`; Forgejo does not.
3. [ ] Add `muxcore/indexer/v1/contract_test.go` that freezes field numbers for `SearchRequest`/`SearchResult`/`IndexerInfo` and asserts documented season-pack (`episode=0` + `season>0`) and anime `absolute` stay on tags 5/4/10.
4. [ ] Add optional `imdb_id`/`tmdb_id`/`tvdb_id` to `SearchRequest` in `indexer.proto` so implementers can do Newznab/Torznab ID search. `media-automation` already has tmdb_id on wanted items but cannot pass it through `IndexerService`.
5. [ ] Add `infohash` (string) to `SearchResult` in `indexer.proto`. `indexer-piratebay` already has the hash and only embeds it in `download_url`; `media-automation` re-parses magnets. Stop requiring URL parsing.
6. [ ] Replace freeform `SearchRequest.type` (and document `categories`) with a proto enum or canonical token list in proto comments + `COMPATIBILITY.md`: `movie`, `tv`, `music`, `book`, `audiobook`, `comic`. `media-automation` already sends `music`; implementers invent their own aliases.
7. [ ] Extend `GetCapabilitiesResponse` in `indexer.proto` with `supports_music_search`, `supports_book_search`, `supports_id_search`, and `supports_season_pack` so callers know how to form queries. Today only movie/tv flags exist.
8. [ ] Specify paging in proto comments + `COMPATIBILITY.md`: `SearchResponse.total` is unpaged hit count (not page length), default `limit` 50, max 100. `indexer-piratebay` currently sets `Total` to `len(results)` after paging.
9. [ ] Document implementer error/privacy contract in `COMPATIBILITY.md` and proto comments: `codes.ResourceExhausted` means rate-limited (`media-automation` already holds 15m); unconfigured indexers return empty results not an error; `SearchResult.download_url` must omit indexer API keys (Torznab `apikey=` currently passes through).
10. [ ] Fix docs drift: add `indexer-torznab` to README known implementers (it already implements `IndexerService`); mark planned `indexer-prowlarr` as superseded by torznab. Pick one capability ID (`indexer` for mesh peers vs `contracts.indexer` in `muxcore.json`/`COMPATIBILITY.md`). Rewrite `AGENTS.md` from sidecar boilerplate to proto regen + compatibility rules.

### Module: `contracts-media`

**Status:** OPEN

1. [ ] `events/events.go`: add payload structs for the six typed events that only have constants today — `MovieUpdatedPayload`, `MovieFileRemovedPayload`, `MovieRequestedPayload`, `TVUpdatedPayload`, `TVEpisodeFileRemovedPayload`, `TVRequestedPayload` — JSON tags matching live publishers (`media-movies` add/remove/update/file, `media-tvshows` same, `request-media/internal/approval.go`: `request_id`, `movie_id`/`series_id`, `tmdb_id`, `title`, `year`, `run_id`, `season_number`, `episode_number`).
2. [ ] `events/events.go`: add `EventMusicRequested = "media.music.requested"` and `MusicRequestedPayload` (`request_id`, `musicbrainz_id`, `title`, `artist_id`, `run_id`, `kind`) so `request-media` can drop the private `eventMusicRequested` literal in `music_fulfill.go` / `music_fulfill_album.go`.
3. [ ] `events/events.go` `FileImportedPayload`: add fields scanners already publish and consumers drop — `season_pack` (`media-scanner/internal/module.go`), `artist`/`album`/`track_title` (`media-scanner/internal/music_import.go`). Add `SeriesID` on `TVEpisodeFileAddedPayload` (removed-file events already send `series_id`).
4. [ ] `events/events.go`: give `TVRemovedPayload` the movie-removed fields (`name`, `tmdb_id`) and `TVAddedPayload` an optional `year` so `notification-default` / `notification-apprise` stop rendering a bare `series_id` and tagging/automation can unmarshal one struct for add+update (they already reuse `TVAddedPayload` for `EventTVUpdated`).
5. [ ] `events/events_test.go`: freeze every `Event*` string; JSON round-trip all payloads including omitempty zeros; table-assert each constant has a payload type. `go test ./...` currently compiles an empty package.
6. [ ] `events/events.go`: add `All() []string` and `Known(eventType string) bool` covering every constant so `media-automation` subscribe lists, notification formatters, and `admin-ui` event filters stop hardcoding `"media.movie.added"` / `"download.completed"`.
7. [ ] `events/events.go` + `README.md`: export deprecated aliases for the names already in the wild (`media.download.dispatched` in `media-automation/internal/module_test.go`; wiki `media.movie.file_added` / `media.tv.episode_file_added`) and state the canonical strings (`download.dispatched`, `media.movie.file.added`, `media.tv.episode.file.added`). Dual-subscribe during migration; do not invent a second live type.
8. [ ] `README.md`: replace the constant-only table with per-event payload field tables and publisher/consumer rows (`media-movies`, `media-tvshows`, `media-scanner`, `request-media`, `media-list-sync`, `downloader-*`, `media-automation`, `notification-*`, `media-tagging`). Note `EventImportListSynced` / `ImportListSyncedPayload` are not re-exported from `core/pkg/contracts/media_events.go`.
9. [ ] `AGENTS.md`: replace sidecar/gRPC/TLS/`PORTS.md` boilerplate with library identity (import `github.com/Muxcore-Media/contracts-media/events`; no binary; `muxcore.json` is catalog-only). Add the add-event checklist: constant + payload + `All()` + test + README row. Clarify `muxcore.json` `capabilities: ["contracts.media"]` is not a runnable sidecar.
10. [ ] Add `CHANGELOG.md` + `COMPATIBILITY.md` (same shape as `contracts-downloader`) for tag `v0.1.0` / `muxcore.json` `0.1.0`: additive JSON fields only; never rename event type strings. Annotate path/PII fields (`file_path`, `original_path`, `destination_path`, `save_path`, `info_hash`) in `events.go` comments plus a short README "PII in payloads" note so notification UIs do not print raw paths.

### Module: `contracts-media-admin`

**Status:** COMPLETE — first-class metadata fields, GrabRelease, SearchIndexers season/episode, ListChildren/GetChild, ListFiles/RemoveFile, SetMonitored, proto enums, collection RPCs, artwork RPCs, AGENTS/README/COMPATIBILITY/proto-check CI, Makefile gen/ fix.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/contracts-media-admin.md -->

### Module: `contracts-metadata`

**Status:** OPEN

1. [ ] README.md: drop the false “Generated Go is not checked in yet” line (stubs already live in `muxcore/metadata/v1/`). Document `make proto` as regen-only and list current consumers (`metadata-tmdb`, `media-movies`, `media-tvshows`, `request-media`, `media-list-sync`, `_mvp/cmd/mediauiprox`).
2. [ ] Add `CHANGELOG.md` + `COMPATIBILITY.md` matching `contracts-indexer` / `contracts-downloader`: tag `v0.1.0`, interface `MetadataProvider`, capability `metadata`, Go import path, and “breaking proto ⇒ new major + coordinated consumer bump.”
3. [ ] `.forgejo/workflows/ci.yml`: add a `proto-check` job (install pinned `protoc` + `protoc-gen-go` / `protoc-gen-go-grpc`, run `make proto`, fail if `git status --porcelain` is dirty). Pin those plugin versions in `Makefile` so local regen matches CI.
4. [ ] `proto/muxcore/metadata/v1/metadata.proto` `GetTVDetailsResponse.in_production`: change `string` → `bool` (reserve field 18 or bump the contract). Implementers currently `strconv.FormatBool` (`metadata-tmdb/internal/module.go`); consumers cannot filter “still airing” without parsing a string.
5. [ ] Add `imdb_id` and `tvdb_id` to `SearchResult` and `GetTVDetailsResponse` (movies already expose `imdb_id`). Arr migrate, `media-list-sync`, `media-subtitles`, and `playback-monitor` all key off these IDs; TV lookup today has none.
6. [ ] Replace TMDB-only request fields (`tmdb_id` on GetMovie/TV/Season/Collection/AlternativeTitles) with a provider-neutral `id` plus an `ExternalIds` message. `MetadataProvider` cannot be implemented by `metadata-musicbrainz` (own proto) or a future TVDB module while every RPC is TMDB-shaped.
7. [ ] Add `GetEpisodeDetails` RPC and `still_url` on `Episode` (parity with movie `poster_url`). `media-tvshows` must `GetSeasonDetails` for a whole season to refresh one episode; stills stay as TMDB paths only.
8. [ ] Add `ListSimilar` and `ListRecommendations` RPCs (media type + id + page). Discover in `media-ui-app` / `mediauiprox` / `request-media` is only trending/popular; Seerr-class “more like this” has no contract to call (fixture/mock implementers only).
9. [ ] `SearchRequest`: add `include_adult` (default unset/false) and `region`. Household search/discover cannot hide adult titles or localize results without leaking TMDB query knobs into each caller.
10. [ ] Rewrite `AGENTS.md` for a contracts repo (edit proto → `make proto` → commit stubs; do not treat this as a gRPC sidecar with ports/TLS). Change `Makefile` `clean` so it does not `rm muxcore/metadata/v1/*.go` (that is the published API).

### Module: `contracts-notification`

**Status:** OPEN

1. [ ] Add proto-check to `.forgejo/workflows/ci.yml` (origin CI): install pinned `protoc-gen-go`/`protoc-gen-go-grpc` matching the committed stubs (`v1.36.11` / `v1.5.1`), run `make proto`, fail if `git status --porcelain` is dirty. GitHub `.github/workflows/ci.yml` pins `v1.36.6` and is not origin.
2. [ ] Add `CHANGELOG.md` and `COMPATIBILITY.md` (mirror `contracts-downloader`) and set `muxcore.json` `version` to the published tag (`v0.1.1`). Document that proto service `NotificationService` is the `NotificationProvider` interface name used by `notification-default`/`notification-apprise`/`contracts-reconciler`.
3. [ ] Specify `ConfigureRequest.settings` keys in `proto/muxcore/notification/v1/notification.proto` comments and a README table: `webhook_url`; email `smtp_host`/`smtp_port`/`smtp_user`/`smtp_pass`/`smtp_from`/`to`; Apprise `urls`/`token`. Implementers currently invent aliases (`from`, `host`, `pass`).
4. [ ] Replace free-string `NotifyRequest.severity` with a proto `Severity` enum (`INFO`/`SUCCESS`/`WARNING`/`ERROR`) in `notification.proto` so Discord/Slack color mapping cannot drift between implementers.
5. [ ] Add a `Test` RPC (`TestRequest` channel + `TestResponse` ChannelResult) on `NotificationService` so admin-ui / muxcorectl can send a fixture ping without a real media event (httptest/mock only).
6. [ ] Add disable/clear semantics: `ConfigureRequest.enabled` (or a `Clear` RPC). Today `Configure` only upserts with `Enabled: true`; there is no contract way to turn a channel off.
7. [ ] Extend `ChannelStatus` with `last_error` and `last_success_at`, and proto-comment that `description` must not include webhook URLs or SMTP credentials (`notification-default` currently prefixes truncated webhook URLs into `description`).
8. [ ] Add `muxcore/notification/v1/compat_test.go` freezing Channel enum numbers, RPC full method names, and `NotifyRequest` field tags so a proto edit cannot silently break consumers (`media-subtitles`, `playback-monitor`, `playback-guard`, `media-library-maintainer`).
9. [ ] Rewrite `AGENTS.md`: this is a proto/stubs repo, not a gRPC sidecar. Document `make proto` plugin pins, `go test ./...`, and that capabilities are `contracts.notification` / interface `NotificationProvider` — drop TLS/ports/sidecar boilerplate and “Contracts: none declared”.
10. [ ] Pin proto plugin versions in `Makefile` `proto` target (`go run` or documented `go install` versions) and stop `make clean` from deleting published stubs (`rm -f muxcore/notification/v1/*.go`).

### Module: `contracts-playback`

**Status:** OPEN

1. [ ] `events/events.go`: add a shared session payload struct (JSON tags aligned with `playback-contract/proto/playbackv1.PlaybackSessionEvent` plus optional `tmdb_id`/`tvdb_id`/`imdb_id` that `playback-monitor` already ingests) for `playback.started`/`playback.progress`/`playback.stopped`.
2. [ ] `events/events.go`: add `LibraryItemPayload` unifying the private copies in `jellyfin`/`emby`/`plex` `library_catalog.go` (include `library_name` that jellyfin omits and optional `tmdb_id`/`tvdb_id`/`imdb_id` that `playback-monitor/internal/library_catalog.go` already reads).
3. [ ] `events/events.go`: add `GuardViolationPayload` matching `playback-guard/internal/notify.go` and `playback-monitor/internal/notification_rules.go` (`user`, `summary`, `rule_type`, `legacy_notify`).
4. [ ] `events/events_test.go`: freeze all five `Event*` string values and JSON round-trip the three payload structs (including omitempty zero values) so CI fails on silent tag/constant drift.
5. [ ] `README.md`: add the missing `EventPlaybackProgress` row; document the three payload types; list publishers (`jellyfin`, `emby`, `plex`, `playback-guard`) and consumers (`playback-monitor`, `admin-ui`); delete the "payloads may be added" hedge.
6. [ ] `AGENTS.md`: replace the sidecar/gRPC/TLS/`PORTS.md` boilerplate with library identity, a never-rename-event-type-strings compatibility rule, and the add-event checklist (constant + payload + test + README row).
7. [ ] `README.md` + `events/events.go` comments: document that this package owns mesh JSON field names and must stay field-aligned with `playback-contract` proto (list the shared session fields) so the two repos stop being undeclared dual sources of truth.
8. [ ] `events/events.go`: add `All() []string` and `Known(eventType string) bool` covering the five constants so `playback-monitor` subscribe sites (`library_catalog.go`, `guard_violation_events.go`) and admin notification rules can stop hardcoding `"playback.library.item"` / `"playback.guard.violation"`.
9. [ ] Add `CHANGELOG.md` and `COMPATIBILITY.md` (same shape as `contracts-downloader`) declaring v0.1.0 payload-add as the compatibility surface; note `muxcore.json` is still `0.1.0` and tag `v0.1.0` already exists.
10. [ ] Annotate PII fields (`ip_address`, `user_id`, `user_name`, `media_path`) on the session/library payloads in `events/events.go` comments and add a short README "PII in payloads" note so publishers do not log raw event bodies.

### Module: `contracts-reconciler`

**Status:** OPEN

1. [ ] Make `ParseDir` / `findInterfaceByName` (`reconciler/parser.go`) walk subpackages. `parser.ParseDir` only reads the clone root; real repos keep interfaces under `muxcore/*/v1` and `gen/...`. `TestParseDir_RecursiveFixture` and `TestParseDir_SiblingContractsMediaAdmin` already fail with "no Go packages found".
2. [ ] Align `canonicalRegistry` (`reconciler/canonical.go`) and README published table with generated proto names: `MediaAdminServiceServer`, `DownloaderServiceServer`, `IndexerServiceServer`, `NotificationServiceServer`, `MetadataServiceServer`, `ScannerServiceServer`, `AutomationServiceServer`. Registry keys (`MediaAdminService`, `Downloader`, `Indexer`, `NotificationProvider`, `MetadataProvider`) do not exist in those repos.
3. [ ] Fix `ApplyReplaceDirectives` (`reconciler/replace.go`) to pass `go mod edit -replace=old=new@version`. `ReplaceDirective.String()` emits `=>`, which `go mod edit` rejects (`separator between old and new is =, not =>`). Add a success-path test that writes a temp `go.mod` and asserts the replace landed.
4. [ ] Implement `CloneAndParse` (`reconciler/parser.go` ~247–288) using the same `git clone --depth 1 --branch` path as `Resolver.cloneRepo`, or unexport/delete it. It always returns `not implemented: use CloneRepo from the spool CLI` while SECURITY.md still lists it as a clone entry point.
5. [ ] Gate reserved registry rows so `Resolve` does not clone unpublished/wrong surfaces. `Playback` → `contracts-playback` (events only, no `Playback` interface, no v1.0.0 contract API); `Transcoder`, `ArtworkProvider`, `MediaDiscovery`, and peers have no workspace repos. Return nil (unknown/reserved) or mark them reserved instead of failing at `git clone`.
6. [ ] Add `reconciler/testdata/` snapshots of nested proto-shaped packages (minimal `DownloaderServiceServer` / `MediaAdminServiceServer` trees). Replace `/opt/repos/...` skip tests in `reconciler_test.go` so CI finds real Server interfaces without sibling checkouts or network.
7. [ ] Give `DryRun` (`reconciler/replace.go`) a `CacheDir` / `*Resolver` argument. It currently constructs `&Resolver{}` so any non-canonical declaration always hits the network and cannot reuse fixture caches used by `TestResolve_FixtureMatchReplace`.
8. [ ] Rewrite `AGENTS.md`: this is a library (`muxcore.json` type `contracts`), not a gRPC sidecar. Drop TLS/capability/ports boilerplate; document `go test ./...`, CacheDir, and that `Resolve` clones by import path.
9. [ ] Harden `Resolver.cloneRepo` / `importPathToRepoURL` (`reconciler/reconciler.go`): reject `file://`, relative paths, and hosts other than `https://` + a short allow-list (or require `github.com/` / configured origin). `https://` + first three path segments is attacker-controlled from `Declaration.Repo`.
10. [ ] Stop hard-pinning replace versions from the static registry (`v0.1.0` / `v1.0.0` in `canonical.go`). Let `Declaration` or `CanonicalRepo` carry an override, or pin the cloned canonical tag, so a third-party match against a newer published contracts-* tag is not rewritten to a stale registry version.

### Module: `contracts-scanner`

**Status:** OPEN

1. [ ] `proto/muxcore/scanner/v1/scanner.proto` `WatchDir` / `AddWatchDirRequest`: add `tv_library_path` and `music_library_path` (media-scanner already stores both; List/Add cannot expose or set them).
2. [ ] Same proto: add `UpdateWatchDir` (path, media_type, library paths) and `SetWatchDirEnabled` RPCs — `WatchDir.enabled` is read-only dead weight; only Add/Remove exist.
3. [ ] `ImportCandidate`: add parse-preview fields (`title`, `year`, `media_type`, `season_number`, `episode_number`, `artist`, `album`) so admin-ui `/import` can show a match instead of path/name/size only.
4. [ ] `ImportPathResponse`: replace counts-only with repeated per-file results (`original_path`, `destination_path`, `status`, `error`) so media-automation and admin-ui can see what imported vs why it skipped.
5. [ ] `ScanRequest` / `ScanLibraryRootsRequest`: add `dry_run`, optional `path`, and `media_type` — empty messages cannot do a targeted or safe scan.
6. [ ] `ListImportedRequest`: add `status` filter; add `error_message` on `ImportedFile` so failed imports are listable (status column exists, query does not).
7. [ ] `ImportedFile` + `ImportPathRequest`: add music fields (`artist`, `album`, `track_number`) and a `MediaType` enum including `music` — proto comments still say `"movie", "tv", "both"` while media-scanner imports audio.
8. [ ] `GetStatsResponse`: add last-scan `files_found` / `files_imported` / `files_skipped` and `last_error` from media-scanner `scan_log` (those columns already exist; stats omit them).
9. [ ] `.forgejo/workflows/ci.yml`: add a proto-check job (`make proto` then `git status --porcelain` fail) matching `contracts-downloader/.github/workflows/ci.yml`; add a `*_test.go` that asserts `ScannerService` RPC names/field numbers stay stable. No live indexers.
10. [ ] Add `CHANGELOG.md` + `COMPATIBILITY.md` (mirror `contracts-downloader`); rewrite `AGENTS.md` (this is not a sidecar, no ports); extend `README.md` with v0.1.0, regen (`make proto`), and music/TV dual-root surface. Fix `Makefile` `clean` so it does not `rm` published `muxcore/scanner/v1/*.go`.

### Module: `core`

**Status:** OPEN

1. [ ] Expand `.forgejo/workflows/ci.yml` (origin gate) to match the real suite: `golangci-lint`/`staticcheck`, `govulncheck`, `go test -tags=integration ./internal/integration/...`, and `sdk/go/{client,mock}` tests. Today Forgejo only runs `go test` + `go build`/`vet`; lint, SDK, integration, fuzz, and vulncheck live only in `.github/workflows/ci.yml`.
2. [ ] Fix the `StopAll`/`cmd.Wait` races in `internal/module/mgr` that caused CHANGELOG v0.5.5 to drop `go test -race` from CI; re-enable `-race` on the Forgejo test job (Makefile `test` still uses it).
3. [ ] Wire `eventstore.NewWithDir` from `cmd/muxcored/init.go` `initEventStore` via a `--eventstore-dir` flag (same pattern as `--task-dir` / `--deadletter-dir`). `NewWithDir` is implemented and tested; muxcored always calls in-memory `New()`.
4. [ ] Make household persistence discoverable: add `--task-dir`, `--idempotency-dir`, `--deadletter-dir`, `MUXCORE_EVENT_JOURNAL_PATH`, and the eventstore dir to `muxcore.example.json`, `muxcore.schema.json`, and `docs/operations.md`. Default them under `~/.muxcore/` (or document that a restart drops tasks, WAL, dead letters, and idempotency keys).
5. [ ] Fan local `bus.Publish` out to `cluster.Members()` via `EventService.Publish` (loop-safe) in `internal/grpcmesh` / `cmd/muxcored`. `EventServer.Publish` is inbound-only; comments still say “when NATS is not in use.” Cover with a two-server fixture in `internal/grpcmesh/events_test.go`.
6. [ ] Add tonic TLS/mTLS to `sdk/rust/src/runtime.rs` using `MUXCORE_TLS_{CERT,KEY,CA}` (match `sdk/python/src/muxcore/sdk/channel.py`). Rust v0.1.0 is plaintext-only and cannot dial production muxcored.
7. [ ] Add Discovery / Storage / Events client wrappers to `sdk/python` (then `sdk/typescript`) with bufconn/fixture tests. Both READMEs list this as out of scope; register/lifecycle alone is not a usable non-Go module SDK.
8. [ ] Fix docs drift: README L45 still says task redistribution is “planned” (`FailNodeTasks` is wired in `internal/bootstrap/wiring.go`); SECURITY.md still lists auth-failure backoff as “In Progress” and supports only `v0.1.x`; CONTRIBUTING.md and `docs/openapi.yaml` still cite deprecated `MUXCORE_DEV_TLS_SKIP`.
9. [ ] Replace README Quick Start `go install github.com/Muxcore-Media/core/cmd/muxcored@latest` (fails under Forgejo `GOPRIVATE`) with `make build` / `scripts/local-image.sh` and link in-repo `docs/operations.md`. Wiki-only Getting Started / Deployment / Security links are the only operator path today.
10. [ ] Make worker-pool redistribution survive a node death: persist by default (or replicate on Submit) so a survivor’s `FailNodeTasks` can see the departed node’s tasks. In-memory pools plus empty `--task-dir` mean the dead node’s tasks vanish. Add an `internal/integration` test: two pools, shared `FileStore`, `ClusterNodeLeft`.

### Module: `database-postgres`

**Status:** COMPLETE — Forgejo Postgres CI, real `rows.Columns()`, unix-socket/IPv6 DSN, transactional Migrate/Rollback, docs/ports fix, Dockerfile sibling clones, connect timeout + pool settings, settings swap drain, loopback bind + error mapping, Backupable via pg_dump.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/database-postgres.md -->

### Module: `database-sqlite`

**Status:** OPEN

1. [ ] Replace `guessColumns` in `internal/server/server.go` with `rows.Columns()` (add `Columns()` on `internal/db.Rows`). `SELECT *`, expressions, and mixed-case aliases currently guess the wrong column count/names (and uppercase them), so `Query` Scan fails or drops values.
2. [ ] Honor `ctx` in `internal/db/database.go`: switch Exec/Query/Begin/Ping/Migrate/Rollback to `*Context` / `BeginTx`. Cancellation and RPC deadlines are ignored today; add tests that a cancelled ctx stops in-flight work.
3. [ ] Implement `contracts.Backupable` (`ExportState`/`ImportState`) on `internal/module.go`: WAL `wal_checkpoint` + consistent snapshot (SQLite backup API or checkpoint+copy), advertise `backupable` in `muxcore.json` / `Info()`, add round-trip tests. `database-postgres/MIGRATION-SQLITE.md` already documents this path; the module has none.
4. [ ] Make Migrate/Rollback match `core/pkg/contracts/database.go`: wrap Up/Down + `_migrations` catalog write in one transaction; do not delete a catalog row when `Down` is empty; return `ErrMigrationTargetNotFound` for unknown targets. Today a failed catalog insert leaves schema applied but unrecorded.
5. [ ] Add `internal/server` tests (package has zero): Exec/Query/Transaction/Migrate/Rollback over the gRPC types, proto value round-trip (null/bytes/bool/int), empty-query `InvalidArgument`, and `rows.Err()` after the Next loop. Replace `test/integration_test.go` (it registers a fake module with core and never calls `DatabaseService`).
6. [ ] Fix live `db_path` swap in `internal/settings.go` / `ReplaceDatabase`: refcount or drain in-flight RPCs before `Close` on the old handle; reject empty/relative-escape paths that would overwrite another file; add a concurrent Query-during-swap test. Current Swap+Close races active Exec/Query.
7. [ ] Fix operator install so the DB actually persists: set `SQLITE_DB_PATH` to the data volume in `deploy/docker-compose.yml` and `_mvp/deploy/kustomize/base/database-sqlite.yaml` (`/data` is mounted but unused); align systemd `ReadWritePaths=/var/lib/database-sqlite` with that path; implement or remove Dockerfile `HEALTHCHECK --health-check` (main has no such flag; `EXPOSE 0` is wrong).
8. [ ] Fix origin CI in `.forgejo/workflows/ci.yml`: `go.mod` `replace => ../core*` fails on a standalone Forgejo checkout (`replacement directory ../core does not exist`). Checkout sibling modules or drop replaces for CI; run `golangci-lint` and `go test -race` (Makefile already does; Forgejo does not).
9. [ ] Repair docs/metadata drift: `COMPATIBILITY.md` still says v0.1.0 / core v0.4.0+ (module is 0.1.5, `go.mod` wants core v0.5.8); fold stale `[Unreleased]` into shipped versions in `CHANGELOG.md`; point README CI badge at Forgejo not GitHub Actions; replace `ghcr.io/yourorg` in `Makefile` and `@yourorg/your-team` in `.github/CODEOWNERS`; set `muxcore.json` `contracts` to the documented `DatabaseProvider` / `DatabaseService`.
10. [ ] Bind and error-map the unauthenticated SQL surface: default `DATABASE_GRPC_ADDR` to `127.0.0.1:9700` (not `:9700` all-interfaces) in `NewModule` / README / `.env.example`; map syntax/constraint failures vs driver faults to `InvalidArgument` vs `Internal` in `internal/server/server.go` (everything is `InvalidArgument` today); wire or delete unused `Server.Metrics()`.

### Module: `data-redaction-pattern`

**Status:** OPEN

1. [ ] In `internal/module.go` `redactValueWithPath`, apply regex rules to non-map array elements (and recurse nested arrays). `{"phones":["555-123-4567"]}` + `/\\d{3}-\\d{3}-\\d{4}/` currently leaks. Cover in `internal/module_test.go`.
2. [ ] Stop raw `strings.Contains` default/extra key matching in `redactMapWithPath`. Short tokens like `auth` redact `author` (media metadata). Match whole key or `_`/camel segments. Add `email`/`ip`/`ip_address` defaults or built-in regexes (Security.md claims these). Test `author` kept, `authorization` redacted.
3. [ ] Add `extra_rules` setting + `REDACTION_EXTRA_RULES` env in `settings.go`/`NewModule` so path/regex defaults persist without every caller passing `Rules`. Merge into `Redact` with request rules. Document in `README.md` and `.env.example`.
4. [ ] Bound `Redact` in `internal/module.go`: reject oversized `req.Data`, cap regex length/complexity in `compileRules`, honor `ctx` deadline on `Match` (ReDoS). Tests for invalid regex and oversized payload.
5. [ ] Make `Health` fail when the gRPC listener is not serving (`lis` nil or post-`Stop`). `Stop` must close `lis` if `Init` ran but `Start` did not. Set `Info().HTTPAddr` empty (it copies `grpcAddr`; this module has no HTTP).
6. [ ] Sync docs: `README.md` (`REDACTION_EXTRA_KEYS`, `settings` capability, `_mvp/run-host.sh` `MVP_ENABLE_DATA_REDACTION=1`); `AGENTS.md` contracts row (`DataRedactionProvider`, not "none"); `COMPATIBILITY.md` current=v0.1.5; `CHANGELOG.md` core pin v0.5.8 not v0.5.2; `.env.example` `REDACTION_EXTRA_KEYS`.
7. [ ] Add `pkg/client` implementing `contracts.DataRedactionProvider` over `dataredactionv1` so log/audit callers can dial this sidecar without hand-rolling proto. Document the call in `README.md` Usage.
8. [ ] Add a `module_test.go` gRPC round-trip: `Init`+`Start` on `:0`, dial `Redact`/`SupportedRules`, then `Stop`. `TestLifecycle` never exercises the RPCs.
9. [ ] Return active default/extra keys (and extra rules) from `SupportedRules` so admin-ui/operators can see what is redacted without reading source. Test in `module_test.go`.
10. [ ] Add golangci-lint to `.forgejo/workflows/ci.yml` (`.golangci.yml` + `Makefile` lint exist; Forgejo only runs `go test`/`go vet`). Add tests for `REDACTION_EXTRA_KEYS` env bootstrap, unknown `UpdateSetting` key, and bare (unprefixed) field rules.

### Module: `distributed-lock-sqlite`

**Status:** OPEN

1. [ ] Stop lock theft in `internal/module.go` `Acquire`: do not re-issue a token on holder_id match alone (use `Renew` or require the current token). Stop putting `holder_id` in the conflict `Error` string. Add a test that a second caller who copies the leaked/guessed holder cannot steal the lock.
2. [ ] Add `pkg/client` implementing `contracts.DistributedLockProvider` / `DistributedLockHandle` over `DistributedLockService` (map not-acquired → `ErrLockHeld`). README claims this module *is* that contract; `Module.Acquire` is the proto RPC and nothing in-tree dials it. Cover with bufconn tests.
3. [ ] Reject empty `key` / `holder_id`, `ttl_ms <= 0`, and empty `lock_token` on Unlock/Renew in `internal/module.go` (`InvalidArgument`). Add `module_test.go` cases. Unbounded/zero TTL and empty keys are accepted today.
4. [ ] Open SQLite with `_pragma=busy_timeout(5000)` (and keep WAL) in `Init` and `reopenDB` (`internal/module.go`, `internal/settings.go`), matching `database-sqlite`. `SetMaxOpenConns(1)` plus sweeper/Acquire/Renew has no timeout today.
5. [ ] Add a restart persistence test: Init → Acquire → Stop → new `Module` on the same `LOCK_DB_PATH` → Init → second holder must fail until TTL. README claims rows survive process restart; no test does.
6. [ ] Add a gRPC Serve test: `Start()` then dial `DistributedLockServiceClient` for Acquire/Unlock/Renew. `TestLifecycle` never issues an RPC; all lock tests call methods in-process.
7. [ ] Drain or refcount in-flight Acquire/Unlock/Renew before `Close` on the old handle in `reopenDB`; persist `db_path` / `sweep_interval` so admin-ui updates survive restart. Current swap races active `*sql.DB` use; settings are memory-only.
8. [ ] Use `filepath.Dir` in `Init` (same as `reopenDB`) instead of the manual `/`-scan; fail `NewModule` / `Init` on invalid `LOCK_SWEEP_INTERVAL` instead of silently keeping the default.
9. [ ] Docs/metadata: set AGENTS.md Contracts to `DistributedLockProvider` (says "none"); document `settings`, live `db_path`/`sweep_interval`, `MUXCORE_GRPC_INSECURE`, and `_mvp` `MVP_ENABLE_DISTRIBUTED_LOCK` in README / `.env.example`; fix CHANGELOG 0.1.3 (`go.mod` is core v0.5.8, not v0.5.2); wire `var version` in `cmd/module/main.go` so Makefile `-X main.version` works (Info is hardcoded `0.1.3`).
10. [ ] Default `LOCK_GRPC_ADDR` to `127.0.0.1:9604` in `NewModule`, README, and `.env.example` (`:9604` is all-interfaces). `Start` uses `grpc.NewServer()` with no TLS/auth; token unlock is the only gate and is bypassed by holder-id re-acquire (todo 1).

### Module: `downloader-debrid`

**Status:** OPEN

1. [ ] Wire mesh `EventPublisher` in `internal/module.go` `Start` (copy `downloader-sabnzbd` `dialCore` → `c.Events.Publish`). `cmd/module` never sets `Publish`, so `download.started`/`completed`/`failed` never reach core/automation/notifications.
2. [ ] After `realdebridAddMagnet` in `internal/debrid/client.go`, `POST /rest/1.0/torrents/selectFiles/{id}` with `files=all` and poll `GET /torrents/info/{id}` until downloaded (emit `download.failed` on error). Extend `internal/debrid/mock.go` + `TestAddCloudMagnet`. Magnets currently sit in `waiting_files_selection` forever.
3. [ ] Teach `handleVFS` / `findDownload` in `internal/vfs.go` to list RD torrents (`/rest/1.0/torrents`) and unrestrict completed files so Settings cloud library / `debrid:{id}` Play works after magnet add. VFS only reads `/downloads`; mock `AddMagnet` never inserts a download row.
4. [ ] Stop `handleVFSStream` from calling `m.client.HTTPClient.Do` (nil on the `cmd/module` path). Use the same fallback as `Client.http()` and add a stream test that does not inject `HTTPClient`.
5. [ ] Implement AllDebrid magnet/list/delete against mocks (`/v4/magnet/upload`, `/v4/magnet/status`) in `internal/debrid/client.go` + `mock.go`. Point `Health` at `/v4/user` — `alldebridList` is `return nil, nil`, so a bad AD token looks healthy.
6. [ ] Gate `POST /api/add` and `GET /api/vfs/stream` in `internal/module.go` with a shared secret or loopback-only bind (`DEBRID_HTTP_ADDR` default `127.0.0.1:9631`). Those endpoints spend the operator token with no auth.
7. [ ] Add `AddCloud` RPC to `proto/muxcore/debrid/v1/debrid.proto` and implement it on `debridServer` so automation can dispatch without raw HTTP. Cover with gRPC tests against httptest mocks.
8. [ ] Set `Info().HTTPAddr` to `httpAddr` (it copies `grpcAddr`). Align version (`muxcore.json` / `Info` / README `0.1.0` vs CHANGELOG `0.1.1`). Rewrite README “magnet/cloud add can expand later”; set AGENTS.md contracts to `DebridDownloaderService`; refresh COMPATIBILITY.md (core `0.5.2` vs go.mod `0.5.8`).
9. [ ] Add tests in `internal/module_test.go` / `vfs_test.go` for `POST /api/add` (magnet + hoster), gRPC Unrestrict/List/Delete, VFS Range/HEAD, and Start/Stop/Health. That HTTP/gRPC surface is untested.
10. [ ] Add a `lint` job to `.forgejo/workflows/ci.yml` (`golangci-lint run` via existing `.golangci.yml`) and set `http.Server` `ReadHeaderTimeout` in `Start` (gosec G112). Origin CI is Forgejo; the lint config is unused.

### Module: `downloader-native-torrent`

**Status:** COMPLETE — VPN rebind session migration, metainfo cache gating, contracts ListTorrents AND filters + file selection, live settings (listen/download/priority/seed), scoped kill switch + DNS, live VPN gate, completed/error persist, seeding upload stats, docs/CHANGELOG, contracts + mesh persist tests, golangci-lint CI.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/downloader-native-torrent.md -->

### Module: `downloader-native-usenet`

**Status:** COMPLETE — gRPC discovery HttpAddr, engine live-switch + settings.json persist, SSRF-safe fetchNZB, zip-slip/category containment, scoped delete, queue/history/NZB persist, multi-file download.completed, gRPC test coverage, streaming NNTP with article retry.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/downloader-native-usenet.md -->

### Module: `downloader-qbittorrent`

**Status:** OPEN

1. [ ] Add a live/mock completion poller in `internal/module.go` (mirror `downloader-sabnzbd` `watchJob`): after WebUI `AddTorrent`, poll `GetTorrent`/`ListTorrents` until `MapState` is `completed` or `failed` and emit `download.completed` / `download.failed`. Live path today only publishes `download.started`. Cover with `qbit.MockServer.Complete` / error state in `internal/module_test.go` (httptest only).
2. [ ] Honor the env names `run-host.sh` already injects (`QBIT_URL`, `QBIT_USERNAME`, `QBIT_PASSWORD`, `QBIT_GRPC_ADDR`, `QBIT_HTTP_ADDR`) in `NewModule` alongside `QBITTORRENT_*` / `MUXCORE_*_ADDR`. If a live URL is set, do not stay in fixture because the host defaults `QBIT_FIXTURE=1`. Document both names in `README.md`.
3. [ ] Make `contractsServer.AddTorrent` resolve the new hash deterministically in `internal/module.go` + `internal/qbit/client.go`: parse magnet `xt=urn:btih` case-insensitively, or add a unique tag and query `/torrents/info?hashes=`. Current list-all + `strings.Contains(url, hash)` + newest-`AddedOn` fallback picks the wrong torrent when the mock (or WebUI) already has peers. Add a two-torrent case to `TestMockWebUIHealthAndList`.
4. [ ] Support qBittorrent 5.x WebAPI in `internal/qbit/client.go`: try `/torrents/stop` and `/torrents/start` (and add form `stopped=true`) with fallback to `/pause`/`/resume`/`paused`. Extend `internal/qbit/mock.go` with the 5.x paths and test both.
5. [ ] Send `Referer`/`Origin` matching `BaseURL` on every WebAPI call (qBit 4.6.1+ CSRF) and, on HTTP 403 in `doForm`/`doGET`, clear `logged` and retry login once. Mock an expired SID in `client_test.go`.
6. [ ] Change `Client.GetTorrent` to query `/torrents/info?hashes=` instead of listing the full queue. Populate `TorrentInfo.files` in `mapTorrent` from `/torrents/files`, and put `content_path` / file paths on live `download.completed` payloads (fixture already sets `ContentPath`). Extend the mock + tests.
7. [ ] Make HTTP `/healthz` in `Module.Start` call `Health()` and return 503 when the live WebUI probe fails; keep 200 for fixture and unconfigured soft-empty. Add a module test that closes the mock and asserts non-200.
8. [ ] Honor `deleteFiles` in `FixtureClient.Delete` (`internal/qbit/fixture.go`): remove the materialized `ContentPath` tree when true. Add `fixture_test.go` coverage.
9. [ ] Take the write lock in `rebuildClient` (`internal/module.go` currently writes `m.client` under `RLock`). Add `settings_test.go` for `UpdateSetting` (`base_url`/`username`/`password` rebuild the client; `mode` stays read-only; masked `********` is a no-op).
10. [ ] Fix docs drift: `AGENTS.md` says "Contracts: none" but `muxcore.json` declares `contracts-downloader` `Downloader` v0.1.0; `README.md`/`COMPATIBILITY.md` omit capability `downloader.torrent`; `COMPATIBILITY.md` Core `0.5.2+` vs `go.mod` `0.5.8` / `minCoreVersion` `0.4.0`. Add live-poll events and env aliases to the README tables.

### Module: `downloader-sabnzbd`

**Status:** COMPLETE

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/downloader-sabnzbd.md -->

### Module: `emby`

**Status:** OPEN

1. [ ] Map `RunTimeTicks` on `embyItem` in `internal/client.go` and request `Fields=Path,Width,Height,RunTimeTicks` on `GET /Sessions` in `listSessions`; set `DurationSeconds` in `sessionToEvent` (`internal/playback.go`). Playback-monitor watched-% (90% of duration) stays 0 for every Emby session today.
2. [ ] Default-deny `POST /emby/sse/events` when `EMBY_SSE_SECRET` is empty (`checkSSEAuth` in `internal/sse.go` currently returns true); require `X-Emby-SSE-Secret` or `Authorization: Bearer`. HTTP listens on `:8477` (all interfaces). Add a 401 httptest and expose `emby_sse_secret` in `settingsDefs`.
3. [ ] After the wait in `pollSessionsLoop`, call `pollSessionsOnce` when `wsConnectedNow()` (jellyfin already does this). Set `SetReadDeadline` on the Emby websocket in `runWSConnection` so a hung socket drops `wsConnected` and poll resumes.
4. [ ] Page `listEmbyItems` with `StartIndex`/`Limit` (body is capped at 8MiB and silently truncates). Sync per VirtualFolder `ParentId` in `syncLibraryCatalog` so `library_name` is set for episodes (today `libNames[it.ParentId]` is the season). Request `ProviderIds` and emit `imdb_id`/`tmdb_id`/`tvdb_id` for playback-monitor.
5. [ ] Persist connection knobs to `{EMBY_DATA_DIR}/settings.json` (jellyfin pattern). `updateSetting` is memory-only; restart drops admin-ui changes. Add settings for `emby_sse_secret`, `EMBY_WEBSOCKET`, and `EMBY_CATALOG_SYNC_SEC`.
6. [ ] Make `GET /healthz` in `internal/module.go` call `probeSystemInfo` / `Health()` and return non-200 when unconfigured or Emby is down. It always writes `{"status":"ok"}` today.
7. [ ] Reconnect in `connectCore` after a successful dial: on `Events.Publish` failure or client close, clear `m.mc` and retry with backoff. First success currently `return`s; a mesh drop silences playback and catalog events until process restart.
8. [ ] Fill `parseSSESessionEvent` (`internal/sse.go`) with `title`, `user_name`, `media_path`, `media_type`, and duration from the Tracearr JSON. Ingest currently publishes started/progress/stopped with only session/item/user/position.
9. [ ] Add httptest coverage in `internal/` for `syncLibraryCatalog`, `handlePluginLibrarySSE` (upsert + removed), `Health` configured vs empty, duration on poll, and `TerminateSession` with empty `session_id`. Catalog and SSE auth have no tests.
10. [ ] Document `EMBY_CATALOG_SYNC_SEC` (default 21600), `EMBY_DATA_DIR`, and SSE auth in `README.md`; add `.env.example`. Set `AGENTS.md` Contracts to `contracts-playback` + `playback-contract` (it says "none"). `muxcore.json` `contracts` is `[]`.

### Module: `encryption-aesgcm`

**Status:** OPEN

1. [ ] In `internal/settings.go` `setKeyFile`, refuse a path that does not already exist (do not call `loadOrBootstrapRing` generate). Switching `key_file` in admin settings today mints a new ring and orphans live ciphertext. Add a `settings_test.go` case that the old blob still decrypts after the error.
2. [ ] In `internal/module.go` `persistRingLocked`, `Sync()` the temp file (and the parent directory) before `Rename`. Rotation can lose the new active key on crash/power loss. Cover with the existing rotate-persist test still reading a valid JSON ring.
3. [ ] Persist `loadEnvKey` to `ENCRYPTION_KEY_FILE` (mode `0600`) when no JSON keyring exists. Env-only bootstrap is memory-only until `RotateKey`; losing the env makes all ciphertext unrecoverable. Add a restart test that unsets `ENCRYPTION_MASTER_KEY` and still decrypts.
4. [ ] Default `ENCRYPTION_GRPC_ADDR` to `127.0.0.1:9601` in `NewModule` and stop host-publishing `9601` in `deploy/docker-compose.yml`. `Encrypt`/`Decrypt`/`RotateKey` are on a raw `grpc.NewServer()` with no auth; published `:9601` is a LAN decrypt/rotate endpoint.
5. [ ] Fix the distroless `nonroot` image: README `docker run` has no volume, so `MkdirAll(/var/lib/encryption-aesgcm)` fails. Ship a writable `ENCRYPTION_KEY_FILE` (volume + documented path). Strip `replace => ../core` in `Dockerfile` so standalone GHCR builds match CHANGELOG (published `core@v0.5.x` pins).
6. [ ] Expose read-only `active_key_id` / `key_count` and a `rotate_key` update in `internal/settings.go`. Document in `README.md` that losing the keyring loses all ciphertext, plus backup/restore of the JSON file. Add `StateDirectory=encryption-aesgcm` to `deploy/systemd/muxcore-module.service` (`ProtectSystem=strict` does not create the dir).
7. [ ] Add negative tests in `internal/module_test.go`: empty/short ciphertext, truncated `MXE1` header, unknown key id, invalid and wrong-length `ENCRYPTION_MASTER_KEY`, empty/garbage key file, `Health` before `Init`, and JSON keyring winning over env.
8. [ ] Run `golangci-lint` and `go test -race` in `.forgejo/workflows/ci.yml` (origin CI). Makefile `make lint` / `make test` already do this; Forgejo only runs `go test` + `go vet`.
9. [ ] Fix docs drift: `AGENTS.md` says "Contracts: none declared" but `muxcore.json` / `Info()` declare `EncryptionProvider`. Point the README CI badge at Forgejo (not GitHub Actions). State `ENCRYPTION_MASTER_KEY` vs JSON precedence and the backup requirement in `README.md` / `.env.example`. Point `CONTRIBUTING.md` clone at Forgejo origin.
10. [ ] Cap `Encrypt` plaintext size and set `grpc.MaxRecvMsgSize` on the module server in `internal/module.go`. Return `google.golang.org/grpc/status` codes (`InvalidArgument` / `FailedPrecondition` / `NotFound`) instead of bare `fmt.Errorf` so callers can tell corrupt blob vs missing key vs not initialized.

### Module: `executor-shell`

**Status:** OPEN

1. [ ] Wire worker-pool dispatch in `internal/module.go`: serve `ModuleMesh.Call` method `Execute` that unmarshals `contracts.WorkerTask` JSON (what `core/internal/workerpool/dispatcher.go` sends) and runs `task.Payload`; `RegisterSettings` currently returns `unknown method "Execute"`, and the local `ExecutorService` proto is unused by core.
2. [ ] Replace prefix `HasPrefix` allowlisting in `commandAllowed` with first-token / argv matching (reject `;|&`$()` and similar) so an allow of `echo` cannot run `echo hi; rm -rf /`; add cases in `internal/module_test.go`.
3. [ ] Apply a default `EXECUTOR_TIMEOUT` (env + settings) in `Execute` when the gRPC/mesh context has no deadline; dispatcher uses the long-lived loop ctx, so `sleep infinity` never dies. Document in `README.md` / `SECURITY.md`.
4. [ ] Bound stdout/stderr in `Execute` (`io.LimitReader` or equivalent) and fail closed on overflow; unbounded `bytes.Buffer` is a memory DoS. Test in `internal/module_test.go`.
5. [ ] Document `EXECUTOR_ALLOW_WILDCARD` in `README.md` env table and `SECURITY.md`; reject `*` in `UpdateSetting` unless the flag is set; fix `deploy/docker-compose.yml` (`EXECUTOR_ALLOWED_COMMANDS=*` without the flag denies everything). Add a test that `*` without the flag is denied.
6. [ ] Change code/README default `EXECUTOR_GRPC_ADDR` from `:9602` (cache-local in `_mvp/PORTS.md`) to `:9605` to match `_mvp/run-host.sh`; add an executor-shell row to `PORTS.md`; update `deploy/docker-compose.yml` published port.
7. [ ] Fix `Dockerfile`: `gcr.io/distroless/static-debian12` has no `sh`, so `exec.CommandContext(..., "sh", "-c", payload)` cannot run. Use a base that ships `sh` (or drop `sh -c` after argv split) and keep non-root.
8. [ ] Stop logging the full rejected payload in `Execute` (`slog.Warn(..., "command", payload)`); log a truncated/hashed form. Same for allowlist values in `Init` if they can contain secrets.
9. [ ] Add missing `internal/module_test.go` cases: empty payload; non-zero exit uses stderr; `UpdateSetting` unknown key; `*` without `EXECUTOR_ALLOW_WILDCARD`; prefix-injection payloads from todo 2.
10. [ ] Fix docs drift: `AGENTS.md` says "Contracts: none declared" but `muxcore.json` / `Info()` declare `Executor`; README should document `MVP_ENABLE_EXECUTOR_SHELL`, live `allowed_commands` settings, and that `Info().HTTPAddr` is the gRPC dial target for admin-ui settings.

### Module: `feature-flags-file`

**Status:** OPEN

1. [ ] Make `Module.Health` and `GET /health` in `internal/module.go` fail (non-ok JSON) when `loadFile` last failed or the flags file is missing; persist `lastLoadErr`. Init currently logs a warning and serves an empty map as healthy.
2. [ ] Set `Module.Info().HTTPAddr` to `httpAddr` (`:9404`) instead of `grpcAddr` (`:9402`) so mesh registration and admin-ui advertise the health listener.
3. [ ] In `internal/settings.go` `updateSetting`, parse and validate the new path before assigning `filePath`; on load failure keep the previous path and flags. Reject empty / `.` / `..` paths. Add tests for missing file and bad YAML.
4. [ ] In `Module.Stop`, shut down the HTTP server (`http.Server.Shutdown` or close `httpLis`). Today only `grpcSrv.GracefulStop` runs; the `/health` goroutine leaks across restart.
5. [ ] Expose flag contents to operators via settings (a `flags_yaml` textarea or per-flag defs in `internal/settings.go`) and ship a repo `flags.yaml` example. Path-only `flags_file` cannot toggle flags from admin-ui.
6. [ ] Honor `FeatureFlagProvider` targeting in YAML (`percent` 0–100 and/or `enabled_for`) inside `IsEnabled`/`GetVariant`, using caller identity from incoming gRPC context/metadata. This is the only `feature.flags` module and it ignores `ctx`.
7. [ ] Implement `cmd/module/main.go --health-check` so `Dockerfile` HEALTHCHECK works; `EXPOSE` 9402/9404 not `0`. Mount `FEATURE_FLAGS_FILE` in `deploy/docker-compose.yml`. Add `Environment=FEATURE_FLAGS_FILE=` and `ExecReload=/bin/kill -HUP $MAINPID` to `deploy/systemd/muxcore-module.service`.
8. [ ] Fix docs drift: `AGENTS.md` "Contracts: none"; `COMPATIBILITY.md` still v0.1.0 / core v0.4.0; `.env.example` still `YOUR_MODULE_*`; `Info().Description` claims JSON; README omits `settings` / `flags_file` / `MVP_ENABLE_FEATURE_FLAGS` and badges GitHub CI.
9. [ ] Replace `test/integration_test.go` (registers a fake `test-module`, never calls FeatureFlagsService) with a gRPC client test for `IsEnabled`/`GetVariant`. Add unit tests that bad YAML on reload keeps old flags and that `GET /health` is reachable after Start.
10. [ ] Run `golangci-lint` in `.forgejo/workflows/ci.yml` (`.golangci.yml` already exists; Makefile `lint` is unused on origin). Wire Makefile/release `-X main.version` to a `version` var in `cmd/module/main.go`.

### Module: `health-monitor`

**Status:** OPEN

1. [ ] Implement active monitoring in `internal/module.go`: after mesh connect, call Discovery `ListAll` (client `Raw()` today) and poll each module's `http_addr` `/health` on `poll_interval`; feed results through `ReportHealth`. Declare the `HealthMonitor` contract in `muxcore.json`. Today the module is a passive sink; `_mvp/cmd/healthtick` fakes a always-`running` `mvp-healthtick` so `/status` is not `idle`.
2. [ ] In `ReportHealth`, emit `module.degraded` on first-seen `degraded` (not only `running`→`degraded`) and emit `module.recovered` on `degraded`/`stale`→`running`. Cover both in `internal/module_test.go`.
3. [ ] Fan-out payload must match `contracts.ModuleDegradedPayload` (`error`, not `message`) and use `contracts.EventModuleDegraded` instead of the `"module.degraded"` literal in `internal/module.go`. Add `EventModuleStale` / `EventModuleRecovered` next to it in `core/pkg/contracts/events.go` and use those constants.
4. [ ] Retry `dialCore` with backoff until `MUXCORE_GRPC_ADDR` is reachable (or until `Stop`). One failed dial currently disables mesh fan-out for the process lifetime; `Health()` always returns nil even when the mesh never connected.
5. [ ] Bind gRPC/HTTP to `127.0.0.1` by default, or require mesh TLS / a shared token on `ReportHealth`, `PublishEvent`, and `GET /status`. Defaults `:9202`/`:9203` listen on all interfaces with no auth; anyone on the network can spoof degradation or read error strings.
6. [ ] On `module.degraded` / `module.stale`, `FindByCapability("notification")` and call `Notify` (severity warning). `notification-default` only subscribes to media/download events, so operator alerts never fire from this module.
7. [ ] Fix `Dockerfile`: `HEALTHCHECK` runs `/module --health-check` (flag does not exist; `cmd/module/main.go` has no handler) and `EXPOSE 0`. Expose `9202`/`9203` and health-check `GET /health`. `deploy/docker-compose.yml` must publish `9203` and set `HEALTH_MONITOR_HTTP_ADDR`.
8. [ ] Docs drift: `README.md` and `.env.example` omit `HEALTH_MONITOR_HTTP_ADDR`, `HEALTH_MONITOR_INTERVAL`, `GET /status`, and settings `poll_interval`. `COMPATIBILITY.md` still says v0.1.0 / core v0.4.0+ (module is 0.1.5 / core 0.5.8). Collapse `CHANGELOG.md` `[Unreleased]` (still labels 0.1.1) into the 0.1.5 notes. Point the README CI badge at Forgejo, not GitHub Actions.
9. [ ] Origin CI (`.forgejo/workflows/ci.yml`): add a `golangci-lint` job matching `Makefile` `lint` / `.golangci.yml`. Forgejo currently only runs `go test` + `go vet`.
10. [ ] Replace `test/integration_test.go` (build-tagged starter that registers a fake module with core and never imports this package) with unit tests in `internal/module_test.go` for mesh allowlist (`custom.alert` stored, not published), empty `module_id` skip, `GET /health`, and the 50-event ring trim.

### Module: `indexer-piratebay`

**Status:** OPEN

1. [ ] In `internal/module.go` `Search`, set `SearchResponse.Total` to the unpaged `len(hits)` before `pageHits`. Today it is `len(results)` after paging. Add a test in `module_test.go` that `offset=1,limit=1` on the search fixture still reports `Total=2`.
2. [ ] In `internal/module.go`, call `cacheHitsInStorage` and `dialCore` with `context.WithoutCancel` (as `indexer-torznab` already does). Add a `mesh_meta_test.go` case that a cancelled Search ctx still writes `torrent/{infohash}/index.json` via an injected fake Storage — no live mesh.
3. [ ] In `internal/module.go` `Search`, when `type=tv`, append `Sxx` / `SxxExx` / absolute to the Apibay `q` if proto `season`/`episode`/`absolute` are set and the token is not already in the query (same `Contains` guard as movie year). Cover with httptest in `module_test.go`.
4. [ ] Add `internal/settings_test.go`: `UpdateSetting("base_url", …)` fails without `WG_CONF`; `enrich_limit` rejects 0 and clamps 99→25; unknown key errors; `Settings()` reflects values after a successful update on an HTTP-injected module.
5. [ ] Origin CI in `.forgejo/workflows/ci.yml`: run `golangci-lint` (Makefile `lint` / `.golangci.yml` exist; GitHub CI lints; Forgejo does not). Checkout sibling `core` + `contracts-indexer` or drop `replace => ../` so `go test` works outside the umbrella.
6. [ ] Docs drift: AGENTS.md says "Contracts: none declared" but `muxcore.json`/`Info()` declare `contracts-indexer`; bump `COMPATIBILITY.md` from v0.1.0 / core v0.4.0+ to v0.1.1 / core v0.5.8+; add `PIRATEBAY_MIN_INTERVAL` and `PIRATEBAY_CACHE_TTL` to the README config table; move CHANGELOG Unreleased (VPN/fixture/year) into a dated version; align `Info()` contract Version (`v1`) with muxcore.json (`v0.1.0`).
7. [ ] Wire `var version` in `cmd/module/main.go` so Makefile `-X main.version` works; pass it into `Config`/`Info()` instead of hardcoded `"0.1.1"` in `internal/module.go`.
8. [ ] `Health` in `internal/module.go`: when live HTTP requires VPN, re-check the bound iface still has IPv4 (`interfaceIPv4` / `validateWGConfReadable`); fixture and unconfigured stay healthy. Test live+missing iface vs fixture — no Apibay dial.
9. [ ] In `internal/client.go` `getJSON`, set a stable `User-Agent` (e.g. `muxcore-indexer-piratebay/<version>`). Assert the header in `TestApibayClientHTTPtest`.
10. [ ] `NewModule` must not install a clearnet `http.Client` when `baseURL` is set and VPN bind fails (today it does at `module.go` ~118–121). `Search` must error if live HTTP is required and `vpnIface` is empty — file-only `enforceLiveIndexerVPN` is not enough. Test: readable WG file, bind failed/skipped, Search does not dial.


### Module: `indexer-torznab`

**Status:** COMPLETE — LAN/docker VPN exemptions, Prowlarr ListIndexers + indexer_ids, caps-driven GetCapabilities, Health probe, ResourceExhausted on 429, apikey stripping, music/book/year, docs v0.1.5, lint CI, Dockerfile.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/indexer-torznab.md -->

### Module: `input-validate-jsonschema`

**Status:** OPEN

1. [ ] Add `pkg/client` implementing `contracts.InputValidator` over `InputValidateService` (bufconn tests). Document `FindByCapability("input.validate")` + dial `HTTPAddr`. The sidecar advertises the contract but nothing in-repo can call it.
2. [ ] In `internal/module.go` `validateJSON`/`validateRegex`, return `Valid=false` + `Errors` (not a gRPC error) for compile failures, missing named files, and bad regex — per `core/pkg/contracts/input_validator.go`. Tests: missing file, invalid schema JSON, invalid regex.
3. [ ] Make `SupportedSchemas` list `json:<name>` for each `*.json` under `dataDir` (plus the type prefixes). Add a test with a fixture file. Contract says callers check this before `Validate`.
4. [ ] Restrict jsonschema `$ref` in `validateJSON`: custom `jsonschema.Compiler` `LoadURL` that rejects `http`/`https` and `file` paths outside `dataDir`. Test inline `$ref` to `file:///etc/passwd` and `http://127.0.0.1`.
5. [ ] Cap request size and regex runtime in `Validate`: reject schema/data over a documented max; run regex match under `ctx` deadline (or regexp2 timeout). Set gRPC `MaxRecvMsgSize` to match. Tests for oversize and timeout.
6. [ ] Flatten `jsonschema.ValidationError` in `validateJSON` (emit root `Message` when `Causes` is empty; recurse). Empty `Errors` with `Valid=false` is a silent fail for callers.
7. [ ] Make `Health` check the listener and a readable `dataDir`; `Init` should `mkdir` `dataDir`; `Stop` should close `lis` if `grpcSrv` is nil. `TestLifecycle` currently cannot detect a dead server.
8. [ ] Origin CI `.forgejo/workflows/ci.yml`: drop or satisfy `replace => ../core` (and `../contracts-media`) so an isolated Forgejo checkout works; run `go test -race` and `golangci-lint` (Makefile already does both).
9. [ ] Docs/version sync: README min core `0.4.0` → `0.5.0`; AGENTS.md "Contracts: none" → `InputValidator`; COMPATIBILITY current `v0.1.0` → `v0.1.3` / core `v0.5.8`; CHANGELOG 0.1.3 `v0.5.2` → `v0.5.8`. Wire `main.version` so Makefile `-X main.version` works; stop hardcoding `"0.1.3"` only in `Info()`.
10. [ ] Operator surface: ship `schemas/` examples (`user-create.json` etc.); `UpdateSetting("data_dir")` reject empty/non-dir paths; document `MVP_ENABLE_INPUT_VALIDATE`, example schemas, and size limits in README + `.env.example`.

### Module: `jellyfin`

**Status:** COMPLETE — Paginated library/userdata fetch, webhook secret gates all HTTP surfaces, PlayURL 10.9+ deep links, mesh subscribe redial, SSE default off, music/book types, ListSessions RPC, library_name catalog + stale link removal, playback userdata kind/href + userdata_user_map setting.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/jellyfin.md -->

### Module: `logging-file`

**Status:** OPEN

1. [ ] Implement advertised `contracts.StructuredLogger` + `RedactingLogger` on `internal.Module` (Debug/Info/Warn/Error/Level/IsRedacting) and add `pkg/client` that wraps `LogService` over gRPC (bufconn tests). `muxcore.json` / `Info()` claim StructuredLogger; nothing in-tree dials `LogService` today so enabling the sidecar writes no household logs.
2. [ ] Flush the 64KiB `bufio.Writer` on a short interval (or after each `Log`) in `internal/module.go`; add setting/env `flush_ms` / `LOG_FLUSH_MS`. Crash or `Health` today can lose a full buffer. Test that a `Log` is readable from disk without `Stop`.
3. [ ] Make `Health` fail when the file is not writable or gRPC is not serving (`lis`/`grpcSrv` nil, after `Stop`). On `Init` listen failure, nil `m.file` after closing it. `TestHealth` only checks a live in-process file pointer.
4. [ ] Constrain `log_path` / `LOG_FILE_PATH` in `internal/settings.go` `reopenLogPath` to a configured root (`LOG_ALLOWED_ROOT` or the initial path's directory). Reject empty and `..` escapes so admin settings cannot append to arbitrary files (mode 0640).
5. [ ] Authenticate `Log` / `SetLevel` on the `:9625` server in `internal/module.go`. `grpc.NewServer()` has no interceptors; any host-local client can inject JSONL or `SetLevel(ERROR)` to hide failures. Require mesh identity or the module token; cover denied calls in tests.
6. [ ] Origin CI in `.forgejo/workflows/ci.yml`: run `golangci-lint` (`.golangci.yml` + `make lint` exist) and `go test -race` (Makefile already does; Forgejo is `-count=1` only).
7. [ ] Docs drift: `AGENTS.md` says "Contracts: none declared" vs `muxcore.json` StructuredLogger; `COMPATIBILITY.md` still lists v0.1.0 / core v0.5.0 as Current (module is 0.1.2, `go.mod` core v0.5.8); README + `.env.example` omit live settings keys and `LOG_MAX_SIZE_MB`/`LOG_MAX_BACKUPS`; document `MVP_ENABLE_LOGGING_FILE=1`.
8. [ ] Rotation: delete leftovers above `max_backups` when that setting is lowered in `internal/module.go` `rotate`/`updateSetting`; extend `TestRotation` to assert `.1`…`.N` count; add settings tests for `stdout`, `max_size_mb`, `max_backups`, unknown key (only `level`/`log_path` are covered).
9. [ ] Fix `Dockerfile`: drop sibling `COPY core/` (`go.mod` has no `replace`); `make docker` uses module-root context and will fail. Distroless `nonroot` cannot write default `/var/lib/logging-file/module.log` — set a writable default or document a volume.
10. [ ] Single version + env parity: declare `var version` in `cmd/module/main.go` so Makefile `-X main.version` works; stop hardcoding `"0.1.2"` only in `Info()`. Accept the same truthy set for `LOG_STDOUT` as settings (`true|1|yes|on`). slog-warn and keep defaults on invalid `LOG_MAX_SIZE_MB` / `LOG_MAX_BACKUPS` (today silently ignored).

### Module: `media-audiobooks`

**Status:** OPEN

1. [ ] Expose library scan to operators: add `rpc ScanLibrary` to `proto/muxcore/audiobooks/v1/audiobooks.proto` and `POST /api/scan` in `internal/http_api.go`, then run `ScanLibrary` from `Module.Start` after the store opens. Scan exists only as an in-process helper (`internal/module.go` / `internal/scan.go`); vault/run-host never imports files. Cover HTTP + startup in `internal/http_api_test.go` / `internal/scan_test.go`.
2. [ ] Stop nesting the library under the data dir. `NewModule` sets `libraryDir = filepath.Join(cfg.DataDir, "audiobooks")`, so `_mvp/run-host.sh` (`AUDIOBOOKS_DATA_DIR=$DATA/audiobooks`, no `AUDIOBOOKS_LIBRARY_DIR`) scans `$DATA/audiobooks/audiobooks`. Default `AUDIOBOOKS_LIBRARY_DIR` to `AUDIOBOOKS_DATA_DIR` when unset; mkdir+validate on `UpdateSetting("library_dir")` in `internal/settings.go`. Document the layout in README.
3. [ ] Add `UpdateAuthor` / `UpdateAudiobook` (name/path/monitored; title/narrator/ASIN/year/monitored) in proto + `internal/store.go` + `abServer`. Monitored is write-once at Add*, so `GET /api/missing` and `media-automation` `syncWantedAudiobooks` cannot drop a title without `RemoveAuthor`. Tests in `internal/store_test.go`.
4. [ ] Honor `RemoveAuthorRequest.delete_files` (ignored in `internal/module.go`) — unlink files only when the path is under configured `library_dir`. Add `GetAudiobook` / `RemoveAudiobook` RPC + store methods (neither exists). Tests for keep-files vs delete-files and out-of-root refusal.
5. [ ] Make missing-on-disk real: `ScanLibraryRoot` must delete `audiobook_files` rows whose paths `Stat` fail; `ListMissingAudiobooks` must treat a monitored book with only vanished files as missing. Today rescan only skips known paths (`internal/scan.go`) so deleted audio stays “present” and never reaches `/api/missing`.
6. [ ] Put files + missing on the gRPC surface: add `rpc ListAudiobookFiles` / `rpc ListMissing` and `files` (id/title/path) on proto `Audiobook`; fill them from `store.ListAudiobookFiles` / `ListMissingAudiobooks`. Mesh clients currently cannot see chapters or wanted-vs-on-disk without scraping health HTTP.
7. [ ] Add fixture-only import: `rpc ImportAudiobookFile` + `POST /api/audiobooks/{id}/import` that attaches an existing file under `library_dir` to a missing audiobook (reject paths outside the root). No Audible/ASIN/indexers — `internal/testdata/library` stubs only. This is the acquire→import hole; automation can mark wanted titles imported after a fixture drop.
8. [ ] Add `GET /api/audiobooks/{id}` (author + `files[]`) and `GET /api/files/{id}/stream` in `internal/http_api.go` (`http.ServeFile` after confirming `AudiobookFile.Path` is under `library_dir`; support Range). Attach `files` + `stream_url` on `GET /api/audiobooks` like `media-books` `bookJSONWithFiles`. Consumer SPA says “Listen” but the list JSON has no playable URL. Test with testdata stubs and a path-escape 404.
9. [ ] Add gRPC integration tests (Start + `AudiobookManagementService` client on `127.0.0.1:0`) for Add/Get/List/Remove author+audiobook, Scan, ListMissing, Import, and stream-adjacent file list. `internal/*_test.go` never dials the service; proto regressions are invisible.
10. [ ] Fix operator/docs drift: set `Module.Info().HTTPAddr` to `httpAddr` (it copies `grpcAddr` in `internal/module.go`); make `Health()` `SELECT 1` the SQLite store; enable `PRAGMA foreign_keys=ON` in `OpenStore` (comment already admits cascades are off); bump `muxcore.json` off `0.1.0` “scaffold”; document `GET /api/missing`, scan/import/stream, and `AUDIOBOOKS_LIBRARY_DIR` in README/CHANGELOG/COMPATIBILITY; drop Audible/ASIN paid-API follow-ups from README Status (Appendix E).

### Module: `media-automation`

**Status:** COMPLETE — non-video indexer types + scoring, library sync httests, RemoveFromQueue inflight cancel, SetMonitored, wanted_search_limit setting, TLS peer dial, operator RPC tests, Forgejo lint/race CI, docs/contracts sync.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-automation.md -->

### Module: `media-books`

**Status:** COMPLETE — ScanLibrary gRPC/HTTP + startup scan, library_dir defaults, UpdateAuthor/Book, delete_files guards, missing-on-disk purge, ListBookFiles/ListMissing, ImportBookFile, stream path guard, gRPC integration tests, MediaAdminService, docs v0.3.0.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-books.md -->

### Module: `media-comics`

**Status:** COMPLETE — library scan RPC/HTTP + startup import, library_dir defaults, UpdateSeries/Issue, delete_files with root guard, missing-on-disk reconciliation, gRPC path/has_file/ListMissing/ImportIssue, issue stream HTTP, integration tests, Health ping + docs.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-comics.md -->

### Module: `media-custom-formats`

**Status:** COMPLETE — parseQuality fixes, TRaSH stale-cache refresh, SQLite settings persistence, ScoreRelease profile thresholds + quality items, release-profile RPC hardening, Forgejo lint CI, docs v0.5.8, trash sync interval ticker.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-custom-formats.md -->

### Module: `media-dlna`

**Status:** OPEN

1. [ ] Restart the DMS process after mesh/admin `UpdateSetting` in `internal/settings.go` / `internal/module.go`. Today `updateSetting` only mutates fields; `internal/dms_runner.go` keeps the old `FriendlyName`, `RootObjectPath`, bind, and transcode flags until process restart. After a successful write, `runner.stop` + `startDLNA`. Cover with a start → change `friendly_name`/`media_paths` → `Status` reflects the new root/name test in `internal/module_test.go`.
2. [ ] Stop hard-failing `Init`/`refreshPaths` when no library path is set (`internal/module.go`). Start gRPC + `/healthz` in degraded mode (`configured=false`) so admin can set `media_paths` and then start DMS. Right now a missing `DLNA_MEDIA_PATH` prevents the module from registering at all, so settings cannot fix it.
3. [ ] Stop exposing sibling directories in `internal/roots.go` `browseRootForPaths`/`commonParent`. Two library dirs that only share `/mnt` or `/data` become that parent and DMS serves everything under it. Refuse a parent that contains entries outside the configured paths, or serve a virtual multi-root listing only those paths. Add a test with two sibling trees plus a secret file in the parent that must not be browsable.
4. [ ] Restrict SSDP and DLNA HTTP to the LAN by default in `internal/dms_runner.go` `activeInterfaces` / `listenTCP`. Empty `DLNA_IFNAME` currently announces on every up iface (ZeroTier, wg, docker) and `:9750` binds all addresses. Skip `zt*`, `wg*`, `docker*`, and down/loopback unless `DLNA_IFNAME` is set; document the default in README.
5. [ ] Surface the remaining env knobs as settings in `internal/settings.go` (`ignore_hidden`, `ignore_unreadable`, `ignore_paths`, `force_transcode`, `no_probe`, `notify_interval`). They exist in `applyEnv` / `dmsConfig` but not in `Settings()`, so admin-ui cannot change them. Apply via the restart path from todo 1.
6. [ ] Make `GET /healthz` in `internal/module.go` return HTTP 503 when not configured or DMS is not running (mesh `Health()` already errors). Encode with `json.Marshal` under `m.mu` — the handler currently always writes 200 and reads `m.browseRoot` unlocked. Add `internal/module_test.go` coverage for 200 vs 503.
7. [ ] Add a fixture ContentDirectory smoke in `internal/module_test.go`: start with `NoTranscode`/`NoProbe` on a temp dir containing `sample.mkv`, GET the bound `Status.HttpAddr` description/browse URL, assert the filename appears. `TestModuleLifecycle` never hits DLNA HTTP. No real renderer or swarm.
8. [ ] Fix the container contract in `Dockerfile`: the distroless static image has no `ffmpeg`/`ffprobe`, but README says those are required for transcode/probe. Either install them in a debian runtime stage or default `DLNA_NO_TRANSCODE=1`/`DLNA_NO_PROBE=1` and document the image as direct-serve only. Add `cmd/module` `--health-check` and a `HEALTHCHECK` that curls `/healthz`.
9. [ ] Repair docs drift: replace the `yourorg/your-module` advisory URL in `SECURITY.md`; expand `.env.example` to every variable in README (`DLNA_MEDIA_PATHS`, `DLNA_IFNAME`, `DLNA_NO_TRANSCODE`, `DLNA_IGNORE_*`, `DLNA_NOTIFY_INTERVAL`, `DLNA_PROBE_CACHE_PATH`); state in README that DLNA is unauthenticated LAN streaming and that a shared browse parent can leak siblings; set AGENTS.md contracts to `proto/dlnav1` (it says none); delete the stub `ROADMAP.md` pointer at a non-existent `TASKS.md`.
10. [ ] Add `last_error` (and keep `http_addr`) on `StatusResponse` in `proto/dlnav1/dlna.proto` so operators can see `srv.Init`/`Run` failures instead of only `running=false`. Make `updateSetting("browse_root")` also reset `mediaPaths` to that directory (`internal/settings.go` currently leaves stale paths). Run `golangci-lint` and `go test -race` in `.forgejo/workflows/ci.yml` (Forgejo is origin CI).

### Module: `media-ffprobe`

**Status:** OPEN

1. [ ] Restrict `Analyze`/`GetCached` in `internal/module.go` to an allowlist (`FFPROBE_ALLOW_PATHS` + SettingsProvider): resolve the path, reject escapes/symlinks outside roots, return gRPC `PermissionDenied`. Any mesh peer can otherwise ffprobe any file the process can read.
2. [ ] Replace `classifySource` pix_fmt/`raw` heuristics in `internal/module.go` (10-bit → BluRay, raw → Remux). Parse basename tokens for Remux/BluRay/WEB-DL/WEBRip/HDTV (proto already documents HDTV). `media-scanner` `probeRenameMeta` writes `Quality.Source` into rename tokens.
3. [ ] In `parseOutput`, skip `disposition.attached_pic` when choosing primary `Video` (covers become the “movie”); pick the largest remaining video stream. Extend `resolutionLabel` for 1440p (`maxDim>=2560`) and 4320p (`>=7680`) — those are labeled 1080p/2160p today.
4. [ ] Stop calling `enrichChaptersFromFile` (full-file ffmpeg scene decode) from `Analyze` by default in `parseOutput`. Add settings `generate_chapters` + `ffmpeg_bin`; interval/scene only when enabled. BFF already interval-falls-back; scanner `probeQuality` is a 30s RPC and currently pays a second ffmpeg pass on every chapter-less file.
5. [ ] Thread the Analyze request `ctx` through `runFFprobe` and chapter enrich (`internal/module.go` uses `context.Background()`). Client cancel/deadline from mediauiprox (12s) and scanner (30s) never kill ffprobe/ffmpeg.
6. [ ] Make `Health()` fail when `ffprobeAvailable` is false. Do not `storeCache` stub filename guesses (`internal/stub.go`) so `GetCached` cannot look like a real hit. Update `internal/stub_test.go` (it currently asserts stub rows are cached).
7. [ ] Add proto fields and parsers: `AudioStream.default`/`commentary`/`sample_rate` from `disposition`/`sample_rate`; `VideoStream.field_order` (or `interlaced`) from the already-parsed `FieldOrder`. Playback direct-play and maintainer probe operands need default audio and interlace; they are dropped today.
8. [ ] Add `Invalidate` + `PurgeCache` RPCs on `AnalysisService` (`proto/ffprobev1/ffprobe.proto` + `internal/module.go`). `analysis_cache` is unbounded with no operator flush after scoring/parser fixes; optional `FFPROBE_CACHE_MAX_AGE` prune on Init.
9. [ ] Fix operator/docs drift: document `FFPROBE_BIN`, `FFPROBE_TIMEOUT`, `FFMPEG_BIN` and chapters in `README.md` + `.env.example`; point the CI badge at Forgejo; bump `COMPATIBILITY.md` off v0.1.0; fix `deploy/docker-compose.yml` (context `.` cannot `COPY core/`); stop `ProtectHome=true` in `deploy/systemd/muxcore-module.service` from blocking typical library paths (document `ReadOnlyPaths=`).
10. [ ] Add a `golangci-lint` job to `.forgejo/workflows/ci.yml` (Makefile `ci` already runs lint; origin CI is test+vet only). Remove `replace => ../core*` from `go.mod` so `internal/gomod_pins_test.go` `TestGoModSiblingFreePins` passes.

### Module: `media-graph`

**Status:** COMPLETE — Multi-library ingest, fixture boot, admin JSON API + auth, ingest lifecycle/prune, idempotent SQLite edges, gRPC status codes, RelatedTitles ranking, Health ping, golangci-lint CI, docs v0.1.3.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-graph.md -->

### Module: `media-intro-outro`

**Status:** COMPLETE — JSON persistence for segments/settings, recap-aware heuristics, OP/ED classification, segment validation, series reuse, min_confidence gate, Forgejo lint CI, docs sync.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-intro-outro.md -->

### Module: `media-library-maintainer`

**Status:** COMPLETE — act_after/grace preservation, postponed resume, TV season delete/move, per-rule auto_act gate, settings SQLite persistence, scheduler cancel, act tests, TV probe enrichment, docs sync, admin-ui collections/protections/postpone.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-library-maintainer.md -->

### Module: `media-list-sync`

**Status:** COMPLETE — music import policy, musicbrainz_id API, failed-sync retry, credential redaction, v0.2.4 docs, httptest Last.fm/Trakt, source validation, persisted poll_interval + sync_log prune, Forgejo lint/peer CI.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-list-sync.md -->

### Module: `media-movies`

**Status:** COMPLETE — sort whitelist, minimum_availability, collection search-on-add, mesh subscribe retry, HTTP stream auth/root sandbox, Backupable restore, mesh TLS dial opts, sibling-free go.mod, docs v0.1.11, Forgejo lint + coverage tests.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-movies.md -->

### Module: `media-music`

**Status:** COMPLETE — MusicBrainz lookup + release groups, HTTPAddr after Init, ScanLibrary mesh/HTTP/start/settings, file-import retry, path guards + stream/lyrics, MediaAdmin tests, docs v0.3.0, TLS peer dials, FK cascade + item_tags, Dockerfile siblings + Forgejo lint.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-music.md -->

### Module: `media-rename`

**Status:** COMPLETE — dest_root sandboxing, overwrite guard, companion files, persisted import_mode, recursive BatchRename, first-class anime, token collapse, docs repair, Forgejo lint CI.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-rename.md -->

### Module: `media-root-folders`

**Status:** OPEN

1. [ ] Add `GetRoot(id)` to `proto/rootsv1/roots.proto` and `internal/module.go`. `admin-ui/handler/roots.go` `RootEdit` currently `ListRoots` and scans for the id; muxcorectl has no get. Return `NotFound` for unknown ids. Tests in `internal/module_test.go`.
2. [ ] Make `UpdateRoot` a merge in `internal/module.go`: empty `path`/`name`/`media_kind`/`naming_template_id` keep the existing row. `muxcorectl-cli` `roots update <id> --name X` sends empty path and `normalizePath` rejects it. Test name-only and template-only updates.
3. [ ] Constrain `BrowsePath` in `internal/module.go`: setting/env `ROOTS_BROWSE_PREFIXES` (default `/` only if unset for backward compat; document that production should set library mounts). Reject paths outside the prefixes; skip dot-directories. Any `media.roots` caller can list `/etc` and home dirs today. Tests for allowlist deny and hidden skip.
4. [ ] Persist `require_accessible` across restart in `internal/settings.go` (sqlite kv next to `root_folders`, load in `Init`). Admin SettingsProvider writes memory only; process restart reverts to `ROOTS_REQUIRE_ACCESSIBLE` / default true. Test update → `Stop`/`Init` → value held.
5. [ ] Honor `require_accessible` in `SeedFromPaths` (`internal/module.go`). It inserts arbitrary paths with no probe; `TestSeedFromPaths` seeds `/data/a` that do not exist. Same gate as Create/Update, or an explicit `skip_validation` proto field for installer-style pre-create. Adjust that test to temp dirs.
6. [ ] Extend `normalizeMediaKind` (`internal/module.go`) to `music` and `books` (and `audiobooks` if those managers stay first-class). Admin `/roots` copy already says “movies, TV, and music”; the select only offers movies/tv/any and Create rejects `books`. `media-music` already stores `root_folder_path`. Filter tests for the new kinds.
7. [ ] Add per-kind default + `PickRoot(media_kind)` in proto/`internal/module.go`: `is_default` column (one default per kind), pick default if accessible else the accessible root with most `free_bytes`. `media-movies`/`media-tvshows` accept empty `root_folder_path` and never ask this module for a destination. Tests for default win and fallback-by-space.
8. [ ] Make `DeleteRoot` fail on empty or unknown id (`internal/module.go`). It always returns OK and ignores `RowsAffected`. Map to gRPC `InvalidArgument` / `NotFound`. Tests for both.
9. [ ] Stop swallowing catalog errors: `loadRootsLocked` returns `nil` on query failure so `ListRoots` looks empty; `CreateRoot`/`UpdateRoot` return `fmt.Errorf` (gRPC `Unknown`). Return the sql error; map validation to `InvalidArgument`, missing row to `NotFound` (`google.golang.org/grpc/status`). Admin-ui currently shows `rpc error: code = Unknown`.
10. [ ] Origin CI and docs: run `golangci-lint` and `go test -race` in `.forgejo/workflows/ci.yml` (Makefile already does; Forgejo does not). Document `ROOTS_REQUIRE_ACCESSIBLE` and `ROOTS_BROWSE_PREFIXES` in `README.md`. Set `COMPATIBILITY.md` current row to v0.1.6 (still says v0.1.0). Drop or skip the GitHub `docker` job — it builds `./Dockerfile` which does not exist; Appendix E origin is Forgejo, not GHCR.

### Module: `media-scanner`

**Status:** COMPLETE — AddWatchDir duplicate UPSERT, persisted settings, music surface + library indexing, importStorageObject parity, failed import events, archive hardening, Scan scan_log+ctx, docs/CI/proto drift, ListImportCandidates tests.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-scanner.md -->

### Module: `media-subtitles`

**Status:** OPEN

1. [ ] Replace the stale `SearchWanted` provider gate in `internal/wanted.go` (~L247–263) so scheduled/webhook wanted search uses the same enabled-provider set as `Module.Search` in `internal/module.go`; today only ~13 providers count and enabling greeksubs/titlovi/etc. alone yields `no enabled searchable provider` and never calls `Search`.
2. [ ] Stop `UpgradeSubtitles` in `internal/upgrade.go` (~L71–74) from aborting the whole batch when OpenSubtitles is missing; search/download via `Module.Search` / `downloadFromProvider` for any enabled provider (same as manual search).
3. [ ] Honor `LanguageProfile.cutoff_language` in `RebuildWanted` / `SearchWanted` (`internal/wanted.go`, proto comment on `cutoff_language`): once the cutoff language is satisfied, do not keep searching remaining profile languages.
4. [ ] Implement `audioHasLang` in `internal/wanted.go` with ffprobe audio-stream tags (fixture/mock `ffprobe` JSON in tests — no live files) so `LanguageRequirement.exclude_audio` actually skips wanted rows.
5. [ ] Wire the `use_embedded` setting (`internal/settings.go`) into `RebuildWanted` / `SearchWanted`; `m.useEmbedded` is persisted but never read — embedded satisfaction currently keys only off `providerEnabled("embedded")`.
6. [ ] Fix `detectEmbeddedSubs` in `internal/module.go` so `disposition.default == 1` is not treated as `forced` (only `disposition.forced`); default tracks currently satisfy/mismatch forced wanted rows incorrectly.
7. [ ] In `internal/webhooks.go`, run webhook work on `context.Background()` (not `r.Context()` after the 202 return — that ctx is already cancelled) and require a documented `SUBS_WEBHOOK_SECRET` (header or query) on `POST /api/webhooks/sonarr|radarr`.
8. [ ] Gate `GET /api/subtitles/{id}/vtt` in `internal/subtitle_serve.go` (token/HMAC or bind `127.0.0.1` by default); default `:9521` currently serves any subtitle file with no auth. Keep mediauiprox fixture-path proxy working.
9. [ ] Dial notification in `internal/events_notify.go` `notifyDownload` with the same TLS posture as the module SDK (`MUXCORE_INSECURE_DISABLE_TLS` / mesh creds) instead of always `insecure.NewCredentials()`.
10. [ ] Add httptest coverage for todos 1–8 (`SearchWanted` gate, upgrade without OS, cutoff/exclude_audio/`use_embedded`, webhook secret+context, VTT deny-unauth) and fix operator drift: `Dockerfile` `EXPOSE 9510` → `9520`/`9521`, `COMPATIBILITY.md` v0.4.0 → 0.5.5, README env table for remaining provider keys + webhook/VTT.

### Module: `media-tagging`

**Status:** COMPLETE — media id aliases + mesh genre lookup, full mesh/HTTP TaggingService, live events_enabled toggle with reconnect, TAGGING_DATA_DIR on vault, Backupable, removal cleanup, rule validation, honest health, v0.2.1 + Forgejo CI/lint.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-tagging.md -->

### Module: `media-transcoder`

**Status:** COMPLETE — playback HTTP hardening, replace disposition, job/pipeline resume, progress persistence, SQLite settings, docs/CI/version sync, pool TLS, ffprobe fail-closed, product-path tests.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-transcoder.md -->

### Module: `media-transcoder-pool`

**Status:** COMPLETE — queue promotion, remote_job_id resume, 48h dispatch timeout, worker cancel, stale/unregister requeue, mesh worker discovery, TLS dial, SQLite health, heartbeat capacity accounting, dispatch on by default + settings.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-transcoder-pool.md -->

### Module: `media-tvos-app`

**Status:** COMPLETE — TOTP login, subtitle tracks + language prefs, autoplay/skip-intro, PlaybackCoordinator on home/queue/playlists/music, unified search, library Request action, Keychain session + sign-out userdata wipe, MuxCoreAPI tests + Forgejo CI, version sync. Physical-device subtitle/request validation DEFERRED (simulator + unit/API coverage).

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-tvos-app.md -->

### Module: `media-tvshows`

**Status:** COMPLETE — episode-aware indexer search, admin filters/has_file, sort allowlist, network on refresh, episode file fields, stream auth, mesh subscribe retry, artwork URLs, backup FK+migrate, Forgejo race+lint CI.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-tvshows.md -->

### Module: `media-ui-app`

**Status:** OPEN

1. [ ] Run `npm test` in `.forgejo/workflows/ci.yml` (origin CI). Vitest coverage exists (`src/**/*.test.tsx`) but Forgejo only typechecks and builds, so player/browse regressions never fail the gate.
2. [ ] Paginate `Movies.tsx` and `TVShows.tsx` using `list.total` / `page` from `api.listMovies` / `api.listTVShows`. Both fetch page 1 only (`libraryPageSize`, default 48); titles past that page never appear. Add Load more (or infinite scroll) and keep Settings page-size as page size, not a hard cap.
3. [ ] Persist genre + sort on `Movies.tsx` and `TVShows.tsx` in the URL query string (`?genre=&sort=`) so filters survive refresh and share. Today they are component state only (AGENTS.md §4.3).
4. [ ] Add a hero Play on `TVShowDetail.tsx` that continues the next-up episode (`resolveNextUp` in `src/lib/userdata.ts` or first playable file) plus a secondary "Play from beginning" (S01E01). The hero currently has Favorite / Jellyfin only; play lives only on episode rows.
5. [ ] On `MovieDetail.tsx`, when `getProgress(id)` has a position, show "Play from beginning" that opens `/player` with a flag `ResumeDialog` / `VideoPlayer.tsx` honors (skip the resume prompt). Play today always goes through the resume dialog with no detail-page escape.
6. [ ] On `MovieDetail.tsx` / `TVShowDetail.tsx`, when `tmdb_id` is set, load `api.getDiscoverDetail` and render the cast row already used by `DiscoverDetail.tsx`. Add a "More like this" shelf from same-genre library items (`listMovies` / `listTVShows` fixtures) — no live TMDB extras.
7. [ ] Replace native `<audio cont ] Persist SettingsProvider `ffmpeg_bin` in `internal/settings.go` (SQLite or a documented settings row). `UpdateSetting` is process-memory only and is lost on `run-host.sh restart`. Expose `max_concurrent` and `scan_interval` the same way so admin-ui changes survive restart.
6. [ ] Fix operator/docs/version drift: README still says module `0.1.1` and omits `ListStepTemplates`/`ApplyStepTemplate`, `/stream/trickplay`, and env `TRANSCODER_FFMPEG_BIN` / `TRANSCODER_SCAN_INTERVAL` / `TRANSCODER_USE_POOL` / `TRANSCODER_POOL_ADDR` / `TRANSCODER_TRICKPLAY_DIR`; `muxcore.json` is `0.3.2` vs `Info()`/`CHANGELOG` `0.3.3`; `COMPATIBILITY.md` stops at v0.1.1; `Dockerfile` `EXPOSE 9520` (collides with media-subtitles; ports are 9525/9526).
7. [ ] Add a `golangci-lint` job and `go test -race` to `.forgejo/workflows/ci.yml` (origin CI per Appendix E). `.golangci.yml` exists and README documents `-race`; Forgejo only runs `go test` + build/vet.
8. [ ] Honor mesh TLS in `internal/pool_forward.go`: stop always using `grpc.WithTransportCredentials(insecure.NewCredentials())`; match `MUXCORE_INSECURE_DISABLE_TLS` / `MUXCORE_GRPC_INSECURE` like `dialCore` in `internal/events.go`. Document `TRANSCODER_USE_POOL` / `TRANSCODER_POOL_ADDR` in README.
9. [ ] Stop hardcoding `ffprobe` and swallowing probe failures in `internal/pipeline.go` `probeFile`: derive the binary from `ffmpeg_bin` (or `TRANSCODER_FFPROBE_BIN`) and fail closed / skip with a reason when probe fails so `filter.skip_if_codec` cannot no-op and re-encode already-HEVC files.
10. [ ] Add tests for uncovered product paths: `internal/events.go` (`eventFilePath` + on_import matching), `internal/pool_forward.go` fallback, `internal/trickplay_http.go` handler, job cancel after DB-only persist, and `action.remux` in `evalStep` (today remux mutates/deletes the source during filter eval — write a sidecar or dedicated output instead).

### Module: `media-ui-app`

**Status:** OPEN

1. [ ] Run `npm test` in `.forgejo/workflows/ci.yml` (origin CI). Vitest coverage exists (`src/**/*.test.tsx`) but Forgejo only typechecks and builds, so player/browse regressions never fail the gate.
2. [ ] Paginate `Movies.tsx` and `TVShows.tsx` using `list.total` / `page` from `api.listMovies` / `api.listTVShows`. Both fetch page 1 only (`libraryPageSize`, default 48); titles past that page never appear. Add Load more (or infinite scroll) and keep Settings page-size as page size, not a hard cap.
3. [ ] Persist genre + sort on `Movies.tsx` and `TVShows.tsx` in the URL query string (`?genre=&sort=`) so filters survive refresh and share. Today they are component state only (AGENTS.md §4.3).
4. [ ] Add a hero Play on `TVShowDetail.tsx` that continues the next-up episode (`resolveNextUp` in `src/lib/userdata.ts` or first playable file) plus a secondary "Play from beginning" (S01E01). The hero currently has Favorite / Jellyfin only; play lives only on episode rows.
5. [ ] On `MovieDetail.tsx`, when `getProgress(id)` has a position, show "Play from beginning" that opens `/player` with a flag `ResumeDialog` / `VideoPlayer.tsx` honors (skip the resume prompt). Play today always goes through the resume dialog with no detail-page escape.
6. [ ] On `MovieDetail.tsx` / `TVShowDetail.tsx`, when `tmdb_id` is set, load `api.getDiscoverDetail` and render the cast row already used by `DiscoverDetail.tsx`. Add a "More like this" shelf from same-genre library items (`listMovies` / `listTVShows` fixtures) — no live TMDB extras.
7. [ ] Replace native `<audio controls>` in `Music.tsx` and `MusicArtist.tsx` with a custom bar (play/pause, seek, time, volume) matching `VideoPlayer` OSD conventions (AGENTS.md §7). Keep lyrics on the artist page.
8. [ ] Open book files in-page in `BookAuthor.tsx` via `/stream/books/` `stream_url` (iframe/object for PDF/EPUB) instead of `target="_blank"` dump. Do not surface raw `path` in the UI.
9. [ ] Stub Chromecast / AirPlay controls on `ControlsBar.tsx` (disabled + `aria-disabled` until a BFF session exists) and list them in `ShortcutsHelp.tsx` (AGENTS.md §7). PiP is shipped; these hook points are missing.
10. [ ] Debounce live search in `HeaderSearch.tsx` (~250ms, min 2 chars) instead of submit-only. Fix README drift: Live TV / Quick Connect are implemented (not "placeholders"); userdata is server-authoritative; origin CI is `.forgejo/workflows/ci.yml`, not GitHub. Update AGENTS.md §0 — hero/shelves/skeletons/icons are already shipped.

### Module: `media-ui-tv`

**Status:** COMPLETE — JVM unit tests + Forgejo CI, TOTP login, playlists/queue userdata, episode upcoming calendar, in-progress routing, player autoplay/progress, hardened session storage, prefs sync via `/api/userdata`, library pagination + watched indicators.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/media-ui-tv.md -->

### Module: `metadata-musicbrainz`

**Status:** OPEN

1. [ ] Add `README.md` + `AGENTS.md`: document `MUSICBRAINZ_FIXTURE`, `MUSICBRAINZ_BASE_URL`, `MUSICBRAINZ_USER_AGENT` (MusicBrainz requires an identifying UA with contact), `METADATA_GRPC_ADDR=:9413`, and that `_mvp/run-host.sh` / installer default `MUSICBRAINZ_FIXTURE=1`.
2. [ ] Implement SettingsProvider (`internal/settings.go`) and `modulesdk.RegisterSettings` in `Start`: `user_agent`, `base_url`, `fixture`. `muxcore.json` and `Info()` already advertise `settings`; admin-ui `/settings` will list this module and time out.
3. [ ] Add `internal/*_http_test.go` httptest coverage for all six live RPCs (`searchArtistHTTP` / `getArtistHTTP` / release-group / recording), assert `User-Agent` + `fmt=json`, and disable the 1.1s sleep so CI stays fast. Only fixture tests exist today.
4. [ ] Map MusicBrainz HTTP 404 → `codes.NotFound` and 400 → `InvalidArgument` in `getJSON` / `GetArtist` / `GetReleaseGroup` / `GetRecording`. Unknown MBIDs currently become `codes.Internal`.
5. [ ] Replace `getJSON`'s post-success `time.Sleep(1100ms)` with a context-aware 1 req/s token bucket (wait *before* the request, honor `ctx`, apply to errors/503). Concurrent callers currently all fire at once; the sleep is not a rate limit.
6. [ ] Add in-process HTTP cache + inflight coalescing (mirror `metadata-tmdb/internal/httpcache.go` + `inflight.go`) keyed by path+query. MusicBrainz is 1 req/s; uncached `GetArtist` during `media-music` / `media-list-sync` refresh is unusable.
7. [ ] Make `Health()` fail in live mode when the client cannot GET `{baseURL}` (or a cheap `/artist?query=x&limit=1`) with 2xx; fixture stays nil. `Health` is a no-op today even when MusicBrainz is down.
8. [ ] Add `cover_art_url` on `ReleaseGroup` / `GetReleaseGroupResponse` (Cover Art Archive `https://coverartarchive.org/release-group/{id}/front-250`, construct in fixture too). `media-music` `poster_url` stays empty because this proto has no art fields.
9. [ ] `GetReleaseGroup`: include recordings (`inc` / browse) as a repeated track list on `GetReleaseGroupResponse`; fixture OK Computer tracks. Album detail / track fulfill otherwise N+1 `GetRecording` calls with no tracklist.
10. [ ] Clamp Search* `limit` to 1–100 (MusicBrainz max). Add `TestFixtureGetRecording` plus empty-id → `InvalidArgument` and unknown fixture MBID → `NotFound` in `internal/module_test.go` / `release_recording_test.go`.

### Module: `metadata-tmdb`

**Status:** COMPLETE — credits/videos append mapping, expanded fixture corpus (seasons 0–5, collections), search adult/person filters, tmdbFetch/Health/cache mutex hardening, RPC tests, TMDB_FIXTURE docs, Forgejo race+lint CI.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/metadata-tmdb.md -->
### Module: `metrics-prometheus`

**Status:** OPEN

1. [ ] In `internal/module.go`, stop treating request labels as `ConstLabels` on separate `Counter`/`Gauge`/`Histogram` plus `MustRegister`. Same metric name with different labels (or a gauge after a counter) panics the process. Use `*Vec` keyed by name: Register defines the family (label names); Increment/Set/Observe apply values. Return gRPC errors instead of panicking. Sort label keys so `metricKey` is stable (map iteration is not).
2. [ ] Validate Prometheus metric names before register; reject empty names and `IncrementCounter` `delta < 0` with `InvalidArgument`. `prometheus.Counter.Add` panics on a negative delta. Cover both in `internal/module_test.go`.
3. [ ] `Health()` always returns nil even if listeners died. Fail Health when gRPC/HTTP servers are not serving. Add `GET /health` on the scrape HTTP server (`internal/module.go`). Fix `Dockerfile`: `HEALTHCHECK` runs `/module --health-check` (flag does not exist) and `EXPOSE 0` — probe `/health` and expose `9900`/`9901`.
4. [ ] `deploy/docker-compose.yml` publishes only `9900` and omits `METRICS_HTTP_ADDR`, so Prometheus cannot scrape. Publish `9901`, set `METRICS_HTTP_ADDR`/`METRICS_PATH`. Set those env vars in `deploy/systemd/muxcore-module.service`. Add `deploy/prometheus.yml` with a scrape job for `:9901`.
5. [ ] Defaults `:9900`/`:9901` listen on all interfaces with no auth; anyone on the network can inject series or read scrape text. Bind to `127.0.0.1` by default, or require `METRICS_SCRAPE_TOKEN` (Bearer) on the scrape path and the same (or mesh TLS) on `MetricsService`. Set `ReadHeaderTimeout` on `http.Server`.
6. [ ] `NewModule` uses a bare `prometheus.NewRegistry()` — `GET /metrics` is empty until something calls gRPC. Register `collectors.NewGoCollector` and `collectors.NewProcessCollector` so a freshly started sidecar is actually scrapable.
7. [ ] Replace `test/integration_test.go` (build-tagged starter that registers a fake module with core and never imports this package) with tests in `internal/module_test.go`: register→increment→scrape exposition text, two label values on one family, same-name counter+gauge error, increment of unknown name, scrape 404 on the old path.
8. [ ] No in-tree client and no module calls `MetricsService`, so the `metrics` capability is unused. Add a small `client/` that dials `METRICS_GRPC_ADDR` and wraps Register/Increment/Set/Observe. Document capability, ports, and a 10-line caller example in `README.md`.
9. [ ] Docs drift: `README.md` omits `METRICS_PATH` and live `metrics_path` settings; CI badge href points at GitHub Actions. `COMPATIBILITY.md` still lists v0.1.0 and omits `settings`. `Makefile` `-X main.version` has no `version` var in `cmd/module/main.go`. Add `9900`/`9901` to `_mvp/PORTS.md`.
10. [ ] Origin CI (`.forgejo/workflows/ci.yml`): checkout sibling `core` (or drop `replace => ../core` in `go.mod`) so isolated `go test`/`go build` work. Add a `golangci-lint` job matching `Makefile` `lint` / `.golangci.yml`.

### Module: `muxcore-android`

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/muxcore-android.md -->

### Module: `muxcorectl-cli`

**Status:** COMPLETE — Standalone Forgejo go.mod, golangci-lint CI, embedded parity routes, Bearer auth + token-derived request identity, MUXCORE_TLS_* module dials, ~/.muxcore/admin-ui data paths, secret input hardening, --json schedules/events, shell completion, httptest/bufconn coverage.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/muxcorectl-cli.md -->

### Module: `muxcore-docs`

**Status:** OPEN

1. [ ] Add origin CI at `.forgejo/workflows/ci.yml` (`runs-on: native`, Node 22, `npm ci`, `make build`, `npm test`, upload `dist/`). Appendix E: do not treat `.github/workflows/ci.yml` or `pages.yml` as the origin gate; keep GitHub Pages as an optional public consumer only.
2. [ ] Rewrite `wiki/Getting-Started.md` Path A and `wiki/Deployment.md` household install to match `_mvp/docs/PUBLIC-INSTALL.md` + Appendix E: Forgejo/LAN `docker-compose.registry.yml` + `publish-muxcored-local.sh` / `local-registry.sh` as the supported first-run. Cite `MUXCORE_REGISTRY` and actual compose filenames; mark GitHub Releases/GHCR as a deferred public mirror, not the required download path.
3. [ ] Stub `wiki/Tasks.md` to `MASTER-ROADMAP.md` (same pattern as `wiki/Roadmap.md`). Drop `../TASKS.md` links from `wiki/Home.md`, `Getting-Started.md`, `Deployment.md`, and `Port-Map.md` — workspace `TASKS.md` is only a pointer, and those hrefs become dead `workspace-file-ref` spans in `dist/`.
4. [ ] Fix dual wiki source in `build.mjs`: default to committed `./wiki/` (or require `MUXCORE_DOCS_WIKI`). Today `wikiSource()` prefers `../core.wiki`, so umbrella `npm test` never builds `wiki/Installer-Pin-Matrix.md` and `make build` / `make sync-wiki` can overwrite `wiki/` from the sibling. Then add that page to `wiki/_Sidebar.md` + `test/accessibility.test.mjs` `ALL_PAGES`, or delete the orphan.
5. [ ] Expand `wiki/Port-Map.md` and the sidecar table in `wiki/Configuration-Reference.md` to cover `_mvp/PORTS.md` rows missing from the wiki: `userdata-local` (9672), `media-subtitles`, `media-transcoder`, `plex`/`emby`, `media-music`/`books`/`comics`/`audiobooks`, `media-list-sync`, `media-dlna`, `cache-local`/`cache-redis`, `storage-s3`/`storage-ceph`.
6. [ ] Correct `wiki/Security.md`: remove “Multiple auth modules can be active simultaneously” (one `AuthProvider` — `auth-local` XOR `auth-oidc` per workspace `AGENTS.md`). Replace `auth.gringotts` examples in Security, Getting-Started, and Deployment with `auth.zem.systems`.
7. [ ] Rewrite `wiki/Contributing.md` for Forgejo origin: clone `ssh://forgejo@git.zem.systems:2222/muxcore/core`, `.forgejo/workflows`, `runs-on: native`, GOPRIVATE rewrite. Treat GitHub clone/PR/wiki-push as the later public consumer path, not origin.
8. [ ] Add client-side search in `build.mjs` / `site/` over generated page text, plus a `404.html`. Configuration-Reference and Contracts are large; mobile nav is only a wrap-all-links header with no find.
9. [ ] Add `AGENTS.md` covering: committed `wiki/` is the snapshot, `MUXCORE_DOCS_SKIP_WIKI_SYNC` / wiki source selection, Forgejo CI, `npm test` a11y gate, “wiki follows MASTER-ROADMAP”, and that `.github/workflows` is not origin CI.
10. [ ] Add `test/build.test.mjs` for `rewriteWikiLinks`, `parseSidebar` completeness vs `wiki/*.md`, workspace-path stripping, and that tests always build from `./wiki` (not the sibling). Today only a11y runs against a hardcoded `ALL_PAGES` list.

### Module: `muxcore-installer`

**Status:** OPEN

1. [ ] Add origin CI at `.forgejo/workflows/ci.yml` (Appendix E): `go test ./...`, `go vet`, gofmt, shellcheck of `get-onboard.sh`/`up.sh`/`bootstrap-auth.sh`/`smoke-fixture.sh`/`lib/*.sh`, and `MUXCORE_I_AGREE=1 MUXCORE_NONINTERACTIVE=1 MUXCORE_DRY_RUN=1`. `.github/workflows/ci.yml` builds and dry-runs but never runs tests.
2. [ ] Make `get-onboard.sh` and `internal/ghrelease` fetch from Forgejo (`git.zem.systems/muxcore`, `scripts/publish-forgejo-release-assets.sh`) first, GitHub as optional public mirror. Fail closed if `SHA256SUMS` is missing or mismatches (today a failed sums download is ignored).
3. [ ] Fetch the `media-ui-app` SPA into the install root and set `MEDIA_UI_DIST` in `.env` / `up.sh`. Pins and `modules.RepoForBin` still target archived `media-ui` v0.1.0; `up.sh` skips the player when `MEDIA_UI_DIST` is unset, but `internal/tui/work.go` still advertises `http://127.0.0.1:5173`. Default `wizard.Default` playback to MuxCore player when Movies/TV are selected so non-interactive matches the TUI.
4. [ ] Honor `Answers.ExistingChoice` in `internal/wizard/pipeline.go`. `restart-only` (`internal/tui/phases_installdir.go`) skips the wizard then `stepConfigure` `SetAll`s `wizard.Default` over the live `.env` (empty library paths, `Playback` nil, empty admin password). Load existing env, fetch/restart only. Cover with a pipeline test.
5. [ ] Make `Pipeline.stepFetch` fail the install if any `modules.PlatformModules` (or `muxcored`) is missing; today `internal/fetch` records `Missing` and `Run` still proceeds to start/health. Verify each module tarball against release `SHA256SUMS` the same way `get-onboard.sh` verifies `muxcore-setup`.
6. [ ] Fix Compose household path in `internal/compose/compose.go`: image prefix from `MUXCORE_REGISTRY` (Forgejo/LAN; drop hardcoded `ghcr.io/muxcore-media`), volume-mount every selected library + import dir (host paths are currently env-only and invisible in the container), and emit a `mediauiprox` service + dist mount when MuxCore player is selected.
7. [ ] Stop defaulting `MVP_ADMIN_PASSWORD` to `admin-dev-only` in `cmd/muxcore-setup/noninteractive.go`, `bootstrap-auth.sh` / `internal/assets/bootstrap-auth.sh`, `lib/common.sh`, and `.env.example`. Generate a 16-char password (same as TUI `genPassword` in `internal/tui/phases_admin.go`) or refuse if unset; write it once to `run/VIEW-ME.txt`.
8. [ ] Fail `Pipeline.Run` (non-zero exit) when `stepAdmin` or `stepSmoke` errors; treat `stepHealth` as ready only on HTTP 200 from `http://127.0.0.1:8080/health` (503 currently counts as success). Household first-run must not print "MuxCore is ready" after a broken bootstrap.
9. [ ] Add tests for the untested install path: `internal/wizard` (dry-run configure, ExistingChoice, health/admin/smoke failure), `internal/fetch` (missing platform bin is hard fail + checksum), `internal/compose` (registry prefix, library mounts, mediauiprox), `internal/ghrelease` (Forgejo vs GitHub URL builders), and non-interactive password generation.
10. [ ] Fix operator-doc drift: rewrite `AGENTS.md` (this is a TUI installer, not a gRPC sidecar); expand `printUsage` in `cmd/muxcore-setup/main.go` to the env vars README already lists (`INSTALL_RUNTIME`, `MUXCORE_KEEP_MODE`, `MUXCORE_BIND_ALL`, library path vars, playback tokens); change README "reuse/upgrade/reinstall" to the real `reconfigure` / `restart-only` / `fresh` labels.

### Module: `muxcore-ios`

**Status:** OPEN

1. [ ] Add `.forgejo/workflows/ci.yml` on label `native` that runs `nix-shell --run 'swift test'` (Appendix E: origin CI is Forgejo only; `.github/workflows/ios-ipa.yml` is not the gate).
2. [ ] Add `APIClient.watchlist()`, `WatchlistView`, `AppRoute.watchlist`, and a `NavCatalog` overflow item gated on `features.watchlist` (add that key to `Capabilities.defaults`); mirror `media-ui-app` `Watchlist.tsx` + `nav-catalog.ts`.
3. [ ] Add `APIClient.discoverBrowse(category:type:)` plus `DiscoverView` / `AppRoute` for `/discover` trending+popular shelves, and a `NavCatalog` overflow `{/discover, feature: request}` matching web `Discover.tsx`.
4. [ ] Wire Settings `playback.autoplayNext` in `PlayerView`: extend `PlayerItem` with `showId`, use `NextUpResolver` to show a next-episode overlay near end and auto-advance (web `useUpNext.ts`). The toggle in `SettingsDetailView` currently does nothing.
5. [ ] Use `PlaybackResolve.transcoderAvailable` in `PlayerOSDView` for a quality picker that reloads via `/stream/transcode?src=&max_height=` (web `usePlaybackSource.ts`). `PlayerView` ignores transcoder fields and always plays `streamURL`.
6. [ ] Fetch subtitle VTT through `APIClient` with the session Cookie — `PlayerView.selectSubtitle` uses `URLSession.shared.data(from:)` and will 401 on protected BFF/stream paths.
7. [ ] Add `InviteJoinView` plus `muxcore://invite` (or openURL `/invite/:token`) calling `GET /api/invite/peek` and `POST /api/invite/redeem`, matching `media-ui-app` `InviteJoin.tsx`.
8. [ ] Replace `InlineAudioPlayer` in `MusicBookViews.swift` (`AVKit.VideoPlayer` embed) with `AVAudioSession` `.playback`, `MPNowPlayingInfoCenter`, remote commands, and `UIBackgroundModes: audio` in `project.yml`.
9. [ ] On `AppState.logout`, call a new `UserDataStore.clearLocal()` that wipes UserDefaults progress/favorites/prefs/playlists/queue so a shared iPad does not leak the previous member's continue-watching.
10. [ ] Add `docs/HARDWARE-VALIDATION.md` (device login/browse/play/subtitles/settings/sign-out checklist like `media-tvos-app/docs/HARDWARE-VALIDATION.md`) and document signed/TestFlight install; `project.yml` `DEVELOPMENT_TEAM` is empty and CI only ships an unsigned IPA.

### Module: `muxcore-module-starter`

**Status:** OPEN

1. [ ] Drop the `replace => ../core` (and sibling SDK/contracts replaces) from root `go.mod` so the reference scaffold matches `cookiecutter/{{cookiecutter.module_slug}}/go.mod` (published `core@v0.5.8`, no sibling tree). Origin `.forgejo/workflows/ci.yml` checks out this repo alone; `go test` cannot resolve `../core`. Bump README/CHANGELOG/COMPATIBILITY and cookiecutter README/COMPATIBILITY off the stale `core@v0.5.0` pin.
2. [ ] Make `Health()` return nil in `internal/module.go` and `cookiecutter/{{cookiecutter.module_slug}}/internal/module.go`. Both return `fmt.Errorf("not implemented")`, so every generated module is immediately degraded. Add `TestModuleHealth` in both `internal/module_test.go` files.
3. [ ] Fix root `Dockerfile`: `HEALTHCHECK` runs `/module --health-check` (flag does not exist in `cmd/module/main.go` or `core/sdk/go/module`) and `EXPOSE 0`. Align with the cookiecutter Dockerfile (distroless, no fake flag) or add a real `/health` HTTP and check that. `deploy/docker-compose.yml` wget against `core:9090/health` is a gRPC port; point compose at the Forgejo/LAN registry image, not `ghcr.io/muxcore-media/core` (Appendix E).
4. [ ] Origin CI (`.forgejo/workflows/ci.yml`): run `scripts/check-cookiecutter.sh` and a `golangci-lint` job matching `Makefile` `lint` / `.golangci.yml`. Rewrite `scripts/check-cookiecutter.sh` to assert Forgejo `runs-on: native` + `git.zem.systems/muxcore` rewrite and leftover-placeholder absence in `.forgejo/workflows/ci.yml`; stop treating GitHub `self-hosted` + `MUXCORE_CI_TOKEN` as the generator gate.
5. [ ] Emit from cookiecutter what generated modules need to pass `make lint` and look like current MuxCore sidecars: copy root `.golangci.yml`, a thin `AGENTS.md` (identity + `go test ./...`), `deploy/docker-compose.yml` + `deploy/systemd/*.service` with slug substitution, and keep `.env.example`. Today `cookiecutter/{{cookiecutter.module_slug}}/` has none of those; `make lint` has no config.
6. [ ] Sync root `Info()` with `muxcore.json`: `internal/module.go` advertises empty `Roles`/`Capabilities` while `muxcore.json` has `your-role`/`your.capability`. Set `MinCoreVersion` / `minCoreVersion` in both Info() and muxcore.json (root + cookiecutter) to the go.mod pin. Assert Info() fields match muxcore.json in `internal/module_test.go`.
7. [ ] Wire Makefile `-X main.version` in both `cmd/module/main.go` files (`var version`) and use it in `Info().Version` instead of hardcoded `"0.1.0"`.
8. [ ] Replace or delete `test/integration_test.go`: it never imports this package, uses deprecated `grpc.DialContext`, and registers a fake `test-module`. Cookiecutter already omits it. Prefer a fixture/httptest mesh smoke that runs this scaffold via `modulesdk.Run`, or drop the file so `go test ./...` is not a dead core Register client.
9. [ ] Docs/operator drift: README CI badge href still points at GitHub Actions (image is Forgejo); `.env.example` claims Insecure is hardcoded (it is env-driven in `cmd/module/main.go`); `ROADMAP.md`, cookiecutter `ROADMAP.md`, and `CHANGELOG.md` point at workspace `TASKS.md` (SoT is `MASTER-ROADMAP.md`); `CONTRIBUTING.md` tells authors to clone sibling `../core`; `SECURITY.md` and `.github/CODEOWNERS` still say `yourorg/your-module` / `@yourorg/your-team` — cookiecutter must substitute `github_org` / slug.
10. [ ] Generated-module origin story: cookiecutter `README.md` and `.github/workflows/ci.yml` still teach GitHub Actions + `MUXCORE_CI_TOKEN` as the install path. Document Forgejo clone (`git.zem.systems/muxcore`) and `GOPRIVATE` rewrite in cookiecutter README/CONTRIBUTING/`scripts/new-module.sh` comments; keep `.github` as optional public-consumer CI, not the documented default (Appendix E).

### Module: `muxcore-operator`

**Status:** OPEN

1. [ ] In `internal/controller/platform_controller.go`, set sidecar `MUXCORE_GRPC_ADDR` (and `MUXCORE_MODULE_ID`) to the mesh Service — modules dial `MUXCORE_GRPC_ADDR` (`muxcore-docs` Configuration-Reference; Helm `templates/modules.yaml`). Stop emitting duplicate `MUXCORE_MESH_ADDR` on muxcored (`:9090` listen vs `muxcored:9090` dial). Cover in `platform_controller_test.go`.
2. [ ] Add PVC/volume fields on `api/v1alpha1` ModuleSpec (claim name, size, mountPath, optional StorageClass) and reconcile PVCs + volumeMounts in `platform_controller.go` the way `_mvp/deploy/helm/muxcore/templates/modules.yaml` does for muxcored/auth-local/database-sqlite/secrets-file. Extend `config/rbac/role.yaml` (persistentvolumeclaims). Test create + prune.
3. [ ] Align `config/samples/*.yaml` and `config/samples/kind/muxcoreplatform.yaml` ports with `_mvp/PORTS.md` (samples invent 9405–9410; real ports are 9700/9550/9601/9101/9102/9202). Add the Helm extra env (`API_REST_*`, `AUTH_DB_PATH`+`AUTH_*_ADDR`, `ADMIN_UI_AUTH_ADDR`, `MUXCORE_MODULE_ID`) so a sample CR can actually register and serve.
4. [ ] Fix `scripts/soak-operator-kind.sh`: it waits on `muxcoreplatform/muxcore` but the kind CR is `minimal-local`. Soak a muxcored-only (or image-preloaded) CR so Ready can become True without pulling nine `localhost:5000` sidecars that the script never loads.
5. [ ] Change origin defaults off GHCR: CRD/`CoreImage` default, `config/manager/manager.yaml`, and GHCR samples still `ghcr.io/muxcore-media/*`. Per Appendix E, default/document Forgejo (`git.zem.systems/muxcore`) or LAN `localhost:5000`; keep GHCR as an optional consumer mirror only.
6. [ ] When `spec.insecureDisableTLS` is false, inject mesh TLS from a Secret (`MUXCORE_TLS_{CERT,KEY,CA}` volume/env) in `reconcileModule` — README already lists this as out of scope; `_mvp/deploy/kustomize/overlays/production/patch-tls.yaml` only flips the flag. Add a sample + fake-client test.
7. [ ] Extend ModuleSpec / `reconcileModule` to emit Helm-parity pod shape: HTTP+gRPC Service ports, readiness/liveness probes, resources, `imagePullPolicy` / `imagePullSecrets`. Operator pods today are a single unnamed port, no probes, no resources. Mirror `_mvp/deploy/helm/muxcore/templates/modules.yaml`.
8. [ ] Expand `internal/controller/platform_controller_test.go` for EnvFromSecret, `InsecureDisableTLS`, default coreImage/meshAddr, Ready when `AvailableReplicas>=1`, Service prune, and empty name/image errors. Add a `golangci-lint` step to `.forgejo/workflows/ci.yml` (`.golangci.yml` exists; Forgejo only runs test/build/vet).
9. [ ] Scope `config/rbac/role.yaml`: ClusterRole can mutate Deployments/Services in every namespace. Prefer a namespaced Role (or documented `--watch-namespace`). Bind metrics off the default unauthenticated `:8080` (or protect it). Set `securityContext` (non-root, drop caps) on `config/manager/manager.yaml` and managed pods.
10. [ ] Validate module names (DNS-1123, unique, reserved `muxcored`) via CRD CEL or a webhook in `api/v1alpha1` + `cmd/main.go`. Prefix owned Deployment/Service names with the Platform name so two CRs in one namespace cannot collide on `muxcored`/`api-rest`. Record Kubernetes Events on reconcile/prune (RBAC already allows `events`).

### Module: `notification-apprise`

**Status:** OPEN

1. [ ] In `internal/module.go` `subscribeToMediaEvents` / `formatNotification`, handle `EventDownloadCompleted`, `EventDownloadFailed`, and `EventImportFailed` the way `notification-default/internal/module.go` already does. Households miss the failure path today. Add `internal/format_test.go` with fixture JSON payloads only (no live Apprise/Discord).
2. [ ] Replace the 15s one-shot in `subscribeToMediaEvents` + `dialCore`: retry mesh dial with backoff until `Stop`, guard `m.mc` with `mu`, and cancel event streams in `Stop`. One failed dial or a late core currently disables auto-notify for the process lifetime.
3. [ ] Redact secrets in `channelDescription` / `Status`. Discord/Slack/Apprise URLs contain tokens; `Status` currently truncates to 60 chars and still leaks them. Use `modulesdk.MaskSecret` (same as `internal/settings.go`). Assert `Status` never returns a raw webhook or `slack://` token.
4. [ ] Persist `UpdateSetting` / `Configure` to a data-dir file (e.g. `$MUXCORE_DATA/notification-apprise/settings.json`, mode `0600`) and reload in `Init`. Admin-ui settings writes are memory-only and vanish on `run-host.sh restart`. Cover persist-across-restart in `internal/settings_test.go`.
5. [ ] Add per-event toggles in `internal/settings.go` (`notify_download_failed`, `notify_import_failed`, `notify_file_added`, …) and honor them in `handleEventStream`. Every subscribed event currently fans out to all channels with no mute (Arr operators turn file-added off).
6. [ ] Validate `Configure` in `internal/module.go`: reject `CHANNEL_UNSPECIFIED` / `CHANNEL_EMAIL` and require `urls` (Apprise) or `webhook_url` (Discord/Slack/webhook). Today any channel is stored `Enabled` and later `Notify` returns "unsupported channel".
7. [ ] Authenticate `Notify`/`Configure`/`Status` on the module gRPC server (`internal/module.go` `grpc.NewServer()` has no interceptors) and default `NOTIFY_GRPC_ADDR` to `127.0.0.1:9445`. Unauthenticated LAN clients can rewrite webhooks and fire household Discord/Slack. Reject non-https / link-local webhook targets in `Configure`/`UpdateSetting`.
8. [ ] Origin CI (`.forgejo/workflows/ci.yml`): run `golangci-lint` (`.golangci.yml` / `make lint`) and `go test -race` (Makefile already does; Forgejo is `-count=1` + `vet` only). Make standalone checkout work: drop `replace => ../core*` / `../contracts-notification` or clone those siblings before `go test` (current replaces break a module-only Forgejo checkout).
9. [ ] Fix `Dockerfile` for module-root context: it `COPY core/` + `COPY notification-apprise/` but `.github/workflows/ci.yml` `docker` and `make docker` use `context: .`. Build from published pins (no sibling COPY) or document an umbrella-only context. Add a real `HEALTHCHECK` against gRPC/health, not a missing flag.
10. [ ] Docs drift: `AGENTS.md` says "Contracts: none declared" vs `muxcore.json`/`Info()` `NotificationProvider`. `COMPATIBILITY.md` still lists v0.1.0 / core 0.4.0+ and omits `settings` (module is 0.1.5, `go.mod` core v0.5.8). `CHANGELOG.md` still says pin `core/sdk/go/module` v0.5.2. README + `.env.example` omit settings keys, `Health()` (needs ≥1 channel), `MVP_ENABLE_NOTIFICATION_APPRISE=1`, and that runtime `Configure` does not persist.

### Module: `notification-default`

**Status:** OPEN

1. [ ] In `internal/module.go` `subscribeToMediaEvents` / `formatNotification`, subscribe to `media.movie.requested` and `media.tv.requested` (`contracts-media/events`; `request-media` already publishes them; audit lists no notification subscriber). Format title/requester from fixture JSON. Add cases in `internal/format_test.go`. Also handle `download.started` (constant exists; only dispatched/completed/failed are wired).
2. [ ] Replace the 15s one-shot in `subscribeToMediaEvents` + `dialCore`: retry mesh dial with backoff until `Stop`, guard `m.mc` with `mu`, pass `Start`'s ctx (not `context.Background()`), and cancel event streams in `Stop`. One failed dial or a late core currently disables auto-notify for the process lifetime.
3. [ ] Add per-event toggles in `internal/settings.go` (`notify_download_failed`, `notify_import_failed`, `notify_file_added`, `notify_requested`, …) and honor them in `handleEventStream`. Every subscribed event fans out to all channels with no mute.
4. [ ] Add a fixture test-send in `internal/settings.go` / `Notify` (e.g. setting `send_test=discord|slack|webhook|email` or empty=all configured) that posts a canned payload through existing `httptest` / `mailSender` hooks. No live Discord/Slack/SMTP. README: how an operator verifies a channel without waiting for a real grab.
5. [ ] Redact secrets in `channelDescription` / `Status`. Discord/Slack webhook URLs contain tokens; `Status` truncates to 60 chars and still leaks them. Use `modulesdk.MaskSecret` (same as `settingsDefs`). Assert `Status` never returns a raw webhook or `smtp_pass`.
6. [ ] Persist `UpdateSetting` / `Configure` to a data-dir file (e.g. `$MUXCORE_DATA/notification-default/settings.json`, mode `0600`) and reload in `Init`. Admin-ui channel/SMTP writes are memory-only and vanish on `run-host.sh restart`. Cover persist-across-restart in `internal/settings_test.go`.
7. [ ] Validate `Configure` / `Health` in `internal/module.go`: require `webhook_url` for Discord/Slack/webhook; reject `CHANNEL_UNSPECIFIED`; treat email as healthy only when `smtp_host` + `smtp_from` + `to` are set (today `SMTP_HOST` alone makes `Health` pass and `Configure` Discord with no URL stores `Enabled: true`). Support clear/disable (empty URL deletes the channel — `UpdateSetting` already does this; `Configure` does not).
8. [ ] Sanitize SMTP headers in `buildSMTPMessage`: strip CR/LF from `from`/`to`/`subject` so a `Notify` title cannot inject extra headers. Add `internal/module_test.go` coverage. Force STARTTLS on port 587 (do not fall back to plaintext AUTH if the server omits the extension).
9. [ ] Authenticate `Notify`/`Configure`/`Status` on `grpc.NewServer()` in `Start` (mesh TLS or shared token) and default `NOTIFY_GRPC_ADDR` to `127.0.0.1:9441`. Unauthenticated LAN clients can rewrite webhooks/`EMAIL_TO` and fire household Discord/email. Reject `CHANNEL_UNSPECIFIED` and empty webhook URLs on `UpdateSetting` as well.
10. [ ] Docs + origin CI: `AGENTS.md` says "Contracts: none declared" vs `muxcore.json`/`Info()` `NotificationProvider` v0.1.0. `COMPATIBILITY.md` still lists v0.1.0 / core 0.4.0+ and omits `settings` (module is 0.1.6, `go.mod` core v0.5.8). README + `.env.example` omit Settings keys, `send_test`, event toggles, and that runtime `Configure` does not persist. `.forgejo/workflows/ci.yml`: add `golangci-lint` and `go test -race` (`Makefile` already does; Forgejo is `-count=1` + `vet` only).

### Module: `playback-contract`

**Status:** OPEN

1. [ ] `proto/playbackv1/session.proto` + `encode.go` `SessionInput`: add `imdb_id`, `tmdb_id`, `tvdb_id` and marshal them. `playback-monitor` `sessionEventFromPlaybackJSON` already reads those via `externalIDsFromMap`; bridges cannot send them today so first-play history filters miss until catalog sync.
2. [ ] `proto/playbackv1/query.proto` `Session`: add `imdb_id`, `tmdb_id`, `tvdb_id`, `library_name` to match `playback-monitor` `SessionRecord` / public `/history` JSON. Read model is missing fields the DB already stores.
3. [ ] `README.md`: delete `PLAYBACK_MONITOR_DB_PATH` / `DATABASE_URL` / Timescale / `playback-plus-postgres` (those belong to `playback-monitor`). Document publishers (`jellyfin`, `plex`, `emby`), sole writer (`playback-monitor`), protojson `UseProtoNames` mesh JSON, and that event-type strings must match `contracts-playback/events`.
4. [ ] `encode.go` `MarshalSessionEvent`: reject empty or unknown `event_type`; allow only `playback.started` / `playback.progress` / `playback.stopped`. Export `KnownEventType` and test it. Monitor already requires `event_type`; the contract currently accepts any string.
5. [ ] `query_helpers.go` `HasMediaScope`: do not treat `media_type` alone as a specific-title scope (category filter ≠ identity). Add `SessionFilter.Validate()` that rejects `since_unix > until_unix`. Cover both in `query_helpers_test.go`.
6. [ ] `.forgejo/workflows/ci.yml`: add a proto-check (`make proto` + fail if `*.pb.go` is dirty). Pin `protoc`/`protoc-gen-go` versions in `Makefile`. Origin CI is Forgejo (Appendix E); generated stubs can drift with no gate.
7. [ ] Add `AGENTS.md` + `muxcore.json` as a library (no sidecar, no ports, no TLS). Capability like `contracts.playback.session`. Checklist: proto field + `make proto` + helper/test + README row. Do not copy sidecar boilerplate.
8. [ ] Annotate PII on `session.proto` / `query.proto` (`ip_address`, `user_id`, `user_name`, `media_path`) and add a README "PII in mesh JSON" note: publishers must not log raw event bodies.
9. [ ] `resolution.go` + `session.proto` comment: document buckets `SD|480|576|720|1080|QHD|4k|unknown`. Stop `normalizeResolutionLabel` returning the raw unrecognized `label`. Extend `resolution_test.go` (576, SD-from-240, unknown label → `""`).
10. [ ] Add `CHANGELOG.md` + `COMPATIBILITY.md` (same shape as `contracts-downloader`) for additive protojson fields only. Expand `encode_test.go` to round-trip every `SessionInput` field, unknown-field discard, and zero `OccurredAt` defaulting to UTC now.

### Module: `playback-guard`

**Status:** COMPLETE — HTTPAddr, DeleteRule, terminate-on-violation, inactivity ticker, event subscribe reconnect, mesh TLS dials, loopback bind + RPC auth, persisted notify_on_violation, name-only history query, docs/CI/contracts.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/playback-guard.md -->

### Module: `playback-monitor`

**Status:** COMPLETE — operator HTTP token auth, Tautulli SSRF guard, history retention + DeleteUserHistory RPC, GeoIP MMDB/plex modes, active session timeout, mesh reconnect, healthz DB ping, README/settings persistence, golangci-lint CI, mesh TLS notification dials.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/playback-monitor.md -->

### Module: `plex`

**Status:** OPEN

1. [ ] Page `listPlexSectionItems` (`internal/library_catalog.go`) with `X-Plex-Container-Start`/`X-Plex-Container-Size` until a short page; for show/artist sections also fetch `type=4` (episode) and `type=10` (track). httptest a library larger than one page. `/library/sections/{key}/all` returns series-only for TV, and `plexGET`’s 8MiB `LimitReader` silently truncates.
2. [ ] Parse Plex `Guid` on `plexMediaItem` and set `imdb_id`/`tmdb_id`/`tvdb_id` on `libraryCatalogPayload` (playback-monitor already filters on them). In `syncLibraryCatalog` track seen ratingKeys and publish `action=removed` for gone items (upsert-only today). Return `fmt.Errorf("plex … status %d", code)` from `listPlexLibrarySections`/`listPlexSectionItems` when `code >= 300` (currently `return nil, err` with `err == nil`).
3. [ ] Gate `GET /sync-lists` (`internal/sync_lists_http.go`) with a shared secret (`PLEX_HTTP_SECRET` / `X-Plex-Bridge-Secret`); empty secret → 401. httptest 401. HTTP listens on `:8476` (all interfaces in `run-host.sh`) and currently returns device names, user IDs, and titles with no auth.
4. [ ] Make `GET /healthz` (`internal/module.go`) call `Health()` and return non-200 + JSON when unconfigured or `/identity` fails. It always writes `{"status":"ok"}` today.
5. [ ] Persist connection knobs to `{PLEX_DATA_DIR}/settings.json` (jellyfin pattern; run-host already creates `$DATA/plex`). Honor `plex_sse` in `updateSetting` (`settingsDefs` lists it; switch hits `unknown setting`). Restart currently drops admin-ui URL/token/poll changes.
6. [ ] Reconnect in `connectCore` after a successful dial: on `Events.Publish` failure or client close, clear `m.mc` and retry with backoff. First success currently `return`s; a mesh drop silences playback and catalog events until process restart.
7. [ ] Add `ListSessions` to `proto/plexv1/plex.proto` (id, user, item, position, paused, device) and extend `Status` with `sse_connected` / `machine_id`. `TerminateSession` exists with no list surface.
8. [ ] In `runSSEConnection` (`internal/sse.go`) drop `X-Plex-Token` from the query string (header is already set) and set a read deadline / client timeout so a hung stream clears `sseConnected` and poll resumes. `pollSessionsLoop` skips `pollSessionsOnce` entirely while `sseConnectedNow()`.
9. [ ] Add `PlayURL` RPC returning Plex web `…/web/index.html#!/server/{machineID}/details?key=/library/metadata/{ratingKey}` from cached `machineID` (`probeIdentity` / `machineIdentifier`). Jellyfin already has this; Plex households have no deep-link from admin/media-ui.
10. [ ] Add httptest coverage for `syncLibraryCatalog` (upsert + removed + guid), `Health` configured vs empty, `GET /healthz` 503, `TerminateSession` with empty `session_id`, and SSE URL without token. Document `PLEX_CATALOG_SYNC_SEC`, `PLEX_DATA_DIR`, and HTTP secret in `README.md`; add `.env.example`. Set `AGENTS.md` Contracts and `muxcore.json` `contracts` to `contracts-playback` + `playback-contract` (both say none / `[]`).

### Module: `publish-policy-default`

**Status:** OPEN

1. [ ] Rewrite shipped `policies.yaml` to match real publishers: drop fictional `notifier-discord`; add `notification-default` (`notification.*`); `jellyfin`/`emby`/`plex` (`playback.*`); `playback-guard` (`playback.guard.violation`); `media-list-sync` (`importlist.synced`); `health-monitor` (`health.*`); `downloader-sabnzbd` / `downloader-native-usenet` / `downloader-debrid` (`download.*`); `indexer-torznab` / `indexer-piratebay` (`indexer.*`). Use `groups` for downloaders/indexers/media managers instead of one-off caller lines.
2. [ ] Add a fixture test that `policy.Load`s repo `policies.yaml` and asserts the allow/deny matrix: listed callers above allowed for their prefixes; unknown caller denied; `media-list-sync` denied on `media.*` unless listed; `importlist.synced` / `playback.started` / `health.degraded` denied for callers that only have `module.*`.
3. [ ] In `internal/policy/registry.go` `registryAllowsCapabilities`, stop treating payload JSON `capability` as a grant for arbitrary event types. A module with cap `download` must not publish `job.done` / `media.imported` via `{"capability":"download"}`. Keep payload matching only on YAML `required_capability` after caller+event_type already matched. Cover in `registry_test.go`.
4. [ ] Wire `PolicyServer.Metrics()` out of dead code: expose read-only `allowed_total` / `denied_total` in `internal/settings.go` (or a scrape path the README already claims). Add a settings test that one AllowPublish allow + one deny increments both counters.
5. [ ] Make `subscribeRegistryEvents` in `internal/registry_sync.go` retry ListAll/subscribe/dial until `Stop` (context-cancel the `PUBLISH_POLICY_REGISTRY_SYNC_DELAY` sleep and streams). A single failed subscribe currently leaves registry capability sync dead for the process lifetime.
6. [ ] Implement a real `grpc_health_v1` `Watch` in `internal/module.go` (stream SERVING until the RPC context ends). `Health()` must fail before `Start` and after `Stop`. Stop the SIGHUP notifier in `Stop` (mesh client close already exists).
7. [ ] Fix deploy artifacts: `Dockerfile` `EXPOSE 9102` (not 9300) and include `policies.yaml` when building from module root (current `.dockerignore` is `*` plus `!cmd`/`!internal`/`!go.mod`/`!go.sum`, so the policy file is excluded). Set systemd `WorkingDirectory` + `Environment=PUBLISH_POLICY_FILE=…` in `deploy/systemd/muxcore-module.service`; pass `PUBLISH_POLICY_FILE` in `deploy/docker-compose.yml`.
8. [ ] Origin CI in `.forgejo/workflows/ci.yml`: checkout sibling `core` (see `_wave1/ci-siblings.json`) so `replace => ../core*` resolves on an isolated Forgejo clone; run `golangci-lint` (`.golangci.yml` exists; workflow is test+vet only) and `go test -race` to match the Makefile.
9. [ ] Repair version/docs drift: set `muxcore.json` version to `0.3.0` to match `Info()` / CHANGELOG; fix `AGENTS.md` ("Contracts: none declared"); add `settings` plus v0.3 registry matching to `COMPATIBILITY.md`. Document `PUBLISH_POLICY_AUDIT_PATH`, `PUBLISH_POLICY_REGISTRY_MATCH`, and `PUBLISH_POLICY_REGISTRY_SYNC_DELAY` in `.env.example`.
10. [ ] Expand `internal/settings.go` / `settings_test.go`: cover `registry_capability_matching` on/off (untested today); add a reload action that re-reads the current `policy_file` without changing the path; log audit JSONL write failures in `internal/policy/policy.go` `audit` instead of swallowing `OpenFile`/`Write` errors.

### Module: `ratelimit-tokenbucket`

**Status:** OPEN

1. [ ] Add `pkg/client` implementing `contracts.RateLimiterProvider` (`Allow` + `Enabled`) over `RateLimitService` (bufconn tests; RPC errors fail-open). Add `WireRateLimit` in `core/internal/bootstrap/wiring.go` (`FindByCapability("ratelimit")` → `DialSidecar(HTTPAddr)` → `api.Server.SetRateLimiter`), matching `WireCallPolicy`. Today nothing calls `SetRateLimiter`; muxcored always uses `DefaultRateLimiter`, so this sidecar never throttles API traffic.
2. [ ] Add `internal/module_test.go` cases for the bucket: disabled always allows (empty key included); enabled rejects empty key (`InvalidArgument`); burst exhaustion then refill after `1/rate` seconds; per-key isolation; `Enabled` RPC. `module_test.go` only covers `Info` + Init/Start/Stop.
3. [ ] Idle-TTL eviction of `Module.buckets` in `internal/module.go` (setting/env `idle_ttl` / `RATELIMIT_IDLE_TTL`, default e.g. 10m). Unique keys (client IPs) grow the map forever. Test that a stale key is dropped and a new Allow recreates it.
4. [ ] Re-validate env after parse in `NewModule`: reject `RATELIMIT_RATE`/`RATELIMIT_BURST` ≤ 0 (clamp happens before env override, so `-1`/`0` stick). Log and keep defaults on garbage values. Align `rate` `SettingDef.Type` with `burst` (`SettingTypeInt` or float, not `SettingTypeString`). Tests for env override + invalid env.
5. [ ] Origin CI (`.forgejo/workflows/ci.yml`): sibling-checkout `core` per `_wave1/ci-siblings.json` (isolated `go test` fails on `replace => ../core`). Run `go test -race` and `golangci-lint` (Makefile/`/.golangci.yml` already define them; Forgejo only does `go test` + `go vet`).
6. [ ] Fix `Dockerfile`: `HEALTHCHECK` runs `/module --health-check` (flag does not exist in `cmd/module/main.go`); `EXPOSE 0`. Expose `9800` and health-check a real probe. Make the image build without a sibling `../core` (drop replace in the image build or copy core). `deploy/docker-compose.yml` must set `RATELIMIT_ENABLED` and publish `9800:9800`.
7. [ ] Bind gRPC to `127.0.0.1` by default (or require mesh TLS / a shared token on `Allow`/`Enabled`). Default `:9800` is all-interfaces, unauthenticated: any LAN peer can lock out keys or explode the bucket map.
8. [ ] Make `Health` fail when gRPC is not serving (nil `lis` / after `Stop`) in `internal/module.go`. Log deny decisions in `allow` (key + remaining tokens) so operators can see limiting fire.
9. [ ] Replace `test/integration_test.go` (`//go:build integration` starter that registers a dummy `test-module` with live core and never imports this package) with in-process `Allow`/`Enabled` gRPC tests that CI runs.
10. [ ] Docs drift: `COMPATIBILITY.md` still lists v0.1.0 / core v0.4.0+ (module is 0.1.2 / `go.mod` core v0.5.8); point the README CI badge at Forgejo, not GitHub Actions; document fail-open default, process-local buckets, and that core must `WireRateLimit` or the sidecar is inert. Collapse `CHANGELOG.md` so `[0.1.2]` sits under the Keep-a-Changelog header. `AGENTS.md` “Contracts: none” is wrong (`RateLimitService` / `RateLimiterProvider`). Wire `var version` in `cmd/module/main.go` so Makefile `-X main.version` works; stop hardcoding `"0.1.2"` in `Info()`.

### Module: `request-media`

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/request-media.md -->

### Module: `secrets-vault`

**Status:** COMPLETE — OpenBao compose smoke, GCP/Azure httptest CRUD, Vault AppRole + token renewal, Infisical path split + auth retry, recursive KV v2 List, applyProvider timeout/swap, loopback bind + systemd env file, secrets-file migrate script, Forgejo lint/race CI, docs/Forgejo badge/AWS delete recovery gate.

<!-- pruned 2026-08-31: all 10 todos landed; see _wave1/module-todos/secrets-vault.md -->