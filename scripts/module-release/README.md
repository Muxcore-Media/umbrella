# Module release helpers

Used for the roadmap T-M1-05 fan-out (TDD §3–4). Each script works on one
module checkout; run them from the umbrella root.

| Script | Does |
|--------|------|
| `modfix.sh <module> [mod@ver ...]` | Drops filesystem `replace`s and the obsolete `core/pkg/tenant` require, pins `core` v0.6.2 / nested core modules v0.6.0 and every other `Muxcore-Media` require to `@latest` (overrides win), then `go mod tidy`. |
| `verify.sh <module>` | `go build`, `go vet`, `go test -p 4 -race`, golangci-lint (0 issues), `gofmt -l`, no filesystem `replace`. Prints `VERIFY <module>: rc=N`. |
| `ship.sh <module> <subject> <body> <changelog-md> [version]` | Picks the next patch tag (or a higher unused `muxcore.json` version), updates `muxcore.json` and `CHANGELOG.md` (folding `[Unreleased]`), commits with `Refs:` (`REFS` env, default `T-M1-05`), pushes the default branch and tag, sets `LINT_BLOCKING=true`. |

Environment: set `MUXCORE_GO_ENV` to a file that puts the pinned Go toolchain
on `PATH` (and `GOWORK=off GOFLAGS=-mod=mod GOPRIVATE=github.com/Muxcore-Media/*`),
and `LINT` to the golangci-lint binary if it is not on `PATH`. On a 12 GB host,
keep test/lint parallelism at 4 or lower. Do not use `ship.sh` on repos whose
`muxcore.json` is not a module manifest (`_mvp`).
