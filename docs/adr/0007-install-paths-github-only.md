# ADR-0007: Install paths with GitHub as the only origin

- **Status:** Accepted (supersedes [ADR-0006](0006-registry-compose-install-path.md))
- **Date:** 2026-10-05
- **Decided by:** Delegated, following Owner decisions ADR-0002 and ADR-0006

## Context

ADR-0006 named a Forgejo or LAN OCI registry as the household install path. Forgejo is retired. The current GitHub token lacks `write:packages` (verified 2026-10-05: scopes `gist, read:org, repo, workflow`), so GHCR publishing remains blocked. The root `docker-compose.yml` includes a non-existent `_mvp/docker-compose.public.yml`, and `_mvp/docker-compose.ghcr.yml` lists only 13 services.

## Decision

Supported install paths, in priority order:

1. **Registry compose (Path A)** — `_mvp/docker-compose.registry.yml` with images built from GitHub sources by `publish-module-images.sh` into a **LAN OCI registry** (`_mvp/local-registry.sh`). Default `MUXCORE_REGISTRY=localhost:5000/muxcore`.
2. **GHCR compose** — same compose file with `MUXCORE_REGISTRY=ghcr.io/muxcore-media`; **Blocked** until a `write:packages` token exists. The separate `docker-compose.ghcr.yml` is retired in favour of one compose file parameterised by registry (removes the 13-service drift).
3. **Dev host binaries** — `_mvp/run-host.sh` for developers and soak.
4. **Installer** — `muxcore-installer` fetches GitHub release binaries (checksums verified) and uses the same household manifest and version pin.

The root `docker-compose.yml` includes `_mvp/docker-compose.registry.yml`. The fixture smoke remains the release gate ([ADR-0008](0008-fixture-only-acquisition-gate.md)).

## Consequences

- FR-INS-001 is met by path 1 without any external registry account.
- When `write:packages` becomes available, GHCR publishing is a workflow addition, not a redesign.
- Kubernetes/operator remain scaffolds (Deferred).
