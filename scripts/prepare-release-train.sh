#!/usr/bin/env bash
# Prepare deterministic release notes from a committed umbrella release train.
# Does not fetch, build, create tags, push, or modify the checkout.
#
# Usage: prepare-release-train.sh [--ref REF] [--tag TAG] [--output FILE]
#   --ref REF      Commit or tag to prepare (default: HEAD).
#   --tag TAG      Require this tag name to match committed MUXCORE_TRAIN.
#   --output FILE  Write notes to FILE instead of stdout.
# Requires full umbrella history; submodule checkouts are not needed.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="${MUXCORE_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
REF=HEAD
EXPECTED_TAG=""
OUTPUT=""

fail() { echo "prepare-release-train: $*" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ref | --tag | --output)
      [[ $# -ge 2 && -n "$2" ]] || fail "$1 requires a value"
      case "$1" in
        --ref) REF="$2" ;;
        --tag) EXPECTED_TAG="$2" ;;
        --output) OUTPUT="$2" ;;
      esac
      shift 2
      ;;
    -h | --help) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) fail "unknown argument: $1" ;;
  esac
done

[[ "$(git -C "$ROOT" rev-parse --is-shallow-repository)" == false ]] || fail "full Git history is required (checkout fetch-depth: 0)"
COMMIT="$(git -C "$ROOT" rev-parse --verify --end-of-options "${REF}^{commit}")" || fail "invalid --ref: $REF"
MANIFEST="$(git -C "$ROOT" show "$COMMIT:release-train.env")" || fail "release-train.env is missing at $COMMIT"

# Parse data, never source shell code from the manifest. Values come from the
# selected commit, so a dirty working tree cannot alter a tagged release's notes.
declare -A pins=()
while IFS= read -r line; do
  [[ "$line" =~ ^[[:space:]]*(#.*)?$ ]] && continue
  [[ "$line" =~ ^(MUXCORE_TRAIN|CORE_TAG|MUXCORE_IMAGE_TAG|SPOOL_CATALOG_VERSION)=([^[:space:]]+)$ ]] || fail "invalid release-train.env assignment"
  key="${BASH_REMATCH[1]}"
  [[ ! -v "pins[$key]" ]] || fail "duplicate $key in release-train.env"
  pins[$key]="${BASH_REMATCH[2]}"
done <<<"$MANIFEST"

for key in MUXCORE_TRAIN CORE_TAG MUXCORE_IMAGE_TAG SPOOL_CATALOG_VERSION; do
  [[ -n "${pins[$key]:-}" ]] || fail "missing $key in release-train.env"
done

TRAIN="${pins[MUXCORE_TRAIN]}"
[[ "$TRAIN" =~ ^train-[0-9]{4}\.(0[1-9]|1[0-2])\.[1-9][0-9]*$ ]] || fail "MUXCORE_TRAIN must be train-YYYY.MM.N (month 01–12, sequence >= 1)"
for key in CORE_TAG MUXCORE_IMAGE_TAG; do
  [[ "${pins[$key]}" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$ ]] || fail "invalid $key version"
done
[[ "${pins[SPOOL_CATALOG_VERSION]}" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$ ]] || fail "invalid SPOOL_CATALOG_VERSION"
[[ "${pins[CORE_TAG]}" == "${pins[MUXCORE_IMAGE_TAG]}" ]] || fail "CORE_TAG and MUXCORE_IMAGE_TAG must agree"
[[ -z "$EXPECTED_TAG" || "$EXPECTED_TAG" == "$TRAIN" ]] || fail "tag $EXPECTED_TAG does not match committed MUXCORE_TRAIN=$TRAIN"

if git -C "$ROOT" show-ref --verify --quiet "refs/tags/$TRAIN"; then
  TAG_COMMIT="$(git -C "$ROOT" rev-parse "refs/tags/$TRAIN^{commit}")"
  [[ "$TAG_COMMIT" == "$COMMIT" ]] || fail "$TRAIN already names another commit; release tags are immutable"
fi

NOTES="$(MUXCORE_ROOT="$ROOT" bash "$SCRIPT_DIR/generate-changelog.sh" \
  --to "$COMMIT" --tag-pattern 'train-*' --version "$TRAIN")"
GITLINKS="$(git -C "$ROOT" ls-tree -r "$COMMIT")"

# Markdown backticks are literal formatting, not command substitutions.
# shellcheck disable=SC2016
render() {
  printf '%s\n\n' "$NOTES"
  printf '### Release train\n\n'
  printf '| Pin | Value |\n| --- | --- |\n'
  printf '| Umbrella commit | `%s` |\n' "$COMMIT"
  printf '| Core | `%s` |\n' "${pins[CORE_TAG]}"
  printf '| Module images | `%s` |\n' "${pins[MUXCORE_IMAGE_TAG]}"
  printf '| Spool catalog | `%s` |\n\n' "${pins[SPOOL_CATALOG_VERSION]}"
  printf '### Pinned module commits\n\n'
  printf '| Module | Commit |\n| --- | --- |\n'
  while IFS=$'\t' read -r entry path; do
    [[ "$entry" == '160000 commit '* ]] || continue
    printf '| `%s` | `%s` |\n' "$path" "${entry##* }"
  done <<<"$GITLINKS"
}

if [[ -n "$OUTPUT" ]]; then
  render >"$OUTPUT"
else
  render
fi
