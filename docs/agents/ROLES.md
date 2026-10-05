# Agent roles

| | |
|---|---|
| **Owns** | The four concurrent agent roles, their lanes, hand-off rules, and the prompt each role runs under |
| **Links, does not duplicate** | Tasks and status → [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) §6 · module-to-lane map → [`scripts/coord.roles.json`](../../scripts/coord.roles.json) · bus protocol → [COORDINATION.md](COORDINATION.md) · decision → [ADR-0021](../adr/0021-concurrent-agent-roles.md) |

MuxCore work runs as **four long-lived agents in parallel**, one per role. A role owns a set of module directories (its *lane*) and the roadmap milestones that live mostly in those modules. Agents talk only through the coordination bus (`scripts/coord.mjs`); they never edit another lane's modules and never edit an item another role owns.

Each agent still follows [`AGENTS.md`](../../AGENTS.md): it reads the roadmap and specs, works as an orchestrator that hands module work to subagents, and verifies before it claims done.

## The four roles

| Role | Mission | Lane (modules) | Milestones |
|------|---------|----------------|-----------|
| **platform** | Runtime, SDK, contracts, security model. Upstream of everyone: a contract or SDK change lands and is tagged here first. | `core`, `contracts-*`, `auth-*`, `secrets-*`, policy/infra plug-ins (cache, database, storage, logging, tracing, metrics, …), `muxcore-module-starter` | M3, plus contract/SDK requests from other lanes |
| **services** | Capability modules: acquisition, library, metadata, playback back ends. | `request-media`, `media-*` (not the UIs), `downloader-*`, `indexer-*`, `metadata-*`, `notification-*`, `backup-local`, `userdata-local`, `playback-*`, `jellyfin`/`emby`/`plex`, `ai-*`, `workflow-tapestry` | M5 back ends, M6 |
| **frontend** | Every human-facing surface. | `media-ui-app`, `admin-ui`, `muxcorectl-cli`, `branding`, `muxcore-ios`, `media-tvos-app`, and the BFF slice `_mvp:cmd/mediauiprox` | M4, M5 pages, M8 |
| **integrator** | Install path, release train, end-to-end gate, and the **only writer of umbrella-wide records**. | umbrella root (roadmap, specs, ADR index, `scripts/`, CI templates, gitlinks), `_mvp` (except the BFF slice), `muxcore-installer`, `muxcore-operator`, `spool`, `integration`, `muxcore-docs`, `core.wiki` | M2, M7, plus bookkeeping for everyone |

The exact module lists are in `scripts/coord.roles.json`; `coord lock` enforces them. Archived dumps (`media-ui`, `media-jellyfin`, `cache-memory`, `custom-scripts`, `muxcorectl`) and `_wave1` are in no lane and stay untouched.

### Why these four

- **Cut along repo boundaries.** Each submodule pushes to its own repo, so lanes made of whole modules can run in parallel without merge conflicts. The one shared checkout (`_mvp`) is split by path; the frontend works in a `git worktree` of `_mvp` for the BFF.
- **Cut along the dependency graph.** Platform → services → frontend is the order in which contract changes must land (AGENTS.md "Delegation practices"). The integrator closes the loop by bumping pins and running the e2e gate.
- **One writer for shared records.** `MASTER-ROADMAP.md`, `docs/specs/SRS.md` status columns, the validation log, the ADR index and the umbrella gitlinks change in almost every PR. If all four agents edited them, every umbrella PR would conflict. Only the integrator edits them; the others report through the bus.

## Hand-off rules

1. **Need something from another lane → open an item for that role.** `coord new <id> --owner <role> --title …`. Then mark your own item `--blocked-by <id>`. Do not edit their module, even "just one line".
2. **Contract changes** (proto, SDK, `core` API): platform lands and tags them, then sets the item `done` with the tag as evidence. Consumers bump to the published tag (ADR-0011) — never a filesystem `replace`.
3. **Done means verified and published.** Before `coord set <id> done --evidence …`, the module's tests/lint ran green and the change is pushed and tagged on GitHub. The evidence string names the command and the tag or SHA.
4. **Status flows to the integrator.** When a role finishes a roadmap task, it sets the item to `review` (or `done`) with evidence. The integrator then updates the roadmap row, SRS status, and validation log, bumps the gitlinks, and sets its own bookkeeping item done.
5. **ADRs.** Any role may draft an ADR inside its PR. The integrator assigns the number and updates the index, so numbers never collide. Ask for a number with `coord send --to integrator "need ADR number for …"`.
6. **One agent per checkout.** Before editing a module, `coord lock <module>`; release it with `coord unlock` when the work is pushed. A lock older than a day with no matching events is stale; ask on the bus before `--force`.

