# MuxCore umbrella workspace

This directory is the **umbrella repo**: one git root that pins every module as a **submodule** (gitlink), in the same spirit as the Mike Pattyn platform shelf.

## Clone

**GitHub (public mirror):**

```bash
git clone --recurse-submodules https://github.com/Muxcore-Media/umbrella.git MuxCore
cd MuxCore
git submodule update --init --recursive
```

**Forgejo (homelab origin):**

```bash
git clone --recurse-submodules ssh://forgejo@git.zem.systems:2222/muxcore/umbrella.git MuxCore
cd MuxCore
git submodule update --init --recursive
```

`.gitmodules` uses `https://github.com/Muxcore-Media/*` URLs. On homelab workstations, `url.*.insteadOf` in git config (see [`AGENTS.md`](AGENTS.md)) rewrites those fetches to Forgejo over SSH.

## Layout

| Path | Role |
| ---- | ---- |
| `AGENTS.md` | Homelab deploy + vault SSH reference |
| `.cursor/skills/` | Agent skill shelf + fan-out orchestrators |
| `scripts/app-fanout.mjs` | Quality planner (submodule-aware) |
| `.gitmodules` | Module URLs (GitHub) + pinned SHAs |
| `*/` (module dirs) | Submodule checkouts — each pushes to its own module repo |

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

Homelab soak deploy and SSH details: [`AGENTS.md`](AGENTS.md). Local MVP reference stack: [`_mvp/README.md`](_mvp/README.md).

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

Regenerate module lists after adding Forgejo repos:

```bash
node scripts/generate-fanout-config.mjs
```

## Bootstrap (existing checkout)

If you already had loose module clones and need to recreate the umbrella metadata:

```bash
./scripts/bootstrap-umbrella.sh
```

Only runs when `.git` is missing at the workspace root.
