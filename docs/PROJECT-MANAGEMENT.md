# MuxCore Project Management — Operational Plan

**Date:** 2026-09-02
**Status:** ACTIVE

## Role Definition
- **Linus (me):** Project Manager — manages Issues, boards, Cursor agents, executive reviews
- **Zemdregon:** Strategic direction — determines priorities, approves major decisions
- **Cursor agents:** Sub-agents — execute development tasks under my direction

## Cursor Agent Model Selection
| Task Type | Model | Rationale |
|-----------|-------|-----------|
| Main tasks, backend dev | Composer 2.5 | Best for code generation, reasoning |
| Sub-agent tasks | Composer 2.5 | Consistency with main tasks |
| Light frontend work | Composer 2.5 | Acceptable for simple UI |
| Frontend design (visual) | Sonnet | Optimal cost-to-performance for design |
| Research, analysis | Cursor (as sub-agent) | Offload from Opencode Go limits |

## Note on Subscriptions
- **Opencode Go:** Lower usage limits — offload heavy tasks to Cursor
- **Cursor Ultra:** Aggressive utilization target — exhaust quota before renewal

## GitHub-Native Tracking
Execution tickets live in GitHub Issues (and [org project 2](https://github.com/orgs/Muxcore-Media/projects/2/views/1) when the agent can read it).

**Markdown that stays (umbrella only):**
- [`MASTER-ROADMAP.md`](../MASTER-ROADMAP.md) — canonical open-work checklist
- [`docs/parity/`](parity/) — shipped competitor-parity records

**Do not keep** `ROADMAP.md`, `TASKS.md`, remaining-work checklists, or status novels in module repos. Do not recreate `TASKS.md` or loop-summary files at the umbrella root.

Historical `_wave1/module-todos/` and loop-state JSON files are retired.

## Heartbeat Responsibilities
Every heartbeat:
1. Check Cursor agent status (running/completed/failed)
2. Update GitHub Issue statuses
3. Assign new tasks to Cursor agents
4. Identify additional Issues to create
5. Report progress to Zemdregon (executive summary)

## Phase 1: Initial Setup (first 3 heartbeats)
- [x] Migrate MASTER-ROADMAP tasks to GitHub Issues
- [ ] Create GitHub Project board
- [x] Set up label taxonomy
- [ ] Research PM best practices for AI-managed projects
- [ ] Identify tools/access needed
- [x] Clean up modules and repos — roadmaps/task lists belong in the umbrella only
- [x] Delete Markdown pointer/legacy task files (`TASKS.md`, per-module `ROADMAP.md` stubs, loop summaries)

## Phase 2: Active Project Management
- Direct Cursor agents on highest-priority issues
- Monitor progress, reassign as needed
- Executive reviews for strategic decisions
- Maintain GitHub Project board state

## Current Issues Created
1. #1: [P0] Public GHCR consumer mirror
2. #2: [P2] media-tvos-app: live validation on physical Apple TV
3. #3: [P1] Household loop: userdata, continue watching, resume player
4. #4: [P1] Quality moat: TRaSH-style format packs, upgrade-until, blocklist
5. #5: [P1] One install + migrate: public images, Arr DB/API import
6. #6: [P2] Calendar + unified queue + failure UX
7. #7: [P2] Acquisition completeness: qBit peer + production Usenet + debrid
