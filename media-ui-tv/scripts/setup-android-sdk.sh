#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NIX_SDK="${ANDROID_HOME:-}"
if [[ -z "$NIX_SDK" ]]; then
  echo "Run inside nix-shell (media-ui-tv/shell.nix)" >&2
  exit 1
fi
WRITABLE="$ROOT/.android-sdk"
mkdir -p "$WRITABLE"
if [[ ! -f "$WRITABLE/licenses/android-sdk-license" ]]; then
  mkdir -p "$WRITABLE/licenses"
  cp -f "$NIX_SDK/licenses/"* "$WRITABLE/licenses/" 2>/dev/null || true
fi
SDKM="$NIX_SDK/cmdline-tools/22.0/bin/sdkmanager"
if [[ -x "$SDKM" ]]; then
  yes | "$SDKM" --sdk_root="$WRITABLE" "platforms;android-35" "build-tools;34.0.0" >/dev/null 2>&1 || true
fi
for pkg in platforms build-tools platform-tools cmdline-tools; do
  if [[ -d "$NIX_SDK/$pkg" ]]; then
    mkdir -p "$WRITABLE/$pkg"
    for item in "$NIX_SDK/$pkg"/*; do
      name="$(basename "$item")"
      if [[ ! -e "$WRITABLE/$pkg/$name" ]]; then
        ln -sf "$item" "$WRITABLE/$pkg/$name"
      fi
    done
  fi
done
echo "sdk.dir=$WRITABLE" > "$ROOT/local.properties"
echo "Writable SDK at $WRITABLE"
