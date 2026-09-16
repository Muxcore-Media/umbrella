# branding

MuxCore shared branding library (`branding`).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `branding` |
| Type | Importable Go package + embedded assets (not a sidecar service) |
| Capabilities | `branding.assets`, `branding.theme` |
| Contracts | none declared |

## Agent rules

- This module is a **library**, not a long-running sidecar. Do not add gRPC/HTTP servers unless explicitly scoped.
- Keep brand constants and theme tokens in sync between `pkg/branding` and `tokens/theme.json`.
- Placeholder SVGs are fine; do not ship full platform art packs in this repo.
- Match existing Go patterns; run `gofmt` and `go test ./...` before finishing.
- Asset path changes require a semver bump in `muxcore.json`.

## Package layout

| Path | Purpose |
|------|---------|
| `pkg/branding/` | Importable Go API (`Brand`, `Theme`, `Assets`) |
| `assets/` | Embedded logo/mark files + naming contract |
| `tokens/theme.json` | Language-neutral theme tokens for web/native clients |

## Build

```bash
cd branding
go test ./...
make test
```

## Consumers

```go
import "github.com/Muxcore-Media/branding/pkg/branding"

brand := branding.DefaultBrand()
theme := branding.DefaultTheme()
logo, _ := branding.DefaultAssets().Read(branding.AssetLogo)
```

Tracked by https://github.com/Muxcore-Media/umbrella/issues/23

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
