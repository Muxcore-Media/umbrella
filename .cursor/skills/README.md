# MuxCore agent skills shelf

Cursor loads project skills from `.cursor/skills/`. MuxCore is an **umbrella repo**: one git root pins every module (GitHub `Muxcore-Media/*`) as a **submodule** (gitlink). Clone with:

```bash
git clone --recurse-submodules https://github.com/Muxcore-Media/umbrella.git MuxCore
```

See [`README-UMBRELLA.md`](../../README-UMBRELLA.md) for bump/push workflow.

Start with workspace [`AGENTS.md`](../../AGENTS.md) and the canonical plan [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md).

Fan-out child agents read [app-fanout/templates/muxcore-agent-setup.md](app-fanout/templates/muxcore-agent-setup.md); use the toolchain the machine provides (Nix shell, mise, or system Go/Node).

## Fan-out orchestrators

These use [`scripts/app-fanout.mjs`](../../scripts/app-fanout.mjs) with per-module git state:

| Skill | Scope |
| ----- | ----- |
| [frontend-page-accessibility](frontend-page-accessibility/) | WCAG on `media-ui-app`, `admin-ui`, `muxcore-docs`, mobile UI repos |
| [frontend-format](frontend-format/) / [frontend-lint](frontend-lint/) | Format/lint frontend modules |
| [backend-format](backend-format/) / [backend-lint](backend-lint/) | `gofmt` / `golangci-lint` on Go service modules |
| [platform-format](platform-format/) / [platform-lint](platform-lint/) | `core`, `_mvp`, `muxcorectl-cli`, installer, operator, … |
| [scripts-to-node](scripts-to-node/) | Convert `_mvp/scripts`, installer, and workspace shell scripts to Node |
| [platform-quality](platform-quality/) | User-invoked umbrella: a11y → convert → lint → format |

Regenerate module lists after adding repos:

```bash
node scripts/generate-fanout-config.mjs
```

Plan one module:

```bash
node scripts/app-fanout.mjs plan --skill frontend-lint --app media-ui-app
```

## General skills

| Skill | When |
| ----- | ---- |
| [tdd](tdd/) | Test-first changes at module boundaries |
| [teach](teach/) | Private multi-session teaching workspace |
| [grill-me](grill-me/) / [grill-with-docs](grill-with-docs/) | Stress-test a design |
| [page-accessibility](page-accessibility/) | WCAG on one page or route subtree |
| [responsive-frontend](responsive-frontend/) | Visual changes in `media-ui-app` / `admin-ui` |
| [privacy-by-design](privacy-by-design/) | Privacy + retention on user/tracking data |
| [conventional-commit](conventional-commit/) | Commit only this chat's paths |
| [cloudflare](cloudflare/) | Cloudflare edge/docs work; **DNS + cloudflared on desk**, grey-cloud `*.zem.systems` ingress in nix-production |
| [research](research/) / [research-summarizer](research-summarizer/) | Primary-source notes and briefs |

## Retired on this shelf

Mike Pattyn–specific skills were removed (portfolio scaffold, AWS draw.io CDK, Angular migration, S3 deploy workflows, umbrella-teach). They do not match the MuxCore deployment model.

## Parent orchestrator notes

- Each plan row includes `modulePath`, `baseBranch`, and `head` for submodule repos.
- Merge happens **inside each submodule**; bump the gitlink in the umbrella with `git add <module> && git commit`.
- `last-runs.json` is committed at the **umbrella root** when it changes.
