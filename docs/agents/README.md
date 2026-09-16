# Module agent notes

Build, test, and deploy instructions for MuxCore modules live **in this umbrella directory**, not as `AGENTS.md` inside each module repo.

| Layer | Where |
|-------|--------|
| Homelab / vault / DNS / Nix | workspace [`AGENTS.md`](../../AGENTS.md) |
| Shared gRPC sidecar rules | [`sidecar.md`](sidecar.md) |
| Module identity (name, capabilities, contracts) | that module's `muxcore.json` |
| Unique module notes | `docs/agents/<module-dir>.md` (tables below) |
| Open work | [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) and umbrella GitHub Issues |

Do **not** add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` to a module repo (including cookiecutter output). `scripts/generate-module-agents.mjs` fails if a leftover `AGENTS.md` reappears.

When a module needs notes that `sidecar.md` does not cover, add `docs/agents/<module-dir>.md` here and link it below.

## Platforms and UIs

| Module | File |
|--------|------|
| `_mvp` (household stack, `run-host.sh`, smoke, vault helpers) | [`_mvp.md`](_mvp.md) |
| `media-ui-app` (consumer SPA design system + UI DoD) | [`media-ui-app.md`](media-ui-app.md) |
| `admin-ui` | [`sidecar.md`](sidecar.md); Tailwind CSS is `go:embed` — see workspace `AGENTS.md` deploy table |
| `muxcore-docs` (wiki → static site) | [`muxcore-docs.md`](muxcore-docs.md) |
| `muxcorectl-cli` | [`muxcorectl-cli.md`](muxcorectl-cli.md) |
| `muxcore-ios` | [`muxcore-ios.md`](muxcore-ios.md) |
| `media-tvos-app` | [`media-tvos-app.md`](media-tvos-app.md) |
| `muxcore-installer` (`muxcore-setup` TUI) | [`muxcore-installer.md`](muxcore-installer.md) |
| `muxcore-module-starter` (cookiecutter) | [`muxcore-module-starter.md`](muxcore-module-starter.md) |
| `branding` (Go theme/asset library) | [`branding.md`](branding.md) |

## Sidecar extras

These follow [`sidecar.md`](sidecar.md) and add env, ports, or build tags:

| Module | File |
|--------|------|
| `userdata-local` | [`userdata-local.md`](userdata-local.md) |
| `media-dlna` | [`media-dlna.md`](media-dlna.md) |
| `storage-ceph` | [`storage-ceph.md`](storage-ceph.md) |
| `logging-file` | [`logging-file.md`](logging-file.md) |
| `circuitbreaker-simple` | [`circuitbreaker-simple.md`](circuitbreaker-simple.md) |

## Contract libraries

Not runnable sidecars. Proto/event repos share “edit source, regenerate, commit stubs, additive v1 only.”

| Module | File |
|--------|------|
| `contracts-media` (mesh events) | [`contracts-media.md`](contracts-media.md) |
| `contracts-playback` | [`contracts-playback.md`](contracts-playback.md) |
| `contracts-ai` | [`contracts-ai.md`](contracts-ai.md) |
| `contracts-scanner` | [`contracts-scanner.md`](contracts-scanner.md) |
| `contracts-indexer` | [`contracts-indexer.md`](contracts-indexer.md) |
| `contracts-metadata` | [`contracts-metadata.md`](contracts-metadata.md) |
| `contracts-notification` | [`contracts-notification.md`](contracts-notification.md) |
| `contracts-media-admin` | [`contracts-media-admin.md`](contracts-media-admin.md) |
| `contracts-reconciler` | [`contracts-reconciler.md`](contracts-reconciler.md) |

Other `contracts-*` repos follow the same proto/event rules as the closest file above plus their `README.md`.

Every other sidecar uses [`sidecar.md`](sidecar.md) plus its `muxcore.json`. Skip polluted dumps listed in `MASTER-ROADMAP.md` Appendix H (`media-ui`, `media-jellyfin`, `cache-memory`, `custom-scripts`, `muxcorectl`).
