#!/usr/bin/env bash
# Fixture tests for check-default-creds.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK="$SCRIPT_DIR/check-default-creds.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }

# new_tree <dir>: empty git repo with a clean file.
new_tree() {
  mkdir -p "$1"
  git -C "$1" init -q
  git -C "$1" config user.email t@example.invalid
  git -C "$1" config user.name t
  echo 'PORT=8080' >"$1/clean.env"
}

# run <dir> [allowfile]: sets OUT and RC.
run() {
  git -C "$1" add -A 2>/dev/null
  RC=0
  OUT="$(MUXCORE_ROOT="$1" CHECK_DEFAULT_CREDS_ALLOW="${2:-/nonexistent}" bash "$CHECK" 2>&1)" || RC=$?
}

# expect_hit <label> <relative file> <content line>
expect_hit() {
  local label="$1" file="$2" line="$3" t="$TMP/$1"
  new_tree "$t"
  mkdir -p "$(dirname "$t/$file")"
  printf '%s\n' "$line" >"$t/$file"
  run "$t"
  [[ $RC -eq 1 ]] || fail "$label: expected rc=1, got $RC ($OUT)"
  grep -q "^$file:1:" <<<"$OUT" || fail "$label: hit not listed: $OUT"
}

# expect_clean <label> <relative file> <content line>
expect_clean() {
  local label="$1" file="$2" line="$3" t="$TMP/$1"
  new_tree "$t"
  mkdir -p "$(dirname "$t/$file")"
  printf '%s\n' "$line" >"$t/$file"
  run "$t"
  [[ $RC -eq 0 ]] || fail "$label: expected rc=0, got $RC ($OUT)"
  grep -q 'check-default-creds: ok' <<<"$OUT" || fail "$label: missing ok line"
}

# shellcheck disable=SC2016 # literal ${...} fixtures
{
  expect_hit literal-admin-dev-only x/.env.example 'MVP_ADMIN_PASSWORD=admin-dev-only'
  expect_hit literal-minioadmin s3.yml '      S3_SECRET_KEY: minioadmin'
  expect_hit literal-household-monitor a.sh 'T=household-monitor'
  expect_hit changeme-lower README.md 'password = changeme'
  expect_hit change-me-dash README.md "--from-literal=pw='change-me'"
  expect_hit change-me-upper values.yaml 'pw: CHANGE_ME'
  expect_hit yaml-password-admin docker-compose.yml '      password: admin'
  expect_hit env-password-admin run.sh 'POSTGRES_PASSWORD=admin'
  expect_hit shell-default-admin run.sh 'PASS="${MVP_ADMIN_PASSWORD:-admin}"'
  expect_hit compose-token-default docker-compose.yml '      X: ${PLAYBACK_MONITOR_HTTP_TOKEN:-hunter2}'
  expect_hit compose-password-default docker-compose.yml '      X: ${DB_PASSWORD:-s3cret}'
  expect_hit compose-secret-default docker-compose.yml '      X: ${APP_SECRET:-abc}'
  expect_hit compose-key-default docker-compose.yml '      X: ${API_KEY:-abc}'

  # Compliant forms.
  expect_clean empty-default docker-compose.yml '      X: ${API_TOKEN:-}'
  expect_clean required-var docker-compose.yml '      X: ${API_TOKEN:?set API_TOKEN (openssl rand -hex 32)}'
  expect_clean nested-default run.sh 'T="${A_TOKEN:-${B_TOKEN:-}}"'
  expect_clean var-default run.sh 'T="${A_TOKEN:-$B_TOKEN}"'
  expect_clean non-secret-default docker-compose.yml '      X: ${HTTP_PORT:-8080} ${MVP_ADMIN_USER:-admin} ${TOKEN_FILE:-/run/t}'
  expect_clean generated-secret-doc README.md 'kubectl create secret --from-literal=pw="$(openssl rand -hex 16)"'
  expect_clean exchange-word README.md 'exchange-media and passwordless'
}

# Skipped paths.
new_tree "$TMP/skips"
for f in docs/history/a.md docs/adr/0001-x.md pkg/a_test.go pkg/testdata/x.env node_modules/m/i.js; do
  mkdir -p "$(dirname "$TMP/skips/$f")"
  echo 'PASSWORD=admin-dev-only' >"$TMP/skips/$f"
done
run "$TMP/skips"
[[ $RC -eq 0 ]] || fail "skips: skipped paths were scanned: $OUT"
# ...but a non-adr doc and non-test go file are scanned.
mkdir -p "$TMP/skips/docs/adr" "$TMP/skips/docs/guide"
echo 'x=admin-dev-only' >"$TMP/skips/docs/guide/a.md"
run "$TMP/skips"
[[ $RC -eq 1 ]] || fail "skips: docs/guide should be scanned"

# Archived submodules are skipped; active submodules are scanned with a prefix.
new_tree "$TMP/subs"
for m in media-ui modx; do
  new_tree "$TMP/subs/$m"
  echo 'P=admin-dev-only' >"$TMP/subs/$m/.env"
  git -C "$TMP/subs/$m" add -A
  git -C "$TMP/subs/$m" commit -qm fixture
done
cat >"$TMP/subs/.gitmodules" <<'EOT'
[submodule "media-ui"]
	path = media-ui
	url = https://github.com/Muxcore-Media/media-ui.git
[submodule "modx"]
	path = modx
	url = https://github.com/Muxcore-Media/modx.git
EOT
run "$TMP/subs"
[[ $RC -eq 1 ]] || fail "subs: expected rc=1"
grep -q '^modx/\.env:1:' <<<"$OUT" || fail "subs: active submodule hit missing prefix: $OUT"
grep -q '^media-ui/' <<<"$OUT" && fail "subs: archived submodule scanned"

# Allowlist: matching path+pattern suppresses; other path/pattern does not.
new_tree "$TMP/allow"
echo 'PASSWORD=changeme' >"$TMP/allow/fixture.env"
echo 'PASSWORD=changeme' >"$TMP/allow/other.env"
cat >"$TMP/allow.list" <<'EOT'
# comment
fixture.env:PASSWORD=changeme  # test fixture
EOT
run "$TMP/allow" "$TMP/allow.list"
[[ $RC -eq 1 ]] || fail "allow: other.env must still fail"
grep -q '^other.env:1:' <<<"$OUT" || fail "allow: other.env not listed"
grep -q '^fixture.env' <<<"$OUT" && fail "allow: fixture.env should be allowed"
cat >"$TMP/allow.list" <<'EOT'
*.env:PASSWORD=changeme  # globbed
EOT
run "$TMP/allow" "$TMP/allow.list"
[[ $RC -eq 0 ]] || fail "allow: glob entry should allow both: $OUT"
printf 'no-colon-entry\n' >"$TMP/allow.list"
run "$TMP/allow" "$TMP/allow.list"
[[ $RC -eq 2 ]] || fail "allow: malformed entry must exit 2 (got $RC)"

echo "OK check-default-creds tests"
