# Agent coordination bus

| | |
|---|---|
| **Owns** | How concurrent role agents hand work to each other and wait on each other: the file layout, the commands, and the waiting pattern |
| **Links, does not duplicate** | Roles and lanes → [ROLES.md](ROLES.md) · decision → [ADR-0021](../adr/0021-concurrent-agent-roles.md) · implementation → [`scripts/coord.mjs`](../../scripts/coord.mjs) |

The bus is a directory of plain text files driven by `node scripts/coord.mjs`. Agents and humans can read it with `cat`. The roadmap stays authoritative for scope and status; the bus only tracks who is doing what right now and what blocks whom.

## Where the bus lives

| Setup | `MUXCORE_COORD_DIR` | Transport |
|-------|---------------------|-----------|
| All agents on one machine (several terminals, worktrees, or `claude-workstation`) | any shared path, e.g. `$HOME/.muxcore-coord` | the filesystem; nothing else to do |
| Agents in separate containers (cloud sessions) | a clone of a dedicated coordination branch, e.g. `git clone -b agent-coord https://github.com/Muxcore-Media/umbrella.git ~/.muxcore-coord` | git: every command commits, rebases and pushes the coord clone. Polling is 60 s by default |

Unset, it defaults to `<umbrella>/.coord/` (git-ignored), which suits a single checkout with several worktrees only if they all set the variable to the same path. The coord directory is never the umbrella checkout itself.

## Layout

```text
$MUXCORE_COORD_DIR/
  items/<id>.md          one work item; created by anyone, edited only by its owner role
  events/<ts>-<role>-<rand>.json   append-only; one file per change or message, never edited
  cursor/<role>          last event the role has read
  locks/<module>/owner.json        claim on a module checkout
```

Every write creates a new file or replaces a file that belongs to one role, so two agents never write the same file. That keeps the shared-filesystem mode race-free and the git mode conflict-free.

An item file:

```text
---
id: T-M4-01
title: server-side parental middleware in BFF
owner: frontend
status: todo            # todo | in-progress | blocked | review | done | dropped
requested_by: integrator
blocked_by: T-M4-02
modules: _mvp:cmd/mediauiprox
evidence:
updated: 2026-10-05T12:00:00.000Z
---
free-form notes; `coord set --note` appends dated lines here
```

`blocked_by` is the dependency edge. An item is *ready* when every item it names is `done` or `dropped`. Use `status: blocked` with an empty `blocked_by` only for waits outside the bus (an owner action such as B-1), and say why in a note.

## Commands

Role comes from `--as <role>` or `MUXCORE_ROLE`.

| Command | Does |
|---------|------|
| `new <id> --title T [--owner R] [--blocked-by A,B] [--modules m,…]` | Create an item (a request when `--owner` is another role). Notifies the owner. |
| `set <id> [status] [--note …] [--evidence …] [--blocked-by …]` | Owner-only update. `done` requires evidence. |
| `next` | Your ready items. Exit 1 when nothing is ready. |
| `list [--owner R] [--status S] [--all]` · `show <id>` | Board views. |
| `send --to <role\|all> [--item id] <text>` | Message another agent (questions, ADR-number requests, heads-ups). |
| `inbox [--peek]` | Unread events that concern you: messages to you, new items for you, changes to items you requested, and blockers of your items resolving. |
| `wait [<id,…>] [--inbox] [--timeout s]` | Block until the items are resolved, or (with `--inbox`) until something lands in your inbox. Exit 0 when satisfied, 2 on timeout (default 1500 s). |
| `lock <module>` · `unlock <module>` · `locks` | Claim a module checkout. Refuses modules outside your lane and modules another role holds. |
| `log [--n N]` · `sync` | Recent events; force a git round trip. |

## Waiting instead of polling

Running each role under `/goal` keeps it working, but a goal loop alone makes a blocked agent spin: it wakes, finds nothing ready, and checks again. Use a blocking wait instead:

1. When `coord next` exits 1, the agent runs `node scripts/coord.mjs wait --inbox` **as a background command** (Claude Code `run_in_background`) and ends its turn.
2. The command sleeps cheaply, polling the files every 10 s (60 s over git), and exits when another agent finishes a blocker, sends a message, or files a new item for this role.
3. The harness wakes the agent when the command exits. The agent reads its inbox and continues.
4. On exit 2 (timeout), the agent re-arms the wait. The default 1500 s timeout stays under the harness's 30-minute background limit.

A role that still has ready work but wants to know as soon as one specific dependency lands runs `coord wait <id>` in the background and keeps working on something else.

With `MUXCORE_ROLE` set, the umbrella's Claude Code `SessionStart` hook (`.claude/settings.json`) prints the role and its unread events, so a restarted or compacted agent sees what it missed. Without `MUXCORE_ROLE` the hook prints nothing.

## Rules

- The bus does not replace the roadmap or GitHub Issues. The integrator copies outcomes into the roadmap and SRS (ROLES.md, hand-off rule 4).
- Evidence on `done` must name the command that ran and the published tag or SHA (AGENTS.md: a claim is not evidence).
- Never put secrets, tokens or host addresses in items or events. In git mode they are pushed to GitHub.
- Clean up: `dropped` items stay as history. Delete the coord directory only when every role is stopped.
