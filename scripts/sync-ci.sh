#!/usr/bin/env bash
# Write each active module's GitHub Actions workflows from the umbrella templates
# (ADR-0003). Go modules get ci.yml (+ proto-check when the Makefile has a
# `proto:` target) and, if they already ship releases, release.yml. Node apps
# with a `typecheck` script get node-app-ci.yml. Modules with bespoke CI
# (core, native apps, docs site, starter template) are listed in SKIP.
#   usage: scripts/sync-ci.sh [--check] [module...]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MUXCORE_ROOT="$ROOT"
# shellcheck source=module-sets.sh
source "$ROOT/scripts/module-sets.sh"
TPL="$ROOT/scripts/ci-templates"
SKIP=(core media-tvos-app muxcore-ios muxcore-docs muxcore-module-starter core.wiki spool branding)

check=0
if [[ "${1:-}" == "--check" ]]; then check=1; shift; fi

render_ci() { # $1 module dir -> stdout
  local dir="$1"
  if [[ -f "$dir/go.mod" ]]; then
    cat "$TPL/go-module-ci.yml"
    if [[ -f "$dir/Makefile" ]] && grep -q '^proto:' "$dir/Makefile"; then
      cat "$TPL/go-module-ci-proto.yml"
    fi
  elif [[ -f "$dir/package.json" ]] && grep -q '"typecheck"' "$dir/package.json"; then
    cat "$TPL/node-app-ci.yml"
  fi
}

modules=("$@")
if [[ ${#modules[@]} -eq 0 ]]; then
  mapfile -t modules < <(muxcore_submodule_paths)
fi

drift=0
for m in "${modules[@]}"; do
  muxcore_is_inactive "$m" && continue
  for s in "${SKIP[@]}"; do [[ "$m" == "$s" ]] && continue 2; done
  dir="$ROOT/$m"
  want=$(render_ci "$dir")
  [[ -z "$want" ]] && continue
  wf="$dir/.github/workflows"
  if [[ $check -eq 1 ]]; then
    if [[ "$(cat "$wf/ci.yml" 2>/dev/null)" != "$want" ]]; then echo "DRIFT $m/.github/workflows/ci.yml"; drift=$((drift + 1)); fi
    if [[ -f "$wf/release.yml" && -f "$dir/go.mod" ]] && ! cmp -s "$wf/release.yml" "$TPL/go-module-release.yml"; then
      echo "DRIFT $m/.github/workflows/release.yml"; drift=$((drift + 1))
    fi
    if grep -lqE 'self-hosted|checkout-mirror' "$wf"/*.yml 2>/dev/null; then echo "SELF-HOSTED $m"; drift=$((drift + 1)); fi
    continue
  fi
  mkdir -p "$wf"
  printf '%s\n' "$want" > "$wf/ci.yml"
  if [[ -f "$wf/release.yml" && -f "$dir/go.mod" ]]; then cp "$TPL/go-module-release.yml" "$wf/release.yml"; fi
  echo "synced $m"
done

if [[ $check -eq 1 ]]; then
  if [[ $drift -gt 0 ]]; then echo "sync-ci --check: $drift problem(s)" >&2; exit 1; fi
  echo "sync-ci --check: ok"
fi
