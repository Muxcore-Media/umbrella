#!/usr/bin/env bash
# Detect common protobuf field mismatches between canonical contracts and Go consumers.
#
# Checks:
#   - contracts-metadata GetMovieDetailsRequest / GetTVDetailsRequest use Id (not TmdbId)
#   - media-custom-formats UpsertReleaseProfileRequest.Enabled is *bool (not bare bool literals)
set -euo pipefail

ROOT="${MUXCORE_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
FAILED=0

GO_GLOBS=(
  --glob '*.go'
  --glob '!**/*.pb.go'
  --glob '!**/*_grpc.pb.go'
  --glob '!**/proto/**'
  --glob '!**/vendor/**'
)

die_match() {
  local rule="$1"
  shift
  echo "proto-drift [$rule]: $*" >&2
  FAILED=1
}

metadata_consumer_files() {
  rg -l 'github\.com/Muxcore-Media/contracts-metadata/muxcore/metadata/v1' \
    "$ROOT" "${GO_GLOBS[@]}" 2>/dev/null || true
}

check_metadata_id_field() {
  local file
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    while IFS= read -r line; do
      die_match "metadata-id" "$line"
    done < <(
      rg -n 'Get(Movie|TV)DetailsRequest\{[^}]*\bTmdbId\s*:' "$file" 2>/dev/null || true
    )
    while IFS= read -r line; do
      die_match "metadata-id" "$line"
    done < <(
      rg -n -U 'func \(.*\) Get(Movie|TV)Details\([^)]*Get(Movie|TV)DetailsRequest[^)]*\)[^{]*\{[\s\S]{0,1200}?\bGetTmdbId\(' \
        "$file" 2>/dev/null || true
    )
  done < <(metadata_consumer_files)
}

formats_consumer_files() {
  rg -l 'github\.com/Muxcore-Media/media-custom-formats/proto/formatsv1' \
    "$ROOT" "${GO_GLOBS[@]}" 2>/dev/null || true
}

check_release_profile_enabled_pointer() {
  local file
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    while IFS= read -r line; do
      die_match "release-profile-enabled" "$line"
    done < <(
      rg -n -U 'UpsertReleaseProfileRequest\{[\s\S]{0,1200}?\bEnabled:\s*(true|false)\b' \
        "$file" 2>/dev/null || true
    )
  done < <(formats_consumer_files)
}

check_metadata_id_field
check_release_profile_enabled_pointer

if ((FAILED)); then
  echo "proto-drift: fix the matches above (Id not TmdbId for contracts-metadata details requests;" \
    "UpsertReleaseProfileRequest.Enabled expects *bool, e.g. Enabled: &enabled)" >&2
  exit 1
fi

echo "proto-drift: ok"
