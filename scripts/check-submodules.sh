#!/usr/bin/env bash
# Verify every submodule pin resolves on GitHub: the repository is non-empty and
# the pinned commit exists (SRS NFR-MNT-009). Requires `gh` with read access.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
bad=0
# fetch_check <url> <sha> — fallback for repos the REST API cannot see (wikis).
fetch_check() {
  local tmp rc
  tmp=$(mktemp -d)
  git -C "$tmp" init -q && git -C "$tmp" fetch -q --depth=1 "$1" "$2" >/dev/null 2>&1
  rc=$?
  rm -rf "$tmp"
  return $rc
}
while read -r mode sha _ path; do
  [[ "$mode" == 160000 ]] || continue
  url=$(git config -f .gitmodules "submodule.${path}.url" || true)
  repo=$(basename "${url%.git}")
  if [[ -z "$repo" ]]; then echo "MISSING-URL $path"; bad=$((bad + 1)); continue; fi
  if ! gh api "repos/Muxcore-Media/${repo}/commits/${sha}" --silent >/dev/null 2>&1 &&
    ! fetch_check "$url" "$sha"; then
    echo "UNRESOLVED $path -> Muxcore-Media/${repo}@${sha:0:12}"
    bad=$((bad + 1))
  fi
done < <(git ls-files -s | awk '$1==160000')
if [[ $bad -gt 0 ]]; then
  echo "check-submodules: $bad unresolved submodule pin(s)" >&2
  exit 1
fi
echo "check-submodules: ok"
