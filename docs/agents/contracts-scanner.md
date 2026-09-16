# contracts-scanner

Protobuf/gRPC contract library — **not** a runnable sidecar. Workspace context: [`AGENTS.md`](../../AGENTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `contracts-scanner` |
| Go module | `github.com/Muxcore-Media/contracts-scanner` |
| Generated import | `github.com/Muxcore-Media/contracts-scanner/muxcore/scanner/v1` |
| Proto source | `proto/muxcore/scanner/v1/scanner.proto` |
| Catalog capability | `contracts.scanner` (`muxcore.json`) |
| Implementer capability | `scanner` (mesh peers, callers) |

## Agent rules

- **Proto changes are the product.** Edit `.proto` first, then regenerate stubs.
- Run `make proto` after any `.proto` edit; commit generated `muxcore/scanner/v1/*.go` with the proto change.
- CI (`proto-check` in `.forgejo/workflows/ci.yml`) fails if stubs drift.
- Additive field changes only on **v1** — new fields get new tag numbers; never renumber or change wire types.
- Breaking changes require a new major package (`muxcore/scanner/v2`) and coordinated consumer bumps.
- Extend `muxcore/scanner/v1/contract_test.go` when adding RPCs or freezing new field numbers.
- Document compatibility rules in `COMPATIBILITY.md` and proto comments (media types, dual roots, import status).
- Do not edit polluted workspace dumps (see [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) Appendix H).

## Regenerate protos

Use `nix-shell` when protoc/plugins are not on PATH:

```bash
cd contracts-scanner
nix-shell -p protobuf go --run 'go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.36.6 && go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.5.1 && export PATH="$(go env GOPATH)/bin:$PATH" && make proto'
```

## Build / test

```bash
cd contracts-scanner
nix-shell -p go --run 'go test ./...'
```

`make clean` clears Go build/test caches only — it does **not** remove committed stubs under `muxcore/scanner/v1/`.

## Compatibility checklist (proto edits)

1. Media types documented (`MediaType` enum + string tokens including `music`).
2. Watch dirs: `library_path`, `tv_library_path`, `music_library_path`.
3. `ImportPathResponse.results` for per-file outcomes; counts remain as summary.
4. Failed imports: `ImportedFile.error_message`, `ListImportedRequest.status`.
5. Update `CHANGELOG.md`, `COMPATIBILITY.md`, and README when behavior is caller-visible.

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
