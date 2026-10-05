# Architecture Decision Records

**Owns:** individual records of architecturally significant decisions — context, alternatives, rationale, consequences, status, and supersession.

## Rules

1. One decision per file: `NNNN-kebab-title.md`, numbered sequentially. Never renumber or delete.
2. **Status** is one of `Proposed`, `Accepted`, `Superseded by ADR-NNNN`, `Deprecated`. To change a decision, write a new ADR that supersedes the old one and update only the old one's status line and a "Superseded by" link — do not rewrite its body.
3. **Decided by** distinguishes who made the call:
   - `Owner` — the project owner (Zemdregon) decided it; cite the source (roadmap section, chat instruction with date).
   - `Delegated` — made by an agent under the owner's standing delegation ("you are the final authority", 2026-10-05). Not owner-approved; the owner may overturn it with a superseding ADR.
   - `Historical` — recorded retroactively from existing implementation; no decision record existed before.
4. Write an ADR when a change affects module boundaries, contracts, data ownership, security model, build/release/CI model, or a product commitment. Do not write ADRs for minor implementation preferences.

## Index

| ADR | Title | Status | Decided by |
|-----|-------|--------|-----------|
| [0001](0001-canonical-roadmap-and-spec-set.md) | Canonical roadmap location and specification set | Accepted | Delegated (2026-10-05) |
| [0002](0002-github-sole-origin.md) | GitHub is the sole origin; Forgejo / git.zem.systems retired | Accepted | Owner (2026-10-05) |
| [0003](0003-ci-on-github-hosted-runners.md) | CI on GitHub-hosted runners from a shared template | Accepted | Delegated (2026-10-05) |
| [0004](0004-modular-sidecar-architecture.md) | Core runtime + capability modules as sidecar processes over gRPC | Accepted | Historical |
| [0005](0005-consumer-ui-replaces-jellyfin-web.md) | `media-ui-app` replaces Jellyfin web; Jellyfin handoff near-term | Accepted | Owner (MASTER-ROADMAP §1, pre-2026-10) |
| [0006](0006-registry-compose-install-path.md) | Registry compose (Path A) is the household install path | Superseded by 0007 | Owner (MASTER-ROADMAP §1) |
| [0007](0007-install-paths-github-only.md) | Install paths with GitHub as the only origin | Accepted | Delegated (2026-10-05), follows Owner ADR-0002 |
| [0008](0008-fixture-only-acquisition-gate.md) | Fixture-only acquisition gate | Accepted | Owner (Appendix E) |
| [0009](0009-per-module-data-ownership.md) | Per-module data ownership (SQLite per module) | Accepted | Historical |
| [0010](0010-bff-direct-module-access.md) | BFF dials modules directly instead of via `ModuleMesh` | Accepted | Historical |
| [0011](0011-go-workspace-and-published-versions.md) | Published versions in `go.mod`, umbrella `go.work` for local dev; core v0.6.0 | Accepted | Delegated (2026-10-05) |
