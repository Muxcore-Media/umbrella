# ADR-0021: Concurrent agent roles and a file-based coordination bus

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Owner asked for about four concurrent agent roles that communicate through text files and learn when a blocker finishes (chat instruction, 2026-10-05). The role split, lanes and protocol are Delegated.

## Context

Delivery so far ran as one agent at a time, which orchestrated subagents (AGENTS.md). The owner wants several long-running agents to work at once. Without structure, parallel agents would collide in three places: the same submodule checkout, the umbrella's shared records (roadmap, SRS, validation log, ADR numbers, gitlinks), and contract changes that consumers must wait for.

## Decision

1. Four roles: **platform**, **services**, **frontend**, **integrator** ([ROLES](../agents/ROLES.md)). Each owns a lane of whole modules, listed in `scripts/coord.roles.json`. `_mvp` is split by path: the BFF (`cmd/mediauiprox`) belongs to frontend and the rest to integrator.
2. The integrator is the only writer of umbrella-wide records and gitlinks, and assigns ADR numbers. Other roles report through the bus.
3. Agents coordinate only through a directory of text files driven by `scripts/coord.mjs` ([COORDINATION](../agents/COORDINATION.md)). Items carry an owner and `blocked_by` edges. Events are append-only files. Module locks use atomic `mkdir`. Every file has one writer.
4. A blocked agent waits with `coord wait`, run as a background command that exits when a blocker resolves or a message arrives. It does not poll in a goal loop.
5. Shared filesystem is the default transport. Agents in separate containers use a git clone of a dedicated `agent-coord` branch as the coord directory. The one-writer layout means rebases never conflict on items or events.

## Alternatives considered

- **GitHub Issues as the bus.** Issues are already the execution tracker, and they work across machines. But they need API calls for every check, can't give an atomic module lock, and a wait would mean polling the API. Kept as the human-facing tracker. The integrator mirrors finished work to the roadmap, as before.
- **One orchestrator session with subagents or a workflow.** This handles dependencies natively, with no bus. But it serializes on one context and doesn't give the owner separate, long-lived agents per area. Still the right tool inside each role.
- **Lanes by milestone instead of by module.** Milestones cross repo boundaries (M4 touches the BFF, the SPA and modules), so two agents would edit the same repos.

## Consequences

- Each role's PRs stay in its lane's repos. Umbrella PRs come from the integrator, apart from new files a role adds under its own work (for example, a drafted ADR).
- Cross-lane work takes one more step: open an item, then wait. The gain is no edit collisions.
- Roadmap and SRS status lag a little behind module work, until the integrator folds results in.
- Git mode depends on push access to the `agent-coord` branch. Items and events must never contain secrets.
