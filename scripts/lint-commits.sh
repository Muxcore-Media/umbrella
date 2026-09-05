#!/usr/bin/env bash
# Validate conventional commit messages in a git revision range.
#
# Usage:
#   lint-commits.sh [BASE_REF] [HEAD_REF]
#
# Defaults:
#   BASE_REF  origin/main (falls back to main, then repository root)
#   HEAD_REF  HEAD
set -euo pipefail

ROOT="${MUXCORE_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
BASE="${1:-origin/main}"
HEAD="${2:-HEAD}"

CONVENTIONAL_RE='^(feat|fix|chore|ci|docs|test|refactor|perf|build|revert|style)(\([a-zA-Z0-9._/-]+\))?!?: .+$'

usage() {
  sed -n '2,9p' "$0" | sed 's/^# //'
  exit "${1:-0}"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage 0
fi

resolve_base() {
  local candidate="$1"
  if git -C "$ROOT" rev-parse --verify "$candidate" >/dev/null 2>&1; then
    printf '%s\n' "$candidate"
    return 0
  fi
  if [[ "$candidate" == "origin/main" ]] && git -C "$ROOT" rev-parse --verify main >/dev/null 2>&1; then
    printf '%s\n' "main"
    return 0
  fi
  git -C "$ROOT" rev-list --max-parents=0 HEAD 2>/dev/null | tail -n 1
}

main() {
  local base_ref head_ref range subject invalid=0 count=0

  base_ref="$(resolve_base "$BASE")"
  head_ref="$(git -C "$ROOT" rev-parse "$HEAD")"
  range="${base_ref}..${head_ref}"

  if ! git -C "$ROOT" rev-list "$range" >/dev/null 2>&1; then
    echo "lint-commits: no commits to check in range ${range}" >&2
    exit 0
  fi

  while IFS= read -r subject; do
    [[ -z "$subject" ]] && continue
    count=$((count + 1))
    if [[ "$subject" =~ $CONVENTIONAL_RE ]]; then
      continue
    fi
    echo "lint-commits: invalid conventional commit message:" >&2
    echo "  $subject" >&2
    invalid=$((invalid + 1))
  done < <(git -C "$ROOT" log --no-merges --format=%s "$range")

  if ((count == 0)); then
    echo "lint-commits: no non-merge commits in ${range}"
    exit 0
  fi

  if ((invalid > 0)); then
    echo "lint-commits: ${invalid}/${count} commit(s) failed conventional commit lint" >&2
    echo "Expected format: type(scope): description" >&2
    echo "Allowed types: feat, fix, chore, ci, docs, test, refactor, perf, build, revert, style" >&2
    exit 1
  fi

  echo "lint-commits: ok (${count} commit(s))"
}

main "$@"
