#!/usr/bin/env bash
# Clean-room household install + fixture smoke (roadmap T-M2-10, SM-1, ADR-0014).
#
# Reproduces the release gate locally (podman) and in CI (docker,
# .github/workflows/clean-room.yml):
#   1. start an ephemeral registry:2 on 127.0.0.1:${CLEAN_ROOM_REGISTRY_PORT:-5000}
#   2. build module binaries once on the host (workspace mode, ADR-0012 flags:
#      _mvp/scripts/build-module-binaries.sh), package + push every image the
#      selected compose profiles need at MUXCORE_IMAGE_TAG (release-train.env);
#      muxcored is built from core/Dockerfile (publish-muxcored-local.sh)
#   3. from the umbrella root: compose pull + `up -d --wait` with generated
#      secrets (temp env file, 0600, never committed) in the household security
#      profile (ADR-0016): mesh enrollment secret + per-module tokens from
#      _mvp/scripts/gen-enrollment.sh (ADR-0017); every module must come up with
#      a verified certificate
#   4. MUXCORE_SMOKE_REGISTRY=1 SMOKE_REQUIRE_ACQUISITION=1 _mvp/smoke.sh with the
#      fixture acquisition profiles (ADR-0008: fixture indexer + fixture engine only);
#      the smoke enrolls its own client identity (mvp-smoke) for mesh TLS
#   5. optional: _mvp/scripts/restore-drill.sh --mode compose
#   6. idle RSS of the stack (NFR-PERF-003, report; enforce with --enforce-rss)
#   7. compose logs on failure; `down -v`, registry removal at the end
#
# Usage:
#   scripts/clean-room.sh [--profiles "indexer-piratebay downloader-torrent"]
#                         [--restore-drill] [--enforce-rss[=MiB]] [--keep]
#                         [--skip-build] [--log-dir DIR] [--dev]
#
#   --dev   run the insecure dev profile instead (adds _mvp/docker-compose.dev.yml)
#
# Env (all optional):
#   CONTAINER_RUNTIME        docker | podman (default: docker if on PATH, else podman)
#   COMPOSE                  compose command line (default: "docker compose")
#   DOCKER_HOST              for podman: the podman API socket compose talks to
#   CLEAN_ROOM_PROFILES      compose profiles (default: indexer-piratebay downloader-torrent)
#   CLEAN_ROOM_REGISTRY_PORT registry host port (default 5000)
#   CLEAN_ROOM_WORKDIR       work dir: binaries, env file, logs (default: mktemp)
#   CLEAN_ROOM_LOG_DIR       where logs/reports go (default: <workdir>/logs)
#   CLEAN_ROOM_RSS_LIMIT_MIB idle RSS budget for --enforce-rss (default 2048, NFR-PERF-003)
#   CLEAN_ROOM_SETTLE_SEC    idle wait before measuring RSS (default 20)
#   CLEAN_ROOM_PRUNE_IMAGES  1 = remove the images this run built at the end
#   BUILD_JOBS / BUILD_GO_P  host build parallelism (build-module-binaries.sh; default 2 / 2)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVP="$ROOT/_mvp"

PROFILES="${CLEAN_ROOM_PROFILES:-indexer-piratebay downloader-torrent}"
RESTORE_DRILL=0
ENFORCE_RSS=0
RSS_LIMIT="${CLEAN_ROOM_RSS_LIMIT_MIB:-2048}"
KEEP=0
SKIP_BUILD=0
LOG_DIR="${CLEAN_ROOM_LOG_DIR:-}"
DEV=0
REG_PORT="${CLEAN_ROOM_REGISTRY_PORT:-5000}"
REG_NAME="muxcore-cleanroom-registry"
REG_IMAGE="docker.io/library/registry:2"

usage() { sed -n '2,/^set -euo/p' "${BASH_SOURCE[0]}" | sed '$d; s/^# \{0,1\}//'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profiles) PROFILES="$2"; shift 2 ;;
    --profiles=*) PROFILES="${1#*=}"; shift ;;
    --restore-drill) RESTORE_DRILL=1; shift ;;
    --enforce-rss) ENFORCE_RSS=1; shift ;;
    --enforce-rss=*) ENFORCE_RSS=1; RSS_LIMIT="${1#*=}"; shift ;;
    --keep) KEEP=1; shift ;;
    --skip-build) SKIP_BUILD=1; shift ;;
    --log-dir) LOG_DIR="$2"; shift 2 ;;
    --dev) DEV=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "clean-room: unknown argument $1" >&2; usage >&2; exit 2 ;;
  esac
