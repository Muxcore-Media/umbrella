# shellcheck shell=bash
# Shared module classifications for umbrella scripts. Source this file.

# Archived workspace dumps (MASTER-ROADMAP §3, PRD NG-7): pinned for history only.
MUXCORE_ARCHIVED=(cache-memory custom-scripts media-ui media-jellyfin muxcorectl)

# Historical / generated-content submodules that are not product code.
MUXCORE_HISTORICAL=(_wave1)

# Umbrella paths that hold historical records or decision history; excluded from
# the retired-origin reference check (ADR-0002).
# shellcheck disable=SC2034 # used by scripts that source this file
MUXCORE_HISTORY_PATHS=(docs/history docs/adr)

# muxcore_is_inactive <path> — true for archived or historical submodules.
muxcore_is_inactive() {
  local p
  for p in "${MUXCORE_ARCHIVED[@]}" "${MUXCORE_HISTORICAL[@]}"; do
    [[ "$1" == "$p" ]] && return 0
  done
  return 1
}

# muxcore_submodule_paths — every submodule path from .gitmodules.
muxcore_submodule_paths() {
  git config -f "${MUXCORE_ROOT:-.}/.gitmodules" --get-regexp 'submodule\..*\.path' | awk '{print $2}'
}

# muxcore_active_go_modules — active submodules that contain a go.mod.
muxcore_active_go_modules() {
  local p
  while read -r p; do
    muxcore_is_inactive "$p" && continue
    [[ -f "${MUXCORE_ROOT:-.}/$p/go.mod" ]] && echo "$p"
  done < <(muxcore_submodule_paths)
}
