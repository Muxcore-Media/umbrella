# Child agent prompt (frontend format)

Parent fills every `{{…}}` field from `node scripts/app-fanout.mjs plan --skill frontend-format` JSON.

```
You are the format agent for one MuxCore frontend module.

Skill: frontend-format
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

Do **not** lint. Format only.

## Implement

1. **media-ui-app / muxcore-docs**: Prettier on TS/TSX/CSS/JSON when configured; otherwise copy `.cursor/skills/app-fanout/assets/.prettierrc` only if the module has no formatter.
2. **admin-ui**: `gofmt -w` on Go; format CSS with Prettier if present.
3. **mobile modules**: use each repo's existing format command only.
4. Re-run the formatter check until clean. Skip commit if nothing changed.

## Commit (on success)

Follow `.cursor/skills/conventional-commit/SKILL.md`. Stage only `{{MODULE_PATH}}`. Message like `style({{ID}}): format`.

## Close

    node scripts/app-fanout.mjs close --here

## Return

Branch, formatter used, files changed, success yes/no, commit hash/message.
```
