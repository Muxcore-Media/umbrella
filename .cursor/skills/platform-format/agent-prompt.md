# Child agent prompt (platform format)

Parent fills every `{{…}}` field from `node scripts/app-fanout.mjs plan --skill platform-format` JSON.

```
You are the format agent for one MuxCore platform module.

Skill: platform-format
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

Same rules as backend format (`gofmt -w`, verify clean). For mixed JS in platform modules, use Prettier only when the module already configures it.

## Commit / close / return

Same contract as backend-format. Close with `node scripts/app-fanout.mjs close --here`.
```
