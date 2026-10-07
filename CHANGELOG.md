# Changelog

All notable changes to MuxCore will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

## [Unreleased]

### Added
- Release-train preparation from committed pins and gitlinks, with offline fixtures and `train-*` GitHub prerelease notes (T-M7-01, ADR-0024).
- Canonical roadmap rewrite and specification set: BRD, PRD, FRD, SRS, HLD, SDD, TDD, LLD, ADR-0001…0011 (`docs/`).
- Umbrella CI on GitHub Actions; module CI templates (`scripts/ci-templates/`) and `scripts/sync-ci.sh`.
- Hygiene scripts: `check-docs.mjs`, `check-retired-origin.sh`, `check-no-replace.sh`, `check-submodules.sh`, `gen-go-work.sh`.

### Changed
- Umbrella release automation preserves historical `v*` tags, uses a separate changelog range for each tag family, and publishes notes without pushing changelog commits to `main`.
- GitHub is the only origin; retired self-hosted origin references, workflows, and scripts removed (ADR-0002, ADR-0003).
- Modules require published versions only; core v0.6.0 (ADR-0011).
- Historical plans and loop logs moved to `docs/history/`.

### Removed
- Empty submodules `media-ui-tv`, `muxcore-android`, `playback-contract` (content folded into `contracts-playback`).


### Added
- CI/CD pipeline — Go test matrix, golangci-lint, release tagging (#10)
- Changelog + release notes automation — this file and commit linting (#16)
- Benchmark suite for gRPC mesh, DB queries, transcoder hot paths (#17)
- Structured logging standardization with shared slog contract (#15)
- REST facade (`api-rest`) over core gRPC (#14)
- gRPC server TLS for auth-local (#13)
- Go module graph cleanup — fixed tenant import ambiguity (#12)
- Module-level test coverage sweep for weak modules (#11)
- Cross-module integration tests — pipeline, media, playback, backup, full-stack
- Circuit breaker with per-module state tracking in gRPC mesh
- Config reference documentation — 433 environment variables
- Household loop — invites, collections, queue, search indexers
- arrmigrate package — Sonarr/Radarr DB/API import
- Calendar sync, unified queue, failure UX
- aarch64/macOS CI, install scripts, deployment docs
- GHCR consumer mirror scaffolding (Dockerfile.monorepo, build-only smoke); publishing not yet enabled
- Deploy pipeline — rollback, atomicity, health verification, audit log

### Fixed
- auth-local security hardening — CSRF, gRPC bind, policy defaults, DB permissions
- media-movies enum type mismatches (artwork, history, module)
- Pre-existing compilation errors across submodules
