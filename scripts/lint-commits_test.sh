#!/usr/bin/env bash
# Fixture tests for lint-commits.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LINT="$SCRIPT_DIR/lint-commits.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }

init_repo() {
  git -C "$TMP" init -q
  git -C "$TMP" config user.email test@example.com
  git -C "$TMP" config user.name "Commit Lint Test"
}

init_repo
git -C "$TMP" commit --allow-empty -m "chore: initial commit"
BASE="$(git -C "$TMP" rev-parse HEAD)"
git -C "$TMP" commit --allow-empty -m "feat: valid feature"
git -C "$TMP" commit --allow-empty -m "fix!: valid breaking fix"

if ! MUXCORE_ROOT="$TMP" bash "$LINT" "$BASE" HEAD; then
  fail "expected valid commits to pass"
fi

git -C "$TMP" commit --allow-empty -m "bad commit message"

if MUXCORE_ROOT="$TMP" bash "$LINT" "$BASE" HEAD >/dev/null 2>&1; then
  fail "expected invalid commit to fail"
fi

echo "lint-commits_test: ok"
