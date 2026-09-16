# contracts-notification

Protobuf/gRPC **contract** repository — not a runnable sidecar. There is no module binary, listen port, or TLS config here. Workspace context: [`AGENTS.md`](../../AGENTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `contracts-notification` |
| Type | contracts (proto + generated Go stubs) |
| Capability (catalog) | `contracts.notification` |
| Interface name | `NotificationProvider` (gRPC service `NotificationService`) |
| Published tag | `v0.1.1` |

Implementers (`notification-default`, `notification-apprise`) advertise capability **`notification`** and serve **`NotificationService`**. `contracts-reconciler` maps the logical name **`NotificationProvider`** to this repo.

## Workflow

1. Edit `proto/muxcore/notification/v1/notification.proto`.
2. Regenerate stubs: `make proto` (pins `protoc-gen-go@v1.36.11`, `protoc-gen-go-grpc@v1.5.1`).
3. Commit proto **and** generated `muxcore/notification/v1/*.go` together.
4. Run tests: `go test ./...` (includes `compat_test.go` stability checks).
5. Bump `muxcore.json` / `CHANGELOG.md` / tag on breaking or release-worthy changes.

Do not edit polluted workspace dumps (see [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) Appendix H).

## Build

```bash
cd contracts-notification
nix-shell -p go protobuf --run 'make proto && go test ./...'
```

On a dev host with Go and protoc on PATH:

```bash
make proto
go test ./...
```

`make clean` clears the Go build cache only — it does **not** remove published stubs.

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
