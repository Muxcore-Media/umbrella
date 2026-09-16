# Sidecar modules

Shared agent rules for MuxCore gRPC sidecar repos (Go modules with `muxcore.json`). Workspace deploy and SSH: [`AGENTS.md`](../../AGENTS.md). Default ports: [`_mvp/PORTS.md`](../../_mvp/PORTS.md).

Identity is **not** duplicated here. Read the module directory's `muxcore.json` for `name`, `capabilities`, and `contracts`.

Unique surfaces (`_mvp`, consumer UIs, installer, docs, CLI) have their own files in [`README.md`](README.md).

## Agent rules

- Modules run as gRPC sidecars; capabilities are the security boundary.
- TLS required in production (`MUXCORE_INSECURE_DISABLE_TLS` is dev-only).
- Match existing Go patterns; run `gofmt` and package tests before finishing.
- Cross-module events: prefer `github.com/Muxcore-Media/contracts-media/events` over deprecated `core/pkg/contracts` aliases.
- Do not edit polluted workspace dumps (see [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) Appendix H).
- Roadmaps, task lists, remaining-work checklists, and agent notes live in this umbrella. Do not add `ROADMAP.md`, `TASKS.md`, or `AGENTS.md` in the module repo.

## Build

From the workspace root, in the module directory:

```bash
cd <module-dir>
nix-shell -p go --run 'go test ./...'
```

Private Go modules resolve from Forgejo — see workspace `AGENTS.md` → **Local dev tools (Nix)** and **Private Go modules**.

## Vault

If the sidecar is already on the soak stack, redeploy with `_mvp/scripts/deploy-module-to-vault.sh <module>` after a code change. Origin-pinned acquisition modules (`media-scanner`, `media-automation`, `downloader-native-torrent`) use `_mvp/scripts/install-origin-module.sh` instead of ad-hoc binary copies.
