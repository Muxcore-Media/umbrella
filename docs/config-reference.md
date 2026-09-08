# MuxCore Configuration Reference

Environment variables read by MuxCore modules at startup. Values in `muxcore.json` or module flags may also apply; **environment variables take highest precedence** where both exist (see `core/internal/config/config.go`).

**Source of truth:** `os.Getenv` / `os.LookupEnv` in Go module code. This document was generated from a full workspace scan (433 unique variables across Go sources).

**Conventions**

- Durations: Go `time.ParseDuration` syntax (`30m`, `6h`, `24h`).
- Booleans: `true` / `1` / `yes` enable; `false` / `0` disable (module-specific; see table).
- Comma-separated lists: no spaces required, but trimming is applied.
- Ports: see [`_mvp/PORTS.md`](../_mvp/PORTS.md) for default listen addresses.

---

## Quick Start

Minimum variables for a new deployment behind public HTTPS (vault/soak pattern). Copy from [`_mvp/.env.example`](../_mvp/.env.example) for local dev.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUXCORE_CONFIG` | core | `./muxcore.json` | Path to core JSON config (`run-host.sh` sets under MVP root). |
| `MUXCORE_GRPC_ADDR` | core | `:9090` | Mesh gRPC listen/dial address for sidecars. |
| `MUXCORE_INSECURE_DISABLE_TLS` | core | `true` in dev | **Dev only.** Disables TLS for mesh and HTTP. Unset in production. |
| `MUXCORE_LOG_LEVEL` | core | `info` | Log level: `debug`, `info`, `warn`, `error`. |
| `MUXCORE_STORAGE_DIR` | core | — | Local storage provider root (orchestrator `storage.local`). |
| `AUTH_HTTP_URL` | auth-local | — | **Public** auth URL for browser redirects (`https://auth.example.com`). |
| `AUTH_GRPC_ADDR` | auth-local | `:9403` | Auth gRPC listen address. |
| `AUTH_HTTP_ADDR` | auth-local | `:9401` | Auth HTTP listen address (login UI). |
| `AUTH_TRUSTED_PROXIES` | auth-local | loopback | CIDRs of edge proxies allowed to set `X-Forwarded-*`. |
| `ADMIN_UI_PUBLIC_URL` | admin-ui | — | Public admin URL (`https://admin.example.com`). |
| `ADMIN_UI_AUTH_ADDR` | admin-ui | `http://localhost:9401` | Browser-facing auth URL for login redirects. |
| `ADMIN_UI_AUTH_INTERNAL_ADDR` | admin-ui | same as auth addr | Server-side auth URL (loopback on vault). |
| `ADMIN_UI_TRUSTED_PROXIES` | admin-ui | — | Edge proxy CIDRs for admin-ui. |
| `MEDIA_UI_PUBLIC_URL` | media-ui (BFF) | — | Public consumer SPA URL (`https://mux.example.com`). |
| `MVP_LIBRARY_ROOT` | _mvp | `./data/library` | Movie library filesystem root. |
| `MVP_TV_LIBRARY_ROOT` | _mvp | `./data/library/tv` | TV library filesystem root. |
| `MVP_DOWNLOADS_DIR` | _mvp | `./data/downloads` | Acquisition download directory. |
| `MVP_TOKEN_FILE` | _mvp | `./run/admin.token` | Admin API token file for `muxcorectl`. |

**Production checklist**

1. Set all three public URLs (`AUTH_HTTP_URL`, `ADMIN_UI_PUBLIC_URL`, `MEDIA_UI_PUBLIC_URL`).
2. Add dawn/dusk (or your edge) ZT `/128` entries to `AUTH_TRUSTED_PROXIES` and `ADMIN_UI_TRUSTED_PROXIES`.
3. Remove `MUXCORE_INSECURE_DISABLE_TLS` (or set `false`).
4. Rotate `MVP_ADMIN_PASSWORD` — dev bootstrap only.

---

## Shared: Mesh, TLS, and Tenant

