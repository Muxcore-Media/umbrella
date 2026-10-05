# ADR-0021: A module's version has one source — its `muxcore.json`

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

Modules report their version in `contracts.ModuleInfo.Version`, which is shown in discovery and checked by the smoke test against the spool catalog. A 2026-10-05 sweep of 95 modules found the reported version hard-coded in `Info()` or a `const`. In about 87 of them it disagreed with `muxcore.json` or with the latest tag, because releases bump the tag and `muxcore.json` (`scripts/module-release/ship.sh`) but not the code.

## Decision

1. `muxcore.json`'s `version` is the only source of a module's version. Each module embeds it from its root package with `//go:embed muxcore.json`.
2. `Info().Version` returns `modulesdk.ManifestVersion(<root>.ManifestJSON)` (core `sdk/go/module` ≥ v0.6.3), which strips a leading `v`. Hard-coded version constants are removed.
3. Releases keep bumping `muxcore.json` and the tag together, as `ship.sh` already does. This rollout is combined with rebuilding modules on the enrollment-capable SDK (T-M3-03f), so each module is touched once.

## Alternatives

- **`-ldflags -X`:** core's canonical spool build (ADR-0012) does not inject flags, so spool-built binaries would report `dev`.
- **`debug.ReadBuildInfo`:** reports `(devel)` for builds from a source checkout, which is how core builds spool modules.

## Consequences

- Every module gains a small root-package file embedding `muxcore.json`. The smoke test's version check becomes meaningful.
- Modules whose `muxcore.json` is a core runtime config (`_mvp`, the installer) are excluded.