done

log() { printf '[clean-room %s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
die() { printf '[clean-room] FAIL: %s\n' "$*" >&2; exit 1; }

# ---------- runtime / compose ----------
if [[ -z "${CONTAINER_RUNTIME:-}" ]]; then
  if command -v docker >/dev/null 2>&1; then CONTAINER_RUNTIME=docker
  elif command -v podman >/dev/null 2>&1; then CONTAINER_RUNTIME=podman
  else die "neither docker nor podman on PATH"; fi
fi
command -v "$CONTAINER_RUNTIME" >/dev/null 2>&1 || die "CONTAINER_RUNTIME=$CONTAINER_RUNTIME not on PATH"
COMPOSE="${COMPOSE:-docker compose}"
RT="$CONTAINER_RUNTIME"
export CONTAINER_RUNTIME

COMPOSE_FILES=(-f "$ROOT/docker-compose.yml")
[[ "$DEV" -eq 1 ]] && COMPOSE_FILES+=(-f "$MVP/docker-compose.dev.yml")
# shellcheck disable=SC2086 # COMPOSE is a command line
compose() { $COMPOSE "${COMPOSE_FILES[@]}" "$@"; }

for t in go npm jq python3 curl openssl tar; do
  command -v "$t" >/dev/null 2>&1 || die "$t not on PATH"
done
# shellcheck disable=SC2086
$COMPOSE version >/dev/null 2>&1 || die "compose not usable: '$COMPOSE version' failed (set COMPOSE=…; podman: start 'podman system service' and export DOCKER_HOST)"

if [[ "$RT" == podman ]]; then
  # Rootless podman 4.x on CNI resolves service names only with the dnsname plugin.
  backend="$(podman info --format '{{.Host.NetworkBackend}}' 2>/dev/null || true)"
  dnsname=0
  for d in /usr/lib/cni /usr/libexec/cni /opt/cni/bin; do [[ -x "$d/dnsname" ]] && dnsname=1; done
  if [[ "$backend" == cni && "$dnsname" -eq 0 ]]; then
    die "podman uses CNI without the dnsname plugin: containers cannot resolve each other (install golang-github-containernetworking-plugin-dnsname or switch to netavark)"
  fi
  [[ -n "${DOCKER_HOST:-}" ]] || die "podman: export DOCKER_HOST=unix://\$XDG_RUNTIME_DIR/podman/podman.sock (podman system service --time=0 …) so compose reaches podman"
fi

# ---------- release train ----------
# shellcheck disable=SC1091
source "$ROOT/release-train.env"
: "${MUXCORE_IMAGE_TAG:?release-train.env has no MUXCORE_IMAGE_TAG}"
: "${CORE_TAG:?release-train.env has no CORE_TAG}"

WORK="${CLEAN_ROOM_WORKDIR:-$(mktemp -d "${TMPDIR:-/tmp}/muxcore-clean-room.XXXXXX")}"
mkdir -p "$WORK"
chmod 700 "$WORK"
WORK="$(cd "$WORK" && pwd)"
LOG_DIR="${LOG_DIR:-$WORK/logs}"
mkdir -p "$LOG_DIR"
LOG_DIR="$(cd "$LOG_DIR" && pwd)"
BIN="$WORK/bin"
rm -f "$WORK/admin.token"  # a token from an earlier stack is never valid
ENV_FILE="$WORK/clean-room.env"
REPORT="$LOG_DIR/clean-room-report.txt"
: >"$REPORT"
report() { printf '%s\n' "$*" | tee -a "$REPORT"; }

export MUXCORE_REGISTRY="localhost:${REG_PORT}/muxcore"
export MUXCORE_IMAGE_TAG

profile_args=()
read -ra profile_list <<<"${PROFILES//,/ }"
if [[ "$RESTORE_DRILL" -eq 1 ]]; then
  # The drill drives compose with --profile backup-local; keep every profile active
  # for all compose calls (smoke, drill) through COMPOSE_PROFILES.
  case " ${profile_list[*]} " in *" backup-local "*) ;; *) profile_list+=(backup-local) ;; esac