Used by most gRPC sidecars. Set once in the host environment or `run-host.sh`.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUXCORE_GRPC_ADDR` | core | `:9090` | Core mesh gRPC address sidecars dial. |
| `MUXCORE_INSECURE_DISABLE_TLS` | core | — | `true`/`1`: skip TLS (dev). Deprecated alias: `MUXCORE_DEV_TLS_SKIP`. |
| `MUXCORE_GRPC_INSECURE` | * | — | Per-sidecar insecure dial (same effect as above for that module). |
| `MUXCORE_MESH_DIAL_LOCAL` | * | — | `true`: rewrite empty discovery hosts to `127.0.0.1` (host stack). |
| `MUXCORE_TLS_CERT` | * | — | Client TLS certificate file for mesh dial. |
| `MUXCORE_TLS_KEY` | * | — | Client TLS private key file. |
| `MUXCORE_TLS_CA` | * | — | CA bundle for mesh TLS verification. |
| `MUXCORE_MODULE_ID` | core SDK | — | Override module identity for mesh registration. |
| `MUXCORE_HTTP_ADDR` | * | — | HTTP health/metrics listen override (pool, music). |
| `TENANT_MODE` | core | — | `1`: enable multi-tenant request routing. |
| `MUXCORE_TENANT_ID` | core | — | This node's tenant ID. |
| `MUXCORE_TENANT_CLUSTER_MAP` | core | — | Tenant → cluster routing map. |
| `MUXCORE_TENANT_CLUSTER_STRICT` | core | — | Strict tenant cluster enforcement. |
| `MUXCORE_TENANT_REGION_MAP` | core | — | Tenant region placement map. |
| `MUXCORE_NODE_REGION` | core | — | This node's region label. |
| `MUXCORE_TENANT_STORAGE_SYNC` | core | — | Cross-tenant storage sync behavior. |
| `DATABASE_URL` | * | — | Shared Postgres URL (playback-monitor fallback). |

---

## core

Core server (`muxcored`) and global infrastructure.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUXCORE_CONFIG` | core | — | JSON config file path. |
| `MUXCORE_SERVER_ADDR` | core | `:8080` | HTTP API listen address. Alias: `MUXCORE_ADDR`. |
| `MUXCORE_SERVER_TLS_CERT` | core | — | HTTP TLS cert. Alias: `MUXCORE_TLS_CERT`. |
| `MUXCORE_SERVER_TLS_KEY` | core | — | HTTP TLS key. Alias: `MUXCORE_TLS_KEY`. |
| `MUXCORE_SERVER_READ_TIMEOUT` | core | `15` | HTTP read timeout (seconds). |
| `MUXCORE_SERVER_WRITE_TIMEOUT` | core | `15` | HTTP write timeout (seconds). |
| `MUXCORE_SERVER_TRUSTED_PROXIES` | core | loopback | Comma-separated CIDRs for `X-Forwarded-For`. |
| `MUXCORE_GRPC_TLS_CERT` | core | — | gRPC server TLS certificate. |
| `MUXCORE_GRPC_TLS_KEY` | core | — | gRPC server TLS key. |
| `MUXCORE_GRPC_MTLS_ENABLED` | core | `false` | Require mutual TLS on gRPC. |
| `MUXCORE_GRPC_MTLS_CA` | core | — | CA cert for mTLS client verification. |
| `MUXCORE_GRPC_CA_CERT_DIR` | core | `<data>/ca` | Ephemeral CA directory when mTLS auto-generates. |
| `MUXCORE_GRPC_MAX_MESSAGE_SIZE_MB` | core | `32` | Max gRPC send/receive message size (MB). |
| `MUXCORE_GRPC_JOIN_TOKEN` | core | — | Pre-shared cluster join token. Alias: `MUXCORE_CLUSTER_JOIN_TOKEN`. |
| `MUXCORE_GRPC_SEED_NODES` | core | — | Comma-separated `host:port` seed nodes. |
| `MUXCORE_LOG_LEVEL` | core | `info` | Logging level. |
| `MUXCORE_LOG_FORMAT` | core | `text` | `text` or `json`. |
| `MUXCORE_DATABASE_DRIVER` | core | — | Database driver name (from database module). |
| `MUXCORE_DATABASE_URL` | core | — | Database connection URL. |
| `MUXCORE_CACHE_DRIVER` | core | — | Cache driver name. |
| `MUXCORE_CACHE_URL` | core | — | Cache connection URL. |
| `MUXCORE_AUDIT_PATH` | core | — | JSONL audit log path; empty disables. |
| `MUXCORE_AUDIT_PATH_FILE` | core | — | File containing audit log path (read at startup). |
| `MUXCORE_AUDIT_MAX_SIZE_MB` | core | `100` | Audit log rotation size (MB). |
| `MUXCORE_AUDIT_MAX_ROTATED_FILES` | core | `5` | Rotated audit files to retain. |
| `MUXCORE_STORAGE_DIR` | core | — | Local storage provider directory. |
| `MUXCORE_STORAGE_READ_TIMEOUT` | core | `30` | Storage read timeout (seconds). |
| `MUXCORE_STORAGE_WRITE_TIMEOUT` | core | `300` | Storage write timeout (seconds). |
| `MUXCORE_STORAGE_DELETE_TIMEOUT` | core | `30` | Storage delete/move timeout (seconds). |
| `MUXCORE_SPOOL_ALLOWED_HOSTS` | core | all | Comma-separated hosts allowed for spool URL fetch (SSRF guard). |
| `MUXCORE_SPOOL_REQUIRE_SIGNATURE` | core | — | `1`: require signed spool packages. |
| `MUXCORE_SPOOL_PUBLIC_KEY` | core | — | Spool signature verification public key. |
| `MUXCORE_SPOOL_TRUSTED_KEYS_DIR` | core | — | Directory of trusted spool publisher keys. |
| `MUXCORE_SPOOL_ALLOWED_PUBLISHERS` | core | — | Comma-separated allowed spool publisher IDs. |
| `MUXCORE_METRICS_ENABLE` | core | `false` | `true`/`1`: expose unauthenticated `/metrics` on HTTP API. |
| `MUXCORE_DEBUG_ENABLE` | core | `false` | `true`/`1`: expose `/debug/pprof/*` endpoints. |
| `MUXCORE_EVENT_JOURNAL_PATH` | core | — | Event WAL journal directory. |
| `MUXCORE_EVENT_REPLAY` | core | — | Event replay mode flag. |
| `MUXCORE_MODULE_SANDBOX` | core | — | Module spawn sandbox backend (`gvisor`, `firecracker`, etc.). |
| `MUXCORE_SANDBOX_GVISOR_BIN` | core | — | Path to `runsc` binary. |
| `MUXCORE_SANDBOX_FIRECRACKER_BIN` | core | — | Path to Firecracker binary. |
| `MUXCORE_SANDBOX_FC_ROOTFS` | core | — | Firecracker rootfs image path. |
| `MUXCORE_SANDBOX_FC_KERNEL` | core | — | Firecracker kernel image. |
| `MUXCORE_SANDBOX_FC_VSOCK_CID` | core | — | Firecracker vsock guest CID. |
| `MUXCORE_SANDBOX_FC_VSOCK_UDS` | core | — | Firecracker vsock UDS path. |
| `MUXCORE_SANDBOX_ROOTFS_AUTO` | core | — | Auto-build minimal rootfs. |
| `MUXCORE_SANDBOX_ROOTFS_BUSYBOX` | core | — | Busybox binary for rootfs. |
| `MUXCORE_SANDBOX_ROOTFS_BUSYBOX_URL` | core | — | URL to download busybox. |
| `MUXCORE_SANDBOX_ROOTFS_TEMPLATE` | core | — | Rootfs template directory. |
| `MUXCORE_SANDBOX_ROOTFS_PROFILE` | core | — | Rootfs profile name. |
| `MUXCORE_SANDBOX_OCI_AUTO` | core | — | Auto OCI runtime selection. |
| `MUXCORE_SANDBOX_SECCOMP` | core | — | Enable seccomp filtering. |
| `MUXCORE_SANDBOX_SECCOMP_PROFILE` | core | — | Seccomp profile path. |

---

## auth-local

