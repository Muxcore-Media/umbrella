# media-ui-tv

Native **Android TV** client for the MuxCore consumer stack. Talks to the same `mediauiprox` BFF as `media-ui-app` (`:5173` / `https://mux.zem.systems`).

## Feature parity with `media-ui-app`

| Area | TV |
|------|-----|
| Capability-gated nav | `GET /api/capabilities` — same libraries/features as web `nav-catalog` |
| Home | Continue watching, next up, recommended, favorites, ready/available shelves (prefs-gated) |
| Libraries | Movies, TV, music (+ artist), books (+ author), comics, audiobooks, home videos, music videos, **mixed** (movies + TV) |
| Search | Unified library + TMDB remote, scopes (`all` / `movies` / `tv` / `music` / `add`), discover detail + request |
| Discover | `/api/discover/{type}/{id}` + `POST /api/request` |
| Studios | Browse by `collection_name` / genre buckets |
| Features | Favorites, queue, playlists (userdata sync), collections, upcoming, Live TV, in progress (requests) |
| Settings | Profile, display, home shelves, playback (resume, skip intro), subtitles |
| Auth | Quick Connect + `POST /api/tv/login` (password); session as `Cookie: session=…` |
| Player | ExoPlayer, `/api/playback/resolve`, `/api/playback/subtitles`, userdata progress |

Intentional TV differences: no forgot-password page (use web); Quick Connect via pairing screen instead of `/quickconnect` route; no home hero banner (shelf-first TV layout).

## Develop

Requires Android SDK 35, JDK 17, Gradle 8.x.

```bash
cd media-ui-tv
nix-shell
bash scripts/setup-android-sdk.sh
./gradlew :app:assembleDebug
```

Install on a TV emulator or device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Default dev server: `http://127.0.0.1:5173` (or `http://10.0.2.2:5173` from the Android emulator).

## Related

| Piece | Location |
|-------|----------|
| Web consumer SPA | `media-ui-app` |
| BFF | `_mvp/cmd/mediauiprox` |
| Ports | `_mvp/PORTS.md` |
