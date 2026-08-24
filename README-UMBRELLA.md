# MuxCore umbrella workspace

This directory is the **umbrella repo**: one git root that pins every Forgejo module as a **submodule** (gitlink), in the same spirit as the Mike Pattyn platform shelf.

## Clone

```bash
git clone --recurse-submodules ssh://forgejo@git.zem.systems:2222/muxcore/umbrella.git MuxCore
cd MuxCore
git submodule update --init --recursive
```

Replace the remote URL once the umbrella repo exists on Forgejo.

## Layout

| Path | Role |
| ---- | ---- |
| `AGENTS.md` | Homelab deploy + vault SSH reference |
| `.cursor/skills/` | Agent skill shelf + fan-out orchestrators |
| `scripts/app-fanout.mjs` | Quality planner (submodule-aware) |
| `.gitmodules` | Forgejo module URLs + pinned SHAs |
| `*/` (module dirs) | Submodule checkouts — each still pushes to its own Forgejo repo |

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