Local authentication provider (default on vault soak stack).

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUTH_GRPC_ADDR` | auth-local | `:9403` | gRPC listen address (mesh dial target). |
| `AUTH_HTTP_ADDR` | auth-local | `:9401` | HTTP listen address (login UI, OAuth callbacks). |
| `AUTH_HTTP_URL` | auth-local | — | Public base URL for redirects and links (no trailing slash). |
| `AUTH_DB_PATH` | auth-local | `~/.muxcore/auth.db` | SQLite database path. |
| `AUTH_POLICY_FILE` | auth-local | `policies.yaml` | RBAC policy YAML path. |
| `AUTH_RP_ID` | auth-local | `localhost` | WebAuthn relying party ID (domain). |
| `AUTH_RP_ORIGINS` | auth-local | `http://localhost:9401` | Comma-separated WebAuthn allowed origins. |
| `AUTH_RP_NAME` | auth-local | `MuxCore` | WebAuthn relying party display name. |
| `AUTH_TRUSTED_PROXIES` | auth-local | loopback | CIDRs trusted for `X-Forwarded-For` / `X-Real-IP`. |
| `AUTH_ALLOWED_REDIRECT_HOSTS` | auth-local | — | Extra allowed post-login redirect hosts. |
| `AUTH_SESSION_TTL` | auth-local | `24h` | Absolute maximum session lifetime. |
| `AUTH_SESSION_IDLE_TIMEOUT` | auth-local | `30m` | Idle timeout; `0` disables idle expiry. |
| `AUTH_SESSION_BIND_IP` | auth-local | `false` | Bind session to client IP at creation. |
| `AUTH_SESSION_BIND_UA` | auth-local | `false` | Bind session to User-Agent at creation. |
| `AUTHCTL_TOKEN` | auth-local | — | CLI token for `authctl` (bypasses login). |

**Login backoff (not env-configurable):** failed logins use exponential lockout starting at **1 minute**, capped at **30 minutes** (`auth-local/internal/ratelimit/limiter.go`). There is no `AUTH_LOGIN_BACKOFF_MAX` variable.

---

## auth-oidc

OIDC authentication provider (mutually exclusive with auth-local).

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUTH_OIDC_GRPC_ADDR` | auth-oidc | — | gRPC listen address. |
| `AUTH_OIDC_HTTP_ADDR` | auth-oidc | — | HTTP listen address. |
| `AUTH_OIDC_DB_PATH` | auth-oidc | — | SQLite database path. |
| `AUTH_OIDC_POLICY_FILE` | auth-oidc | — | RBAC policy YAML. |
| `AUTH_OIDC_ISSUER` | auth-oidc | — | OIDC issuer URL. |
| `AUTH_OIDC_SCOPES` | auth-oidc | — | Comma-separated OIDC scopes. |
| `AUTH_OIDC_ROLE_MAP` | auth-oidc | — | OIDC claim → role mapping. |
| `AUTH_OIDC_DEFAULT_ROLES` | auth-oidc | — | Default roles for new users. |
| `AUTH_OIDC_TRUSTED_PROXIES` | auth-oidc | — | Proxy CIDRs (falls back to `AUTH_TRUSTED_PROXIES`). |
| `AUTH_OIDC_SESSION_IDLE_TIMEOUT` | auth-oidc | — | Session idle timeout (overrides `AUTH_SESSION_IDLE_TIMEOUT`). |
| `AUTH_OIDC_DB_ENCRYPTION_KEY` | auth-oidc | — | DB encryption key for token storage. |
| `AUTH_ALLOWED_REDIRECT_HOSTS` | auth-oidc | — | Allowed post-login redirect hosts. |

---

## admin-ui

Operator web console.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `ADMIN_UI_ADDR` | admin-ui | `:8080` | HTTP listen address (`:8082` on vault). |
| `ADMIN_UI_CORE_ADDR` | admin-ui | `localhost:9090` | Core mesh gRPC dial address. |
| `ADMIN_UI_AUTH_ADDR` | admin-ui | `http://localhost:9401` | Browser auth URL for login redirects. |
| `ADMIN_UI_AUTH_INTERNAL_ADDR` | admin-ui | same as auth addr | Server-side auth exchange URL. |
| `ADMIN_UI_PUBLIC_URL` | admin-ui | — | Public URL for links and CSRF origin checks. |
| `ADMIN_UI_TRUSTED_PROXIES` | admin-ui | — | Edge proxy CIDRs. |
| `ADMIN_UI_SESSION_TTL` | admin-ui | `30m` | Admin session cookie TTL. |
| `ADMIN_UI_INSECURE` | admin-ui | `false` | `true`: insecure gRPC to core. |
| `ADMIN_UI_TLS_CERT` | admin-ui | — | TLS certificate for HTTPS. |
| `ADMIN_UI_TLS_KEY` | admin-ui | — | TLS private key. |
| `ADMIN_UI_LOG_LEVEL` | admin-ui | `info` | Log level. |
| `ADMIN_UI_LOG_FORMAT` | admin-ui | `text` | Log format. |
| `ADMIN_UI_HEALTH_MONITOR_URL` | admin-ui | `http://127.0.0.1:9203` | Health monitor upstream. |
| `ADMIN_UI_METRICS_TOKEN` | admin-ui | — | Bearer token for metrics endpoint. |
| `ADMIN_UI_API_REST_URL` | admin-ui | — | Override api-rest base URL for marketplace. |
| `ADMIN_UI_USERDATA_URL` | admin-ui | — | Userdata service URL for parental controls. |
| `ADMIN_UI_DATA_DIR` | admin-ui | — | Persistent admin data directory. |
| `ADMIN_UI_PLAYBACK_FILE` | admin-ui | — | Playback policy JSON file path. |
| `ADMIN_UI_LOG_DIR` | admin-ui | — | Module log directory for log viewer. |
| `ADMIN_UI_SPOOL_TRUST_DIR` | admin-ui | — | Spool trust keys directory override. |
| `API_REST_CACHE_TTL` | api-rest | disabled | GET response cache TTL (seconds); `0` or unset disables. |
| `API_REST_RATE_LIMIT_PER_MIN` | api-rest | `120` | Per-IP read request rate limit. |
| `API_REST_WRITE_RATE_LIMIT_PER_MIN` | api-rest | `30` | Per-IP write request rate limit. |

