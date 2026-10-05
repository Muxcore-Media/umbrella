#!/usr/bin/env bash
# Verify every release-train pin in the umbrella agrees with release-train.env
# (T-M2-04, FR-INS-006; TDD §5 "Release train"). A check is skipped with a WARN
# line when its file or submodule is not checked out.
set -euo pipefail
ROOT="${MUXCORE_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
# shellcheck source=../release-train.env
source "$ROOT/release-train.env"

bad=0
m="" ln=""
mismatch() { echo "$1: $2"; bad=$((bad + 1)); }
warn() { echo "WARN: $1 not found, skipping"; }

# have FILE... : true when the first path exists; otherwise warn and fail.
have() {
  if [[ -e "$ROOT/$1" ]]; then return 0; fi
  warn "$1"
  return 1
}

# expect FILE LABEL WANT GOT : record a mismatch when WANT != GOT.
expect() {
  [[ "$3" == "$4" ]] || mismatch "$1" "$2 is $4, want $3"
}

# check_tags FILE REGEX WANT : every regex match (as "<what>:<tag>") must end
# with the wanted tag; reported with its line number.
check_tags() {
  local file="$1" re="$2" want="$3" m ln
  while IFS= read -r m; do
    [[ -n "$m" ]] || continue
    ln="${m%%:*}"
    m="${m#*:}"
    expect "$file:$ln" "${m%:*}" "$want" "${m##*:}"
  done < <(grep -noE "$re" "$ROOT/$file" || true)
}

# a. installer versions.env
f=muxcore-installer/versions.env
if have "$f"; then
  core=$(sed -n 's/^CORE_TAG=\(.*\)$/\1/p' "$ROOT/$f" | head -n1 | tr -d '"')
  expect "$f" CORE_TAG "$CORE_TAG" "$core"
  if have spool/catalog.json; then
    declare -A cat=()
    while IFS=$'\t' read -r name ver; do
      cat["$name"]="$ver"
    done < <(jq -r '.modules[] | [(.repo | sub(".*/"; "")), .version] | @tsv' "$ROOT/spool/catalog.json")
    while IFS='=' read -r repo tag; do
      [[ -n "${cat[$repo]:-}" ]] || continue
      expect "$f" "$repo" "${cat[$repo]}" "$tag"
    done < <(sed -n '/^MODULES="/,/^"/{/^[A-Za-z0-9._-]*=v[^[:space:]]*$/p}' "$ROOT/$f")
  fi
fi

# b. operator
f=muxcore-operator/api/v1alpha1/platform_types.go
if have "$f"; then check_tags "$f" 'muxcored:v[0-9][^"[:space:]]*' "$CORE_TAG"; fi
if have muxcore-operator/config; then
  while IFS= read -r f; do
    check_tags "${f#"$ROOT"/}" 'muxcored:v[0-9][^"[:space:]]*' "$CORE_TAG"
  done < <(find "$ROOT/muxcore-operator/config" -name '*.yaml' | sort)
fi

# c. compose
f=_mvp/docker-compose.registry.yml
if have "$f"; then
  while IFS= read -r m; do
    ln="${m%%:*}"
    m="${m#*:-}"
    expect "$f:$ln" 'MUXCORE_IMAGE_TAG default' "$MUXCORE_IMAGE_TAG" "${m%\}}"
  done < <(grep -noE '\$\{MUXCORE_IMAGE_TAG:-v[^}]*\}' "$ROOT/$f" || true)
fi

# d. household manifest
f=_mvp/household-manifest.yaml
if have "$f"; then
  v=$(sed -n 's/^core_tag:[[:space:]]*\([^[:space:]#]*\).*/\1/p' "$ROOT/$f" | head -n1 | tr -d '"')
  expect "$f" core_tag "$CORE_TAG" "$v"
fi

# e. helm values
f=_mvp/deploy/helm/muxcore/values.yaml
if have "$f"; then
  v=$(sed -n 's/^coreTag:[[:space:]]*\([^[:space:]#]*\).*/\1/p' "$ROOT/$f" | head -n1 | tr -d '"')
  expect "$f" coreTag "$MUXCORE_IMAGE_TAG" "$v"
fi

# f. kustomize
if have _mvp/deploy/kustomize; then
  while IFS= read -r f; do
    f="${f#"$ROOT"/}"
    check_tags "$f" '[A-Za-z0-9_.:-]+(/[A-Za-z0-9_.-]+)+:v[0-9]+\.[0-9]+\.[0-9]+' "$MUXCORE_IMAGE_TAG"
    while IFS= read -r t; do
      [[ -n "$t" ]] || continue
      expect "$f" newTag "$MUXCORE_IMAGE_TAG" "$t"
    done < <(sed -n 's/^[[:space:]-]*newTag:[[:space:]]*"\{0,1\}\([^"[:space:]#]*\).*/\1/p' "$ROOT/$f")
  done < <(find "$ROOT/_mvp/deploy/kustomize" -name '*.yaml' | sort)
fi

# g. module starter go.mod (nested core modules are versioned separately)
f=muxcore-module-starter/go.mod
if have "$f"; then
  v=$(sed -n 's|^[[:space:]]*\(require[[:space:]]\{1,\}\)\{0,1\}github.com/Muxcore-Media/core[[:space:]]\{1,\}\(v[^[:space:]]*\).*|\2|p' "$ROOT/$f" | head -n1)
  if [[ -n "$v" ]]; then expect "$f" github.com/Muxcore-Media/core "$CORE_TAG" "$v"; fi
fi

# h. spool catalog
f=spool/catalog.json
if have "$f"; then
  expect "$f" version "$SPOOL_CATALOG_VERSION" "$(jq -r '.version' "$ROOT/$f")"
fi

if [[ $bad -gt 0 ]]; then
  echo "check-pins: $bad mismatch(es)" >&2
  exit 1
fi
echo "check-pins: ok"
