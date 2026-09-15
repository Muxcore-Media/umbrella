#!/usr/bin/env bash
# Cloud Agent install for the MuxCore umbrella dev environment.
#
# Brings up a working local MuxCore MVP host stack (the `_mvp/run-host.sh`
# mesh) from the public GitHub mirror. The public mirror is only partially
# coherent: some module HEADs/pins do not compile against each other and a
# few repos are empty. This script therefore builds each module at the
# tested "wave" release tag documented in _mvp/README.md, trusting the
# mirror's archive contents (regenerated go.sum), which is the combination
# that actually compiles and interoperates over the mesh.
#
# Idempotent: re-running skips already-built binaries and only refreshes
# what is missing. Safe to run repeatedly.
set -uo pipefail

WS="$(cd "$(dirname "$0")/.." && pwd)"
MVP="$WS/_mvp"
BIN="$MVP/bin"
mkdir -p "$BIN"

log() { echo "==> $*"; }

# ---------------------------------------------------------------------------
# 1. Go toolchain / module resolution config
# ---------------------------------------------------------------------------
# core requires `go 1.26.x`; the host ships go1.22 which auto-fetches the
# newer toolchain via GOTOOLCHAIN. GOPRIVATE + default GOSUMDB let go fetch
# the private Muxcore-Media modules from GitHub (auth via the global
# url.insteadOf token rewrite) while still verifying the public toolchain.
export GOTOOLCHAIN=go1.26.6
export GOFLAGS=-mod=mod
export GOPRIVATE='github.com/Muxcore-Media/*'
go env -w GOTOOLCHAIN=go1.26.6 GOFLAGS=-mod=mod GOPRIVATE='github.com/Muxcore-Media/*' GOSUMDB=sum.golang.org 2>/dev/null || true

CORE_MODS=(core core/pkg/contracts core/sdk/go/client core/sdk/go/module core/sdk/go/mock)

# ---------------------------------------------------------------------------
# 2. Initialise submodules (tolerate the mirror's stale pins / empty repos)
# ---------------------------------------------------------------------------
init_submodules() {
  log "initialising submodules (best effort; some mirror pins are stale)"
  cd "$WS"
  # Bulk attempt first (fast path). Never fatal.
  git submodule update --init --jobs 8 >/dev/null 2>&1 || true
  # Per-module retry for any left empty; fall back to the remote default branch
  local d n
  for d in $(git submodule status | awk '{print $2}'); do
    n=$(ls -A "$d" 2>/dev/null | grep -v '^\.git$' | wc -l)
    [ "$n" -ne 0 ] && continue
    git submodule update --init --force "$d" >/dev/null 2>&1 && continue
    # stale pin: take the remote default branch instead
    ( cd "$d" 2>/dev/null && git fetch origin >/dev/null 2>&1 \
        && br=$(git remote show origin 2>/dev/null | sed -n 's/.*HEAD branch: //p') \
        && [ -n "$br" ] && git checkout -f "origin/$br" >/dev/null 2>&1 ) || \
        log "  (skip $d: not available on mirror)"
  done
}

# add sibling core-family replaces + drop the folded-in pkg/tenant|log
apply_core_siblings() {
  local c
  for c in "${CORE_MODS[@]}"; do go mod edit -replace="github.com/Muxcore-Media/$c=$WS/$c"; done
  go mod edit -dropreplace=github.com/Muxcore-Media/core/pkg/tenant -droprequire=github.com/Muxcore-Media/core/pkg/tenant 2>/dev/null || true
  go mod edit -dropreplace=github.com/Muxcore-Media/core/pkg/log -droprequire=github.com/Muxcore-Media/core/pkg/log 2>/dev/null || true
}

# replace every populated Muxcore-Media sibling; drop refs to empty repos.
# Operates on the go.mod in the current directory.
apply_all_siblings() {
  [ -f go.mod ] || { log "  (apply_all_siblings: no go.mod in $PWD)"; return 1; }
  local p sub
  for p in $(grep -oE 'github\.com/Muxcore-Media/[A-Za-z0-9._/-]+' go.mod | sed -E 's#\s.*##' | sort -u); do
    grep -qE "^module[[:space:]]+$p" go.mod && continue
    sub="${p#github.com/Muxcore-Media/}"
    if [ -f "$WS/$sub/go.mod" ]; then go mod edit -replace="$p=$WS/$sub"
    else go mod edit -dropreplace="$p" -droprequire="$p" 2>/dev/null || true; fi
  done
}

