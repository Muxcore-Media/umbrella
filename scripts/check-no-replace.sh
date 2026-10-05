#!/usr/bin/env bash
# Fail if an active module's go.mod has a filesystem replace directive
# (ADR-0011, SRS NFR-MNT-007). Local multi-module work uses go.work instead.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MUXCORE_ROOT="$ROOT"
# shellcheck source=module-sets.sh
source "$ROOT/scripts/module-sets.sh"

bad=0
while read -r m; do
  if out=$(grep -nE '^[[:space:]]*(replace[[:space:]]+)?[^[:space:]]+([[:space:]]+v[^[:space:]]+)?[[:space:]]+=>[[:space:]]+(\.\./|/)' "$ROOT/$m/go.mod"); then
    echo "$m/go.mod:"; while IFS= read -r l; do echo "  $l"; done <<<"$out"
    bad=$((bad + 1))
  fi
done < <(muxcore_active_go_modules)
# In-repo replaces (=> ./sub) are allowed: consumers ignore them and they never leave the repo.
if [[ $bad -gt 0 ]]; then
  echo "check-no-replace: $bad module(s) with filesystem replace directives" >&2
  exit 1
fi
echo "check-no-replace: ok"
