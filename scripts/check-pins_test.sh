#!/usr/bin/env bash
# Fixture tests for check-pins.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK="$SCRIPT_DIR/check-pins.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }

# Build a consistent fake umbrella tree under $1.
make_tree() {
  local r="$1"
  mkdir -p "$r/muxcore-installer" "$r/muxcore-operator/api/v1alpha1" \
    "$r/muxcore-operator/config/samples" "$r/_mvp/deploy/helm/muxcore" \
    "$r/_mvp/deploy/kustomize/base" "$r/_mvp/deploy/kustomize/overlays/dev" \
    "$r/muxcore-module-starter" "$r/spool"
  cat >"$r/release-train.env" <<'EOT'
MUXCORE_TRAIN=train-test
CORE_TAG=v0.6.6
MUXCORE_IMAGE_TAG=v0.6.6
SPOOL_CATALOG_VERSION=2.5.0
EOT
  cat >"$r/spool/catalog.json" <<'EOT'
{"version":"2.5.0","modules":[{"repo":"https://github.com/Muxcore-Media/api-rest","name":"api-rest","version":"v0.1.9"}]}
EOT
  cat >"$r/muxcore-installer/versions.env" <<'EOT'
CORE_TAG=v0.6.6
MODULES="
api-rest=v0.1.9
not-in-catalog=v9.9.9
"
EOT
  cat >"$r/muxcore-operator/api/v1alpha1/platform_types.go" <<'EOT'
const DefaultCoreImage = "ghcr.io/muxcore-media/muxcored:v0.6.6"
EOT
  echo '  coreImage: muxcored:v0.6.6' >"$r/muxcore-operator/config/samples/s.yaml"
# shellcheck disable=SC2016
  echo '    image: ${REG:-x}/api-rest:${MUXCORE_IMAGE_TAG:-v0.6.6}' >"$r/_mvp/docker-compose.registry.yml"
  echo 'core_tag: v0.6.6' >"$r/_mvp/household-manifest.yaml"
  echo 'coreTag: v0.6.6' >"$r/_mvp/deploy/helm/muxcore/values.yaml"
  echo '          image: localhost:5000/muxcore/core:v0.6.6' >"$r/_mvp/deploy/kustomize/base/core.yaml"
  printf 'images:\n  - name: x\n    newTag: v0.6.6\n' >"$r/_mvp/deploy/kustomize/overlays/dev/kustomization.yaml"
  cat >"$r/muxcore-module-starter/go.mod" <<'EOT'
module example.com/m

require github.com/Muxcore-Media/core/pkg/contracts v0.6.0

require github.com/Muxcore-Media/core v0.6.6 // indirect
EOT
}

run() { MUXCORE_ROOT="$1" bash "$CHECK" 2>&1; }

# expect_bad TREE FILE_SUBSTRING : check fails and output names the file.
expect_bad() {
  local out
  if out=$(run "$1"); then fail "expected failure ($2)"; fi
  grep -q "$2" <<<"$out" || fail "output does not name $2: $out"
  grep -q 'check-pins: 1 mismatch(es)' <<<"$out" || fail "expected exactly 1 mismatch ($2): $out"
}

# mutate NAME FILE SED_EXPR : copy the good tree, edit FILE, expect failure.
mutate() {
  rm -rf "$TMP/t"
  cp -r "$TMP/good" "$TMP/t"
  sed -i "$3" "$TMP/t/$2"
  expect_bad "$TMP/t" "$2"
}

make_tree "$TMP/good"
out=$(run "$TMP/good") || fail "expected ok on consistent tree: $out"
[[ "$out" == "check-pins: ok" ]] || fail "unexpected output: $out"

mutate installer-core muxcore-installer/versions.env 's/^CORE_TAG=.*/CORE_TAG=v0.5.0/'
mutate installer-module muxcore-installer/versions.env 's/^api-rest=.*/api-rest=v0.1.7/'
mutate operator-go muxcore-operator/api/v1alpha1/platform_types.go 's/v0.6.6/v0.5.4/'
mutate operator-yaml muxcore-operator/config/samples/s.yaml 's/v0.6.6/v0.5.4/'
mutate compose _mvp/docker-compose.registry.yml 's/v0.6.6/v0.5.7/'
mutate manifest _mvp/household-manifest.yaml 's/v0.6.6/v0.5.7/'
mutate helm _mvp/deploy/helm/muxcore/values.yaml 's/v0.6.6/v0.5.7/'
mutate kustomize-image _mvp/deploy/kustomize/base/core.yaml 's/v0.6.6/v0.5.7/'
mutate kustomize-newtag _mvp/deploy/kustomize/overlays/dev/kustomization.yaml 's/v0.6.6/v0.5.7/'
mutate starter muxcore-module-starter/go.mod 's/core v0.6.6/core v0.6.0/'
mutate catalog spool/catalog.json 's/"version":"2.5.0","modules"/"version":"2.4.0","modules"/'

# Missing submodule files are skipped with a WARN, not a failure.
rm -rf "$TMP/t"
cp -r "$TMP/good" "$TMP/t"
rm -rf "$TMP/t/muxcore-installer"
out=$(run "$TMP/t") || fail "expected ok when installer is absent: $out"
grep -q 'WARN: muxcore-installer/versions.env' <<<"$out" || fail "expected WARN: $out"

echo "check-pins_test: ok"
