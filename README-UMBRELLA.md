# MuxCore umbrella workspace

This directory is the **umbrella repo**: one git root that pins every module as a **submodule** (gitlink), in the same spirit as the Mike Pattyn platform shelf.

## Clone

GitHub (`github.com/Muxcore-Media`) is the only origin ([ADR-0002](docs/adr/0002-github-sole-origin.md)). All repositories are private; authenticate once with `gh auth login` and `gh auth setup-git`.

```bash
git clone --recurse-submodules https://github.com/Muxcore-Media/umbrella.git MuxCore
cd MuxCore
git submodule update --init --recursive
```

Start with agent rules are held outside this repository and the canonical plan [`MASTER-ROADMAP.md`](MASTER-ROADMAP.md).

## Layout

| Path | Role |
| ---- | ---- |
| `AGENTS.md` | Agent instructions (reading order, doc ownership, orchestration) + owner reference deployment |
| `MASTER-ROADMAP.md` | Canonical plan: milestones, tasks, status, doc map |
| `docs/specs/`, `docs/architecture/`, `docs/adr/` | Requirements (BRD/PRD/FRD/SRS), design (HLD/SDD/TDD/LLD), decisions |
| `docs/history/` | Superseded plans and loop logs (read-only) |
| `.cursor/skills/` | Agent skill shelf + fan-out orchestrators |
| `scripts/app-fanout.mjs` | Quality planner (submodule-aware) |
| `.gitmodules` | Module URLs (GitHub) + pinned SHAs |
| `*/` (module dirs) | Submodule checkouts — each pushes to its own module repo |

## Workspace builds (go.work)

Module `go.mod` files require published tags only ([ADR-0011](docs/adr/0011-go-workspace-and-published-versions.md)). To build against local sibling checkouts, generate a workspace file (gitignored):

```bash
scripts/gen-go-work.sh
```

Hygiene checks used by CI: `scripts/check-docs.mjs`, `scripts/check-retired-origin.sh`, `scripts/check-no-replace.sh`, `scripts/check-submodules.sh`, `scripts/sync-ci.sh --check`. Module CI workflows are generated from `scripts/ci-templates/` by `scripts/sync-ci.sh`.

## Day to day

```bash
# Pull umbrella + move submodules to pinned SHAs
git pull
git submodule update --init --recursive

# Work inside a module (commit + push from module dir as today)
cd media-ui-app
git checkout -b feat/foo
# … commit …
git push origin HEAD

# Record new submodule SHA in umbrella when ready
cd ..
git add media-ui-app
git commit -m "chore: bump media-ui-app"
```

## Vault deploy & local stack

Homelab soak deploy and SSH details: agent rules are held outside this repository. Local MVP reference stack: [`_mvp/README.md`](_mvp/README.md).

| Task | Command |
|------|---------|
| List deploy targets | `_mvp/scripts/deploy-module-to-vault.sh --list` |
| Deploy module to vault | `_mvp/scripts/deploy-module-to-vault.sh <module> --verify-all` |
| SSH preflight (before deploy) | `_mvp/scripts/preflight-vault-ssh.sh` |
| Full vault smoke | `_mvp/scripts/smoke-vault-all.sh` |
| Vault mesh CLI | `_mvp/scripts/muxcorectl-vault.sh health status` |
| Vault HTTP smoke | `_mvp/scripts/smoke-vault-health.sh` |
| Public edge smoke | `_mvp/scripts/smoke-vault-public.sh` |
| Local stack status | `cd _mvp && ./run-host.sh status` |
| Script tests (offline) | `_mvp/scripts/run-script-tests.sh` |

## Fan-out quality skills

Planning uses the umbrella branch at root; file diffs run inside each submodule repo:

```bash
node scripts/app-fanout.mjs plan --skill frontend-lint --app media-ui-app
```

Regenerate module lists after adding repositories:

```bash
node scripts/generate-fanout-config.mjs
```

## Bootstrap (existing checkout)

If you already had loose module clones and need to recreate the umbrella metadata:

```bash
./scripts/bootstrap-umbrella.sh
```

Only runs when `.git` is missing at the workspace root.
