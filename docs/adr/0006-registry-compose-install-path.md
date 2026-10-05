# ADR-0006: Registry compose (Path A) is the household install path

- **Status:** Superseded by [ADR-0007](0007-install-paths-github-only.md)
- **Date:** recorded 2026-10-05; decision from `MASTER-ROADMAP.md` §1 "Install product decision (locked)"
- **Decided by:** Owner

## Decision (as recorded at the time)

Origin / household install is a **Forgejo or LAN OCI registry**: `docker-compose.registry.yml` + `publish-muxcored-local.sh` + `local-registry.sh` + `_mvp/docs/PUBLIC-INSTALL.md`. Fixture smoke (`DOWNLOADER_ENGINE=fixture`) is the gate. A public GHCR consumer mirror is deferred until GitHub `write:packages` exists and is not an origin blocker.

## Why superseded

The Forgejo registry option no longer exists after [ADR-0002](0002-github-sole-origin.md). The LAN-registry and fixture-gate parts are carried forward unchanged by ADR-0007.
