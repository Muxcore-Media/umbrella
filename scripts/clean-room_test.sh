#!/usr/bin/env bash
# Offline E12a orchestration fixtures: run clean-room with the real erasure
# generator/manifest/compose, stubbing enrollment and every runtime/network call.
# Stop at the first compose config; this is not a household runtime test.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
fail() { echo "FAIL: $*" >&2; exit 1; }

# The root-only docs/scripts CI job does not initialize private submodules.
# An initialized checkout must provide the real inputs; missing files there fail.
if [[ ! -e "$ROOT/_mvp/.git" && ! -f "$ROOT/_mvp/scripts/gen-erasure-set.sh" ]]; then
  echo 'SKIP: clean-room_test needs the _mvp submodule for actual-generator fixtures'
  exit 0
fi
for input in scripts/gen-erasure-set.sh household-manifest.yaml docker-compose.registry.yml; do
  [[ -f "$ROOT/_mvp/$input" ]] || fail "missing _mvp/$input"
done
for tool in yq jq; do command -v "$tool" >/dev/null || fail "$tool is required"; done
yq --version | grep -q mikefarah || fail "mikefarah yq v4 is required"

make_tree() {
  local tree="$1"
  mkdir -p "$tree/scripts" "$tree/_mvp/scripts" "$tree/stubs"
  cp "$SCRIPT_DIR/clean-room.sh" "$tree/scripts/"
  cp "$ROOT/release-train.env" "$ROOT/docker-compose.yml" "$tree/"
  cp "$ROOT/_mvp/household-manifest.yaml" "$ROOT/_mvp/docker-compose.registry.yml" "$tree/_mvp/"
  cp "$ROOT/_mvp/scripts/gen-erasure-set.sh" "$tree/_mvp/scripts/gen-erasure-set-real.sh"
  cat >"$tree/_mvp/scripts/gen-enrollment.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
[[ "$1" == --env-file ]]
printf 'MUXCORE_ENROLL_SECRET=fixture-secret-must-not-be-logged\n' >>"$2"
EOF
  cat >"$tree/_mvp/scripts/gen-erasure-set.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
args=("$@")
mode=generate
profiles=()
manifest= compose_file= env_file=
while [[ $# -gt 0 ]]; do
  case "$1" in
    --manifest) manifest="$2"; shift 2 ;;
    --compose) compose_file="$2"; shift 2 ;;
    --env-file) env_file="$2"; shift 2 ;;
    --profile) profiles+=("$2"); shift 2 ;;
    --check) mode=check; shift ;;
    *) exit 91 ;;
  esac
done
[[ "$manifest" == "$TEST_ROOT/_mvp/household-manifest.yaml" ]]
[[ "$compose_file" == "$TEST_ROOT/_mvp/docker-compose.registry.yml" ]]
[[ "$env_file" == "$TEST_ROOT/work/clean-room.env" ]]
printf '%s profiles=%s\n' "$mode" "${profiles[*]}" >>"$TEST_ROOT/generator-calls"
bash "$(dirname "$0")/gen-erasure-set-real.sh" "${args[@]}"
if [[ "$mode" == generate && "$TEST_FAULT" == stale ]]; then
  sed -i 's/^AUTH_ERASURE_REQUIRED=.*/AUTH_ERASURE_REQUIRED=stale-owner/' "$env_file"
fi
EOF
  cat >"$tree/stubs/fixture-compose" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
[[ "$1" == version ]] && exit 0
printf '%s\n' "$*" >>"$TEST_ROOT/compose-calls"
if [[ "$*" == *'config --images' ]]; then
  printf '%s\n' "${AUTH_ERASURE_CONSUMERS:-}" "${AUTH_ERASURE_REQUIRED:-}" \
    "${COMPOSE_PROFILES:-}" >"$TEST_ROOT/compose-env"
fi
# Return no image list: clean-room stops before any build, pull or startup.
exit 92
EOF
  cat >"$tree/stubs/fixture-runtime" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >>"$TEST_ROOT/runtime-calls"
case "$1" in inspect|start) exit 0 ;; *) exit 93 ;; esac
EOF
  cat >"$tree/stubs/curl" <<'EOF'
#!/usr/bin/env bash
# Only the registry readiness request can be reached in these fixtures.
[[ "$*" == '-sf http://127.0.0.1:5000/v2/' ]]
EOF
  chmod +x "$tree/_mvp/scripts/"*.sh "$tree/stubs/"*
}