## Initial board

The integrator seeds the board once from roadmap §6 (task IDs are the roadmap's; `.svc`/`.ui` suffixes split a task across lanes). The dependencies below are the starting proposal; owners adjust them with `coord set <id> --blocked-by …`.

```bash
export MUXCORE_ROLE=integrator
c() { node scripts/coord.mjs "$@"; }
c new T-M3-02 --owner platform --title "security profiles: remaining parts"
c new T-M3-03 --owner platform --title "authenticated registration: remaining parts"
c new T-M3-06 --owner platform --title "end-user identity: remaining parts"
c new T-M5-07 --owner platform --title "secrets-vault registry-compose profile (#151)"
c new T-M5-08 --owner platform --title "downloader contract unification (contract side)"
c new T-M5-08.svc --owner services --title "downloader modules on the unified contract" --blocked-by T-M5-08
c new T-M5-02.svc --owner services --title "unified wanted API across media"
c new T-M5-04.svc --owner services --title "transcoder queue/pool profiles back end (#121)"
c new T-M5-05 --owner services --title "migration depth + dry-run"
c new T-M5-06 --owner services --title "decision corpus + quality-tier cutoff"
c new T-M5-09 --owner services --title "subtitle providers beyond OpenSubtitles"
c new T-M4-07 --owner services --title "user deletion fan-out"
c new T-M4-02 --owner frontend --title "profiles incl. kids/PIN" --blocked-by T-M3-06
c new T-M4-01 --owner frontend --title "server-side parental middleware in BFF" --blocked-by T-M4-02
c new T-M4-03 --owner frontend --title "hide unsupported Cast/AirPlay"
c new T-M4-04 --owner frontend --title "operator sections from consumer Settings to admin-ui"
c new T-M4-05 --owner frontend --title "vitest-axe + admin-ui a11y tests"
c new T-M4-06 --owner frontend --title "Playwright fixture e2e J-02/J-03/J-08" --blocked-by T-M2-03
c new T-M5-01 --owner frontend --title "indexer & downloader admin pages" --blocked-by T-M5-08.svc
c new T-M5-02.ui --owner frontend --title "unified wanted page" --blocked-by T-M5-02.svc
c new T-M5-03 --owner frontend --title "per-page RBAC + OIDC-aware pages" --blocked-by T-M3-06
c new T-M5-04.ui --owner frontend --title "transcoder queue/pool admin page" --blocked-by T-M5-04.svc
c new T-M2-03 --owner integrator --title "smoke asserts fixture search->dispatch->import->library"
c new T-M2-05 --owner integrator --title "state_dirs + restore drill"
c new T-M2-11 --owner integrator --title "embedded module version metadata"
c new T-M2-10 --owner integrator --title "e2e gate job" --blocked-by T-M2-03,T-M2-05
c new T-M7-01 --owner integrator --title "release-train tagging + notes" --blocked-by T-M2-10
```

M6 (services) and M8 (frontend) items are added when their lanes run out of M4/M5 work.

## Running a role

Start each role in its own terminal or cloud session, from its own umbrella checkout (or a `git worktree` of one). All four must see the same coord directory; see [COORDINATION.md](COORDINATION.md#where-the-bus-lives).

```bash
export MUXCORE_ROLE=frontend                  # platform | services | frontend | integrator
export MUXCORE_COORD_DIR=$HOME/.muxcore-coord # same value in all four terminals
claude
```

Then give it the role prompt below, as a `/goal` or as the first message.

### Role prompt (same for every role; the role comes from `MUXCORE_ROLE`)

```text
You are the $MUXCORE_ROLE agent for MuxCore. Read AGENTS.md, docs/agents/ROLES.md and
docs/agents/COORDINATION.md. Loop until `node scripts/coord.mjs next` reports no open items
for your role and your inbox is empty:
1. `node scripts/coord.mjs inbox` and act on every event (answer questions, pick up new items).
2. `node scripts/coord.mjs next`; take the first ready item, `coord set <id> in-progress`,
   `coord lock` each module you will edit, do the work through subagents, verify, push, tag.
3. `coord set <id> done --evidence "<command> green; <repo>@<tag>"` (integrator: also update the
   roadmap/SRS/validation log for items other roles finished), then `coord unlock`.
4. If you need another lane, `coord new` an item for that role and block your item on it.
5. If nothing is ready, run `node scripts/coord.mjs wait --inbox` as a background command and
   end your turn; you will be woken when another agent finishes something you need or messages you.
Never edit modules outside your lane or items you do not own.
```

Suggested `/goal` condition: *"`coord next` prints no open items for my role, my inbox is empty, and every module lock I took is released."*
