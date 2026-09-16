# circuitbreaker-simple

MuxCore sidecar module (`circuitbreaker-simple`). Workspace deploy and SSH: [`AGENTS.md`](../../AGENTS.md). Default ports: [`_mvp/PORTS.md`](../../_mvp/PORTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `circuitbreaker-simple` |
| Capabilities | `circuitbreaker`, `circuitbreaker.simple`, `settings` |
| Contracts | `CircuitBreaker` (`core/pkg/contracts`) |

## Agent rules

- Modules run as gRPC sidecars; capabilities are the security boundary.
- TLS required in production (`MUXCORE_INSECURE_DISABLE_TLS` is dev-only).
- Match existing Go patterns; run `gofmt` and package tests before finishing.
- Cross-module events: prefer `github.com/Muxcore-Media/contracts-media/events` over deprecated `core/pkg/contracts` aliases.
- Do not edit polluted workspace dumps (see [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) Appendix H).

## Build

```bash
cd circuitbreaker-simple
nix-shell -p go --run 'go test -race ./...'
```

After proto changes in `core/proto/muxcore/circuitbreaker/v1/`, regenerate with `make proto` in `core/`.

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
