#!/usr/bin/env bash
# Release helpers for roadmap T-M1-05-style module releases (see README.md).
# usage: modfix.sh <module-dir> [mod@ver override ...]
# Drops fs replaces + core/pkg/tenant, pins core v0.6.2 / nested v0.6.0,
# pins every other Muxcore-Media require to @latest (tag or default-branch pseudo), tidy.
set -euo pipefail
# Optional toolchain env (e.g. Go 1.26.6 on PATH): MUXCORE_GO_ENV=/path/to/env.sh
# shellcheck source=/dev/null
if [[ -n "${MUXCORE_GO_ENV:-}" ]]; then source "$MUXCORE_GO_ENV"; fi
LINT="${LINT:-golangci-lint}"
cd "$1"; shift
declare -A OVR; for o in "$@"; do OVR[${o%@*}]=${o#*@}; done
for r in $(go mod edit -json | jq -r '.Replace[]?.Old.Path'); do go mod edit -dropreplace="$r"; done
go mod edit -droprequire=github.com/Muxcore-Media/core/pkg/tenant 2>/dev/null || true
self=$(go mod edit -json | jq -r .Module.Path)
for p in $(go mod edit -json | jq -r '.Require[]?.Path' | grep '^github.com/Muxcore-Media/' || true); do
  [[ "$p" == "$self" ]] && continue
  case "$p" in
    github.com/Muxcore-Media/core) v=v0.6.2;;
    github.com/Muxcore-Media/core/*) v=v0.6.0;;
    *) if [[ -n "${OVR[$p]:-}" ]]; then v=${OVR[$p]}; else v=$(go list -m -json "$p@latest" | jq -r .Version); fi;;
  esac
  go mod edit -require="$p@$v"; echo "pin $p@$v"
done
for p in "${!OVR[@]}"; do go mod edit -require="$p@${OVR[$p]}"; done
go mod tidy
