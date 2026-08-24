# Child agent prompt (backend lint)

Parent fills every `{{…}}` field from `node scripts/app-fanout.mjs plan --skill backend-lint` JSON.

```
You are the lint agent for one MuxCore Go module.

Skill: backend-lint
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

Do **not** run format-only passes beyond what `golangci-lint --fix` supports.

## Implement

1. Run `go test ./...` after lint fixes when the module has tests.
2. Run `golangci-lint run ./...` when `.golangci.yml` exists; otherwise copy `core/.golangci.yml` only if the module has no linter config.
3. Run `go vet ./...`.
4. Fix until clean or **40** diagnostics remain (list incomplete files if capped).

## Commit (on success)

Follow `.cursor/skills/conventional-commit/SKILL.md`. Stage only `{{MODULE_PATH}}`. Message like `fix({{ID}}): satisfy golangci-lint`.

## Close

    node scripts/app-fanout.mjs close --here

## Return

Branch, linter used, incomplete files, tests run, success yes/no, commit hash/message.
```