---

## api-rest

HTTP REST gateway to mesh gRPC.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `API_REST_HTTP_ADDR` | api-rest | — | HTTP listen address. |
| `API_REST_GRPC_ADDR` | api-rest | — | Module gRPC listen address. |
| `API_REST_MODULE_ID` | api-rest | — | Module ID override. |
| `API_REST_CORS_ORIGINS` | api-rest | — | Allowed CORS origins (comma-separated). |
| `API_REST_CACHE_TTL` | api-rest | disabled | Response cache TTL in seconds. |
| `API_REST_RATE_LIMIT_PER_MIN` | api-rest | `120` | Read rate limit per client IP per minute. |
| `API_REST_WRITE_RATE_LIMIT_PER_MIN` | api-rest | `30` | Write rate limit per client IP per minute. |

---

## media-transcoder

FFmpeg transcoding sidecar.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `TRANSCODER_DB_PATH` | media-transcoder | `/var/lib/media-transcoder/transcoder.db` | SQLite job database. |
| `TRANSCODER_GRPC_ADDR` | media-transcoder | `:9525` | gRPC listen address. |
| `TRANSCODER_HTTP_ADDR` | media-transcoder | `:9526` | Playback transcode HTTP server. |
| `TRANSCODER_FFMPEG_BIN` | media-transcoder | `ffmpeg` | FFmpeg executable path. |
| `TRANSCODER_FFPROBE_BIN` | media-transcoder | `ffprobe` | FFprobe executable path. |
| `TRANSCODER_MAX_CONCURRENT` | media-transcoder | `2` | Max parallel transcode jobs. |
| `TRANSCODER_MIN_FREE_MB` | media-transcoder | `1024` | Minimum free disk (MB) before starting a job. |
| `TRANSCODER_MAX_PLAYBACK` | media-transcoder | `4` | Max concurrent playback transcodes. |
| `TRANSCODER_HLS_CACHE` | media-transcoder | next to `TRANSCODER_DB_PATH` | Seekable HLS playlist/segment cache. |
| `TRANSCODER_TRICKPLAY_DIR` | media-transcoder | — | Trickplay thumbnail output directory. |
| `TRANSCODER_SCAN_INTERVAL` | media-transcoder | — | Library event scan interval. |
| `TRANSCODER_VAAPI_DEVICE` | media-transcoder | — | VAAPI hardware device path. |
| `TRANSCODER_QSV_DEVICE` | media-transcoder | — | Intel QSV device path. |
| `TRANSCODER_USE_POOL` | media-transcoder | — | `true`: forward jobs to transcoder pool. |
| `TRANSCODER_POOL_ADDR` | media-transcoder | — | Transcoder pool gRPC address. |

**Note:** There is no `TRANSCODER_FFMPEG_TIMEOUT` environment variable; job timeouts are configured per-job or via pool `POOL_DISPATCH_TIMEOUT_SEC`.

---

## media-transcoder-pool

Distributed transcoding coordinator.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `POOL_DB_PATH` | media-transcoder-pool | `data/pool.db` | SQLite pool state database. |
| `POOL_DISPATCH` | media-transcoder-pool | `true` | `false`/`0`: disable job dispatch to workers. |
| `POOL_STALE_AFTER_SEC` | media-transcoder-pool | `60` | Mark worker stale after N seconds without heartbeat. |
| `POOL_DISPATCH_TIMEOUT_SEC` | media-transcoder-pool | `172800` (48h) | Max seconds to wait for remote job completion. |
| `POOL_MAX_WORKERS` | media-transcoder-pool | `256` | Max registered workers. |
| `POOL_MAX_QUEUE_DEPTH` | media-transcoder-pool | `10000` | Max queued jobs. |
| `POOL_MAX_WORKER_CAPACITY` | media-transcoder-pool | `32` | Max concurrent jobs per worker registration. |
| `POOL_ALLOWED_MEDIA_ROOT` | media-transcoder-pool | — | Restrict input/output paths to this root (empty = no restriction). |
| `POOL_REQUIRE_ABSOLUTE_PATHS` | media-transcoder-pool | `false` | `true`/`1`: reject relative paths. |

---

## media-movies

Movie library management.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MOVIES_DB_PATH` | media-movies | — | SQLite database path. |
| `MOVIES_GRPC_ADDR` | media-movies | — | gRPC listen address. |
| `MOVIES_HTTP_ADDR` | media-movies | — | HTTP listen address. |
| `MOVIES_ANNOUNCE_ADDR` | media-movies | — | Announce/listen address override. |
| `MOVIES_IMAGE_DIR` | media-movies | — | Poster/backdrop image cache directory. |
| `MOVIES_METADATA_REFRESH_ENABLED` | media-movies | `true` | `false`/`0`: disable background TMDB refresh. |
| `MOVIES_METADATA_STALE_DAYS` | media-movies | `30` | Re-fetch metadata older than N days. |
| `MOVIES_METADATA_REFRESH_HOURS` | media-movies | `24` | Background refresh loop interval (hours). |
| `MOVIES_METADATA_REFRESH_BATCH` | media-movies | `10` | Max movies refreshed per cycle (max 50). |

---

## media-tvshows

TV series library management.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `TVSHOWS_DB_PATH` | media-tvshows | — | SQLite database path. |
| `TVSHOWS_GRPC_ADDR` | media-tvshows | — | gRPC listen address. |
| `TVSHOWS_HTTP_ADDR` | media-tvshows | — | HTTP listen address. |
| `TVSHOWS_IMAGE_DIR` | media-tvshows | — | Series image cache directory. |
| `TVSHOWS_METADATA_REFRESH_INTERVAL` | media-tvshows | `6h` | Background refresh loop interval (duration or hours). |
| `TVSHOWS_METADATA_STALE_AFTER` | media-tvshows | `168h` (7d) | Re-fetch when `updated_at` older than this. |

---

## media-music

Music library management.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUSIC_DATA_DIR` | media-music | — | Module data directory. |
| `MUSIC_LIBRARY_DIR` | media-music | — | Music library filesystem root. |
| `MUSIC_GRPC_ADDR` | media-music | — | gRPC listen address. |
| `MUSIC_METADATA_REFRESH_ENABLED` | media-music | `true` | `false`/`0`: disable background metadata refresh. |
| `MUSIC_METADATA_STALE_DAYS` | media-music | `30` | Re-fetch metadata older than N days. |
| `MUSIC_METADATA_REFRESH_HOURS` | media-music | `24` | Refresh loop interval (hours). |
| `MUSIC_METADATA_REFRESH_BATCH` | media-music | `10` | Max albums refreshed per cycle. |