run_case() {
  local name="$1" fault="$2" expected="$3" profiles="$4" tree="$TMP/$1" rc=0
  local -a inherited=()
  shift 4
  make_tree "$tree"
  if [[ "$fault" == manifest ]]; then
    yq -i '.state."userdata-local".erasure = "invalid"' "$tree/_mvp/household-manifest.yaml"
  fi
  case "$name" in
    inherited-gates)
      inherited=(MVP_ENABLE_PLAYBACK_MONITOR=1 MVP_ENABLE_PLAYBACK_GUARD=true
        MVP_ENABLE_JELLYFIN=1 MVP_ENABLE_MEDIA_LIBRARY_MAINTAINER=true
        "COMPOSE_PROFILES=playback-monitor,playback-guard,jellyfin,library-maintainer") ;;
    optional)
      inherited=(MVP_ENABLE_PLAYBACK_MONITOR=0 MVP_ENABLE_PLAYBACK_GUARD=false
        MVP_ENABLE_JELLYFIN=0 MVP_ENABLE_MEDIA_LIBRARY_MAINTAINER=false) ;;
  esac
  env -i PATH="$tree/stubs:$PATH" \
    TEST_ROOT="$tree" TEST_FAULT="$fault" CONTAINER_RUNTIME=fixture-runtime \
    COMPOSE=fixture-compose CLEAN_ROOM_WORKDIR="$tree/work" \
    "${inherited[@]}" bash "$tree/scripts/clean-room.sh" --keep "$@" >"$tree/output" 2>&1 || rc=$?
  [[ "$rc" -ne 0 ]] || fail "$name: must stop before running a stack"
  if grep -q 'fixture-secret-must-not-be-logged' "$tree/output"; then
    fail "$name: enrollment secret was logged"
  fi
  [[ -f "$tree/generator-calls" ]] || fail "$name: erasure generator was not called"
  if [[ "$fault" == none ]]; then
    printf 'generate profiles=%s\ncheck profiles=%s\n' "$profiles" "$profiles" >"$tree/expected-calls"
    cmp -s "$tree/expected-calls" "$tree/generator-calls" || fail "$name: generation/check profiles differ"
    [[ -f "$tree/compose-env" ]] || fail "$name: did not reach compose config"
    printf '%s\n' "$expected" "$expected" "${profiles// /,}" >"$tree/expected-env"
    cmp -s "$tree/expected-env" "$tree/compose-env" || fail "$name: compose received the wrong consumer set or profiles"
    [[ "$(stat -c %a "$tree/work/clean-room.env")" == 600 ]] || fail "$name: env file is not private"
    grep -q 'compose config --images returned nothing' "$tree/output" || fail "$name: unexpected stop point"
  else
    [[ ! -f "$tree/compose-calls" && ! -f "$tree/runtime-calls" ]] || fail "$name: failure reached compose config/start or the runtime"
    if [[ "$fault" == manifest ]]; then
      grep -q 'gen-erasure-set.sh failed' "$tree/output" || fail "$name: missing generation failure"
      [[ "$(wc -l <"$tree/generator-calls")" == 1 ]] || fail "$name: checked after failed generation"
    else
      grep -q 'gen-erasure-set.sh --check failed' "$tree/output" || fail "$name: missing check failure"
      [[ "$(wc -l <"$tree/generator-calls")" == 2 ]] || fail "$name: did not check the generated file"
    fi
  fi
  echo "ok: $name"
}

BASE=admin-ui,media-ui,request-media,userdata-local
ALL=admin-ui,jellyfin,media-library-maintainer,media-ui,playback-guard,playback-monitor,request-media,userdata-local
run_case default none "$BASE" 'indexer-piratebay downloader-torrent'
run_case inherited-gates none "$BASE" 'indexer-piratebay downloader-torrent'
run_case optional none "$ALL" 'playback-monitor playback-guard jellyfin library-maintainer backup-local' \
  --profiles 'playback-monitor,playback-guard jellyfin library-maintainer' --restore-drill
run_case generation-failure manifest '' 'indexer-piratebay downloader-torrent'
run_case check-failure stale '' 'indexer-piratebay downloader-torrent'
echo 'clean-room_test: ok (offline orchestration only)'
