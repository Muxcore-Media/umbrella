# Changelog

All notable changes to MuxCore will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

### Changed
- Roadmaps and task lists live only in this umbrella (`MASTER-ROADMAP.md`, Issues). Per-module `ROADMAP.md` / `TASKS.md` stubs and pointer files are removed. Shipped parity records moved to `docs/parity/`.
- CI/CD pipeline — Forgejo Actions for Go test matrix, golangci-lint, release tagging (#10)
- Changelog + release notes automation — this file and commit linting (#16)
- Benchmark suite for gRPC mesh, DB queries, transcoder hot paths (#17)
- Structured logging standardization with shared slog contract (#15)
- OpenAPI/REST facade from proto definitions (#14)
- gRPC server TLS for auth-local (#13)
- Go module graph cleanup — fixed tenant import ambiguity (#12)
- Module-level test coverage sweep for weak modules (#11)
- Cross-module integration tests — pipeline, media, playback, backup, full-stack
- Circuit breaker with per-module state tracking in gRPC mesh
- Config reference documentation — 433 environment variables
- Household loop — invites, collections, queue, search indexers
- arrmigrate package — Sonarr/Radarr DB/API import
- docker-compose.public.yml — public image compose
- Calendar sync, unified queue, failure UX
- aarch64/macOS CI, install scripts, deployment docs
- Public GHCR consumer mirror (Dockerfile.monorepo, vault publish)
- Deploy pipeline — rollback, atomicity, health verification, audit log

### Fixed
- auth-local security hardening — CSRF, gRPC bind, policy defaults, DB permissions
- media-movies enum type mismatches (artwork, history, module)
- Pre-existing compilation errors across submodules
