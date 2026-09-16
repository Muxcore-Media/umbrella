# contracts-ai

MuxCore contract library (`contracts-ai`). Workspace deploy and SSH: [`AGENTS.md`](../../AGENTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `contracts-ai` |
| Capabilities | `contracts.ai` |
| Contracts | AI domain events + inference types |

## Agent rules

- This is a library, not a sidecar. Do not add a gRPC server or binary.
- Event type strings are stable; payload fields are additive only.
- Cross-module media events stay in `contracts-media`.
- Run `gofmt` and package tests before finishing.

## Build

```bash
cd contracts-ai
go test ./...
```

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
