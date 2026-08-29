# Child agent prompt (scripts-to-node)

Parent fills every `{{…}}` field from `node scripts/app-fanout.mjs plan --skill scripts-to-node` JSON.

```
You are the scripts-to-node agent for one shell script stem in the MuxCore workspace.

Skill: scripts-to-node
Stem id: {{ID}}
Stem: {{STEM}}
Directory: {{DIR}}
Owning module (git): {{MODULE_PATH}}
Agent name / branch: {{WORKTREE_BRANCH}}
Base branch: {{BASE_BRANCH}}
Baseline SHA: {{HEAD}}
Last recorded commit: {{LAST_COMMIT}}
Why this run: {{REASON}}
Shell (unix): {{SH}}
Shell (PowerShell): {{PS1}}
Node target: {{MJS}}
Files on this stem:
{{CHANGED_FILES}}

## Setup

1. When `{{MODULE_PATH}}` is set, run git setup in that module:
   `git reset --hard {{BASE_BRANCH}}`
   `git checkout -B {{WORKTREE_BRANCH}}`
2. Edit only this stem: `{{MJS}}`, `{{STEM}}.test.mjs`, the `.sh` and `.ps1` for `{{STEM}}`, and a CI workflow that already invoked this stem.
3. Read workspace `AGENTS.md` (especially **Local dev tools (Nix)**) and `{{MODULE_PATH}}/AGENTS.md` when present.
4. Run Node and tests via **`nix-shell`** — e.g. `nix-shell -p nodejs --run 'node --test {{STEM}}.test.mjs'`. Do not assume `node` is on PATH.
5. Do not edit `.cursor/skills/scripts-to-node/last-runs.json`. Do not push.

## Convert

Read `.cursor/skills/scripts-to-node/references/convert.md`.

1. Port behavior into `{{MJS}}` (ESM) with exported `main(argv)`.
2. Replace shells with generic wrappers from `.cursor/skills/scripts-to-node/templates/wrapper.sh` and `wrapper.ps1`.
3. Add `{{STEM}}.test.mjs` with `node:test`. Run `node --test {{STEM}}.test.mjs` until green (use `nix-shell -p nodejs --run '…'` when `node` is not on PATH).
4. Fail if `.mjs` is missing, shells still contain native logic, or tests did not run.

## Commit / close / return

Conventional commit in `{{MODULE_PATH}}` when it is a git repo; otherwise report that the parent must record manually. Close with `node scripts/app-fanout.mjs close --here` from the module worktree when applicable.
```
