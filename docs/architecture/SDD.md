# MuxCore — Software Design Document (SDD)

| | |
|---|---|
| **Status** | Active — v1.0 (2026-10-05) |
| **Owns** | The integrated software design: how subsystems collaborate, the shared design patterns every module follows, interface responsibilities, and how the design satisfies the [SRS](../specs/SRS.md) |
| **Does not own** | Component inventory and topology (→ [HLD](HLD.md)); engineering choices, tooling and migration strategy (→ [TDD](TDD.md)); per-subsystem detail (→ [LLD](lld/README.md)) |

---

## 1. Design principles

| ID | Principle | Consequence | Source |
|----|-----------|-------------|--------|
| DP-1 | **Core knows nothing about media.** Core hosts capabilities; domain logic lives in modules. | No core package imports a domain contract (only `contracts-reconciler`). | `core/README.md`; [ADR-0004](../adr/0004-modular-sidecar-architecture.md) |
| DP-2 | **Capability, not address.** Modules find peers by capability (`FindByCapability`) rather than configured hostnames. | Swappable implementations (e.g. `auth-local` ↔ `auth-oidc`, `secrets-file` ↔ `secrets-vault`). Exactly one provider per exclusive capability. | `core/internal/registry` |
| DP-3 | **Soft-fail optional peers.** Missing optional capability → feature hidden or `available:false`, never a crash. | Required set is the household manifest; everything else is optional. | NFR-REL-001, RULE-VAL-4 |
| DP-4 | **Own your data.** Each module owns its store; integration is by IDs and events. | No shared schema; eventual consistency. | [ADR-0009](../adr/0009-per-module-data-ownership.md) |
| DP-5 | **Fixture-first.** Every external integration has an offline fixture mode used by tests and smoke. | `*_FIXTURE` env flags; CI never touches live services. | [ADR-0008](../adr/0008-fixture-only-acquisition-gate.md) |
| DP-6 | **Fail closed on security wiring.** Missing policy → deny. | No `call.policy` → mesh calls fail; empty role policy → deny. | `grpcmesh/server.go`, `auth-local` |

## 2. Shared patterns

### 2.1 Module anatomy (every Go module)

```
<module>/
  cmd/module/main.go      # entrypoint: config from env, dial core, Register, serve
  internal/               # domain logic, store, gRPC/HTTP handlers
  proto/ or uses contracts-*  # service definitions
  muxcore.json            # manifest: id, version, capabilities, minCoreVersion, contracts
  .github/workflows/ci.yml
  Makefile, .golangci.yml, README.md, CHANGELOG.md, AGENTS.md
```

Lifecycle: `Info → Init → Start → (Health)* → Stop` per `core/pkg/contracts/module.go`, driven by core for in-process modules and by the SDK (`core/sdk/go/module`) for sidecars, which register via `ModuleRegistration.Register` and heartbeat health.

### 2.2 Collaboration styles

| Style | When used | Example |
|-------|-----------|---------|
| **Direct gRPC after discovery** | Request/response between known contracts; latency-sensitive | `media-automation` → `IndexerService.Search` on every `indexer` provider |
| **`ModuleMesh.Call`** (policy-checked, opaque JSON) | Generic calls where call policy should mediate | SDK settings calls |
| **Domain events** (core event bus) | State changes other modules react to | `media.file.imported`, `download.completed`, `media.movie.requested` |
| **HTTP between modules** | Legacy/simple JSON APIs, library-plus imports | `media-automation` → `media-books` `/api/books/{id}/import` |
| **BFF aggregation** | Browser/native clients | BFF dials modules directly (bypasses `ModuleMesh`), see [ADR-0010](../adr/0010-bff-direct-module-access.md) |

**Rule for new work:** prefer a `contracts-*` gRPC service + events. New module-to-module HTTP APIs require an ADR.

### 2.3 Events

