#!/usr/bin/env bash
# Generate Keep a Changelog markdown sections from conventional commits.
#
# Usage:
#   generate-changelog.sh [options]
#
# Options:
#   --from REF       Start after this ref (default: latest tag reachable from --to)
#   --to REF         End ref (default: HEAD)
#   --version VER    Version heading, e.g. v1.2.3 (default: Unreleased)
#   --output FILE    Write markdown to FILE instead of stdout
#   --update FILE    Insert generated section into FILE before ## [Unreleased]
set -euo pipefail

ROOT="${MUXCORE_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"

FROM=""
TO="HEAD"
VERSION="Unreleased"
OUTPUT=""
UPDATE=""

usage() {
  sed -n '2,12p' "$0" | sed 's/^# //'
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --from)
      FROM="${2:-}"
      shift 2
      ;;
    --to)
      TO="${2:-}"
      shift 2
      ;;
    --version)
      VERSION="${2:-}"
      shift 2
      ;;
    --output)
      OUTPUT="${2:-}"
      shift 2
      ;;
    --update)
      UPDATE="${2:-}"
      shift 2
      ;;
    -h | --help)
      usage 0
      ;;
    *)
      echo "generate-changelog.sh: unknown argument: $1" >&2
      usage 1
      ;;
  esac
done

section_for_type() {
  case "$1" in
    feat) printf '%s\n' "Added" ;;
    fix) printf '%s\n' "Fixed" ;;
    perf | refactor | docs | build | ci | test | chore | revert | style)
      printf '%s\n' "Changed"
      ;;
    *)
      printf '%s\n' ""
      ;;
  esac
}

parse_commit() {
  local subject="$1"
  local type rest description

  if [[ ! "$subject" =~ ^(feat|fix|chore|ci|docs|test|refactor|perf|build|revert|style)(\(([a-zA-Z0-9._/-]+)\))?!?:[[:space:]]+(.+)$ ]]; then
    return 1
  fi

  type="${BASH_REMATCH[1]}"
  description="${BASH_REMATCH[4]}"
  printf '%s\t%s\n' "$type" "$description"
}

resolve_from_ref() {
  local to_ref="$1"
  if [[ -n "$FROM" ]]; then
    printf '%s\n' "$FROM"
    return 0
  fi

  if git -C "$ROOT" describe --tags --abbrev=0 "${to_ref}^" >/dev/null 2>&1; then
    git -C "$ROOT" describe --tags --abbrev=0 "${to_ref}^"
    return 0
  fi

  if git -C "$ROOT" rev-list --max-parents=0 "$to_ref" >/dev/null 2>&1; then
    git -C "$ROOT" rev-list --max-parents=0 "$to_ref" | tail -n 1
    return 0
  fi

  return 1
}

generate_markdown() {
  local version="$1"
  local from_ref="$2"
  local to_ref="$3"
  local range date subject line type description section
  local -a added=() changed=() fixed=()

  if [[ -n "$from_ref" ]]; then
    range="${from_ref}..${to_ref}"
  else
    range="$to_ref"
  fi

  date="$(git -C "$ROOT" log -1 --format=%cs "$to_ref")"

  while IFS= read -r subject; do
    [[ -z "$subject" ]] && continue
    if ! line="$(parse_commit "$subject")"; then
      continue
    fi
    type="${line%%$'\t'*}"
    description="${line#*$'\t'}"
    section="$(section_for_type "$type")"
    case "$section" in
      Added) added+=("$description") ;;
      Changed) changed+=("$description") ;;
      Fixed) fixed+=("$description") ;;
    esac
  done < <(git -C "$ROOT" log --no-merges --format=%s "$range")

  printf '## [%s] - %s\n\n' "$version" "$date"

  if ((${#added[@]} > 0)); then
    printf '### Added\n'
    for item in "${added[@]}"; do
      printf -- '- %s\n' "$item"
    done
    printf '\n'
  fi

  if ((${#changed[@]} > 0)); then
    printf '### Changed\n'
    for item in "${changed[@]}"; do
      printf -- '- %s\n' "$item"
    done
    printf '\n'
  fi

  if ((${#fixed[@]} > 0)); then
    printf '### Fixed\n'
    for item in "${fixed[@]}"; do
      printf -- '- %s\n' "$item"
    done
    printf '\n'
  fi

  if ((${#added[@]} + ${#changed[@]} + ${#fixed[@]} == 0)); then
    printf '### Changed\n'
    printf -- '- No conventional commits in range\n\n'
  fi
}

insert_into_changelog() {
  local target="$1"
  local section_file="$2"
  local tmp marker

  [[ -f "$target" ]] || {
    echo "generate-changelog.sh: changelog file not found: $target" >&2
    return 1
  }

  marker='## [Unreleased]'
  if ! grep -Fq "$marker" "$target"; then
    echo "generate-changelog.sh: missing $marker in $target" >&2
    return 1
  fi

  tmp="$(mktemp)"
  awk -v marker="$marker" -v section_file="$section_file" '
    $0 == marker && !done {
      while ((getline line < section_file) > 0) {
        print line
      }
      close(section_file)
      done = 1
    }
    { print }
  ' "$target" >"$tmp"
  mv "$tmp" "$target"
}

main() {
  local from_ref tmp_section

  from_ref="$(resolve_from_ref "$TO")"

  if [[ -n "$OUTPUT" ]]; then
    generate_markdown "$VERSION" "$from_ref" "$TO" >"$OUTPUT"
    return 0
  fi

  if [[ -n "$UPDATE" ]]; then
    tmp_section="$(mktemp)"
    trap 'rm -f "$tmp_section"' RETURN
    generate_markdown "$VERSION" "$from_ref" "$TO" >"$tmp_section"
    insert_into_changelog "$UPDATE" "$tmp_section"
    return 0
  fi

  generate_markdown "$VERSION" "$from_ref" "$TO"
}

main "$@"
