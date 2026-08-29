# Downloader qBittorrent

MuxCore sidecar that bridges [qBittorrent](https://www.qbittorrent.org/) via the WebUI API.

Implements `muxcore.downloader.v1.DownloaderService` from `contracts-downloader` (AddTorrent, status, pause/resume, delete) plus SettingsProvider.

## Network safety / CI

- **Unit tests and CI never talk to a real qBittorrent.** Use `DOWNLOADER_ENGINE=fixture` / `QBIT_FIXTURE=1` (in-memory) or the httptest mock (`internal/qbit.NewMockServer`).
- Live WebUI is **operator opt-in only**: set `QBITTORRENT_URL` (+ username/password). Leave URL empty for soft-empty health (optional peer).

## Fixture mode

| Env | Effect |
|-----|--------|
| `DOWNLOADER_ENGINE=fixture` (or `fake`) | In-memory backend; AddTorrent completes instantly |
| `QBIT_FIXTURE=1` | Same as fixture engine |

When a publisher is injected (tests or mesh adapter), fixture AddTorrent emits:

| Event | When |
|-------|------|
| `download.started` | Torrent accepted |
| `download.completed` | Fixture finishes immediately (unless `paused`) |

## Config

| Env | Setting | Default |
|-----|---------|---------|
| `QBITTORRENT_URL` | `base_url` | — (empty = unconfigured / soft-empty) |
| `QBITTORRENT_USERNAME` | `username` | `admin` |
| `QBITTORRENT_PASSWORD` | `password` | — (operator opt-in) |
| `DOWNLOADER_ENGINE` / `QBIT_FIXTURE` | fixture mode | off |
| gRPC listen | — | `:9462` |
| Health | — | `:9463` (`/healthz`) |

## Capabilities

- `downloader`
- `downloader.qbittorrent`
- `settings`

## Build / test

```bash
CGO_ENABLED=0 go test ./...   # fixture + httptest only; no live qBit
CGO_ENABLED=0 go build -o bin/downloader-qbittorrent ./cmd/module
```

## Status

v0.1.0 — WebUI client + contracts DownloaderService + fixture path. Optional peer (not default host).
