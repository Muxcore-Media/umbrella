## MuxCore workspace setup

MuxCore is a **multi-repo workspace**: `~/Projects/MuxCore` is not one git root. Each module (`media-ui-app`, `core`, `admin-ui`, …) is its own Forgejo repo with its own branch and history.

1. Work inside **`{{MODULE_PATH}}`** (the module directory from the plan row). Run git only in that module unless the step says otherwise.
2. Reset this module onto `{{BASE_BRANCH}}` and name the branch before edits:
   `git reset --hard {{BASE_BRANCH}}`
   `git checkout -B {{WORKTREE_BRANCH}}`
   Create the worktree under `{{MODULE_PATH}}/.worktrees/{{WORKTREE_BRANCH}}` when the orchestrator placed you there.
3. Read workspace `AGENTS.md`, then `{{MODULE_PATH}}/AGENTS.md` when it exists.
4. Do not edit `.cursor/skills/*/last-runs.json`. The parent records those files.
5. Do not push. Never amend. Never skip hooks.
6. On success, create one Conventional Commit scoped to this module only.

### Stack tooling

| Module kind | Format | Lint / verify |
|-------------|--------|----------------|
| `media-ui-app`, `muxcore-docs` | Prettier on TS/TSX/CSS if configured | `npm run typecheck`, `npm test` when present |
| `admin-ui` | `gofmt -w` on Go; Prettier on CSS if present | `go test ./...`, `golangci-lint run ./...` when configured |
| Go service modules | `gofmt -w` | `go vet ./...`, `golangci-lint run ./...` (copy `core/.golangci.yml` only when a module has none) |
| `muxcore-ios` / `muxcore-android` / `media-tvos-app` | Platform formatters already in repo | Run the module's existing CI-equivalent checks only |

Never replace an existing formatter or linter config that the module already ships.