fi
for p in "${profile_list[@]}"; do profile_args+=(--profile "$p"); done
COMPOSE_PROFILES="$(IFS=,; echo "${profile_list[*]}")"
export COMPOSE_PROFILES

SEC_PROFILE="$([[ "$DEV" -eq 1 ]] && echo dev || echo household)"
log "train=${MUXCORE_TRAIN:-?} tag=$MUXCORE_IMAGE_TAG core=$CORE_TAG runtime=$RT compose='$COMPOSE' profiles=$COMPOSE_PROFILES security=$SEC_PROFILE"
log "workdir=$WORK logs=$LOG_DIR"

[[ -f "$MVP/.env" ]] && log "WARN: $MVP/.env exists; smoke.sh sources it and its values override this run's"

# ---------- secrets (temp env file; never in the repo) ----------
gen() { openssl rand -hex "$1"; }
(
  umask 077
  cat >"$ENV_FILE" <<EOF
MUXCORE_REGISTRY=$MUXCORE_REGISTRY
MUXCORE_IMAGE_TAG=$MUXCORE_IMAGE_TAG
MVP_ADMIN_USER=admin
MVP_ADMIN_PASSWORD=$(gen 16)
HEALTH_MONITOR_HTTP_TOKEN=$(gen 32)
TRANSCODER_HTTP_TOKEN=$(gen 32)
SECRETS_MASTER_KEY=$(gen 32)
DOWNLOADER_ENGINE=fixture
INDEXER_FIXTURE=1
QBIT_FIXTURE=1
USENET_ENGINE=fixture
EOF
)
# Mesh enrollment (ADR-0017): MUXCORE_ENROLL_SECRET + one single-use token per
# module ID of the registry compose, appended to the same 0600 file.
"$MVP/scripts/gen-enrollment.sh" --env-file "$ENV_FILE" >/dev/null || die "gen-enrollment.sh failed"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a
if [[ "${GITHUB_ACTIONS:-}" == true ]]; then
  # Keep generated secrets out of uploaded logs / the job transcript.
  for v in "$MVP_ADMIN_PASSWORD" "$HEALTH_MONITOR_HTTP_TOKEN" "$TRANSCODER_HTTP_TOKEN" "$SECRETS_MASTER_KEY" "$MUXCORE_ENROLL_SECRET"; do echo "::add-mask::$v"; done
  while IFS='=' read -r _ v; do echo "::add-mask::$v"; done < <(grep '^MUXCORE_ENROLL_TOKEN_' "$ENV_FILE")
fi

# ---------- cleanup / failure handling ----------
REG_STARTED=0
DROPIN=""
STACK_UP=0
BUILT_IMAGES=()

dump_logs() {
  log "collecting compose ps + logs -> $LOG_DIR"
  compose ps -a >"$LOG_DIR/compose-ps.txt" 2>&1 || true
  compose logs --no-color --timestamps >"$LOG_DIR/compose-logs.txt" 2>&1 || true
  local svc
  for svc in $(compose config --services 2>/dev/null); do
    compose logs --no-color --tail 400 "$svc" >"$LOG_DIR/svc-$svc.log" 2>&1 || true
  done
  echo "---- compose ps ----" >&2
  cat "$LOG_DIR/compose-ps.txt" >&2 || true
  echo "---- last 150 log lines ----" >&2
  tail -n 150 "$LOG_DIR/compose-logs.txt" >&2 || true
}

