# Child agent prompt (frontend lint)

Parent fills every `{{…}}` field from `node scripts/app-fanout.mjs plan --skill frontend-lint` JSON.

```
You are the lint agent for one MuxCore frontend module.

Skill: frontend-lint
Module id: {{ID}}
Module path: {{MODULE_PATH}}
Agent name / branch: {{WORKTREE_BRANCH}}
Base branch: {{BASE_BRANCH}}
Baseline SHA: {{HEAD}}
Last recorded commit: {{LAST_COMMIT}}
Why this run: {{REASON}}
Incomplete files from last run: {{INCOMPLETE_FILES}}
Changed files since last recorded commit (may be truncated):
{{CHANGED_FILES}}

Read `.cursor/skills/app-fanout/templates/muxcore-agent-setup.md` and apply it with `{{MODULE_PATH}}` as the module directory.

## What to work on

- `never-run`, `unknown-last-commit`, or `force`: lint the whole module.
- `git-diff`: lint changed files and incomplete files from last run; still run the module's full lint/typecheck command.
- `incomplete-files`: continue the listed files first, then any new findings until clean or the cap.

## Implement

1. Detect stack in `{{MODULE_PATH}}`.
2. **media-ui-app / muxcore-docs**: run `npm run typecheck` and `npm test` when scripts exist; add ESLint only if missing.
3. **admin-ui**: `go test ./...`; `golangci-lint run ./...` when configured; `gofmt -l` must be clean after fixes.
4. **mobile modules** (`muxcore-ios`, `muxcore-android`, `media-tvos-app`): run the checks those repos already use in CI — do not invent new tooling.
5. Fix findings until exit 0, **or** until **40** leftover diagnostics. List incomplete files if capped.
6. Tool crash or a fix that breaks the build = failed run.

## Commit (on success)

1. Read and follow `.cursor/skills/conventional-commit/SKILL.md`.
2. Stage only files under `{{MODULE_PATH}}`. Never `git add -A`. Never stage `.cursor/skills/frontend-lint/last-runs.json`.
3. Message like `fix({{ID}}): satisfy lint` or `fix({{ID}}): clear typecheck findings`.

## Close this worktree

    node scripts/app-fanout.mjs close --here

Run from the worktree directory inside `{{MODULE_PATH}}`.

## Return to the parent

- Worktree branch (`{{WORKTREE_BRANCH}}`)
- Checks run, incomplete files, remaining diagnostic count
- Whether this module succeeded (yes/no)
- Commit hash and message (empty if none)
```
