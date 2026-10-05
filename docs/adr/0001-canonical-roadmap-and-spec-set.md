# ADR-0001: Canonical roadmap location and specification set

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

Planning was fragmented (audit 2026-10-05): two diverging copies of the roadmap (`MASTER-ROADMAP.md` and `_mvp/docs/MASTER-ROADMAP.md`), a pointer file (`TASKS.md`), a PM plan saying tracking moved to GitHub Issues (`.project-management.md`), loop-state JSON claiming "0 open", 22 per-module `ROADMAP.md` files with checked-off history, parity docs that overclaim, and no requirements or design documents with stable IDs. Draft PR `Muxcore-Media/umbrella#155` proposed moving parity docs and deleting module `AGENTS.md` files, but was never merged.

The root `MASTER-ROADMAP.md` already had the owner's decisions (playback, install), constraints (Appendix E), and market analysis (Appendix A), and was referenced by `TASKS.md`, `core.wiki/Roadmap.md`, and `.cursor/rules`. It was not sufficient on its own: no milestones with acceptance criteria, no current-state assessment, no document map, and no traceability.

## Decision

1. **Keep `MASTER-ROADMAP.md` at the umbrella root as the single canonical roadmap** (existing naming convention), rewritten to own scope, priorities, milestones, dependencies, tasks, and completion criteria.
2. Create the specification set under `docs/`: `docs/specs/{BRD,PRD,FRD,SRS}.md`, `docs/architecture/{HLD,SDD,TDD}.md`, `docs/architecture/lld/*.md`, `docs/adr/*.md`. Requirements have one home (SRS) with stable IDs; other docs reference IDs.
3. GitHub Issues are the execution tracker for individual tasks; each issue cites a roadmap task ID. The roadmap stays authoritative for scope and status.
4. Superseded documents are kept with a banner pointing to the replacement (not deleted).
5. PR #155's direction is partly adopted (single home for planning) but not its deletion of module `AGENTS.md` files, which carry useful per-module identity; per-module `ROADMAP.md` files are marked superseded.

## Alternatives

- **`docs/ROADMAP.md` as a new file** — rejected: competes with an existing, referenced canonical name.
- **GitHub Issues/Projects as the roadmap** — rejected as the *sole* source: issues lack the narrative, constraints, and traceability an agent needs; kept as execution layer.
- **Merge PR #155 as-is** — rejected: deletes per-module agent notes without replacement and predates the spec set.

## Consequences

- Agents read `AGENTS.md` → `MASTER-ROADMAP.md` → linked specs.
- Every implementation PR that changes behaviour must update the SRS status and roadmap task status.
- `_mvp/docs/MASTER-ROADMAP.md`, `TASKS.md`, `.project-management.md`, loop summaries, and module `ROADMAP.md` files carry superseded banners.

## Revisit when

The project adopts a docs site that renders specs from another source, or GitHub Projects gains the owner's preference as the primary planning surface.