- Canonical names and payloads: `contracts-media/events` (domain), `contracts-playback` (playback events), `contracts-ai`. Names are never renamed; payload changes are additive (`COMPATIBILITY.md` in each contract repo).
- Publishing requires a `publish.policy` provider; core's own lifecycle events depend on the `module.*` rule in `publish-policy-default`.
- Delivery is at-most-once in memory; with `MUXCORE_EVENT_JOURNAL_PATH` the WAL enables replay. Consumers must be idempotent (NFR-REL-004).

### 2.4 Persistence

SQLite per module in WAL mode with `SetMaxOpenConns(1)`; schema created/extended at startup with `CREATE TABLE IF NOT EXISTS` and guarded `ALTER TABLE ADD COLUMN`. Detail and migration rules: [LLD data ownership](lld/data-ownership.md); improvement path: [TDD §6](TDD.md#6-data-and-migration-strategy).

### 2.5 Configuration

Environment variables are the primary interface (documented in `docs/config-reference.md`); core additionally reads `muxcore.json` (schema `core/muxcore.schema.json`) and passes `MUXCORE_CFG_*` to spawned modules. Runtime-editable settings use the `settings` capability, rendered generically by admin-ui.

### 2.6 Security pattern

Browser → app session (auth-local code exchange) → app forwards the auth-local bearer as `x-auth-token` on mesh calls → core `AuthInterceptor` → `authorizer.Can(session, method)`. Module-to-module identity is TLS/mTLS (target: CN-bound, NFR-SEC-002). Detail: [LLD auth](lld/auth-and-sessions.md).

### 2.7 Unsupported identity-provider operations

For FR-AUTH-009, admin-ui distinguishes an actual AuthService
`codes.Unimplemented` response from discovery, authentication, permission and
backend failures. Only the former renders an explanatory state without mutation
controls. No provider-name heuristic or capability cache is used; provider
behavior may differ by operation or change over time. Existing `admin.access`
authorization still runs first. Detail and coverage limits are in the
[auth LLD](lld/auth-and-sessions.md#6-oidc-variant).

### 2.8 Secrets-provider selection

The default file provider and optional external provider implement the same
`secrets` capability, but core does not treat that capability as one of its
exclusive security providers. Supported deployment configuration selects one
provider explicitly (FR-INS-008, [ADR-0029](../adr/0029-secrets-provider-deployment-selection.md)).
Replacing a provider requires deliberate stop/configuration/migration/start
steps; discovery order is not a selection mechanism. Runtime backend health and
secret-data migration remain separate from compose/launcher selection checks.

## 3. Interface responsibilities

| Contract (repo) | Provider(s) | Main consumers | Notes |
|-----------------|-------------|----------------|-------|
| Core services (`core/proto`) | `core` | all | Registration, discovery, mesh, events, health, storage, spool, lifecycle, audit |
| `AutomationService` (`contracts-automation`) | `media-automation` | admin-ui, BFF, request-media | Queue, search, dispatch, blocklist, delay profiles |
| `IndexerService` (`contracts-indexer`) | `indexer-torznab`, `indexer-piratebay` | `media-automation`, admin-ui | Search + indexer CRUD |
| `DownloaderService` (`contracts-downloader`) | `downloader-native-torrent`, `downloader-qbittorrent` | `media-automation` | **Torrent-only today.** Usenet (`downloader-sabnzbd` local `muxcore.usenet.v1`), `downloader-native-usenet`, `downloader-debrid` diverge → unify (FR-ACQ-011, TDD §5) |
| `MediaAdminService` (`contracts-media-admin`) | `media-movies`, `media-tvshows`, `media-music`, `media-books` | admin-ui, BFF | Items, metadata, artwork, grab, tags, collections, calendar |
| `MetadataService` (`contracts-metadata`) | `metadata-tmdb` (partial: 3 RPCs missing), `metadata-musicbrainz` (empty repo) | libraries, BFF, request-media | |
| `ScannerService` (`contracts-scanner`) | `media-scanner` | `media-automation`, admin-ui | ImportPath, Scan, watch dirs |
| `NotificationService` (`contracts-notification`) | `notification-default`, `notification-apprise` | request-media, automation | |
| Events (`contracts-media`, `contracts-playback`, `contracts-ai`) | various | various | See §2.3 |
| `contracts-reconciler` | (library) | core | Structural contract matching at deploy |
| Auth gRPC (`authv1`) + HTTP | `auth-local` (full), `auth-oidc` (subset) | core, admin-ui, BFF, api-rest | |
| BFF HTTP | `_mvp/cmd/mediauiprox` | SPA, iOS, tvOS | Partially documented in `_mvp/BFF-API.md` → complete it (TDD §7) |

## 4. How the design satisfies the requirements

| Requirement group | Design elements | Remaining design work |
|-------------------|-----------------|-----------------------|
| FR-INS-* | Household manifest as single module list; env-first config; compose + run-host share the manifest | Remove phantom compose include; single version train ([ADR-0011](../adr/0011-go-workspace-and-published-versions.md), [ADR-0007](../adr/0007-install-paths-github-only.md)) |
| FR-AUTH-* | Pluggable `auth`/`authorizer`/`identity` capabilities; role policy file; code-exchange sessions | Capability-aware admin-ui (FR-AUTH-009); profiles (FR-AUTH-010) as BFF + userdata feature |
| FR-REQ-* | `request-media` state machine + events + quotas | Library-plus request routing (FR-REQ-006) |
| FR-ACQ-* | `media-automation` orchestrates via contracts; scorer is a separate capability; fixture guard | Quality-tier cutoff; downloader contract unification |
| FR-LIB-* | Scanner as the single import path; rename as a service | Library-plus imports through scanner (FR-MM-004) |
| FR-PLAY-* | BFF resolve + stream; userdata module; transcoder capability | Server-side parental filter middleware in BFF (FR-PLAY-007) |
| FR-ADM-* | admin-ui discovers capabilities and renders pages per capability | Indexer/downloader pages; unified wanted over `MediaAdminService` for all media |
| FR-BAK-* | `Backupable` export + directory archives | Manifest-driven coverage; restore allow-list |
| FR-EXT-* | Spool deploy pipeline with verification | Signature-required household profile |
| NFR-SEC-* | Fail-closed policy wiring; TLS/mTLS available; CSRF in admin-ui | See [TDD §8](TDD.md#8-security-hardening-approach) |
| NFR-MNT-* | Module-per-repo; umbrella pins; Conventional Commits | GitHub-only CI, `go.work`, published versions ([ADR-0002](../adr/0002-github-sole-origin.md), [0003](../adr/0003-ci-on-github-hosted-runners.md), [0011](../adr/0011-go-workspace-and-published-versions.md)) |

## 5. Known design debts (tracked as requirements)

| Debt | Requirement |
|------|-------------|
| Unauthenticated `Register`; first-provider-wins for security capabilities | NFR-SEC-002 |
| BFF bypasses mesh policy and dials modules directly | Accepted ([ADR-0010](../adr/0010-bff-direct-module-access.md)); revisit if multi-node |
| Two userdata stores (BFF file fallback + `userdata-local`) | FR-PLAY-004 — make `userdata-local` authoritative; file store dev-only |
| `media-ui-app/Dockerfile` + `nginx.conf` builds a non-BFF image that cannot work as the product image | Remove or mark dev-only (M2 task) |
| `worker-pool-memory` deprecated in favour of core's pool | Keep until removed from spool tags (M1 cleanup) |
| Core never uses `spool-resolver-http`, `circuitbreaker-simple`, `data-redaction-pattern`, `input-validate-jsonschema`, `serialization-safe` | They are optional module-to-module services; documented as such, not fabric features |
