# Media DLNA

DLNA/UPnP Digital Media Server for MuxCore library paths. Exposes scanned media to smart TVs, game consoles, and other DLNA renderers on the LAN using [anacrolix/dms](https://github.com/anacrolix/dms).

## Capabilities

- `media.dlna` — UPnP/DLNA media server
- `settings` — operator settings via mesh/admin

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `DLNA_MEDIA_PATH` | — | Single library directory to browse |
| `DLNA_MEDIA_PATHS` | — | Comma-separated library directories |
| `SCANNER_LIBRARY_ROOT` / `MVP_LIBRARY_ROOT` | — | Fallback when DLNA paths unset |
| `DLNA_FRIENDLY_NAME` | `MuxCore DLNA` | UPnP device name |
| `DLNA_HTTP_ADDR` | `:9750` | DLNA/UPnP HTTP bind address |
| `DLNA_GRPC_ADDR` | `:9751` | Module gRPC listen address |
| `DLNA_HEALTH_HTTP_ADDR` | `:8751` | Module health HTTP |
| `DLNA_IFNAME` | — | Network interface for SSDP (all up ifaces when empty) |
| `DLNA_NO_TRANSCODE` | `false` | Disable ffmpeg transcodes |
| `DLNA_FORCE_TRANSCODE` | — | Force profile: `chromecast`, `vp8`, `web` |
| `DLNA_NO_PROBE` | `false` | Skip ffprobe metadata probing |
| `DLNA_IGNORE_HIDDEN` | `false` | Skip dotfiles |
| `DLNA_IGNORE_UNREADABLE` | `false` | Skip unreadable entries |
| `DLNA_IGNORE_PATHS` | — | Comma-separated relative dir names to skip |
| `DLNA_NOTIFY_INTERVAL` | `30s` | SSDP notify interval |
| `DLNA_PROBE_CACHE_PATH` | `~/.cache/media-dlna/ffprobe-cache.json` | ffprobe cache file |
| `MUXCORE_GRPC_ADDR` | — | Core mesh address |

When multiple paths are configured, the browse root is their common parent directory if one exists; otherwise set a single `DLNA_MEDIA_PATH`.

## HTTP

- `GET /healthz` on `DLNA_HEALTH_HTTP_ADDR` — module health (not the DLNA content server)

DLNA clients connect to `DLNA_HTTP_ADDR` (SSDP + ContentDirectory).

## gRPC

- `Status` — configured/running state, browse root, bound DLNA HTTP address

## MVP stack

Enable on the host reference stack:

```bash
MVP_ENABLE_MEDIA_DLNA=1 ./_mvp/run-host.sh up
```

Requires `ffmpeg`/`ffprobe` in `PATH` for transcoding and rich metadata (optional when `DLNA_NO_TRANSCODE=1` and `DLNA_NO_PROBE=1`).

## Build

```bash
make proto
make test
make build
```

## Status

v0.1.0 — Filesystem DLNA server wired as a MuxCore sidecar. Jellyfin/Emby DLNA bridges are out of scope; this module serves library paths directly.
