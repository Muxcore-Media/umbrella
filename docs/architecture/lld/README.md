# Low-Level Design (LLD)

**Owns:** implementation-level detail where it is justified — module responsibilities, API contracts, schemas, state machines, algorithms, error handling, retries, idempotency, and concurrency.

LLD is written **only for subsystems whose detail is needed to execute roadmap work or to review safety-critical behaviour**. Everything else is specified by code (protos, Go packages) and module READMEs; this folder does not try to describe all ~100 modules.

| Document | Subsystem | Requirements |
|----------|-----------|--------------|
| [acquisition-pipeline.md](acquisition-pipeline.md) | Wanted → search → score → grab → download → import | FR-ACQ-*, FR-LIB-001, NFR-SEC-010 |
| [auth-and-sessions.md](auth-and-sessions.md) | auth-local, OIDC, app sessions, CSRF, mesh authorisation | FR-AUTH-*, NFR-SEC-002…007 |
| [core-mesh-and-modules.md](core-mesh-and-modules.md) | Core startup, registration, mesh call path, marketplace deploy, v0.6.0 layout | FR-EXT-*, NFR-SEC-002/003 |
| [data-ownership.md](data-ownership.md) | Per-module stores, personal data, retention, user deletion | NFR-DATA-*, FR-BAK-* |

Existing implementation-level documents elsewhere that remain authoritative for their subject (linked, not duplicated):

| Path | Subject | Note |
|------|---------|------|
| `_mvp/BFF-API.md` | Consumer BFF HTTP API (subset) | Incomplete — completion is a roadmap task |
| `_mvp/docs/DUAL-LIBRARY-API.md` | Multi-quality library API | — |
| `_mvp/docs/INFRA-BACKENDS.md` | Swapping storage/db/cache backends | — |
| `_mvp/PORTS.md` | Default ports | — |
| `media-ui-app/docs/PLAYBACK.md` | Player architecture | Best player reference |
| `docs/config-reference.md` | Environment variables | Some phantom variables; drift check planned (NFR-OPS-005) |
| `core/proto/**`, `contracts-*/proto/**` | gRPC contracts | Normative |
| `core.wiki/*` | Module system, contracts, events, workflow engine, TLS | Partly stale — see [roadmap doc map](../../../MASTER-ROADMAP.md#3-documentation-map) |