# build_mod <module-dir> <cmd-path> <space-separated-tag-candidates> [core|all|pinned]
build_mod() {
  local m="$1" cmd="$2" tags="$3" mode="${4:-pinned}"
  local d="$WS/$m" out="$BIN/${m##*/}"
  [ "$m" = core ] && out="$BIN/muxcored"
  if [ -x "$out" ]; then log "skip $m (already built)"; return 0; fi
  [ -d "$d" ] || { log "MISS $m (no checkout)"; return 1; }
  ( cd "$d" && git fetch origin --tags -q >/dev/null 2>&1 || true )
  local t
  for t in $tags; do
    ( cd "$d" && git checkout -f -q "$t" 2>/dev/null ) || continue
    ( cd "$d" && rm -f go.sum
      case "$mode" in core) apply_core_siblings;; all) apply_all_siblings;; esac )
    if ( cd "$d" && go build -ldflags="-s -w" -o "$out" "$cmd" ) >/tmp/cai-$m.log 2>&1; then
      log "built $m @ $t -> $(basename "$out")"; return 0
    fi
  done
  log "FAIL $m (see /tmp/cai-$m.log)"; return 1
}

# ---------------------------------------------------------------------------
# 3. Build the tested MVP wave (README wave pins)
# ---------------------------------------------------------------------------
build_stack() {
  build_mod core                 ./cmd/muxcored v0.5.7
  build_mod auth-local           ./cmd/module   "v0.1.1 v0.1.0"          # v0.1.1 predates unreleased invite RPCs
  build_mod api-rest             ./cmd/module   "v0.1.3 v0.1.2 v0.1.1"
  build_mod database-sqlite      ./cmd/module   "v0.1.3 v0.1.2 v0.1.1"
  build_mod secrets-file         ./cmd/module   "v0.1.3 v0.1.2 v0.1.1"
  build_mod encryption-aesgcm    ./cmd/module   "v0.1.3 v0.1.2"          # mandates mTLS; won't join insecure dev mesh
  build_mod call-policy-default  ./cmd/module   "v0.1.2 v0.1.1"
  build_mod publish-policy-default ./cmd/module "v0.1.2 v0.1.1"
  build_mod ratelimit-tokenbucket ./cmd/module  "v0.1.2 v0.1.1"
  build_mod health-monitor       ./cmd/module   "v0.1.3 v0.1.2"
  build_mod metadata-tmdb        ./cmd/module   "v0.1.1 v0.1.2"
  build_mod media-root-folders   ./cmd/module   "v0.1.1 v0.1.2"
  build_mod request-media        ./cmd/module   "v0.2.2 v0.2.1"
  build_mod notification-default ./cmd/module   "v0.1.0 v0.1.1"
  build_mod media-movies         ./cmd/module   "v0.1.1 v0.1.2"
  build_mod media-tvshows        ./cmd/module   "v0.1.1 v0.1.2"
  build_mod media-automation     ./cmd/module   "v0.1.0 v0.1.1"
  build_mod media-scanner        ./cmd/module   "v0.1.1 v0.1.2"
  build_mod media-custom-formats ./cmd/module   "v0.1.2 v0.1.1"
  build_mod media-rename         ./cmd/module   "v0.2.2 v0.2.1"
  build_mod media-ffprobe        ./cmd/module   "v0.1.2 v0.1.1"
  build_mod media-subtitles      ./cmd/module   "v0.4.2 v0.4.1"
  build_mod media-intro-outro    ./cmd/module   "v0.1.0 v0.1.1"
  build_mod media-transcoder     ./cmd/module   "v0.1.0 v0.1.1"
  build_mod admin-ui             .              "v0.1.3 v0.1.4"
  build_mod jellyfin             ./cmd/module   "v0.2.1 v0.2.0"
  # userdata-local: only builds against the sibling core (no matching mirror tag)
  build_mod userdata-local       ./cmd/module   "$(cd "$WS/userdata-local" && git rev-parse HEAD 2>/dev/null)" core
}

# ---------------------------------------------------------------------------
# 4. Stubs for always-on modules whose GitHub mirror repo is empty
#    (run-host.sh builds these inline under `set -e`; a stub skips that build
#     without joining the mesh, so the rest of the stack still comes up)
# ---------------------------------------------------------------------------
build_stubs() {
  local stub_src=/tmp/muxcore-stub
  mkdir -p "$stub_src"
  cat > "$stub_src/main.go" <<'GO'
package main
import "time"
func main() { for { time.Sleep(time.Hour) } }
GO
  ( cd "$stub_src" && go mod init stub >/dev/null 2>&1 || true; go build -o "$BIN/_stub" . )
  local s
  for s in metadata-musicbrainz media-music; do
    [ -x "$BIN/$s" ] || cp "$BIN/_stub" "$BIN/$s"
  done
  log "stub placed for: metadata-musicbrainz media-music (empty mirror repos)"
}

