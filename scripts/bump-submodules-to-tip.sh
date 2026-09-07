#!/usr/bin/env bash
# Bump umbrella gitlinks to each module's default-branch tip.
# Usage: bump-submodules-to-tip.sh [--dry-run] [--skip path1,path2,...]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DRY_RUN=0
EXTRA_SKIP=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    --skip) EXTRA_SKIP="$2"; shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

# Rename twins — canonical paths are bumped instead; leave these pinned.
DEFAULT_SKIP="media-ui,media-jellyfin,muxcorectl"
SKIP_PATHS="${DEFAULT_SKIP}"
if [[ -n "$EXTRA_SKIP" ]]; then
  SKIP_PATHS="${SKIP_PATHS},${EXTRA_SKIP}"
fi

is_skipped() {
  local path="$1"
  IFS=',' read -ra parts <<< "$SKIP_PATHS"
  for s in "${parts[@]}"; do
    [[ "$path" == "$s" ]] && return 0
  done
  return 1
}

MANIFEST="$ROOT/docs/FREEZE-PINS.md"
mkdir -p "$(dirname "$MANIFEST")"

FREEZE_DATE="$(date -u +%Y-%m-%d)"
FREEZE_TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

{
  echo "# Umbrella safe-pin freeze"
  echo ""
  echo "Freeze recorded **${FREEZE_DATE}** (\`${FREEZE_TS}\` UTC). Every **active** submodule"
  echo "gitlink points at the default-branch tip at freeze time. Rename twins"
  echo "(\`media-ui\`, \`media-jellyfin\`, \`muxcorectl\`) are intentionally excluded."
  echo ""
  echo "| Path | Repository | SHA | Branch |"
  echo "| ---- | ---------- | --- | ------ |"
} > "$MANIFEST"

BUMPED=0
UNCHANGED=0
FAILED=0
SKIPPED=0

while IFS= read -r path; do
  if is_skipped "$path"; then
    SKIPPED=$((SKIPPED + 1))
    old_sha="$(git rev-parse "HEAD:${path}" 2>/dev/null || echo "")"
    url="$(git config -f .gitmodules --get "submodule.${path}.url")"
    repo="${url##*/}"
    repo="${repo%.git}"
    echo "SKIP (rename twin): $path @ ${old_sha:0:12}" >&2
    {
      echo "| \`${path}\` | Muxcore-Media/${repo} | \`${old_sha}\` | *(frozen twin — not bumped)* |"
    } >> "$MANIFEST"
    continue
  fi

  url="$(git config -f .gitmodules --get "submodule.${path}.url")"
  repo="${url##*/}"
  repo="${repo%.git}"
  old_sha="$(git rev-parse "HEAD:${path}" 2>/dev/null || echo "")"

  default_branch="$(gh api "repos/Muxcore-Media/${repo}" --jq .default_branch 2>/dev/null || true)"
  if [[ -z "$default_branch" || "$default_branch" == "null" ]]; then
    echo "WARN: no GitHub repo for $path ($repo) — keeping pin ${old_sha:0:12}" >&2
    FAILED=$((FAILED + 1))
    {
      echo "| \`${path}\` | Muxcore-Media/${repo} | \`${old_sha}\` | *(unchanged — repo unavailable)* |"
    } >> "$MANIFEST"
    continue
  fi

  tip_sha="$(gh api "repos/Muxcore-Media/${repo}/commits/${default_branch}" --jq .sha 2>/dev/null || true)"
  if [[ ! "$tip_sha" =~ ^[0-9a-f]{40}$ ]]; then
    echo "WARN: no tip commit for $path ($repo@${default_branch}) — keeping pin ${old_sha:0:12}" >&2
    FAILED=$((FAILED + 1))
    {
      echo "| \`${path}\` | Muxcore-Media/${repo} | \`${old_sha}\` | *(unchanged — empty or missing branch)* |"
    } >> "$MANIFEST"
    continue
  fi

  if [[ "$old_sha" == "$tip_sha" ]]; then
    echo "OK (unchanged): $path @ ${tip_sha:0:12}"
    UNCHANGED=$((UNCHANGED + 1))
  else
    echo "BUMP: $path ${old_sha:0:12} -> ${tip_sha:0:12} ($default_branch)"
    if [[ "$DRY_RUN" -eq 0 ]]; then
      git update-index --add --cacheinfo 160000,"$tip_sha","$path"
    fi
    BUMPED=$((BUMPED + 1))
  fi

  {
    echo "| \`${path}\` | Muxcore-Media/${repo} | \`${tip_sha}\` | \`${default_branch}\` |"
  } >> "$MANIFEST"

done < <(git config -f .gitmodules --get-regexp '^submodule\..*\.path$' | awk '{print $2}' | sort)

{
  echo ""
  echo "## Summary"
  echo ""
  echo "- Bumped: ${BUMPED}"
  echo "- Unchanged (already at tip): ${UNCHANGED}"
  echo "- Skipped (rename twins): ${SKIPPED}"
  echo "- Failed / kept prior pin: ${FAILED}"
} >> "$MANIFEST"

echo "" >&2
echo "Done: bumped=$BUMPED unchanged=$UNCHANGED skipped=$SKIPPED failed=$FAILED" >&2
echo "Manifest: $MANIFEST" >&2

[[ "$FAILED" -eq 0 ]] || echo "WARN: ${FAILED} submodule(s) kept at prior pin (see manifest notes)" >&2
exit 0
