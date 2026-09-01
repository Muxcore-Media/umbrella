#!/usr/bin/env bash
# Create Forgejo repos for desk-only extras, register in umbrella, fix iOS/tvOS pins.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
FORGEJO_SSH='ssh://forgejo@git.zem.systems:2222/muxcore'
TVOS_SHA='6018c4cb8b4f4d5d0e6b4edb62caef4d1726ae9a'
IOS_SHA='2506411f05ccfd2e5190cac552c20c71bfb0209b'

log() { echo "add-extras: $*" >&2; }

create_forgejo_repo() {
  local name=$1
  if git ls-remote "${FORGEJO_SSH}/${name}.git" HEAD &>/dev/null; then
    log "repo muxcore/${name} exists"
    return 0
  fi
  log "creating muxcore/${name}"
  [[ -n "${FORGEJO_TOKEN:-}" ]] || FORGEJO_TOKEN=$(ssh vault "sudo -u forgejo forgejo --config /mnt/fast-storage/appdata/forgejo/custom/conf/app.ini admin user generate-access-token --username zemdregon --token-name add-extras --scopes all --raw 2>/dev/null | tail -1")
  curl -fsS -X POST "https://git.zem.systems/api/v1/orgs/muxcore/repos" \
    -H "Authorization: token ${FORGEJO_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"${name}\",\"private\":false,\"auto_init\":false}" >/dev/null
}

push_module_dir() {
  local path=$1 repo=$2
  local dir="$ROOT/$path"
  [[ -d "$dir" ]] || { log "skip missing $path"; return 0; }
  create_forgejo_repo "$repo"
  if [[ -d "$dir/.git" ]]; then
    log "$path already a git repo — pushing"
    git -C "$dir" remote set-url origin "${FORGEJO_SSH}/${repo}.git" 2>/dev/null \
      || git -C "$dir" remote add origin "${FORGEJO_SSH}/${repo}.git"
    git -C "$dir" checkout -B main 2>/dev/null || true
    git -C "$dir" add -A
    if ! git -C "$dir" diff --cached --quiet 2>/dev/null; then
      git -C "$dir" commit -m "chore: sync desk workspace to forgejo"
    fi
    git -C "$dir" push -u origin main
    return 0
  fi
  log "initializing git in $path"
  git -C "$dir" init -b main
  git -C "$dir" add -A
  git -C "$dir" commit -m "chore: initial import from desk workspace"
  git -C "$dir" remote add origin "${FORGEJO_SSH}/${repo}.git"
  git -C "$dir" push -u origin main
}

register_submodule() {
  local path=$1 repo=$2
  local url="${FORGEJO_SSH}/${repo}.git"
  if grep -Fq "path = ${path}" .gitmodules 2>/dev/null; then
    log "submodule $path already in .gitmodules"
    return 0
  fi
  log "registering submodule $path"
  if [[ -e "$path" ]]; then
    rm -rf "$path"
  fi
  git submodule add "$url" "$path"
}

fix_mobile_pins() {
  log "fixing media-tvos-app and muxcore-ios URLs and SHAs"
  git config -f .gitmodules submodule.media-tvos-app.url "${FORGEJO_SSH}/muxcore-tvos.git"
  git config -f .gitmodules submodule.muxcore-ios.url "${FORGEJO_SSH}/muxcore-ios.git"
  git submodule sync media-tvos-app muxcore-ios
  git submodule update --init media-tvos-app muxcore-ios
  git -C media-tvos-app fetch origin
  git -C media-tvos-app checkout "$TVOS_SHA"
  git -C muxcore-ios fetch origin
  git -C muxcore-ios checkout "$IOS_SHA"
  git add .gitmodules media-tvos-app muxcore-ios
}

# Desk-only module dirs (scripts/ is umbrella-owned — not a submodule)
NEW_MODULES=(
  branding:branding
  downloader-native-usenet:downloader-native-usenet
  downloader-qbittorrent:downloader-qbittorrent
  media-dlna:media-dlna
  media-ui-tv:media-ui-tv
  metadata-musicbrainz:metadata-musicbrainz
  muxcore-android:muxcore-android
  playback-contract:playback-contract
  userdata-local:userdata-local
)

for pair in "${NEW_MODULES[@]}"; do
  path=${pair%%:*}
  repo=${pair##*:}
  push_module_dir "$path" "$repo"
done

# Already git repos on desk
register_submodule core.wiki core-wiki
register_submodule _wave1 wave1

for pair in "${NEW_MODULES[@]}"; do
  path=${pair%%:*}
  repo=${pair##*:}
  register_submodule "$path" "$repo"
done

fix_mobile_pins

git add .gitmodules
if git diff --cached --quiet; then
  log "nothing to commit"
else
  git commit -m "$(cat <<'EOF'
chore: add desk extras to umbrella and pin iOS/tvOS on Forgejo.

Register loose module checkouts (branding, downloaders, media-dlna, android/tv, etc.),
core.wiki, and _wave1 as submodules. Point mobile apps at Forgejo SSH remotes.
EOF
)"
  git push origin main
fi

log "done"