---

## media-library-maintainer

Library maintenance, overlays, and download client integration.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MAINTAINER_DB_PATH` | media-library-maintainer | — | SQLite database path. |
| `MAINTAINER_GRPC_ADDR` | media-library-maintainer | — | gRPC listen address. |
| `MAINTAINER_MOVE_PATH` | media-library-maintainer | — | Destination for move-instead-of-delete actions. |
| `MAINTAINER_FREE_UP_ROOT` | media-library-maintainer | — | Root for free-space operations. |
| `MAINTAINER_ADD_LIST_EXCLUSION` | media-library-maintainer | `false` | `true`: auto-add list exclusions. |
| `MAINTAINER_DOWNLOAD_CLIENT_ALLOWED_HOSTS` | media-library-maintainer | — | Comma-separated allowed download client hostnames (SSRF guard). |
| `MAINTAINER_QBITTORRENT_URL` | media-library-maintainer | — | qBittorrent WebUI URL (alias: `QBITTORRENT_URL`). |
| `MAINTAINER_QBITTORRENT_USERNAME` | media-library-maintainer | — | qBittorrent username (alias: `QBITTORRENT_USERNAME`). |
| `MAINTAINER_QBITTORRENT_PASSWORD` | media-library-maintainer | — | qBittorrent password (alias: `QBITTORRENT_PASSWORD`). |
| `MAINTAINER_QBITTORRENT_KEEP_DATA` | media-library-maintainer | `false` | `true`: keep torrent data on remove. |
| `MAINTAINER_TRANSMISSION_URL` | media-library-maintainer | — | Transmission RPC URL (alias: `TRANSMISSION_URL`). |
| `MAINTAINER_TRANSMISSION_USERNAME` | media-library-maintainer | — | Transmission username. |
| `MAINTAINER_TRANSMISSION_PASSWORD` | media-library-maintainer | — | Transmission password. |
| `MAINTAINER_DELUGE_URL` | media-library-maintainer | — | Deluge RPC URL (alias: `DELUGE_URL`). |
| `MAINTAINER_DELUGE_PASSWORD` | media-library-maintainer | — | Deluge password. |
| `MAINTAINER_USERDATA_DIR` | media-library-maintainer | — | Userdata store for watch-based rules. |
| `MAINTAINER_TRAKT_CLIENT_ID` | media-library-maintainer | — | Trakt API client ID (alias: `TRAKT_CLIENT_ID`, `TRAKT_API_KEY`). |
| `MAINTAINER_OMDB_API_KEY` | media-library-maintainer | — | OMDb API key (alias: `OMDB_API_KEY`). |
| `MAINTAINER_MDBLIST_API_KEY` | media-library-maintainer | — | MDBList API key (alias: `MDBLIST_API_KEY`). |
| `MAINTAINER_MDBLIST_BASE_URL` | media-library-maintainer | — | MDBList API base URL (alias: `MDBLIST_BASE_URL`). |
| `MAINTAINER_PLEX_URL` | media-library-maintainer | — | Plex server URL for leaving-soon sync. |
| `MAINTAINER_PLEX_TOKEN` | media-library-maintainer | — | Plex authentication token. |
| `MAINTAINER_PLEX_MOVIE_SECTION` | media-library-maintainer | — | Plex movie library section name. |
| `MAINTAINER_PLEX_SHOW_SECTION` | media-library-maintainer | — | Plex TV library section name. |
| `MAINTAINER_JELLYFIN_URL` | media-library-maintainer | — | Jellyfin URL for leaving-soon sync. |
| `MAINTAINER_JELLYFIN_API_KEY` | media-library-maintainer | — | Jellyfin API key. |
| `MAINTAINER_OVERLAY_ENABLED` | media-library-maintainer | `false` | `true`: enable overlay generation. |
| `MAINTAINER_OVERLAY_TITLE_CARD` | media-library-maintainer | `false` | `true`: title-card overlay mode. |
| `MAINTAINER_OVERLAY_TEMPLATE_JSON` | media-library-maintainer | — | Custom overlay template JSON. |
| `MAINTAINER_OVERLAY_IMAGE_DIR` | media-library-maintainer | — | Overlay image assets directory. |
| `MAINTAINER_OVERLAY_DATE_FORMAT` | media-library-maintainer | — | Date format string for overlays. |
| `MAINTAINER_OVERLAY_SHOW_DATE` | media-library-maintainer | `true` | `false`: hide date on overlays. |
| `MAINTAINER_LEAVING_SOON_NOTIFY` | media-library-maintainer | `true` | `false`: disable leaving-soon notifications. |
| `MAINTAINER_PROTECT_UNWATCHED_REQUESTERS` | media-library-maintainer | `false` | `true`: protect items requested but unwatched. |
| `MAINTAINER_REQUEST_USER_MAP` | media-library-maintainer | — | Requester user ID mapping JSON. |

---

## media-custom-formats

TRaSH-style custom format scoring.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `FORMATS_DB_PATH` | media-custom-formats | `/var/lib/media-custom-formats/formats.db` | SQLite database. |
| `FORMATS_GRPC_ADDR` | media-custom-formats | `:9490` | gRPC listen address. |
| `FORMATS_SEED_DEFAULTS` | media-custom-formats | `false` | `true`: seed built-in format definitions on start. |

Additional TRaSH sync variables are set by `run-host.sh` (not `os.Getenv` in module): `FORMATS_TRASH_SYNC`, `FORMATS_TRASH_IMPORT_PROFILES`, `FORMATS_TRASH_SCORE_SET`, `FORMATS_TRASH_GUIDES_PATH`, `FORMATS_TRASH_CACHE_DIR`.

---

## backup-local

Module state backup and restore.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `BACKUP_DIR` | backup-local | `backups` | Backup archive output directory. |
| `BACKUP_SOURCE_DIRS` | backup-local | — | Comma-separated source directories to include. |
| `BACKUP_ALLOWED_ROOTS` | backup-local | — | Allowed filesystem roots (path safety). |
| `BACKUP_EXCLUDE` | backup-local | — | Comma-separated glob exclude patterns. |
| `BACKUP_SCHEDULE` | backup-local | — | Cron expression for scheduled backups. |
| `BACKUP_VERIFY_ON_CREATE` | backup-local | `false` | `true`/`1`: verify archive after creation. |
| `BACKUP_INCREMENTAL_DEFAULT` | backup-local | `false` | `true`/`1`: default new backups to incremental. |
| `BACKUP_ENCRYPT_KEY` | backup-local | — | AES encryption key for archives. |
| `BACKUP_HTTP_ADDR` | backup-local | `:9303` | HTTP health listen address. |

---

## playback-monitor

Playback session analytics and history.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `PLAYBACK_MONITOR_DB_PATH` | playback-monitor | `/var/lib/muxcore-playback-monitor/monitor.db` | SQLite database path. |
| `PLAYBACK_MONITOR_DATABASE_URL` | playback-monitor | — | Postgres URL (overrides SQLite; alias: `DATABASE_URL`). |
| `PLAYBACK_MONITOR_TIMESCALE` | playback-monitor | — | Set to enable TimescaleDB hypertable mode. |
| `PLAYBACK_MONITOR_GRPC_ADDR` | playback-monitor | `:9560` | gRPC listen address. |
| `PLAYBACK_MONITOR_HTTP_ADDR` | playback-monitor | `:8560` | HTTP listen address. |
| `PLAYBACK_MONITOR_HISTORY_RETENTION_DAYS` | playback-monitor | `0` (unlimited) | Purge history older than N days; `0` = keep all. |
| `PLAYBACK_MONITOR_ACTIVE_TIMEOUT` | playback-monitor | `15m` | Mark session inactive after no progress. |
| `PLAYBACK_MONITOR_NOTIFY_ON_START` | playback-monitor | `false` | `true`: notify on session start. |
| `PLAYBACK_MONITOR_NOTIFY_ON_STOP` | playback-monitor | `false` | `true`: notify on session stop. |
| `PLAYBACK_MONITOR_PUBLIC_API_KEY` | playback-monitor | — | API key for public ingest endpoints. |
| `PLAYBACK_MONITOR_HTTP_TOKEN` | playback-monitor | — | Bearer token for HTTP API. |
| `PLAYBACK_MONITOR_GEOIP` | playback-monitor | — | `plex`: use Plex geo API; requires DB for `mmdb`. |
| `PLAYBACK_MONITOR_GEOIP_DB` | playback-monitor | — | MaxMind GeoIP2 `.mmdb` database path. |
| `PLAYBACK_MONITOR_APPRISE_URL` | playback-monitor | — | Default Apprise notification URL (alias: `APPRISE_URL`). |
| `PLAYBACK_MONITOR_APPRISE_TOKEN` | playback-monitor | — | Apprise auth token (alias: `APPRISE_TOKEN`). |
| `PLAYBACK_MONITOR_ALLOW_LOCAL_WEBHOOKS` | playback-monitor | `false` | `true`: allow webhook URLs to private IPs. |

---

## playback-guard

Sharing detection and playback policy enforcement.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `PLAYBACK_GUARD_DB_PATH` | playback-guard | `/var/lib/muxcore-playback-guard/guard.db` | SQLite database. |
| `PLAYBACK_GUARD_GRPC_ADDR` | playback-guard | `127.0.0.1:9561` | gRPC listen address. |
| `PLAYBACK_GUARD_HTTP_ADDR` | playback-guard | `127.0.0.1:9562` | HTTP listen address. |
| `PLAYBACK_GUARD_NOTIFY_ON_VIOLATION` | playback-guard | `false` | `true`: notify on policy violation. |
| `PLAYBACK_GUARD_GEOIP_DB` | playback-guard | — | MaxMind database for geo rules. |
| `PLAYBACK_GUARD_INACTIVITY_SWEEP_SEC` | playback-guard | — | Inactivity sweep interval (seconds). |
| `PLAYBACK_GUARD_POLICY_YAML` | playback-guard | — | Policy YAML file path. |

---

## jellyfin

Jellyfin bridge module.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `JELLYFIN_GRPC_ADDR` | jellyfin | — | gRPC listen address. |
| `JELLYFIN_HTTP_ADDR` | jellyfin | — | HTTP listen address. |
| `JELLYFIN_DATA_DIR` | jellyfin | — | Module data directory. |
| `JELLYFIN_BASE_URL` | jellyfin | — | Jellyfin server URL (alias: `JELLYFIN_URL`). |
| `JELLYFIN_API_KEY` | jellyfin | — | Jellyfin API key. |
| `JELLYFIN_WEBHOOK_SECRET` | jellyfin | — | Webhook HMAC secret. |
| `JELLYFIN_CONFLICT_MODE` | jellyfin | — | Library conflict resolution mode. |
| `JELLYFIN_SESSIONS_POLL_SECONDS` | jellyfin | — | Active session poll interval. |
| `JELLYFIN_HEALTH_PROBE_SECONDS` | jellyfin | — | Health probe interval. |
| `JELLYFIN_CATALOG_SYNC_SEC` | jellyfin | — | Library catalog sync interval. |
| `JELLYFIN_PLAY_URL_STYLE` | jellyfin | — | Playback URL format style. |
| `JELLYFIN_SSE` | jellyfin | — | Server-sent events for live updates. |
| `JELLYFIN_INSECURE_DISABLE_TLS` | jellyfin | `false` | `true`: skip TLS to Jellyfin server. |
| `USERDATA_SYNC` | jellyfin | — | `true`: enable userdata sync loop. |
| `USERDATA_SYNC_INTERVAL` | jellyfin | — | Sync interval (duration or seconds). |
| `USERDATA_SYNC_INTERVAL_SECONDS` | jellyfin | — | Sync interval in seconds. |
| `USERDATA_LOCAL_URL` | jellyfin | — | Household userdata mesh URL. |
| `USERDATA_PUSH_TO_JELLYFIN` | jellyfin | `false` | `true`: push userdata changes to Jellyfin. |
| `USERDATA_USER_MAP` | jellyfin | — | MuxCore ↔ Jellyfin user ID map. |
| `JELLYFIN_USERDATA_PUSH_URL` | media-ui (BFF) | — | Jellyfin userdata push endpoint for BFF. |

---

## media-ui (BFF: mediauiprox)

Consumer SPA backend (`_mvp/cmd/mediauiprox`).

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MEDIA_UI_PUBLIC_URL` | media-ui | — | Public SPA URL. |
| `MEDIA_UI_TRUSTED_PROXIES` | media-ui | — | Edge proxy CIDRs. |
| `MEDIA_UI_UPSTREAM_TIMEOUT` | media-ui | — | Upstream HTTP client timeout. |
| `MEDIA_UI_USERDATA_DIR` | media-ui | — | Userdata storage directory. |
| `USERDATA_LOCAL_URL` | media-ui | — | Userdata-local mesh URL. |
| `USERDATA_PREFER_MESH` | media-ui | `true` | `0`: prefer direct URL over mesh. |
| `ADMIN_UI_PLAYBACK_FILE` | media-ui | — | Shared playback policy file with admin-ui. |

