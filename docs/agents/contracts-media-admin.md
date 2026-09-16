# contracts-media-admin

Protobuf/gRPC **contract library** — not a runnable sidecar. There is no module binary, listen port, or TLS config. Workspace context: [`AGENTS.md`](../../AGENTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `contracts-media-admin` |
| Type | contracts (proto + generated Go) |
| Proto | `proto/muxcore/media/admin/v1/media_admin.proto` |
| Go module | `github.com/Muxcore-Media/contracts-media-admin` |
| Generated package | `gen/muxcore/media/admin/v1` (`mediaadminv1`) |
| Capability (catalog) | `contracts.media-admin` (see `muxcore.json`) |
| Published tag | `v0.1.0` |

`go_package` in the proto points at `gen/muxcore/media/admin/v1`; **do not** use `paths=source_relative` in `make proto`.

## Agent rules

- Edit the `.proto` first, then regenerate with `make proto` and commit both source and `gen/`.
- Breaking RPC or field renames require a new major contract version and coordinated updates in implementers (`media-movies`, `media-tvshows`, `media-music`, …) and callers (`admin-ui`, `muxcorectl-cli`).
- Per-type `Unimplemented` expectations live in `COMPATIBILITY.md`.
- Do not edit polluted workspace dumps (see [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) Appendix H).

## Build

```bash
cd contracts-media-admin
nix-shell -p go protobuf --run 'make proto && go test ./...'
```

On a dev host with Go and protoc plugins on PATH:

```bash
make proto
go test ./...
```

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
