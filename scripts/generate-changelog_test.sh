#!/usr/bin/env bash
# Fixture tests for generate-changelog.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GEN="$SCRIPT_DIR/generate-changelog.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }

init_repo() {
  git -C "$TMP" init -q
  git -C "$TMP" config user.email test@example.com
  git -C "$TMP" config user.name "Changelog Test"
}

init_repo
git -C "$TMP" commit -q --allow-empty -m "chore: seed repository"
FROM="$(git -C "$TMP" rev-parse HEAD)"

# A first release includes its root commit, even before a second commit exists.
OUT="$(MUXCORE_ROOT="$TMP" bash "$GEN" --version v0.0.1)"
grep -Fq 'seed repository' <<<"$OUT" || fail "first release lost the root commit"

git -C "$TMP" tag v0.0.1
git -C "$TMP" commit -q --allow-empty -m "feat: add release automation"
git -C "$TMP" commit -q --allow-empty -m "fix(api): handle empty payload"
git -C "$TMP" tag train-2026.10.1
git -C "$TMP" commit -q --allow-empty -m "chore: bump submodule pins"
git -C "$TMP" commit -q --allow-empty -m "not a conventional commit"

OUT="$(MUXCORE_ROOT="$TMP" bash "$GEN" --from "$FROM" --to HEAD --version "v0.1.0")"

echo "$OUT" | grep -Fq "## [v0.1.0]" || fail "missing version heading"
echo "$OUT" | grep -Fq "### Added" || fail "missing Added section"
echo "$OUT" | grep -Fq "add release automation" || fail "missing feat entry"
echo "$OUT" | grep -Fq "### Fixed" || fail "missing Fixed section"
echo "$OUT" | grep -Fq "handle empty payload" || fail "missing fix entry"
echo "$OUT" | grep -Fq "### Changed" || fail "missing Changed section"
echo "$OUT" | grep -Fq "bump submodule pins" || fail "missing chore entry"
echo "$OUT" | grep -Fq "not a conventional commit" && fail "non-conventional commit should be skipped"

cat >"$TMP/CHANGELOG.md" <<'EOF'
# Changelog

Intro text.

## [Unreleased]

### Added
- pending work
EOF

MUXCORE_ROOT="$TMP" bash "$GEN" --from "$FROM" --to HEAD --version "v0.2.0" --update "$TMP/CHANGELOG.md"

grep -Fq "## [v0.2.0]" "$TMP/CHANGELOG.md" || fail "update did not insert version section"
grep -Fq "## [Unreleased]" "$TMP/CHANGELOG.md" || fail "update removed Unreleased section"
head -n 1 "$TMP/CHANGELOG.md" | grep -Fq "# Changelog" || fail "update corrupted header"

FIRST_VERSION_LINE="$(grep -n '^## \[v0.2.0\]' "$TMP/CHANGELOG.md" | cut -d: -f1)"
UNRELEASED_LINE="$(grep -n '^## \[Unreleased\]' "$TMP/CHANGELOG.md" | cut -d: -f1)"
if ((FIRST_VERSION_LINE >= UNRELEASED_LINE)); then
  fail "version section should appear before Unreleased"
fi

# Historical v* releases must not start at a newer train tag.
OUT="$(MUXCORE_ROOT="$TMP" bash "$GEN" --tag-pattern 'v*' --version v0.2.0)"
grep -Fq 'add release automation' <<<"$OUT" || fail "v* range selected a train tag"
grep -Fq 'seed repository' <<<"$OUT" && fail "v* range included the previous release"

# Invalid refs must fail before creating/truncating a requested artifact.
printf 'keep me\n' >"$TMP/notes.md"
if MUXCORE_ROOT="$TMP" bash "$GEN" --from missing-ref --output "$TMP/notes.md" >/dev/null 2>&1; then
  fail "invalid --from succeeded"
fi
grep -Fxq 'keep me' "$TMP/notes.md" || fail "invalid --from overwrote output"
if MUXCORE_ROOT="$TMP" bash "$GEN" --to missing-ref >/dev/null 2>&1; then
  fail "invalid --to succeeded"
fi
if MUXCORE_ROOT="$TMP" bash "$GEN" --from >/dev/null 2>&1; then
  fail "missing option value succeeded"
fi
TREE="$(git -C "$TMP" mktree </dev/null)"
UNRELATED="$(git -C "$TMP" commit-tree "$TREE" -m 'feat: unrelated history')"
if MUXCORE_ROOT="$TMP" bash "$GEN" --from "$UNRELATED" >/dev/null 2>&1; then
  fail "unrelated --from succeeded"
fi

echo "generate-changelog_test: ok"