# ---------------------------------------------------------------------------
# 5. mvp-smoke helpers (mesh CLIs + auth token minting). These live in the
#    _mvp (mvp-smoke) module and need sibling replaces across its go.mod.
#    mediauiprox (consumer SPA BFF) is intentionally NOT built here: its
#    master needs media-subtitles master APIs that are not published as the
#    wave tag, and it also panics under Go 1.26's stricter net/http mux.
#    The consumer SPA is disabled in .env accordingly.
# ---------------------------------------------------------------------------
build_mvp_helpers() {
  if [ ! -f "$MVP/go.mod" ]; then log "WARN _mvp not populated; skipping helpers"; return 0; fi
  ( cd "$MVP" && { git checkout -- go.mod go.sum 2>/dev/null || true; }; rm -f go.sum; apply_all_siblings )
  local h
  for h in gettoken listmodules addmovie addtvshow importscan listall; do
    [ -x "$BIN/$h" ] && continue
    ( cd "$MVP" && go build -o "$BIN/$h" "./cmd/$h" ) >/tmp/cai-mvp-$h.log 2>&1 \
      && log "built helper $h" || log "FAIL helper $h (see /tmp/cai-mvp-$h.log)"
  done
  # authctl lives in auth-local (built at the same tag as the module)
  if [ ! -x "$BIN/authctl" ]; then
    ( cd "$WS/auth-local" && rm -f go.sum && apply_core_siblings && go build -o "$BIN/authctl" ./cmd/authctl ) \
      >/tmp/cai-authctl.log 2>&1 && log "built helper authctl" || log "FAIL helper authctl (see /tmp/cai-authctl.log)"
  fi
}

# ---------------------------------------------------------------------------
# 6. Consumer SPA (media-ui-app). master fails `tsc --noEmit`; use a
#    transpile-only vite build to produce a serveable bundle for dev.
# ---------------------------------------------------------------------------
build_spa() {
  local app="$WS/media-ui-app"
  [ -d "$app" ] || { log "skip SPA (no checkout)"; return 0; }
  [ -d "$app/dist-app" ] && { log "skip SPA (dist-app exists)"; return 0; }
  ( cd "$app" && (npm ci >/tmp/cai-spa.log 2>&1 || npm install >/tmp/cai-spa.log 2>&1) \
      && npx vite build >>/tmp/cai-spa.log 2>&1 ) \
    && log "built SPA -> media-ui-app/dist-app" || log "SPA build skipped (see /tmp/cai-spa.log)"
}

# ---------------------------------------------------------------------------
# 7. Local dev .env (fixtures, admin creds, localhost auth URLs)
# ---------------------------------------------------------------------------
write_env() {
  local envf="$MVP/.env"
  [ -f "$envf" ] && { log ".env already present (kept)"; return 0; }
  cat > "$envf" <<'ENV'
TMDB_FIXTURE=1
TMDB_API_KEY=
MVP_ADMIN_USER=admin
MVP_ADMIN_PASSWORD=admin-dev-only
MUXCORE_INSECURE_DISABLE_TLS=true
# Consumer SPA BFF (mediauiprox) master needs unpublished dep versions and
# panics under Go 1.26's stricter net/http mux; disable it. The operator
# admin-ui remains the primary web app.
MVP_ENABLE_MEDIA_UI=0
# playback-monitor depends on the empty playback-contract mirror repo
MVP_ENABLE_PLAYBACK_MONITOR=0
# browser-reachable auth/admin URLs (no Caddy/edge in this VM)
ADMIN_UI_AUTH_ADDR=http://localhost:9401
ADMIN_UI_PUBLIC_URL=http://localhost:8082
AUTH_HTTP_URL=http://localhost:9401
MEDIA_UI_PUBLIC_URL=http://localhost:5173
AUTH_ALLOWED_REDIRECT_HOSTS=localhost:8082,localhost:5173,127.0.0.1:8082,127.0.0.1:5173
ENV
  log "wrote $envf"
}

main() {
  init_submodules
  build_stack
  build_stubs
  build_mvp_helpers
  build_spa
  write_env
  log "install complete. Start with: cd _mvp && ./run-host.sh up && ./bootstrap-auth.sh"
}
main "$@"