---

## Other media modules

### media-root-folders

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `ROOTS_DB_PATH` | media-root-folders | — | SQLite database. |
| `ROOTS_GRPC_ADDR` | media-root-folders | — | gRPC listen address. |
| `ROOTS_REQUIRE_ACCESSIBLE` | media-root-folders | — | Require paths to exist at registration. |
| `ROOTS_BROWSE_PREFIXES` | media-root-folders | — | Allowed browse path prefixes. |

### media-list-sync

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `LISTSYNC_DB_PATH` | media-list-sync | — | SQLite database. |
| `LISTSYNC_GRPC_ADDR` | media-list-sync | — | gRPC listen address. |
| `LISTSYNC_HTTP_ADDR` | media-list-sync | — | HTTP listen address. |
| `LISTSYNC_POLL_INTERVAL` | media-list-sync | — | Import list poll interval. |

### media-subtitles

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SUBS_DB_PATH` | media-subtitles | — | SQLite database. |
| `SUBS_DIR` | media-subtitles | — | Subtitle file storage directory. |
| `SUBS_GRPC_ADDR` | media-subtitles | — | gRPC listen address. |
| `OPENSUBTITLES_API_KEY` | media-subtitles | — | OpenSubtitles API key. |

### media-rename

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `RENAME_DB_PATH` | media-rename | — | SQLite database. |
| `RENAME_GRPC_ADDR` | media-rename | — | gRPC listen address. |
| `RENAME_IMPORT_MODE` | media-rename | — | File import naming mode. |

---

## _mvp (stack launcher)

Variables consumed by `run-host.sh`, deploy scripts, and smoke tests. See [`_mvp/.env.example`](../_mvp/.env.example) for the full annotated list.

### Paths and bootstrap

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MVP_LIBRARY_ROOT` | _mvp | `./data/library` | Movie library root passed to modules. |
| `MVP_TV_LIBRARY_ROOT` | _mvp | `./data/library/tv` | TV library root. |
| `MVP_MUSIC_LIBRARY_ROOT` | _mvp | `./data/library/music` | Music library root. |
| `MVP_DOWNLOADS_DIR` | _mvp | `./data/downloads` | Acquisition downloads directory. |
| `MVP_TOKEN_FILE` | _mvp | `./run/admin.token` | Admin token output path. |
| `MVP_ADMIN_USER` | _mvp | — | Dev bootstrap admin username. |
| `MVP_ADMIN_PASSWORD` | _mvp | — | Dev bootstrap admin password (**not for production**). |
| `MVP_ALLOW_ADHOC_BUILD` | _mvp | `0` | `1`: allow local `go build` for origin-pinned modules. |

