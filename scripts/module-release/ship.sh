#!/usr/bin/env bash
# usage: ship.sh <module-dir> "<conventional subject>" "<body>" "<changelog bullets (markdown lines)>" [version]
set -euo pipefail
# Optional toolchain env (e.g. Go 1.26.6 on PATH): MUXCORE_GO_ENV=/path/to/env.sh
# shellcheck source=/dev/null
if [[ -n "${MUXCORE_GO_ENV:-}" ]]; then source "$MUXCORE_GO_ENV"; fi
LINT="${LINT:-golangci-lint}"
# shellcheck source=../muxcore-git-env.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/muxcore-git-env.sh"
cd "$1"; m=$(basename "$PWD")
br=$(gh repo view "Muxcore-Media/$m" --json defaultBranchRef -q .defaultBranchRef.name)
[[ "$(git rev-parse --abbrev-ref HEAD)" == "$br" ]] || { echo "not on $br"; exit 1; }
git fetch -q --tags origin
last=$(git tag -l 'v*' | { grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' || true; } | sort -V | tail -1); last=${last:-v0.0.0}
IFS=. read -r a b c <<<"${last#v}"; next="$a.$b.$((c+1))"
mj=""; [[ -f muxcore.json ]] && mj=$(jq -r '.version // empty' muxcore.json)
if [[ -n "${5:-}" ]]; then ver=$5
elif [[ -n "$mj" ]] && [[ "$(printf '%s\n%s\n' "$mj" "$next" | sort -V | tail -1)" == "$mj" ]] && ! git rev-parse -q --verify "refs/tags/v$mj" >/dev/null; then ver=$mj
else ver=$next; fi
if [[ -f muxcore.json ]]; then jq --arg v "$ver" '.version=$v' muxcore.json > muxcore.json.tmp && mv muxcore.json.tmp muxcore.json; fi
entry="## [$ver] - $(date +%F)"$'\n\n'"${4:+$4
}"
if [[ -f CHANGELOG.md ]]; then
  python3 - "$entry" <<'PY'
import sys,re
e=sys.argv[1];s=open('CHANGELOG.md').read()
u=re.search(r'^## \[Unreleased\][^\n]*\n',s,re.M|re.I)
if u:
    hdr,body=e.split("\n",1)
    s=s[:u.start()]+hdr+"\n"+body.rstrip()+"\n\n"+s[u.end():].lstrip("\n")
else:
    m=re.search(r'^## ',s,re.M)
    s=(s[:m.start()]+e+"\n"+s[m.start():]) if m else s.rstrip()+"\n\n"+e
open('CHANGELOG.md','w').write(s)
PY
else printf '# Changelog\n\n%s' "$entry" > CHANGELOG.md; fi
git add -A
git commit -q -m "$2" -m "$3" -m "Refs: Muxcore-Media/umbrella roadmap ${REFS:-T-M1-05}"
git push -q origin "$br"
git tag -a "v$ver" -m "v$ver"
git push -q origin "v$ver"
gh variable set LINT_BLOCKING -b true -R "Muxcore-Media/$m"
echo "SHIPPED $m v$ver $(git rev-parse --short HEAD)"
