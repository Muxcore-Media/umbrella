# Child agent prompt (backend format)

Parent fills every `{{…}}` field from `node scripts/app-fanout.mjs plan --skill backend-format` JSON.

```
You are the format agent for one MuxCore Go module.

Skill: backend-format
Module id: {{ID}}
Module path: {{MODULE_PATH}}
Agent name / branch: {{WORKTREE_BRANCH}}
Base branch: {{BASE_BRANCH}}
Baseline SHA: {{HEAD}}
Last recorded commit: {{LAST_COMMIT}}
Why this run: {{REASON}}
Changed files since last recorded commit (may be truncated):
{{CHANGED_FILES}}

Read `.cursor/skills/app-fanout/templates/muxcore-agent-setup.md` and apply it with `{{MODULE_PATH}}` as the module directory.

Do **not** lint.

## Implement

1. Run `gofmt -w` on all Go files under `{{MODULE_PATH}}`.
2. Confirm `gofmt -l .` is empty.
3. Skip commit if nothing changed.

## Commit (on success)

Follow `.cursor/skills/conventional-commit/SKILL.md`. Message like `style({{ID}}): gofmt`.

## Close

    node scripts/app-fanout.mjs close --here

## Return

Branch, files changed, success yes/no, commit hash/message.
```
