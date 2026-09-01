#!/usr/bin/env bash
# Fixture tests for check-proto-drift.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK="$SCRIPT_DIR/check-proto-drift.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }

mkdir -p "$TMP/good" "$TMP/bad-metadata" "$TMP/bad-formats"
cat >"$TMP/good/ok.go" <<'EOF'
package good

import metadatav1 "github.com/Muxcore-Media/contracts-metadata/muxcore/metadata/v1"

func movie(req *metadatav1.GetMovieDetailsRequest) int32 {
	return req.GetId()
}
EOF

cat >"$TMP/bad-metadata/wrong.go" <<'EOF'
package badmetadata

import metadatav1 "github.com/Muxcore-Media/contracts-metadata/muxcore/metadata/v1"

func bad() *metadatav1.GetMovieDetailsRequest {
	return &metadatav1.GetMovieDetailsRequest{TmdbId: 1}
}
EOF

cat >"$TMP/bad-formats/wrong.go" <<'EOF'
package badformats

import formatsv1 "github.com/Muxcore-Media/media-custom-formats/proto/formatsv1"

func bad() *formatsv1.UpsertReleaseProfileRequest {
	return &formatsv1.UpsertReleaseProfileRequest{Name: "x", Enabled: true}
}
EOF

if MUXCORE_ROOT="$TMP" bash "$CHECK" >/dev/null 2>&1; then
  fail "expected failure for bad fixtures"
fi

rm -rf "$TMP/bad-metadata" "$TMP/bad-formats"
if ! MUXCORE_ROOT="$TMP" bash "$CHECK" >/dev/null; then
  fail "expected success when only good fixtures remain"
fi

echo "check-proto-drift_test: ok"
