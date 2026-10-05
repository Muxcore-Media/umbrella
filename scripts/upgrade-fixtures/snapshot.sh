#!/usr/bin/env bash
# Produce an upgrade snapshot for a module (ADR-0015, roadmap T-M2-06).
#   usage: scripts/upgrade-fixtures/snapshot.sh <module-dir> <ref> <store-pkg-dir> [label]
#
# <ref> is a tag or commit of the module; <store-pkg-dir> is the package (relative
# to the module root) whose testdata/upgrade/ holds the seed source:
#   seed_upgrade_test.<label>.go.txt, or seed_upgrade_test.go.txt as a fallback.
# The seed is a `//go:build upgradeseed` test named TestUpgradeSeed that writes a
# database to $UPGRADE_SEED_DB using that version's own store code. Output:
#   <store-pkg-dir>/testdata/upgrade/<label>.db and <label>.schema.sql
# <label> defaults to <ref>. Needs git, go and sqlite3. The module's own go.mod and
# go.sum at <ref> are used unchanged (set GOFLAGS/GOSUMDB in the environment if an
# old ref's checksums no longer verify).
set -euo pipefail

[[ $# -ge 3 ]] || { sed -n '2,13p' "$0" >&2; exit 2; }
mod=$(cd "$1" && pwd); ref=$2; pkg=${3%/}; label=${4:-$2}
for tool in git go sqlite3; do command -v "$tool" >/dev/null || { echo "snapshot: $tool not found" >&2; exit 2; }; done

dest="$mod/$pkg/testdata/upgrade"
seed="$dest/seed_upgrade_test.$label.go.txt"
[[ -f "$seed" ]] || seed="$dest/seed_upgrade_test.go.txt"
[[ -f "$seed" ]] || { echo "snapshot: no seed source in $dest" >&2; exit 1; }

work=$(mktemp -d)
cleanup() { git -C "$mod" worktree remove --force "$work/src" >/dev/null 2>&1 || true; rm -rf "$work"; }
trap cleanup EXIT

git -C "$mod" worktree add --detach "$work/src" "$ref" >/dev/null
cp "$seed" "$work/src/$pkg/zz_seed_upgrade_test.go"
(cd "$work/src" && UPGRADE_SEED_DB="$work/seed.db" GOWORK=off go test -count=1 -tags upgradeseed -run '^TestUpgradeSeed$' "./$pkg/")
[[ -f "$work/seed.db" ]] || { echo "snapshot: seed test wrote no database" >&2; exit 1; }

sqlite3 "$work/seed.db" 'PRAGMA journal_mode=DELETE; VACUUM;' >/dev/null
mkdir -p "$dest"
cp "$work/seed.db" "$dest/$label.db"
sqlite3 "$dest/$label.db" .schema > "$dest/$label.schema.sql"
echo "snapshot: wrote $dest/$label.db ($(wc -c < "$dest/$label.db") bytes)"