cleanup() {
  local rc=$?
  set +e
  [[ -n "${HEALTH_PUMP_PID:-}" ]] && kill "$HEALTH_PUMP_PID" 2>/dev/null
  if [[ "$rc" -ne 0 && "$STACK_UP" -eq 1 ]]; then
    dump_logs
  fi
  if [[ "$KEEP" -eq 1 ]]; then
    log "--keep: stack, registry and $WORK left in place (env file holds secrets)"
  else
    if [[ "$STACK_UP" -eq 1 ]]; then
      log "compose down -v"
      compose down -v --remove-orphans >/dev/null 2>&1
    fi
    if [[ "$REG_STARTED" -eq 1 ]]; then
      log "removing registry $REG_NAME"
      "$RT" rm -f -v "$REG_NAME" >/dev/null 2>&1
    fi
    [[ -n "$DROPIN" ]] && rm -f "$DROPIN"
    if [[ "${CLEAN_ROOM_PRUNE_IMAGES:-0}" == "1" && ${#BUILT_IMAGES[@]} -gt 0 ]]; then
      log "removing ${#BUILT_IMAGES[@]} built images"
      "$RT" rmi -f "${BUILT_IMAGES[@]}" >/dev/null 2>&1
      "$RT" image prune -f >/dev/null 2>&1
    fi
    rm -f "$ENV_FILE"
    rm -rf "$BIN"
  fi
  if [[ "$rc" -eq 0 ]]; then
    log "PASS (report: $REPORT)"
  else
    log "FAILED rc=$rc (logs: $LOG_DIR)"
  fi
  exit "$rc"
}
trap cleanup EXIT

# ---------- 1. registry ----------
if [[ "$RT" == podman ]]; then
  # registry:2 serves plain HTTP; podman (CLI and API service) needs it marked insecure.
  conf_d="${XDG_CONFIG_HOME:-$HOME/.config}/containers/registries.conf.d"
  if ! grep -rqs "localhost:${REG_PORT}" "$conf_d" 2>/dev/null; then
    mkdir -p "$conf_d"
    DROPIN="$conf_d/50-muxcore-clean-room.conf"
    printf '[[registry]]\nlocation = "localhost:%s"\ninsecure = true\n' "$REG_PORT" >"$DROPIN"
    log "podman: marked localhost:${REG_PORT} insecure ($DROPIN, removed at exit)"
  fi
fi
if "$RT" inspect "$REG_NAME" >/dev/null 2>&1; then
  log "registry $REG_NAME already exists; reusing"
  "$RT" start "$REG_NAME" >/dev/null
else
  "$RT" run -d --name "$REG_NAME" -p "127.0.0.1:${REG_PORT}:5000" "$REG_IMAGE" >/dev/null
  REG_STARTED=1
fi
for _ in $(seq 1 30); do
  curl -sf "http://127.0.0.1:${REG_PORT}/v2/" >/dev/null && break
  sleep 1
done
curl -sf "http://127.0.0.1:${REG_PORT}/v2/" >/dev/null || die "registry on 127.0.0.1:${REG_PORT} not answering"
log "registry up on localhost:${REG_PORT}"

# ---------- 2. build + push images ----------
mapfile -t images < <(compose "${profile_args[@]}" config --images | sort -u)
[[ ${#images[@]} -gt 0 ]] || die "compose config --images returned nothing"
modules=()
need_core=0
for img in "${images[@]}"; do
  [[ "$img" == "$MUXCORE_REGISTRY/"* ]] || die "image $img is not under $MUXCORE_REGISTRY"
  name="${img#"$MUXCORE_REGISTRY"/}"
  name="${name%:*}"
  if [[ "$name" == muxcored ]]; then need_core=1; else modules+=("$name"); fi
  BUILT_IMAGES+=("$img")
done
report "images (${#images[@]}): ${images[*]##*/}"

t0=$SECONDS
if [[ "$SKIP_BUILD" -eq 0 ]]; then
  log "building ${#modules[@]} module binaries (workspace mode) into $BIN"
  "$MVP/scripts/build-module-binaries.sh" "$BIN" "${modules[@]}" 2>&1 | tee "$LOG_DIR/build-binaries.log"
fi
log "packaging + pushing module images"
CONTAINER_RUNTIME="$RT" PREBUILT_DIR="$BIN" PREBUILT_NO_BUILD="$([[ "$SKIP_BUILD" -eq 1 ]] && echo 0 || echo 1)" \
  MODULES="${modules[*]}" "$MVP/scripts/publish-module-images.sh" "$MUXCORE_IMAGE_TAG" >"$LOG_DIR/publish-modules.log" 2>&1 \
  || { tail -40 "$LOG_DIR/publish-modules.log" >&2; die "publish-module-images.sh failed (log: $LOG_DIR/publish-modules.log)"; }
if [[ "$need_core" -eq 1 ]]; then
  log "building + pushing muxcored ($CORE_TAG) from core/Dockerfile"
  CONTAINER_RUNTIME="$RT" MUXCORE_CORE_DIR="$ROOT/core" "$MVP/scripts/publish-muxcored-local.sh" "$MUXCORE_IMAGE_TAG" \
    >"$LOG_DIR/publish-muxcored.log" 2>&1 \
    || { tail -40 "$LOG_DIR/publish-muxcored.log" >&2; die "publish-muxcored-local.sh failed (log: $LOG_DIR/publish-muxcored.log)"; }
  BUILT_IMAGES+=("localhost/muxcored:$MUXCORE_IMAGE_TAG" "localhost/muxcored:latest" "$MUXCORE_REGISTRY/core:$MUXCORE_IMAGE_TAG")
fi
report "build+push: $((SECONDS - t0))s"

# ---------- 3. install from the registry ----------
# Podman runs HEALTHCHECKs from systemd timers; without systemd (containers, CI
# shells) they never run, health stays "starting" and `up --wait` / depends_on
# service_healthy hang. Drive them from here instead.
HEALTH_PUMP_PID=""
if [[ "$RT" == podman && ! -d /run/systemd/system ]]; then
  project="$(compose config --format json | jq -r .name)"
  (
    while :; do
      for id in $(podman ps -q --filter "label=com.docker.compose.project=$project" 2>/dev/null); do
        podman healthcheck run "$id" >/dev/null 2>&1 || true
      done
      sleep 3
    done
  ) &
  HEALTH_PUMP_PID=$!
  log "podman without systemd: driving container healthchecks (pid $HEALTH_PUMP_PID)"
fi
log "compose pull (from $MUXCORE_REGISTRY)"
compose "${profile_args[@]}" pull --quiet 2>&1 | tail -5
STACK_UP=1
t0=$SECONDS
log "compose up -d --wait"
compose "${profile_args[@]}" up -d --wait --wait-timeout "${CLEAN_ROOM_UP_TIMEOUT_SEC:-300}" 2>&1 | tail -n 60
report "compose up --wait: $((SECONDS - t0))s"
compose ps --format '{{.Service}}\t{{.State}}\t{{.Status}}' | sort | tee "$LOG_DIR/compose-ps-up.txt"

# ---------- 3b. security profile evidence ----------
# household: core runs the household profile over TLS, every running module
# enrolled for (or reused) a core-issued certificate, nothing ran insecure.
if [[ "$DEV" -eq 0 ]]; then
  compose logs --no-color core >"$LOG_DIR/core-security.log" 2>&1 || true
  grep -q 'security profile resolved.*profile=household' "$LOG_DIR/core-security.log" \
    || die "core did not resolve the household profile (log: $LOG_DIR/core-security.log)"
  grep -q 'gRPC TLS enabled' "$LOG_DIR/core-security.log" || die "core gRPC TLS not enabled"
  # Services are "up" before they finish enrolling; give stragglers a moment.
  mapfile -t missing_id < <(compose ps --services --status running | grep -vx core)
  id_deadline=$((SECONDS + ${CLEAN_ROOM_ID_WAIT_SEC:-60}))
  while :; do
    still=()
    for svc in "${missing_id[@]}"; do
      compose logs --no-color "$svc" 2>&1 \
        | grep -Eq 'meshid: (enrolled with core|using stored mesh identity)|media-ui: (enrolled with core|mesh identity)|mesh identity.*cert=' \
        || still+=("$svc")
    done
    missing_id=("${still[@]}")
    [[ ${#missing_id[@]} -eq 0 || $SECONDS -ge $id_deadline ]] && break
    sleep 3
  done
  [[ ${#missing_id[@]} -eq 0 ]] || die "no mesh identity logged by: ${missing_id[*]}"
  if compose logs --no-color 2>&1 \
      | grep -E 'insecure=true|!!! INSECURE|DEV PROFILE|gRPC TLS is disabled|without TLS|meshid: insecure dev mode' \
      >"$LOG_DIR/insecure-lines.log"; then
    die "insecure-mode log lines in a household run (see $LOG_DIR/insecure-lines.log)"
  fi
  enrolled="$(compose exec -T core ./muxcored enroll list 2>/dev/null | grep -c . || true)"
  report "household: core profile=household + TLS; $(compose ps --services --status running | grep -vc '^core$') services with a mesh identity; enrollment ledger entries: ${enrolled}"
fi

# ---------- 4. fixture smoke ----------
smoke_env=(
  MUXCORE_SMOKE_REGISTRY=1
  SMOKE_REQUIRE_ACQUISITION="${SMOKE_REQUIRE_ACQUISITION:-1}"
  MUXCORE_CONTAINER_CLI="$RT"
  MUXCORE_COMPOSE="$COMPOSE"
  MVP_TOKEN_FILE="$WORK/admin.token"
  MUXCORE_SMOKE_ID_DIR="$WORK/smoke-id"
)
[[ "$DEV" -eq 1 ]] && smoke_env+=(MUXCORE_COMPOSE_EXTRA_FILES=docker-compose.dev.yml)
log "smoke: ${smoke_env[*]:0:2} _mvp/smoke.sh"
t0=$SECONDS
env "${smoke_env[@]}" "$MVP/smoke.sh" 2>&1 | tee "$LOG_DIR/smoke.log" || die "smoke.sh failed (log: $LOG_DIR/smoke.log)"
grep -q '^PASS: MVP smoke' "$LOG_DIR/smoke.log" || die "smoke did not PASS"
report "smoke: PASS in $((SECONDS - t0))s ($(grep '^PASS:' "$LOG_DIR/smoke.log"))"
grep -E '^OK acquisition fixture' "$LOG_DIR/smoke.log" | tee -a "$REPORT" || die "smoke passed without the fixture acquisition step"

# ---------- 5. idle RSS (NFR-PERF-003) ----------
# Sum of resident set sizes of every container's process tree, read from the
# host's /proc (runtime-agnostic: rootless podman without cgroup delegation
# reports host-wide numbers in `stats`).
measure_rss() {
  local ids id pids=""
  mapfile -t ids < <(compose ps -q)
  [[ ${#ids[@]} -gt 0 ]] || return 1
  for id in "${ids[@]}"; do
    pids+="$("$RT" inspect -f '{{.State.Pid}} {{.Name}}' "$id")"$'\n'
  done
  ps -e -o pid=,ppid=,rss= | PIDS="$pids" python3 -c '
import os, sys
kids, rss = {}, {}
for line in sys.stdin:
    pid, ppid, kb = (int(x) for x in line.split())
    kids.setdefault(ppid, []).append(pid)
    rss[pid] = kb
def tree(p):
    total, stack = 0, [p]
    while stack:
        q = stack.pop()
        total += rss.get(q, 0)
        stack.extend(kids.get(q, []))
    return total
rows = []
for line in os.environ["PIDS"].splitlines():
    pid, _, name = line.partition(" ")
    if int(pid) > 0:
        rows.append((tree(int(pid)) / 1024.0, name.lstrip("/")))
for mib, name in sorted(rows, reverse=True):
    print("  %8.1f MiB  %s" % (mib, name))
print("TOTAL %.1f MiB over %d containers" % (sum(r[0] for r in rows), len(rows)))
'
}
settle="${CLEAN_ROOM_SETTLE_SEC:-20}"
log "idle ${settle}s, then measuring container memory (NFR-PERF-003 budget ${RSS_LIMIT} MiB)"
sleep "$settle"
measure_rss >"$LOG_DIR/rss.txt" || die "could not measure container memory"
cat "$LOG_DIR/rss.txt"
rss_total="$(sed -n 's/^TOTAL \([0-9.]*\) MiB.*/\1/p' "$LOG_DIR/rss.txt")"
report "idle RSS: ${rss_total} MiB (budget ${RSS_LIMIT} MiB, $([[ "$ENFORCE_RSS" -eq 1 ]] && echo enforced || echo report-only))"
if [[ "$ENFORCE_RSS" -eq 1 ]] && python3 -c 'import sys; sys.exit(0 if float(sys.argv[1]) > float(sys.argv[2]) else 1)' "$rss_total" "$RSS_LIMIT"; then
  die "idle RSS ${rss_total} MiB exceeds ${RSS_LIMIT} MiB (NFR-PERF-003)"
fi

# ---------- 6. optional restore drill ----------
if [[ "$RESTORE_DRILL" -eq 1 ]]; then
  log "restore drill (compose mode)"
  t0=$SECONDS
  env "${smoke_env[@]}" "$MVP/scripts/restore-drill.sh" --mode compose --confirm-wipe \
    --workdir "$WORK/restore-drill" 2>&1 | tee "$LOG_DIR/restore-drill.log" \
    || die "restore drill failed (log: $LOG_DIR/restore-drill.log)"
  report "restore drill: PASS in $((SECONDS - t0))s"
fi

report "clean-room: PASS (train ${MUXCORE_TRAIN:-?}, tag $MUXCORE_IMAGE_TAG, profiles $COMPOSE_PROFILES, security $SEC_PROFILE)"
