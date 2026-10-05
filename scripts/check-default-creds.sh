#!/usr/bin/env bash
# Fail when tracked files in the umbrella or an active submodule ship a known
# default credential (NFR-SEC-001, FR-INS-004; roadmap T-M3-01).
#
# Flags: the removed literals (admin-dev-only, minioadmin, household-monitor),
# `changeme` / `change-me` / `CHANGE_ME`, `password: admin`, `PASSWORD=admin`,
# `<PASS|SECRET|TOKEN|KEY var>:-admin`, and shell/compose defaults of the form
# `${NAME_TOKEN|PASSWORD|SECRET|KEY:-<non-empty literal>}` (empty defaults and
# defaults that expand another variable, e.g. `:-$X` / `:-${X}`, are fine).
#
# Skips archived/historical submodules, docs/history, docs/adr/*.md, *_test.go,
# testdata/, node_modules/, vendor/, lockfiles and this script's own files.
# Justified hits go in scripts/check-default-creds.allow:
#   <path-glob>:<ERE matched against the line>  # reason
#
#   usage: scripts/check-default-creds.sh
#   env:   MUXCORE_ROOT (tree to scan), CHECK_DEFAULT_CREDS_ALLOW (allowlist file)
set -euo pipefail
ROOT="${MUXCORE_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export MUXCORE_ROOT="$ROOT"
# shellcheck source=module-sets.sh
source "$HERE/module-sets.sh"
ALLOW="${CHECK_DEFAULT_CREDS_ALLOW:-$HERE/check-default-creds.allow}"

# One ERE, matched case-insensitively.
pattern='admin-dev-only|minioadmin|household-monitor'
pattern+='|\bchange[-_]?me\b'
pattern+='|password[[:space:]]*:[[:space:]]*["'"'"']?admin(["'"'"']|[[:space:]]|$)'
pattern+='|password[a-z0-9_]*=["'"'"']?admin(["'"'"']|[[:space:]]|$)'
pattern+='|(pass|passwd|password|secret|token|key)[a-z0-9_]*:-admin'
pattern+='|\$\{[a-z0-9_]*(token|password|secret|key):-[^$}][^}]*\}'

# Paths never scanned (regex on the repo-relative path).
skip_re='(^|/)(testdata|node_modules|vendor)/|_test\.go$|(^|/)(package-lock\.json|yarn\.lock|pnpm-lock\.yaml|go\.sum)$'
skip_re+='|^docs/history/|^docs/adr/.*\.md$'
skip_re+='|^scripts/check-default-creds(_test\.sh|\.sh|\.allow)$'

# Allowlist entries: parallel arrays of globs and regexes.
allow_globs=()
allow_res=()
if [[ -f "$ALLOW" ]]; then
  while IFS= read -r raw || [[ -n "$raw" ]]; do
    entry="${raw%% \#*}"
    entry="${entry#"${entry%%[![:space:]]*}"}"
    entry="${entry%"${entry##*[![:space:]]}"}"
    [[ -z "$entry" || "$entry" == \#* ]] && continue
    if [[ "$entry" != *:* ]]; then
      echo "check-default-creds: bad allowlist entry (want path:pattern # reason): $raw" >&2
      exit 2
    fi
    allow_globs+=("${entry%%:*}")
    allow_res+=("${entry#*:}")
  done <"$ALLOW"
fi

is_allowed() { # <path> <line text>
  local i
  for i in "${!allow_globs[@]}"; do
    # shellcheck disable=SC2053 # glob match is intended
    if [[ "$1" == ${allow_globs[$i]} ]] && grep -Eiq -- "${allow_res[$i]}" <<<"$2"; then
      return 0
    fi
  done
  return 1
}

# Repos to scan: "<dir>|<prefix>" (prefix is the path relative to the umbrella).
repos=("$ROOT|")
if [[ -f "$ROOT/.gitmodules" ]]; then
  while read -r p; do
    [[ -n "$p" ]] || continue
    muxcore_is_inactive "$p" && continue
    [[ -e "$ROOT/$p/.git" ]] || continue
    repos+=("$ROOT/$p|$p/")
  done < <(muxcore_submodule_paths || true)
fi

hits=0
for r in "${repos[@]}"; do
  dir="${r%%|*}"
  prefix="${r#*|}"
  while IFS= read -r line; do
    f="${line%%:*}"
    rest="${line#*:}"
    n="${rest%%:*}"
    text="${rest#*:}"
    path="$prefix$f"
    is_allowed "$path" "$text" && continue
    echo "$path:$n:$text"
    hits=$((hits + 1))
  done < <(
    cd "$dir"
    git ls-files -z | tr '\0' '\n' | grep -Ev "$skip_re" | tr '\n' '\0' |
      xargs -0 -r grep -nIHiE -- "$pattern" 2>/dev/null || true
  )
done

if ((hits > 0)); then
  echo "check-default-creds: $hits default-credential hit(s) (NFR-SEC-001, FR-INS-004); remove them or allowlist with a reason in scripts/check-default-creds.allow" >&2
  exit 1
fi
echo "check-default-creds: ok"
