# contracts-metadata

Protobuf/gRPC **contract** repository — not a runnable sidecar. There is no module binary, listen port, or TLS config here. Workspace context: [`AGENTS.md`](../../AGENTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `contracts-metadata` |
| Type | contracts (proto + generated Go stubs) |
| Capability (catalog) | `contracts.metadata` |
| Interface name | `MetadataProvider` (gRPC service `MetadataService`) |
| Published tag | `v0.2.0` |

Implementers (`metadata-tmdb`, future TVDB/MusicBrainz adapters) advertise capability **`metadata`** and serve **`MetadataService`**. `contracts-reconciler` maps the logical name **`MetadataProvider`** to this repo.

## Workflow

1. Edit `proto/muxcore/metadata/v1/metadata.proto`.
2. Regenerate stubs: `make proto` (pins `protoc-gen-go@v1.36.6`, `protoc-gen-go-grpc@v1.5.1`).
3. Commit proto **and** generated `muxcore/metadata/v1/*.go` together.
4. Run tests: `go test ./...` (includes `contract_test.go` stability checks).
5. Bump `muxcore.json` / `CHANGELOG.md` / tag on breaking or release-worthy changes.

Do not edit polluted workspace dumps (see [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) Appendix H).

## Build

```bash
cd contracts-metadata
nix-shell -p go protobuf --run 'make proto && go test ./...'
```

On a dev host with Go and protoc on PATH:

```bash
make proto
go test ./...
```

`make clean` clears the Go build cache only — it does **not** remove published stubs.

## Compatibility checklist (proto edits)

1. Use provider-neutral **`id`** + **`ExternalIds`** on requests — not TMDB-only field names.
2. Additive field changes only on **v1** unless reserving and re-tagging (see `in_production`).
3. Extend `muxcore/metadata/v1/contract_test.go` when adding RPCs or freezing new field numbers.
4. Document caller-visible behavior in `COMPATIBILITY.md` and README.
5. Breaking changes require a new major package and coordinated consumer bumps.

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
