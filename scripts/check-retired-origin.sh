#!/usr/bin/env bash
# Fail if active content references the retired self-hosted origin (ADR-0002,
# SRS NFR-MNT-006). Scans the umbrella and every checked-out active submodule;
# skips archived/historical submodules and docs/history + docs/adr.
#   usage: scripts/check-retired-origin.sh [path...]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MUXCORE_ROOT="$ROOT"
# shellcheck source=module-sets.sh
source "$ROOT/scripts/module-sets.sh"

# Split so this file does not match itself.
pattern='forge''jo|git\.zem\.sys''tems'

globs=(--hidden -g '!.git' -g '!**/node_modules/**')
for p in "${MUXCORE_ARCHIVED[@]}" "${MUXCORE_HISTORICAL[@]}" "${MUXCORE_HISTORY_PATHS[@]}"; do
  globs+=(-g "!$p/**")
done

targets=("$@")
[[ ${#targets[@]} -eq 0 ]] && targets=("$ROOT")

if hits=$(cd "$ROOT" && rg -n -i "${globs[@]}" "$pattern" "${targets[@]}"); then
  echo "$hits"
  echo "check-retired-origin: $(echo "$hits" | wc -l) reference(s) to the retired origin (ADR-0002)" >&2
  exit 1
fi
echo "check-retired-origin: ok"
