#!/usr/bin/env bash
# Add standard sibling replace directives to go.mod files missing them.
# Only applies when ../core exists relative to the module directory.
#
# Usage:
#   ./scripts/ensure-go-replaces.sh            # all go.mod files under workspace
#   ./scripts/ensure-go-replaces.sh path/to/module
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

declare -a REPLACES=(
  'github.com/Muxcore-Media/core|../core'
  'github.com/Muxcore-Media/core/pkg/contracts|../core/pkg/contracts'
  'github.com/Muxcore-Media/core/pkg/tenant|../core/pkg/tenant'
  'github.com/Muxcore-Media/core/pkg/log|../core/pkg/log'
  'github.com/Muxcore-Media/core/sdk/go/module|../core/sdk/go/module'
  'github.com/Muxcore-Media/core/sdk/go/client|../core/sdk/go/client'
)

add_replace() {
  local gomod="$1" pkg="$2" rel="$3"
  if grep -qE "^replace[[:space:]]+${pkg}[[:space:]]+=>" "$gomod"; then
    return 0
  fi
  printf '\nreplace %s => %s\n' "$pkg" "$rel" >>"$gomod"
}

ensure_module() {
  local mod_dir gomod
  mod_dir="$(cd "$1" && pwd)"
  gomod="$mod_dir/go.mod"
  [[ -f "$gomod" ]] || return 0
  [[ -d "$mod_dir/../core" ]] || return 0
  if grep -qE '^module[[:space:]]+github\.com/Muxcore-Media/core[[:space:]]*$' "$gomod"; then
    return 0
  fi

  local changed=0 entry pkg rel
  for entry in "${REPLACES[@]}"; do
    pkg="${entry%%|*}"
    rel="${entry#*|}"
    if ! grep -qE "^replace[[:space:]]+${pkg}[[:space:]]+=>" "$gomod"; then
      add_replace "$gomod" "$pkg" "$rel"
      changed=1
    fi
  done

  if ((changed)); then
    echo "updated $gomod"
  fi
}

if (($# > 0)); then
  for arg in "$@"; do
    ensure_module "$arg"
  done
else
  while IFS= read -r gomod; do
    ensure_module "$(dirname "$gomod")"
  done < <(find "$ROOT" -name go.mod -not -path '*/vendor/*' | sort)
fi
