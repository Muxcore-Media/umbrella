# contracts-media

Go **event contract library** — not a runnable sidecar. There is no module binary, listen port, or TLS config. Workspace context: [`AGENTS.md`](../../AGENTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | `contracts-media` |
| Type | contracts (Go package + catalog entry) |
| Import path | `github.com/Muxcore-Media/contracts-media/events` |
| Capability (catalog) | `contracts.media` |
| Published tag | `v0.1.0` |

`muxcore.json` `capabilities: ["contracts.media"]` identifies this repo in the umbrella catalog only — **do not deploy or `run-host.sh` start it**.

## Add-event checklist

When introducing a new mesh event:

1. Add `Event…` constant string in `events/events.go` (never rename existing type strings).
2. Add matching `…Payload` struct with JSON tags aligned to live publishers.
3. Register the constant in `All()` and `Known()`.
4. Extend `events/events_test.go` — freeze the string, JSON round-trip the payload, map constant → payload type.
5. Document the event in `README.md` (fields + publisher/consumer rows).
6. Note additive changes in `CHANGELOG.md`; breaking type renames are forbidden.

Annotate path/PII fields in struct comments (`file_path`, `original_path`, `destination_path`, `save_path`, `info_hash`).

Do not edit polluted workspace dumps (see [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) Appendix H).

## Build

```bash
cd contracts-media
nix-shell -p go --run 'go test ./...'
```

On a dev host with Go on PATH:

```bash
go test ./...
```

Do not add `AGENTS.md`, `ROADMAP.md`, or `TASKS.md` in the module repo; this file is the umbrella copy.