### Deploy notifications

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MVP_DEPLOY_NOTIFY_URL` | _mvp | — | Webhook URL for deploy success/failure alerts. |
| `MVP_DEPLOY_APPRISE_URL` | _mvp | — | Apprise URL for deploy notifications. |

### Module enable flags (`MVP_ENABLE_*`)

Set to `1` to start optional sidecars. Defaults vary; see `run-host.sh`.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MVP_ENABLE_MEDIA_UI` | _mvp | `1` | Consumer SPA + BFF (`mediauiprox`). |
| `MVP_ENABLE_MEDIA_TRANSCODER` | _mvp | auto with UI | FFmpeg transcoder sidecar. |
| `MVP_ENABLE_MEDIA_TRANSCODER_POOL` | _mvp | `0` | Distributed transcoder pool. |
| `MVP_ENABLE_JELLYFIN` | _mvp | `0` | Jellyfin bridge (separate from NixOS Jellyfin). |
| `MVP_ENABLE_USERDATA_LOCAL` | _mvp | `1` | Household userdata mesh. |
| `MVP_ENABLE_BACKUP_LOCAL` | _mvp | `0` | Backup module. |
| `MVP_ENABLE_PLAYBACK_MONITOR` | _mvp | auto with UI | Playback sessions / Now watching. Compose still `--profile playback-monitor`. |
| `MVP_ENABLE_PLAYBACK_GUARD` | _mvp | `0` | Sharing detection. |
| `MVP_ENABLE_MEDIA_LIBRARY_MAINTAINER` | _mvp | `0` | Library maintainer. |
| `MVP_ENABLE_MEDIA_LIST_SYNC` | _mvp | `0` | Import list sync. |
| `MVP_ENABLE_NOTIFICATION_APPRISE` | _mvp | `0` | Apprise notification sink. |
| `MVP_ENABLE_AUTH_OIDC` | _mvp | `0` | OIDC auth (**conflicts with auth-local**). |
| `MVP_ENABLE_CACHE_REDIS` | _mvp | `0` | Redis cache sidecar. |
| `MVP_ENABLE_DATABASE_POSTGRES` | _mvp | `0` | Postgres database module. |
| `MVP_ENABLE_DOWNLOADER_TORRENT` | _mvp | `0` | Native torrent downloader. |
| `MVP_ENABLE_DOWNLOADER_QBITTORRENT` | _mvp | `0` | qBittorrent bridge. |
| `MVP_ENABLE_DOWNLOADER_USENET` | _mvp | `0` | Native usenet downloader. |
| `MVP_ENABLE_DOWNLOADER_SABNZBD` | _mvp | `0` | SABnzbd bridge. |
| `MVP_ENABLE_INDEXER_PIRATEBAY` | _mvp | `0` | Pirate Bay indexer. |
| `MVP_ENABLE_INDEXER_TORZNAB` | _mvp | `0` | Torznab indexer. |
| `MVP_ENABLE_WORKFLOW_TAPESTRY` | _mvp | `0` | Request workflow DAGs. |
| `MVP_ENABLE_STORAGE_S3` | _mvp | `0` | S3/MinIO storage sidecar. |
| `MVP_ENABLE_STORAGE_CEPH` | _mvp | `0` | Ceph storage sidecar. |
| `MVP_ENABLE_STORAGE_OVERLAY` | _mvp | `0` | Encrypted overlay storage. |
| `MVP_ENABLE_MEDIA_GRAPH` | _mvp | `0` | Media relationship graph. |
| `MVP_ENABLE_MEDIA_TAGGING` | _mvp | `0` | File tagging sidecar. |
| `MVP_ENABLE_MEDIA_DLNA` | _mvp | `0` | DLNA server. |
| `MVP_ENABLE_MEDIA_AUDIOBOOKS` | _mvp | `0` | Audiobooks module. |
| `MVP_ENABLE_MEDIA_BOOKS` | _mvp | `0` | Books module. |
| `MVP_ENABLE_MEDIA_COMICS` | _mvp | `0` | Comics module. |
| `MVP_ENABLE_MEDIA_INTRO_OUTRO` | _mvp | `0` | Intro/outro skip detection. |
| `MVP_ENABLE_PLEX` | _mvp | `0` | Plex bridge. |
| `MVP_ENABLE_EMBY` | _mvp | `0` | Emby bridge. |
| `MVP_ENABLE_SECRETS_VAULT` | _mvp | `0` | HashiCorp Vault secrets (**conflicts with secrets-file**). |
| `MVP_ENABLE_CACHE_LOCAL` | _mvp | `0` | In-process cache module. |
| `MVP_ENABLE_CIRCUITBREAKER_SIMPLE` | _mvp | `0` | Circuit breaker module. |
| `MVP_ENABLE_CONFIG_WATCHER` | _mvp | `0` | Config hot-reload watcher. |
| `MVP_ENABLE_DATA_REDACTION` | _mvp | `0` | PII redaction module. |
| `MVP_ENABLE_DISTRIBUTED_LOCK` | _mvp | `0` | Distributed lock module. |
| `MVP_ENABLE_EXECUTOR_SHELL` | _mvp | `0` | Shell executor module. |
| `MVP_ENABLE_FEATURE_FLAGS` | _mvp | `0` | Feature flags file module. |
| `MVP_ENABLE_INPUT_VALIDATE` | _mvp | `0` | JSON schema validator. |
| `MVP_ENABLE_LOGGING_FILE` | _mvp | `0` | File logging sink. |
| `MVP_ENABLE_METRICS_PROMETHEUS` | _mvp | `0` | Prometheus metrics exporter. |
| `MVP_ENABLE_SCHEDULER_CRON` | _mvp | `0` | Cron scheduler module. |
| `MVP_ENABLE_SERIALIZATION_SAFE` | _mvp | `0` | Safe serialization module. |
| `MVP_ENABLE_SPOOL_RESOLVER` | _mvp | `0` | HTTP spool resolver. |
| `MVP_ENABLE_TRACING_OTLP` | _mvp | `0` | OpenTelemetry tracing. |
| `MVP_ENABLE_WORKER_POOL` | _mvp | `0` | In-memory worker pool. |

