# App fanout (MuxCore)

Shared planner for quality orchestrators. **Not** a user-facing skill.

MuxCore is a multi-repo workspace. Each enrolled module is its own git checkout. Planning uses `module-trees` lists in [`scripts/app-fanout.config.json`](../../../scripts/app-fanout.config.json).

Regenerate lists when modules are added:

```bash
node scripts/generate-fanout-config.mjs
```

## Commands

```
node scripts/app-fanout.mjs list
node scripts/app-fanout.mjs plan --skill <id> [--force] [--app <id> ...] [--wave <n>]
node scripts/app-fanout.mjs record --skill <id> [--commit <sha>] <id> [<id> ...]
node scripts/app-fanout.mjs close --here
node scripts/app-fanout.mjs close --skill <id> <id> [<id> ...]
```

Each plan row includes `modulePath`, `baseBranch`, and `head` for that module's repo. Child prompts use `{{MODULE_PATH}}`, `{{BASE_BRANCH}}`, and `{{HEAD}}`.

## Assets

| Path | Purpose |
| ---- | ------- |
| [templates/muxcore-agent-setup.md](templates/muxcore-agent-setup.md) | Shared child setup, **nix-shell** dev tools, stack tooling |
| [assets/.prettierrc](assets/.prettierrc) | Copy only when a JS module has no formatter |
| [assets/eslint.config.js](assets/eslint.config.js) | Copy only when a JS module has no ESLint |

Do not replace formatters or linters a module already ships (`core/.golangci.yml`, `media-ui-app` Vitest/TS, etc.).
