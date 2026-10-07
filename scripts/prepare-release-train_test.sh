#!/usr/bin/env bash
# Offline Git fixtures for train notes, immutable tags and committed pins.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PREPARE="$SCRIPT_DIR/prepare-release-train.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
REPO="$TMP/repo"
mkdir -p "$REPO"
git -C "$REPO" init -q -b main
git -C "$REPO" config user.name 'Release Train Test'
git -C "$REPO" config user.email test@example.com

fail() { echo "FAIL: $*" >&2; exit 1; }
prepare() { MUXCORE_ROOT="$REPO" bash "$PREPARE" "$@"; }
commit() { git -C "$REPO" add release-train.env; git -C "$REPO" commit -q --allow-empty -m "$1"; }
expect_bad() {
  local expected="$1" out
  shift
  if out="$(prepare "$@" 2>&1)"; then fail "expected failure: $expected"; fi
  grep -Fq -- "$expected" <<<"$out" || fail "missing error '$expected': $out"
}

cat >"$REPO/release-train.env" <<'EOF'
# A committed release train; never executable code.
MUXCORE_TRAIN=train-2026.10.1
CORE_TAG=v0.6.15
MUXCORE_IMAGE_TAG=v0.6.15
SPOOL_CATALOG_VERSION=2.7.0
EOF
commit 'feat: first release capability'
SEED="$(git -C "$REPO" rev-parse HEAD)"
git -C "$REPO" update-index --add --cacheinfo "160000,$SEED,core"
commit 'chore: pin core'
FIRST="$(git -C "$REPO" rev-parse HEAD)"

OUT="$(prepare)"
grep -Fq 'first release capability' <<<"$OUT" || fail "first train omitted root commit"
grep -Fq "| \`core\` | \`$SEED\` |" <<<"$OUT" || fail "missing committed gitlink"
grep -Fq "| Umbrella commit | \`$FIRST\` |" <<<"$OUT" || fail "missing umbrella commit"
# shellcheck disable=SC2016 # Literal Markdown backticks.
grep -Fq '| Core | `v0.6.15` |' <<<"$OUT" || fail "missing core pin"
# shellcheck disable=SC2016
grep -Fq '| Spool catalog | `2.7.0` |' <<<"$OUT" || fail "missing spool pin"

# Dirty manifest and staged gitlinks do not change notes or get modified.
printf 'invalid local manifest\n' >"$REPO/release-train.env"
git -C "$REPO" update-index --cacheinfo "160000,$FIRST,core"
BEFORE="$(git -C "$REPO" status --porcelain)"
[[ "$(prepare)" == "$OUT" ]] || fail "working-tree data changed committed notes"
[[ "$(git -C "$REPO" status --porcelain)" == "$BEFORE" ]] || fail "prepare modified the checkout"
git -C "$REPO" reset -q --hard HEAD

git -C "$REPO" tag -a train-2026.10.1 -m 'First fixture train'
sed -i 's/train-2026.10.1/train-2026.10.2/' "$REPO/release-train.env"
commit 'fix: repair playback before semver tag'
SECOND_BASE="$(git -C "$REPO" rev-parse HEAD)"
git -C "$REPO" tag v9.0.0
commit 'feat: second train capability'

# A train on a merged branch must not define the mainline release boundary.
git -C "$REPO" checkout -q -b feature "$FIRST"
commit 'feat: merged capability'
git -C "$REPO" tag train-2026.10.99
git -C "$REPO" checkout -q main
git -C "$REPO" merge -q --no-ff feature -m 'chore: merge feature'

OUT="$(prepare --tag train-2026.10.2)"
grep -Fq '## [train-2026.10.2]' <<<"$OUT" || fail "wrong train heading"
grep -Fq 'repair playback before semver tag' <<<"$OUT" || fail "semver tag truncated train range"
grep -Fq 'second train capability' <<<"$OUT" || fail "missing new capability"
grep -Fq 'merged capability' <<<"$OUT" || fail "missing merged capability"
grep -Fq 'first release capability' <<<"$OUT" && fail "included previous train"
prepare --output "$TMP/notes.md"
[[ "$(cat "$TMP/notes.md")" == "$OUT" ]] || fail "file output differs from stdout"

printf 'keep me\n' >"$TMP/notes.md"
expect_bad 'does not match committed' --tag train-2026.10.3 --output "$TMP/notes.md"
grep -Fxq 'keep me' "$TMP/notes.md" || fail "failed validation overwrote notes"
expect_bad 'invalid --ref' --ref missing-ref
expect_bad 'requires a value' --ref

git -C "$REPO" tag train-2026.10.2 "$SECOND_BASE"
expect_bad 'already names another commit'
git -C "$REPO" tag -d train-2026.10.2 >/dev/null
git -C "$REPO" tag -a train-2026.10.2 -m 'Second fixture train'
[[ "$(prepare --ref train-2026.10.2 --tag train-2026.10.2)" == "$OUT" ]] || fail "tagged notes differ"

# Missing, duplicated or malformed manifest data cannot produce a release.
GOOD="$(git -C "$REPO" rev-parse HEAD)"
check_manifest() {
  commit 'test: invalid manifest fixture'
  expect_bad "$1"
  git -C "$REPO" reset -q --hard "$GOOD"
}
sed -i '/^CORE_TAG=/d' "$REPO/release-train.env"
check_manifest 'missing CORE_TAG'
printf 'CORE_TAG=v0.6.15\n' >>"$REPO/release-train.env"
check_manifest 'duplicate CORE_TAG'
sed -i 's/train-2026.10.2/train-2026.13.2/' "$REPO/release-train.env"
check_manifest 'MUXCORE_TRAIN must be'
sed -i 's/MUXCORE_IMAGE_TAG=v0.6.15/MUXCORE_IMAGE_TAG=v0.6.14/' "$REPO/release-train.env"
check_manifest 'CORE_TAG and MUXCORE_IMAGE_TAG must agree'
# shellcheck disable=SC2016 # Intentionally hostile data; must never execute.
printf 'CORE_TAG=$(touch %s)\n' "$TMP/executed" >>"$REPO/release-train.env"
check_manifest 'invalid release-train.env assignment'
[[ ! -e "$TMP/executed" ]] || fail "manifest was executed"

git clone -q --depth 1 "file://$REPO" "$TMP/shallow"
if MUXCORE_ROOT="$TMP/shallow" bash "$PREPARE" >"$TMP/shallow.out" 2>&1; then
  fail "shallow history accepted"
fi
grep -Fq 'full Git history is required' "$TMP/shallow.out" || fail "missing shallow-history diagnostic"

echo 'prepare-release-train_test: ok'
