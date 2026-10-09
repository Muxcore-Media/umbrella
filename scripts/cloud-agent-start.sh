#!/usr/bin/env bash
# Cloud Agent start: bring up the MuxCore MVP host stack and bootstrap admin
# auth. Idempotent and safe to run on every boot. Returns once the mesh is
# healthy (does not block in the foreground).
set -uo pipefail

WS="$(cd "$(dirname "$0")/.." && pwd)"
MVP="$WS/_mvp"

export GOTOOLCHAIN=go1.26.6 GOFLAGS=-mod=mod GOPRIVATE='github.com/Muxcore-Media/*'
export MUXCORE_INSECURE_DISABLE_TLS=true

cd "$MVP" || exit 1

echo "==> starting MuxCore host stack (run-host.sh up)"
./run-host.sh up || true

echo "==> waiting for core /health"
for _ in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/health 2>/dev/null || echo 000)
  [ "$code" = "200" ] && break
  sleep 1
done
echo "    core /health = ${code:-000}"

echo "==> bootstrapping admin auth (idempotent)"
./bootstrap-auth.sh || true

echo "==> MuxCore stack ready:"
echo "    admin-ui : http://localhost:8082  (login admin / admin-dev-only)"
echo "    auth     : http://localhost:9401/login"
echo "    core     : http://localhost:8080/health"
echo "    api-rest : http://localhost:18080/api/v1/health"
