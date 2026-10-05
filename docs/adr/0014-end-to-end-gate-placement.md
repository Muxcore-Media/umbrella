# ADR-0014: Placement of the end-to-end release gate

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

M2 requires a passing fixture smoke covering request → grab → import → play on a fresh install (SM-1, FR-INS-003, ADR-0008). There are two candidate levels:

- **In-process Go test:** uses `core/integsupport` and the modules' `integsupport` packages. It is fast, but it doesn't exercise compose wiring, volumes, discovery between containers, or the BFF.
- **The real compose stack:** exercises all of those. The M2 design review found several defects that only the real stack shows (missing state volumes, BFF wiring, fixture marker, scanner size floor).

## Decision

1. The release gate is the **compose fixture smoke** (`_mvp/smoke.sh` with `SMOKE_REQUIRE_ACQUISITION=1`) on a stack started from the root `docker-compose.yml`.
2. The **clean-room job** lives in the umbrella (`.github/workflows/clean-room.yml`, `workflow_dispatch`), with steps in `scripts/clean-room.sh` so they can be reproduced locally with podman. It builds module binaries once in workspace mode with the ADR-0012 flags, packages them into images, pushes to an ephemeral `registry:2`, pulls via the root compose, then runs the smoke (and optionally the restore drill and an RSS measurement).
3. The in-process cross-module suite (`integration/`) is a **secondary, fast gate** in normal CI.

## Consequences

- The gate needs private module access, so it is blocked by B-1 in CI and runs locally until then.
- Images built in workspace mode come from the pinned commits rather than published tags. A `GOWORK=off` mode can be added when exact artefact fidelity is needed.
