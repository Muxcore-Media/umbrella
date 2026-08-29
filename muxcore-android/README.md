# muxcore-android

Native Android client for MuxCore — movies and TV shows only, with feature
parity against the web consumer UI (`media-ui-app`). Kotlin, Jetpack Compose,
Material 3, Media3 ExoPlayer.

## Requirements

- JDK 17 and the Android SDK (platforms 35/36, build-tools 35). On NixOS use
  the provided shell:

```bash
nix-shell            # provides jdk17, gradle 8.14, android sdk
```

- Elsewhere: install Android Studio or the command-line SDK and set
  `ANDROID_HOME`; a `local.properties` with `sdk.dir=...` also works.

## Build

```bash
nix-shell --run 'gradle :app:assembleDebug'      # debug APK
nix-shell --run 'gradle :app:assembleRelease'    # minified release APK
nix-shell --run 'gradle :app:testDebugUnitTest'  # unit tests (normalizers, acquisition, user data)
nix-shell --run 'gradle :app:lintDebug'          # android lint
```

APK output: `app/build/outputs/apk/<variant>/`.

The release build is signed with the debug keystore so it is installable for
soak testing; supply a real `signingConfig` before distributing.

## Install on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Default server is `https://mux.zem.systems` (changeable on the first screen).
Sign-in uses the server's own login page in a WebView; the session cookie is
captured and persisted, and all API/image/video requests send it.

## Architecture

- `data/` — OkHttp `ApiClient` against the mediauiprox BFF, tolerant JSON
  `Normalizers` (snake_case + camelCase), `SessionStore` (DataStore),
  `UserDataStore` (local cache + server sync of progress/favorites/queue/
  playlists/prefs), `Acquisition` helpers (request status labels/phases).
- `ui/` — single-activity Compose app. `MuxApp` gates login vs the main
  scaffold (bottom bar: Home, Movies, Shows, Search, Library).
- `ui/screens/` — one file per page: home, browse grids, detail pages,
  search, discover (+ request flow), watchlist/favorites/queue/in-progress/
  collections/upcoming/studios/playlists, quick connect, invite join,
  forgot password, settings.
- `ui/player/` — Media3 ExoPlayer with custom OSD: transcode-aware seeking,
  quality/speed/track sheets, skip-intro/outro segments, chapter navigation,
  trickplay seek previews, up-next countdown, resume dialog, stats overlay,
  brightness/volume gestures, double-tap seek, PiP.

Capabilities from `/api/capabilities` gate features exactly like the web
router (request/queue/collections/upcoming/studios/playlists/quickconnect/
watchlist/debrid). Music/books/comics/live-TV surfaces are intentionally
absent.

## Tests

Unit tests live in `app/src/test/` and cover the JSON normalizers, request
status/merge logic, and next-episode derivation. Run with
`gradle :app:testDebugUnitTest`.
