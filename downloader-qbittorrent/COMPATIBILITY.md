# Compatibility

## Core Version

| Module Version | Core Version | Status |
|----------------|-------------|--------|
| v0.1.0         | 0.5.2+      | Current |

## Capabilities

- `downloader` / `downloader.qbittorrent`
- `settings`

## Contracts

- `github.com/Muxcore-Media/contracts-downloader` — `Downloader` / `DownloaderService` v0.1.0

## Notes

Talks to an external qBittorrent instance via WebUI HTTP API. Does not embed a torrent engine.
Fixture mode requires no qBittorrent process.
