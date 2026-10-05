#!/usr/bin/env bash
# usage: verify.sh <module-dir>
set -uo pipefail
# Optional toolchain env (e.g. Go 1.26.6 on PATH): MUXCORE_GO_ENV=/path/to/env.sh
# shellcheck source=/dev/null
if [[ -n "${MUXCORE_GO_ENV:-}" ]]; then source "$MUXCORE_GO_ENV"; fi
LINT="${LINT:-golangci-lint}"
cd "$1" || exit 2
rc=0
go build ./... || rc=1
go vet ./... || rc=1
go test -p 4 -race -count=1 ./... 2>&1 | grep -vE '^(ok|\?)\s' ; [[ ${PIPESTATUS[0]} -eq 0 ]] || rc=1
$LINT run --allow-parallel-runners --concurrency 4 ./... 2>&1 | tail -15 | grep -q '^0 issues' || { $LINT run --allow-parallel-runners --concurrency 4 ./... 2>&1 | tail -30; rc=1; }
! gofmt -l . | grep -qv '^vendor/' || { echo "gofmt:"; gofmt -l .; rc=1; }
grep -nE '=>\s*(\.|/)' go.mod && rc=1
echo "VERIFY $(basename "$PWD"): rc=$rc"; exit $rc
