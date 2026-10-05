# MuxCore — Business Requirements Document (BRD)

| | |
|---|---|
| **Status** | Active — v1.0 (2026-10-05) |
| **Owns** | Business problem, outcomes (`BO-*`), stakeholders, business constraints (`BC-*`), value proposition, success measures (`SM-*`) |
| **Does not own** | Product capabilities (→ [PRD](PRD.md)), requirements (→ [SRS](SRS.md)), delivery order (→ [roadmap](../../MASTER-ROADMAP.md)) |
| **Sources** | `MASTER-ROADMAP.md` Appendix A (market research, 2026-08-20) and Appendix E (constraints); owner decisions recorded in [ADR index](../adr/README.md) |

Provenance tags used below: **[Owner]** = decided by the project owner (Zemdregon) in a recorded source; **[Derived]** = derived during the 2026-10-05 documentation task from repository evidence; **[Proposed]** = a target set under delegation, not yet approved.

---

## 1. Business problem

Self-hosted media households run a *fragmented stack*: Sonarr, Radarr, Lidarr, Readarr, Prowlarr, Bazarr, Tdarr, Seerr/Overseerr, Wizarr, Tautulli, a torrent and a Usenet client, plus Jellyfin or Plex for playback. Each app has its own database, auth, UI, upgrade cycle, and failure modes. Operators glue them together with API keys and guides (TRaSH, Servarr wiki); families are exposed to several UIs. Running two quality tiers (e.g. 1080p and 4K) usually means *duplicate instances*. (Source: `MASTER-ROADMAP.md` Appendix A "Competitive map".)

MuxCore's thesis **[Owner]**: replace that stack with **one modular platform** — a core runtime (`muxcored`) hosting capability modules over a gRPC mesh — that delivers Arr-grade automation, a family-friendly UI, and simpler day-2 operations.

## 2. Stakeholders

| ID | Stakeholder | Interest | Source |
|----|-------------|----------|--------|
| SH-1 | **Project owner** (Zemdregon, GitHub `zemdregon`) | Strategic direction, approves major decisions, operates the reference deployment | `.project-management.md` "Role Definition" |
| SH-2 | **Household members** (viewers / requesters) | Find, request, and watch media without touching admin tools | Appendix A §1 |
| SH-3 | **Household operator** (homelab admin) | Install once, trust automation, triage failures quickly, back up and upgrade safely | Appendix A §2, §4, §5 |
| SH-4 | **Module developers** | Build and publish modules against stable contracts | `muxcore-module-starter/`, `core.wiki/Writing-Modules.md` |
| SH-5 | **Delivery agents** (AI coding agents orchestrated per `AGENTS.md`) | Unambiguous scope, requirements, and verification | `AGENTS.md` "Subagent routing" |

## 3. Business outcomes

| ID | Outcome | Rationale | Provenance |
|----|---------|-----------|------------|
| **BO-1** | **One install replaces the stack.** A non-developer installs MuxCore on one machine and gets request → acquire → library → play without cloning sibling repos. | The "Definition of done (product)" | [Owner] Appendix E |
| **BO-2** | **Household self-service.** Family members request, watch, and track progress in one consumer UI and stay out of admin. | "Homelabbers leave Arr when family can request, watch, and stay out of admin" | [Owner] Appendix A §1, §"Priority" #1 |
| **BO-3** | **Arr-grade decision quality.** Release selection, upgrades, and blocklisting are trustworthy enough that operators stop second-guessing grabs. | "Without TRaSH-class scoring + battle-tested parsing, MuxCore feels like a clever beta" | [Owner] Appendix A §2, #2 |
| **BO-4** | **Trust: install, migrate, stay updated, secure by default.** Operators can migrate from Sonarr/Radarr, upgrade with rollback, and are not exposed by insecure defaults. | Market standard is linuxserver images + years of stable versions | [Owner] Appendix A §4, #3; secure-defaults emphasis [Derived] from audit |
| **BO-5** | **Daily-driver control center.** One operator UI for health, queue, calendar, wanted/missing, failures, users, and backups. | "The daily-driver control center people open 20×/day is not there yet" | [Owner] Appendix A §5, #6 |
| **BO-6** | **Own the watch experience.** The consumer UI replaces Jellyfin web including playback; Jellyfin handoff is acceptable only near-term. | Playback product decision | [Owner] `MASTER-ROADMAP.md` §1 (see [ADR-0005](../adr/0005-consumer-ui-replaces-jellyfin-web.md)) |
| **BO-7** | **Beyond movies/TV.** Music, books, comics, and audiobooks are managed without separate apps. | Lidarr/Readarr-grade loop is a market gap | [Owner] Appendix A §2, #7 (explicitly *after* movies/TV are better than Arr) |

Priority order for investment is BO-1/BO-2 → BO-3 → BO-4 → BO-6 → BO-5 → BO-7 **[Owner]** (Appendix A "Priority: what to build to actually win", items 1–7), with security of defaults (part of BO-4) pulled forward **[Derived]** because the audit found insecure shipped defaults (see [SRS](SRS.md) `NFR-SEC-*`).

