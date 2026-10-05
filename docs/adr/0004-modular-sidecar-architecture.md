# ADR-0004: Core runtime + capability modules as sidecar processes over gRPC

- **Status:** Accepted
- **Date:** recorded 2026-10-05 (decision predates the record)
- **Decided by:** Historical — documented from implementation (`core/README.md`, `core.wiki/Architecture.md`, `core/internal/module`, `core/internal/grpcmesh`)

## Context

MuxCore's thesis is to replace a set of separate apps with one platform without becoming a monolith. The implementation chose a small core ("loom") that knows no media domain and hosts capability modules, each a separate Go process (sidecar) registering over gRPC.

## Decision (as implemented)

- `muxcored` provides registry, discovery by capability, policy-checked mesh calls, an event bus, storage orchestration, audit, and spool-based deploy.
- Every feature is a module in its own repository with a `muxcore.json` manifest and versioned contracts (`contracts-*`).
- Modules discover peers by capability; exclusive capabilities (auth, policy, secrets) have exactly one provider.

## Alternatives (not chosen historically)

Monolith with plugins (in-process), or orchestration of the existing Arr apps (dashboard approach — explicitly a non-goal NG-2).

## Consequences

- **Positive:** independent releases, swappable providers, fault isolation per process, marketplace deploy from source.
- **Negative (observed in audit):** ~100 repos to keep building (NFR-MNT-001); version skew across install paths (FR-INS-006); cross-module consistency is eventual; security relies on correct mesh identity (NFR-SEC-002); modules run with loom privileges unless sandboxed.
- These consequences drive milestones M0–M3.

## Revisit when

Repository count or cross-repo change cost becomes the main delivery bottleneck after M1 — consider consolidating small platform modules into fewer repos.
