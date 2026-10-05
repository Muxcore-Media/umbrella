# ADR-0012: Reproducible module builds for spool checksums

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

Spool tags pin each required module as `repo@version` plus a `sha256` checksum, and core refuses to run a required module whose built binary does not match it (FR-EXT-002, `Manager.VerifyChecksum`). Core produces that binary by cloning the tag and running a plain `go build ./cmd/module/` in a temp directory. `_mvp/scripts/populate-spool-checksums.sh` computes the expected checksum differently: it builds the **local workspace checkout** (not the tag), injects filesystem `replace` directives into the module's `go.mod` (contrary to [ADR-0011](0011-go-workspace-and-published-versions.md)), and also builds without `-trimpath`.

Without `-trimpath`, `-buildvcs=false` and a fixed toolchain, the binary embeds the build directory, VCS stamps and whichever `go` is on the host's `PATH`. The pinned checksums are therefore host-specific, and a correct install can be refused.

## Decision

1. A module tag's **canonical build** is:
   `CGO_ENABLED=0 GOFLAGS=-mod=readonly GOTOOLCHAIN=go<V> go build -trimpath -buildvcs=false -ldflags=-buildid= -o <out> ./cmd/module/`
   run in a clean clone of the tag. `<V>` is the version in the module's `go.mod` `go` line, with `.0` appended when that line has no patch number. The build uses only the `go.sum` committed at the tag, with no `replace` added and no `go.mod` edits. `GOOS`/`GOARCH` are those of the host that will run the module; spool checksums are recorded for `linux/amd64`.
2. Core's spool resolver (`internal/module/mgr`) uses exactly the canonical build. The populate script computes checksums with the same command, in a fresh clone of the tag, and never touches workspace checkouts.
3. Contract reconciliation (`reconcileContracts`) must not change the build of a curated tag. A curated tag's `go.mod` must already require the contract versions its `muxcore.json` declares. The populate script fails when it does not.

## Alternatives

- **Checksum the source tree instead of the binary:** this needs a new spool schema and a core change to verification semantics. It may follow later, but not before v1.0.
- **Checksums of GitHub Release artifacts:** blocked for most modules until per-repo release CI is green (B-1). It also adds a second distribution path, which ADR-0007 avoids.
- **Status quo:** checksums cannot be reproduced on another host.

## Consequences

- Checksums can be reproduced on any host with network access to GitHub and the Go toolchain download.
- A core release is required (spool builds change byte-for-byte), and all spool checksums must be recomputed once.
- Bumping a module's `go` line changes its checksum, which is intended.
