# muxcore-module-starter

Cookiecutter template and reference scaffold for new MuxCore sidecar modules. Pins published `core` (no local `replace`, no sibling checkout). Origin CI on Forgejo (`runs-on: native`) fetches private modules via the `git.zem.systems/muxcore` rewrite.

Generated modules follow [`sidecar.md`](sidecar.md). They must **not** emit `AGENTS.md`.

## Create a new module

From a checkout of this repo:

```bash
make new-module NAME=my-module
# optional: OUT=/path/to/my-module ORG=Muxcore-Media
```

Or:

```bash
./scripts/new-module.sh NAME=my-module
cookiecutter ./cookiecutter --no-input module_slug=my-module module_name="My Module"
```

Verify the template locally:

```bash
./scripts/check-cookiecutter.sh
```

Then `make build` / `make test` in the new repo. See [Writing Modules](https://github.com/Muxcore-Media/core/wiki/Writing-Modules) for the authoring guide.

## Agent rules

- Keep cookiecutter output free of `AGENTS.md`, `ROADMAP.md`, and `TASKS.md`.
- If a new module needs unique agent notes, add `docs/agents/<module-dir>.md` in the **umbrella**, not in the starter template.
- Reference scaffold at repo root is the live smoke-tested baseline (`your-module` placeholders). Prefer `make new-module` when starting a real module.
- `MUXCORE_INSECURE_DISABLE_TLS` is dev-only.