## 4. Value proposition

**Winning claim (target, not current)** **[Owner]**: "One install replaces Sonarr + Radarr + Prowlarr + Seerr + download client — and multi-quality without dual instances — with Arr-grade picks and a family UI."

**Explicit non-claims:** MuxCore does not compete with start-page dashboards (Homarr, Dashy) **[Owner]**, and architecture ("modular mesh with 100 modules") is not a selling point on its own **[Owner]**.

## 5. Business constraints

| ID | Constraint | Provenance |
|----|-----------|------------|
| **BC-1** | No task may require the owner to create paid accounts or enter payment details. Paid Real-Debrid/AllDebrid/Usenet/TMDB may be optional integrations, never hard dependencies. | [Owner] Appendix E |
| **BC-2** | Acquisition is developed and verified only against fixtures/mocks (`DOWNLOADER_ENGINE=fixture`, httptest). No tasks that browse or hit pirate sites or real swarms. | [Owner] Appendix E "Acquisition rule" ([ADR-0008](../adr/0008-fixture-only-acquisition-gate.md)) |
| **BC-3** | **GitHub (`github.com/Muxcore-Media`) is the only source-code origin, issue tracker, and CI host.** The former self-hosted git origin is retired and must not be referenced. | [Owner] instruction 2026-10-05 ([ADR-0002](../adr/0002-github-sole-origin.md)) |
| **BC-4** | The GitHub organisation is on the **Free plan with private repositories** (verified 2026-10-05 via `gh api orgs/Muxcore-Media`). CI minutes and package storage are therefore limited; GHCR publishing additionally needs a token with `write:packages`, which the current token lacks. | [Derived] |
| **BC-5** | The product is self-hosted by households; MuxCore operates no hosted service and collects no telemetry from installs. | [Derived] — no telemetry code or hosted service found; recorded as a constraint so it is not silently changed |
| **BC-6** | Software is pre-1.0 (`core` v0.5.x, most modules v0.1–v0.5). Public positioning must carry a beta disclaimer until BO-1…BO-4 success measures are met. | [Owner] Appendix A §4 "Version trust … beta disclaimer correct" |

## 6. Success measures

All numeric thresholds below are **[Proposed]** targets set under delegation on 2026-10-05; they are not owner-approved commitments and are not SLAs.

| ID | Measure | Outcome | How measured | Target |
|----|---------|---------|--------------|--------|
| SM-1 | Fresh single-machine install reaches a passing fixture smoke (`_mvp/smoke.sh` + fixture acquisition assertions) using only published artefacts and documented steps | BO-1 | Scripted clean-room run (container) | Passes on every release candidate |
| SM-2 | A household member completes request → available → play without opening admin-ui | BO-2 | Fixture e2e journey test (J-03 in [PRD](PRD.md)) | Passes in CI |
| SM-3 | Fixture release-selection corpus: grab decision matches expected choice | BO-3 | `media-automation` + `media-custom-formats` decision tests | ≥ 95 % of corpus cases, 100 % of "must never grab" cases |
| SM-4 | Shipped defaults pass the secure-defaults checklist (no default passwords, no unauthenticated mutating endpoints, TLS on mesh outside dev profile) | BO-4 | `NFR-SEC-*` verification in [SRS](SRS.md) | 100 % of Must items |
| SM-5 | Sonarr/Radarr migration imports library catalogue, quality profiles, root folders, and indexers from a fixture Arr DB/API | BO-4 | `admin-ui/arrmigrate` tests against fixtures | All listed entity types |
| SM-6 | Operator can see every failed/stuck item and act on it from one page | BO-5 | admin-ui handler tests + fixture failure injection | Covers movie, TV, music, books, comics, audiobooks |
| SM-7 | Consumer playback works without Jellyfin for direct-play and transcode paths | BO-6 | Fixture playback tests (`/api/playback/resolve`, HLS) | Direct + transcode paths green |
| SM-8 | Every active repository builds and tests green on GitHub-hosted CI at the umbrella-pinned SHA | BO-4 (trust) | GitHub Actions status per repo | 100 % of active repos |

## 7. Business risks

| ID | Risk | Effect | Mitigation (owner of detail) |
|----|------|--------|------------------------------|
| BR-1 | Free-plan CI minutes exhausted by ~100 private repos | CI stops gating merges | Path filters, concurrency cancellation, umbrella-level matrix ([TDD](../architecture/TDD.md) §4) |
| BR-2 | GHCR publishing stays blocked (`write:packages`) | No public images; BO-1 relies on build-from-source or a LAN registry | LAN-registry and source installs remain supported ([ADR-0007](../adr/0007-install-paths-github-only.md)) |
| BR-3 | Feature breadth outruns quality (100+ modules, many scaffolds) | Trust erodes (BO-4) | Roadmap prioritises build health and security before new surface |
| BR-4 | Legal exposure from acquisition features | Reputational/legal | BC-2 fixture-only gate; no bundled indexer lists; operator supplies sources |