### Smoke / CI

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SMOKE_API_URL` | _mvp | — | api-rest URL for smoke tests. |
| `SMOKE_CORE_URL` | _mvp | — | Core HTTP URL for smoke tests. |
| `SMOKE_ADMIN_URL` | _mvp | — | Admin UI URL for smoke tests. |
| `SMOKE_JELLYFIN_URL` | _mvp | — | Jellyfin URL for smoke tests. |
| `SMOKE_TIMEOUT_SEC` | _mvp | `180` | Smoke test timeout. |
| `SMOKE_CATALOG` | _mvp | — | Catalog fixture selector. |
| `SMOKE_MODULES` | _mvp | — | Module subset for smoke. |
| `SMOKE_REQUIRE_TMDB_SEARCH` | _mvp | — | Require live TMDB in smoke. |
| `SMOKE_LIVE_JELLYFIN` | _mvp | auto | Force live Jellyfin smoke on/off. |

---

## Related documentation

- [`AGENTS.md`](../AGENTS.md) — deployment, vault SSH, public URLs
- [`_mvp/PORTS.md`](../_mvp/PORTS.md) — default listen ports
- [`_mvp/.env.example`](../_mvp/.env.example) — annotated local dev template
- [`admin-ui/docs/SETTINGS-COVERAGE.md`](../admin-ui/docs/SETTINGS-COVERAGE.md) — admin UI settings ↔ env mapping
- Per-module `AGENTS.md` — build and test commands

---

*Last scanned: 2026-09-03. Regenerate after adding `os.Getenv` calls: `rg 'os\.Getenv\("([A-Z0-9_]+)"\)' --glob '*.go' -r '$1' | sort -u`*
