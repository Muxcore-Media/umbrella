## MuxCore workspace setup

MuxCore is an **umbrella repo**: the workspace root tracks gitlinks for Forgejo modules. Each module (`media-ui-app`, `core`, `admin-ui`, …) is still its own git repo with its own branch and history.

1. Work inside **`{{MODULE_PATH}}`** (the module directory from the plan row). Run git only in that module unless the step says otherwise.
2. Reset this module onto `{{BASE_BRANCH}}` and name the branch before edits:
   `git reset --hard {{BASE_BRANCH}}`
   `git checkout -B {{WORKTREE_BRANCH}}`
   Create the worktree under `{{MODULE_PATH}}/.worktrees/{{WORKTREE_BRANCH}}` when the orchestrator placed you there.
3. Read workspace `AGENTS.md` — especially **Local dev tools (Nix)**, **Key hosts (vault & desk)**, and **Roadmaps and task lists** — then `{{MODULE_PATH}}/AGENTS.md` when it exists.
4. Do not add or update `ROADMAP.md`, `TASKS.md`, remaining-work checklists, or status novels in the module. Open work belongs in workspace `MASTER-ROADMAP.md` and umbrella GitHub Issues.
5. Do not edit `.cursor/skills/*/last-runs.json`. The parent records those files.
6. Do not push. Never amend. Never skip hooks.
7. On success, create one Conventional Commit scoped to this module only.

### Private Go modules (required before `go test` / `go mod tidy`)

MuxCore modules import `github.com/Muxcore-Media/*` but fetch from Forgejo. **Use SSH, not HTTPS** — HTTPS triggers GUI login prompts in Cursor.

Before any Go command that may download modules, export:

```bash
export GOPRIVATE='github.com/Muxcore-Media/*'
export GONOSUMDB='github.com/Muxcore-Media/*'
export GIT_TERMINAL_PROMPT=0
```

Confirm the host git config (run once per machine if missing):

```bash
git config --global url."ssh://forgejo@git.zem.systems:2222/muxcore/".insteadOf "https://github.com/Muxcore-Media/"
```

Do **not** set `insteadOf` to `https://git.zem.systems/...` on dev workstations. Umbrella checkouts already use `replace => ../…` for sibling modules — prefer those over `go mod tidy` when linting.

### Dev tools (nix-shell)

Dev machines have **Nix**. Do **not** assume `go`, `node`, `npm`, `golangci-lint`, or other tools are on PATH outside a shell. **Use `nix-shell` for every build, lint, format, and test command** you run.

Pre-warm `golangci-lint` once per machine before fan-out waves (`nix-shell -p go golangci-lint --run 'golangci-lint version'`). Parallel `nix-shell` downloads of the same package stall and time out.

1. **Module `shell.nix`** when present in `{{MODULE_PATH}}` — run all commands via `nix-shell --run '…'` or enter `nix-shell` first.
2. **Otherwise** `nix-shell -p <packages> --run '…'` from `{{MODULE_PATH}}` (see workspace `AGENTS.md` for examples).
3. Do not install system packages with apt/brew/curl when Nix can provide the tool.

Examples (adjust paths to `{{MODULE_PATH}}`):

```bash
export GOPRIVATE='github.com/Muxcore-Media/*' GONOSUMDB='github.com/Muxcore-Media/*' GIT_TERMINAL_PROMPT=0
nix-shell -p nodejs --run 'npm ci && npm run typecheck && npm test'
nix-shell -p go golangci-lint --run 'golangci-lint run ./...'
nix-shell -p go --run 'go test ./...'
```

### Stack tooling

Run these **inside `nix-shell`** as above.

| Module kind | Format | Lint / verify |
|-------------|--------|----------------|
| `media-ui-app`, `muxcore-docs` | Prettier on TS/TSX/CSS if configured | `npm run typecheck`, `npm test` when present |
| `admin-ui` | `gofmt -w` on Go; Prettier on CSS if present | `go test ./...`, `golangci-lint run ./...` when configured |
| Go service modules | `gofmt -w` | `go vet ./...`, `golangci-lint run ./...` (copy `core/.golangci.yml` only when a module has none) |
| `muxcore-ios` / `muxcore-android` / `media-tvos-app` | Platform formatters already in repo | Run the module's existing CI-equivalent checks only (usually via module `shell.nix`) |

Never replace an existing formatter or linter config that the module already ships.

### Infrastructure (when the task needs it)

Do not SSH to vault or desk unless the user asked for deploy, DNS, or infra work. When they did:

| Host | Use for |
|------|---------|
| **vault** (`ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95`) | MuxCore dev deployment — `muxcore-host.service`, `$MVP/bin/`, `./run-host.sh`, Forgejo CI |
| **desk** (`ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:93b2:af65:48bb`) | DNS (`flarectl`) and Cloudflare Tunnel (`cloudflared`) for `*.zem.systems` |

Full paths, deploy recipes, and `CF_API_TOKEN` setup: workspace `AGENTS.md` → **Key hosts (vault & desk)**.
