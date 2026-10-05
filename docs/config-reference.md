# MuxCore Configuration Reference

Environment variables read by MuxCore modules at startup. Values in `muxcore.json` or module flags may also apply; **environment variables take highest precedence** where both exist (see `core/internal/config/config.go`).

**Source of truth:** `os.Getenv` / `os.LookupEnv` in Go module code. Coverage is enforced by `node scripts/check-env-docs.mjs` (NFR-OPS-005, run in CI): every variable read by an active module must be documented here, and every documented variable must still be read. Variables not described by hand live in the generated block under [Additional variables](#additional-variables-generated); run `node scripts/check-env-docs.mjs --write` to refresh it, then improve the descriptions. Internal/test-only variables are listed with reasons in `scripts/check-env-docs.allow`.

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

Mesh identity (ADR-0017, read by `sdk/go/module/meshid`; prose because `scripts/check-env-docs.mjs` cannot resolve these constants yet):

- `MUXCORE_TLS_DIR` (default `<MUXCORE_DATA_DIR>/mesh-id`): identity directory (`module.crt`, `module.key`, `ca.crt`; 0600 files), reused on restart, written on enrollment. Registry compose: a `<service>-id` volume at `/data/mesh-id`.
- `MUXCORE_BOOTSTRAP_TOKEN`: single-use enrollment token (`mct_2_<id>_…`) used when no identity is stored. Registry compose passes `MUXCORE_ENROLL_TOKEN_<ID>` (module ID upper-cased, `-`/`.` as `_`) from `.env`, written by `_mvp/scripts/gen-enrollment.sh`.

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
| `MUXCORE_GRPC_CA_CERT_DIR` | core | `<data>/ca` | Core CA directory (CA key, enrollment ledger `enrolled.json`, core's auto-issued server certificate). Registry compose: the `core-ca` volume at `/app/ca`, outside the backed-up `core-data`. |
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

Enrollment (ADR-0017; prose because `scripts/check-env-docs.mjs` cannot resolve these constants yet):

- `MUXCORE_ENROLL_SECRET`: HMAC key (≥ 16 bytes) for single-use module enrollment tokens, `mct_2_<id>_` + hex(HMAC-SHA256(secret, id)) (`muxcored enroll token <id>`); unset disables them. The registry compose requires it (`_mvp/scripts/gen-enrollment.sh`).
- `MUXCORE_ENROLL_SAN_ALLOW`: comma-separated extra DNS names/IPs (`*.suffix` wildcards) modules may request as certificate SANs, e.g. compose service names that differ from the module ID (`jellyfin-bridge`).

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
| `AUTHCTL_TOKEN` | auth-local | — | CLI token for `authctl` (bypasses login). |

**Session lifetime and login backoff (not env-configurable):** sessions use a fixed 24h TTL (`auth-local/internal/store/store.go`; there are no `AUTH_SESSION_*` variables). Failed logins use exponential lockout starting at **1 minute**, capped at **30 minutes** (`auth-local/internal/ratelimit/limiter.go`). There is no `AUTH_LOGIN_BACKOFF_MAX` variable.

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

---

## api-rest

HTTP REST gateway to mesh gRPC.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `API_REST_HTTP_ADDR` | api-rest | — | HTTP listen address. |
| `API_REST_GRPC_ADDR` | api-rest | — | Module gRPC listen address. |
| `API_REST_MODULE_ID` | api-rest | — | Module ID override. |
| `API_REST_CORS_ORIGINS` | api-rest | — | Allowed CORS origins (comma-separated). |

---

## media-transcoder

FFmpeg transcoding sidecar.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `TRANSCODER_DB_PATH` | media-transcoder | `/var/lib/media-transcoder/transcoder.db` | SQLite job database. |
| `TRANSCODER_GRPC_ADDR` | media-transcoder | `:9525` | gRPC listen address. |
| `TRANSCODER_HTTP_ADDR` | media-transcoder | `:9526` | Playback transcode HTTP server. |
| `TRANSCODER_FFMPEG_BIN` | media-transcoder | `ffmpeg` | FFmpeg executable path. |
| `TRANSCODER_MAX_CONCURRENT` | media-transcoder | `2` | Max parallel transcode jobs. |
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

---

## media-tvshows

TV series library management.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `TVSHOWS_DB_PATH` | media-tvshows | — | SQLite database path. |
| `TVSHOWS_GRPC_ADDR` | media-tvshows | — | gRPC listen address. |
| `TVSHOWS_HTTP_ADDR` | media-tvshows | — | HTTP listen address. |
| `TVSHOWS_IMAGE_DIR` | media-tvshows | — | Series image cache directory. |

---

## media-music

Music library management.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUSIC_DATA_DIR` | media-music | — | Module data directory. |
| `MUSIC_LIBRARY_DIR` | media-music | — | Music library filesystem root. |
| `MUSIC_GRPC_ADDR` | media-music | — | gRPC listen address. |

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
| `MAINTAINER_PLEX_URL` | media-library-maintainer | — | Plex server URL for leaving-soon sync. |
| `MAINTAINER_PLEX_TOKEN` | media-library-maintainer | — | Plex authentication token. |
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
| `BACKUP_SCHEDULE_CRON` | backup-local | — | Cron expression for scheduled backups; empty disables scheduling. |

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

---

## playback-guard

Sharing detection and playback policy enforcement.

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `PLAYBACK_GUARD_DB_PATH` | playback-guard | `/var/lib/muxcore-playback-guard/guard.db` | SQLite database. |
| `PLAYBACK_GUARD_GRPC_ADDR` | playback-guard | `127.0.0.1:9561` | gRPC listen address. |
| `PLAYBACK_GUARD_NOTIFY_ON_VIOLATION` | playback-guard | `false` | `true`: notify on policy violation. |
| `PLAYBACK_GUARD_INACTIVITY_SWEEP_SEC` | playback-guard | — | Inactivity sweep interval (seconds). |

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
| `JELLYFIN_CATALOG_SYNC_SEC` | jellyfin | — | Library catalog sync interval. |
| `JELLYFIN_PLAY_URL_STYLE` | jellyfin | — | Playback URL format style. |
| `JELLYFIN_SSE` | jellyfin | — | Server-sent events for live updates. |
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

## Additional variables (generated)

Variables read by active modules that are not described in the sections above. Maintained by `node scripts/check-env-docs.mjs --write` (new rows are appended with derived descriptions; existing rows are preserved, so refine them in place).

<!-- BEGIN GENERATED: check-env-docs -->

### _mvp

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUDIOBOOKS_HTTP_URL` | _mvp | `http://127.0.0.1:9671` | HTTP base URL of the module. |
| `AUTH_HTTP_INTERNAL_URL` | _mvp | — | Auth http internal url. |
| `AUTOMATION_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9460` | gRPC address the BFF dials for this module. |
| `BACKUP_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9302` | gRPC address the BFF dials for this module. |
| `BACKUP_RESTORE_DIR` | backup-local, _mvp | — | Restore extract allow-list. backup-local refuses a target outside this directory. |
| `BOOKS_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9650` | gRPC address the BFF dials for this module. |
| `BOOKS_HTTP_URL` | _mvp | `http://127.0.0.1:9651` | HTTP base URL of the module. |
| `COMICS_HTTP_URL` | _mvp | `http://127.0.0.1:9661` | HTTP base URL of the module. |
| `DEBRID_HTTP_URL` | _mvp | `http://127.0.0.1:9631` | HTTP base URL of the module. |
| `DOWNLOADER_ENGINE` | _mvp, downloader-native-torrent, downloader-qbittorrent, downloader-sabnzbd, media-automation | — | Downloader engine. |
| `DOWNLOADER_QBIT_HTTP_URL` | _mvp | `http://127.0.0.1:9463` | HTTP base URL of the module. |
| `DOWNLOADER_SAB_HTTP_URL` | _mvp | `http://127.0.0.1:9621` | HTTP base URL of the module. |
| `DOWNLOADER_TORRENT_HTTP_URL` | _mvp | `http://127.0.0.1:9464` | HTTP base URL of the module. |
| `DOWNLOADER_USENET_HTTP_URL` | _mvp | `http://127.0.0.1:9623` | HTTP base URL of the module. |
| `FFPROBE_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9480` | gRPC address the BFF dials for this module. |
| `FORMATS_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9490` | gRPC address the BFF dials for this module. |
| `GRAPH_HTTP_TOKEN` | _mvp | — | Credential (secret). |
| `GRAPH_HTTP_URL` | _mvp | `http://127.0.0.1:9731` | HTTP base URL of the module. |
| `GRAPH_MODULE_TOKEN` | _mvp, media-graph | — | Mesh module token. |
| `HEALTH_MONITOR_HTTP_TOKEN` | health-monitor | — | Bearer token for `/status`; required when `HEALTH_MONITOR_HTTP_ADDR` is not loopback (NFR-SEC-011). |
| `INDEXER_PIRATEBAY_HTTP_URL` | _mvp | `http://127.0.0.1:9487` | HTTP base URL of the module. |
| `INDEXER_TORZNAB_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9486` | gRPC address the BFF dials for this module. |
| `INTRO_OUTRO_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9710` | gRPC address the BFF dials for this module. |
| `JELLYFIN_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9475` | gRPC address the BFF dials for this module. |
| `LISTSYNC_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9530` | gRPC address the BFF dials for this module. |
| `MAINTAINER_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9545` | gRPC address the BFF dials for this module. |
| `MEDIA_UI_ALLOWED_ORIGINS` | _mvp (mediauiprox) | — | Extra browser origins (comma-separated) accepted by the BFF CSRF check besides `MEDIA_UI_PUBLIC_URL`. |
| `MEDIA_UI_CSP` | _mvp (mediauiprox) | built-in SPA policy | Overrides the BFF Content-Security-Policy header. |
| `MEDIA_UI_DIST` | _mvp | — | Media ui dist. |
| `MEDIA_UI_LIBRARY_PATHS_FILE` | _mvp | — | Media ui library paths file. |
| `MEDIA_UI_LISTEN` | _mvp | `:5173` | Media ui listen. |
| `MEDIA_UI_LIVETV_FILE` | _mvp | — | Media ui livetv file. |
| `MEDIA_UI_PASSWORD_RESET_FILE` | _mvp | — | Media ui password reset file. |
| `MEDIA_UI_REQUIRE_AUTH` | _mvp | `1` | Media ui require auth. |
| `MEDIA_UI_SESSION_KEY` | _mvp (mediauiprox) | generated 0600 `session.key` | 32-byte key (base64 or hex) encrypting the auth-local token in the BFF `sessions.json`. |
| `METADATA_TMDB_GRPC_ADDR` | _mvp | `127.0.0.1:9411` | gRPC listen address. |
| `MOVIES_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9420` | gRPC address the BFF dials for this module. |
| `MOVIES_HTTP_URL` | _mvp | `http://127.0.0.1:9430` | HTTP base URL of the module. |
| `MUSIC_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9640` | gRPC address the BFF dials for this module. |
| `MUSIC_HTTP_URL` | _mvp | `http://127.0.0.1:9641` | HTTP base URL of the module. |
| `MVP_ADMIN_PASSWORD_FILE` | _mvp | `data/auth/admin.password` | File holding the generated first-admin password (0600); `MVP_ADMIN_PASSWORD` overrides. |
| `MVP_BIN` | _mvp | — | Bin. |
| `MVP_CADDYFILE` | _mvp | — | Caddyfile. |
| `MVP_CADDY_BIN` | _mvp | — | Caddy bin. |
| `MVP_DEPLOY_MEDIA_UI_DIST` | _mvp | — | Deploy media ui dist. |
| `MVP_DEPLOY_MVP` | _mvp | — | Deploy mvp. |
| `MVP_DEPLOY_RUN` | _mvp | — | Deploy run. |
| `MVP_DEPLOY_SSH` | _mvp | — | Deploy ssh. |
| `MVP_DOWNLOAD_DIR` | _mvp | — | Download dir. |
| `MVP_ENABLE_ACQUISITION` | _mvp | — | Enable acquisition. |
| `MVP_ENABLE_AI` | _mvp | — | Enable ai. |
| `MVP_ENABLE_AI_FILTER` | _mvp | — | Enable ai filter. |
| `MVP_ENABLE_AI_LIBRARIAN` | _mvp | — | Enable ai librarian. |
| `MVP_ENABLE_AI_RECOMMEND` | _mvp | — | Enable ai recommend. |
| `MVP_ENABLE_AI_RUNTIME` | _mvp | — | Enable ai runtime. |
| `MVP_ENABLE_AI_SUBTITLES` | _mvp | — | Enable ai subtitles. |
| `MVP_ENABLE_AI_TICKETS` | _mvp | — | Enable ai tickets. |
| `MVP_ORIGIN_RECORD` | _mvp | — | Origin record. |
| `MVP_SKIP_PREFLIGHT` | _mvp | — | Skip preflight. |
| `MVP_STOP_TIMEOUT_SEC` | _mvp (run-host) | `15` | Seconds `run-host.sh stop` waits for processes to exit before force-killing them. |
| `NOTIFY_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9441` | gRPC address the BFF dials for this module. |
| `PLAYBACK_GUARD_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9561` | gRPC address the BFF dials for this module. |
| `PLAYBACK_MONITOR_HTTP_URL` | _mvp | `http://127.0.0.1:8560` | HTTP base URL of the module. |
| `PLEX_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9476` | gRPC address the BFF dials for this module. |
| `PROWLARR_URL` | _mvp, indexer-torznab | — | Prowlarr url. |
| `RENAME_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9510` | gRPC address the BFF dials for this module. |
| `REQUEST_MEDIA_HTTP_URL` | _mvp | `http://127.0.0.1:9380` | HTTP base URL of the module. |
| `ROOTS_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9540` | gRPC address the BFF dials for this module. |
| `SCANNER_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9470` | gRPC address the BFF dials for this module. |
| `SMOKE_ACQUISITION_DOWNLOADERS` | _mvp | native-torrent, qbittorrent | Downloader module IDs that enable the smoke acquisition step. |
| `SMOKE_ACQUISITION_FILE_TIMEOUT_SEC` | _mvp | — | Smoke: seconds to wait for the imported file to appear on the movie. |
| `SMOKE_ACQUISITION_INDEXERS` | _mvp | indexer-piratebay | Indexer module IDs that enable the smoke acquisition step. |
| `SMOKE_ACQUISITION_TIMEOUT_SEC` | _mvp | 120 | Smoke: seconds to wait for the grab to complete. |
| `SMOKE_HEALTH_MONITOR_STATUS` | _mvp | — | Smoke health monitor status. |
| `SMOKE_JELLYFIN_WEBHOOK` | _mvp | — | Smoke jellyfin webhook. |
| `SMOKE_MEDIA_UI_URL` | _mvp | — | Smoke media ui url. |
| `SUBTITLES_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9520` | gRPC address the BFF dials for this module. |
| `SUBTITLES_HTTP_URL` | _mvp | `http://127.0.0.1:9521` | HTTP base URL of the module. |
| `TAGGING_HTTP_URL` | _mvp | `http://127.0.0.1:9741` | HTTP base URL of the module. |
| `TORZNAB_URL` | _mvp, indexer-torznab | — | Torznab url. |
| `TRANSCODER_HTTP_URL` | _mvp | `http://127.0.0.1:9526` | media-transcoder playback base URL. Dev and run-host stay `http://127.0.0.1:9526`. Household compose sets `https://media-transcoder:9526`; the BFF then presents its mesh client certificate. |
| `TVSHOWS_GRPC_CLIENT_ADDR` | _mvp | `127.0.0.1:9440` | gRPC address the BFF dials for this module. |
| `TVSHOWS_HTTP_URL` | _mvp | `http://127.0.0.1:9450` | HTTP base URL of the module. |
| `WG_CONF` | _mvp, downloader-native-torrent, indexer-piratebay, indexer-torznab | — | Wg conf. |

### admin-ui

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `ADMIN_UI_BRANDING_FILE` | admin-ui | under `ADMIN_UI_DATA_DIR` | Path of the branding settings file. |
| `ADMIN_UI_HEALTH_MONITOR_TOKEN` | admin-ui | — | Bearer token admin-ui sends to health-monitor `/status` (falls back to `HEALTH_MONITOR_HTTP_TOKEN`). |
| `ADMIN_UI_LIVETV_FILE` | admin-ui | under `ADMIN_UI_DATA_DIR` | Path of the Live TV settings file. |
| `ADMIN_UI_NETWORKING_FILE` | admin-ui | under `ADMIN_UI_DATA_DIR` | Path of the networking settings file. |
| `ADMIN_UI_PARENTAL_FILE` | admin-ui | under `ADMIN_UI_DATA_DIR` | Path of the parental-controls settings file. |
| `ADMIN_UI_PASSWORD_RESET_FILE` | admin-ui | under `ADMIN_UI_DATA_DIR` | Path of the password-reset request file shared with the BFF. |
| `ADMIN_UI_REQUEST_MEDIA_URL` | admin-ui | — | Request media url. |
| `ADMIN_UI_RESTORE_ROOT` | admin-ui | backup-local restore area | Only directory restores may target (FR-BAK-003, RULE-VAL-1). |
| `ADMIN_UI_SCHEDULER_TOKEN` | admin-ui | — | Bearer token admin-ui sends to scheduler-cron (falls back to `SCHEDULER_HTTP_TOKEN`). |
| `ADMIN_UI_SESSION_FILE` | admin-ui | under `ADMIN_UI_DATA_DIR` | Path of the persisted session store (0600, hashed tokens). |
| `ADMIN_UI_SESSION_KEY` | admin-ui | generated, persisted 0600 under `ADMIN_UI_DATA_DIR` | Key protecting session tokens at rest (NFR-SEC-005). |
| `MVP_RUN` | admin-ui, muxcorectl-cli | — | Mvp run. |
| `SCHEDULER_HTTP_TOKEN` | scheduler-cron | — | Bearer token for every HTTP path except `/health`; required when `SCHEDULER_HTTP_ADDR` is not loopback (NFR-SEC-011). |

### ai-filter

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AI_FILTER_GRPC_ADDR` | ai-filter | — | gRPC listen address. |

### ai-librarian

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AI_LIBRARIAN_GRPC_ADDR` | ai-librarian | — | gRPC listen address. |

### ai-recommend

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AI_RECOMMEND_GRPC_ADDR` | ai-recommend | — | gRPC listen address. |

### ai-runtime

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AI_RUNTIME_API_KEY` | ai-runtime | `OPENAI_API_KEY` | API key. |
| `AI_RUNTIME_BASE_URL` | ai-runtime | — | Base url. |
| `AI_RUNTIME_GRPC_ADDR` | ai-runtime | — | gRPC listen address. |
| `AI_RUNTIME_MODEL` | ai-runtime | — | Model. |
| `AI_RUNTIME_PROVIDER` | ai-runtime | — | Provider. |
| `OPENAI_API_KEY` | ai-runtime | `OPENAI_API_KEY` | API key. |

### ai-subtitles

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AI_SUBTITLES_AUTO` | ai-subtitles | — | Auto. |
| `AI_SUBTITLES_GRPC_ADDR` | ai-subtitles | — | gRPC listen address. |

### ai-tickets

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AI_TICKETS_GRPC_ADDR` | ai-tickets | — | gRPC listen address. |

### auth-local

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUTH_BOOTSTRAP_PASSWORD` | auth-local | — | Credential (secret). |
| `AUTH_BOOTSTRAP_USER` | auth-local | — | Bootstrap user. |
| `AUTH_SECRET_KEY` | auth-local | generated key file | 32-byte key (hex or base64) encrypting TOTP secrets at rest; wins over `AUTH_SECRET_KEY_FILE`. Back it up with the auth database. |
| `AUTH_SECRET_KEY_FILE` | auth-local | `<db dir>/auth-secret.key` (0600) | File holding the TOTP encryption key; generated once when neither variable is set. |
| `AUTH_TLS_CA` | auth-local | — | CA bundle for mesh TLS verification. |
| `AUTH_TLS_CERT` | auth-local | — | TLS certificate file for the module's mesh client/server. |
| `AUTH_TLS_DIR` | auth-local | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `AUTH_TLS_KEY` | auth-local | — | TLS private key file. |
| `AUTH_TLS_SERVER_NAME` | auth-local | — | TLS server name override for mesh dial. |

### auth-oidc

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUTH_OIDC_CLIENT_ID` | auth-oidc | `CLIENT_ID` | Client id. |
| `AUTH_OIDC_CLIENT_SECRET` | auth-oidc | `CLIENT_SECRET` | Credential (secret). |
| `AUTH_OIDC_REDIRECT_URL` | auth-oidc | `REDIRECT_URL` | Redirect url. |
| `AUTH_OIDC_ROLES_CLAIM` | auth-oidc | `ROLES_CLAIM` | Roles claim. |
| `AUTH_OIDC_SESSION_TTL` | auth-oidc | — | Session ttl. |
| `CLIENT_ID` | auth-oidc | `CLIENT_ID` | Client id. |
| `CLIENT_SECRET` | auth-oidc | `CLIENT_SECRET` | Credential (secret). |
| `REDIRECT_URL` | auth-oidc | `REDIRECT_URL` | Redirect url. |
| `ROLES_CLAIM` | auth-oidc | `ROLES_CLAIM` | Roles claim. |

### backup-local

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `BACKUP_GRPC_ADDR` | backup-local | — | gRPC listen address. |
| `BACKUP_GRPC_BIND_ALL` | backup-local | — | Grpc bind all. |
| `BACKUP_RETENTION_COUNT` | backup-local | `0` | Retention count. |
| `BACKUP_RETENTION_DAYS` | backup-local | `0` | Retention days. |
| `BACKUP_TLS_CA` | backup-local | — | CA bundle for mesh TLS verification. |
| `BACKUP_TLS_CERT` | backup-local | — | TLS certificate file for the module's mesh client/server. |
| `BACKUP_TLS_DIR` | backup-local | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `BACKUP_TLS_KEY` | backup-local | — | TLS private key file. |

### cache-local

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `CACHE_LOCAL_GRPC_ADDR` | cache-local | — | gRPC listen address. |
| `CACHE_LOCAL_TLS_CA` | cache-local | — | CA bundle for mesh TLS verification. |
| `CACHE_LOCAL_TLS_CERT` | cache-local | — | TLS certificate file for the module's mesh client/server. |
| `CACHE_LOCAL_TLS_DIR` | cache-local | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `CACHE_LOCAL_TLS_KEY` | cache-local | — | TLS private key file. |
| `CACHE_LOCAL_TTL` | cache-local | — | Ttl. |

### cache-redis

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `CACHE_GRPC_ADDR` | cache-redis | — | gRPC listen address. |
| `CACHE_REDIS_TLS_CA` | cache-redis | — | CA bundle for mesh TLS verification. |
| `CACHE_REDIS_TLS_CERT` | cache-redis | — | TLS certificate file for the module's mesh client/server. |
| `CACHE_REDIS_TLS_DIR` | cache-redis | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `CACHE_REDIS_TLS_KEY` | cache-redis | — | TLS private key file. |
| `REDIS_ADDR` | cache-redis | — | Addr. |
| `REDIS_DB` | cache-redis | — | Db. |
| `REDIS_PASSWORD` | cache-redis | — | Credential (secret). |

### call-policy-default

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `CALL_POLICY_DEV_FILE` | call-policy-default | — | Policy dev file. |
| `CALL_POLICY_EVENT_SUBSCRIBE_DELAY` | call-policy-default | — | Policy event subscribe delay. |
| `CALL_POLICY_FILE` | call-policy-default | — | Policy file. |
| `CALL_POLICY_GRPC_ADDR` | call-policy-default | — | gRPC listen address. |
| `CALL_POLICY_TLS_CA` | call-policy-default | — | CA bundle for mesh TLS verification. |
| `CALL_POLICY_TLS_CERT` | call-policy-default | — | TLS certificate file for the module's mesh client/server. |
| `CALL_POLICY_TLS_DIR` | call-policy-default | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `CALL_POLICY_TLS_KEY` | call-policy-default | — | TLS private key file. |

### circuitbreaker-simple

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `CB_CIRCUIT_IDLE_TTL` | circuitbreaker-simple | — | Cb circuit idle ttl. |
| `CB_COOLDOWN` | circuitbreaker-simple | — | Cb cooldown. |
| `CB_FAILURE_THRESHOLD` | circuitbreaker-simple | — | Cb failure threshold. |
| `CB_GRPC_ADDR` | circuitbreaker-simple | — | gRPC listen address. |
| `CB_HALF_OPEN_MAX` | circuitbreaker-simple | — | Cb half open max. |
| `CB_TLS_CA` | circuitbreaker-simple | — | CA bundle for mesh TLS verification. |
| `CB_TLS_CERT` | circuitbreaker-simple | — | TLS certificate file for the module's mesh client/server. |
| `CB_TLS_DIR` | circuitbreaker-simple | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `CB_TLS_KEY` | circuitbreaker-simple | — | TLS private key file. |

### config-watcher

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `CONFIG_WATCHER_GRPC_ADDR` | config-watcher | — | gRPC listen address. |
| `CONFIG_WATCHER_STREAM_BUFFER` | config-watcher | — | Stream buffer. |
| `CONFIG_WATCHER_TLS_CA` | config-watcher | — | CA bundle for mesh TLS verification. |
| `CONFIG_WATCHER_TLS_CERT` | config-watcher | — | TLS certificate file for the module's mesh client/server. |
| `CONFIG_WATCHER_TLS_DIR` | config-watcher | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `CONFIG_WATCHER_TLS_KEY` | config-watcher | — | TLS private key file. |

### core

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUXCORE_AUDIT_HMAC_KEY` | core | — | HMAC-SHA256 key for audit-log entry signatures (NFR-OPS-004). Prefer `MUXCORE_AUDIT_HMAC_KEY_FILE`. |
| `MUXCORE_AUDIT_HMAC_KEY_FILE` | core | — | File holding the audit HMAC key (mode 0600); wins over `MUXCORE_AUDIT_HMAC_KEY`. Missing or empty file aborts startup. |
| `MUXCORE_BOOTSTRAP_TOKEN` | SDK (`meshid`) | — | Single-use enrollment token for this module (`MUXCORE_ENROLL_TOKEN_<ID>` in compose). |
| `MUXCORE_CA_EXPORT_DIR` | core | — | Directory core writes the public mesh `ca.crt` to (household profile); never the CA key (ADR-0016). |
| `MUXCORE_DATA_DIR` | core | `./data` (`/app/data` in images) | Core data directory; the automatic CA lives in `<data>/ca` (ADR-0016). |
| `MUXCORE_ENROLL_DNS_NAMES` | core SDK (`meshid`) | — | Extra DNS SANs (comma-separated) a module requests when it enrolls, e.g. its compose service name; core keeps only names in `MUXCORE_ENROLL_SAN_ALLOW` (the module ID is always a SAN). |
| `MUXCORE_ENROLL_SAN_ALLOW` | core | module ID + loopback | Extra DNS names/IPs (comma list, `*.suffix` wildcards) allowed as SANs in enrolled module certificates. |
| `MUXCORE_ENROLL_SECRET` | core | — | HMAC secret for single-use module enrollment tokens (`mct_2_`, ADR-0017); ≥ 16 bytes. Generated by `_mvp/scripts/gen-enrollment.sh`. |
| `MUXCORE_METRICS_ADDR` | core | `127.0.0.1:9464` | Dedicated metrics listener (`/metrics`); a non-loopback address requires `MUXCORE_METRICS_TOKEN` (NFR-SEC-011). |
| `MUXCORE_METRICS_TOKEN` | core | — | Bearer token for the metrics listener (or `MUXCORE_METRICS_TOKEN_FILE`). |
| `MUXCORE_PROFILE` | core, run-host | inferred (ADR-0016) | Security profile: `dev` (insecure allowed, warning) or `household` (TLS + automatic CA, insecure flag fatal); `staging` = household. Unset: `dev` if the insecure flag is set, else `household`. |
| `MUXCORE_REQUIRE_MODULE_CERTS` | core | `false` | Household stage 2 (ADR-0018): reject module registrations without a verified mesh certificate for every capability. |
| `MUXCORE_TLS_DIR` | SDK (`meshid`) | `<MUXCORE_DATA_DIR>/mesh-id` | Where a module stores its enrolled mesh certificate and key (0600). |
| `MUXCORE_TLS_SERVER_NAME` | core SDK (`meshid`) | host of `MUXCORE_GRPC_ADDR` | Name used to verify core's certificate when a module enrolls. |
| `MUXCORE_TLS_SERVER_SANS` | core | — | Extra names/IPs (comma-separated) for core's automatic server certificate, e.g. the compose service name modules dial. |
| `TENANT_TRUST_HEADERS` | core (`pkg/tenant`) | — | `1` lets the tenant middleware honour `X-Tenant-ID`/`X-Auth-Claims-Tenant` headers; otherwise only an authenticated tenant is used (ADR-0019). |

### data-redaction-pattern

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `REDACTION_EXTRA_KEYS` | data-redaction-pattern | — | Redaction extra keys. |
| `REDACTION_EXTRA_RULES` | data-redaction-pattern | — | Redaction extra rules. |
| `REDACTION_GRPC_ADDR` | data-redaction-pattern | — | gRPC listen address. |
| `REDACTION_TLS_CA` | data-redaction-pattern | — | CA bundle for mesh TLS verification. |
| `REDACTION_TLS_CERT` | data-redaction-pattern | — | TLS certificate file for the module's mesh client/server. |
| `REDACTION_TLS_DIR` | data-redaction-pattern | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `REDACTION_TLS_KEY` | data-redaction-pattern | — | TLS private key file. |

### database-postgres

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `DATABASE_GRPC_ADDR` | database-postgres, database-sqlite | — | gRPC listen address. |
| `PGCONNECT_TIMEOUT` | database-postgres | — | Pgconnect timeout. |
| `PGDATABASE` | database-postgres | `muxcore` | Pgdatabase. |
| `PGHOST` | database-postgres | `localhost` | Pghost. |
| `PGPASSWORD` | database-postgres | — | Pgpassword. |
| `PGPOOL_MAX_IDLE` | database-postgres | — | Pgpool max idle. |
| `PGPOOL_MAX_OPEN` | database-postgres | — | Pgpool max open. |
| `PGPORT` | database-postgres | `5432` | Pgport. |
| `PGSCHEMA` | database-postgres | `public` | Pgschema. |
| `PGSSLMODE` | database-postgres | `disable` | Pgsslmode. |
| `PGUSER` | database-postgres | `muxcore` | Pguser. |

### database-sqlite

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `DATABASE_MODULE_TOKEN` | database-sqlite | — | Mesh module token. |
| `DATABASE_TLS_CA` | database-sqlite | — | CA bundle for mesh TLS verification. |
| `DATABASE_TLS_CERT` | database-sqlite | — | TLS certificate file for the module's mesh client/server. |
| `DATABASE_TLS_DIR` | database-sqlite | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `DATABASE_TLS_KEY` | database-sqlite | — | TLS private key file. |
| `MUXCORE_MODULE_TOKEN` | database-sqlite, encryption-aesgcm, logging-file, media-graph, notification-apprise, playback-guard | — | Mesh module token. |
| `SQLITE_DB_PATH` | database-sqlite | — | SQLite database file path. |

### distributed-lock-sqlite

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `LOCK_DB_PATH` | distributed-lock-sqlite | — | SQLite database file path. |
| `LOCK_GRPC_ADDR` | distributed-lock-sqlite | — | gRPC listen address. |
| `LOCK_SWEEP_INTERVAL` | distributed-lock-sqlite | — | Lock sweep interval. |
| `LOCK_TLS_CA` | distributed-lock-sqlite | — | CA bundle for mesh TLS verification. |
| `LOCK_TLS_CERT` | distributed-lock-sqlite | — | TLS certificate file for the module's mesh client/server. |
| `LOCK_TLS_DIR` | distributed-lock-sqlite | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `LOCK_TLS_KEY` | distributed-lock-sqlite | — | TLS private key file. |

### downloader-debrid

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `DEBRID_API_BASE` | downloader-debrid | — | Api base. |
| `DEBRID_HTTP_ADDR` | downloader-debrid | — | HTTP listen address. |
| `DEBRID_HTTP_TOKEN` | downloader-debrid | — | Credential (secret). |
| `DEBRID_PROVIDER` | downloader-debrid | — | Provider. |
| `DEBRID_TOKEN` | downloader-debrid | — | Credential (secret). |
| `REAL_DEBRID_TOKEN` | downloader-debrid | — | Credential (secret). |

### downloader-native-torrent

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `DOWNLOADER_GRPC_ADDR` | downloader-native-torrent | — | gRPC listen address. |
| `DOWNLOADER_INDEXER_HOSTS` | downloader-native-torrent, downloader-sabnzbd | — | Indexer hosts (host[:port], comma-separated) whose private/LAN addresses may serve torrent/NZB files; other URLs use the strict user-URL SSRF profile. |
| `DOWNLOADER_PUBLIC_METAINFO_CACHE` | downloader-native-torrent | — | Public metainfo cache. |
| `DOWNLOAD_DIR` | downloader-native-torrent, downloader-native-usenet | — | Download dir. |
| `DOWNLOAD_STORAGE` | downloader-native-torrent | — | Download storage. |
| `NAT_PMP_PORT` | downloader-native-torrent | — | Nat pmp port. |
| `SEED_MINUTES` | downloader-native-torrent | — | Seed minutes. |
| `SEED_RATIO` | downloader-native-torrent | — | Seed ratio. |
| `TORRENT_ENABLE_DHT` | downloader-native-torrent | — | Enable dht. |
| `TORRENT_ENABLE_PEX` | downloader-native-torrent | — | Enable pex. |
| `TORRENT_FILE_PRIORITY` | downloader-native-torrent | — | File priority. |
| `TORRENT_LISTEN_PORT` | downloader-native-torrent | — | Listen port. |
| `WG_KILL_SWITCH` | downloader-native-torrent | — | Wg kill switch. |
| `WG_KILL_SWITCH_ALLOW` | downloader-native-torrent | — | Wg kill switch allow. |
| `WG_KILL_SWITCH_SCOPE` | downloader-native-torrent | — | Wg kill switch scope. |

### downloader-native-usenet

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUXCORE_GRPC_ADDR_OVERRIDE` | downloader-native-usenet, downloader-qbittorrent, downloader-sabnzbd | `MUXCORE_GRPC_ADDR_OVERRIDE` | Muxcore grpc addr override. |
| `NNTP_ARTICLE_RETRIES` | downloader-native-usenet | — | Nntp article retries. |
| `NNTP_HOST` | downloader-native-usenet | — | Nntp host. |
| `NNTP_PASS` | downloader-native-usenet | — | Nntp pass. |
| `NNTP_PORT` | downloader-native-usenet | — | Nntp port. |
| `NNTP_SSL` | downloader-native-usenet | — | Nntp ssl. |
| `NNTP_TLS_INSECURE` | downloader-native-usenet | — | Nntp tls insecure. |
| `NNTP_USER` | downloader-native-usenet | — | Nntp user. |
| `SEVENZIP_CMD` | downloader-native-usenet | — | Sevenzip cmd. |
| `UNRAR_CMD` | downloader-native-usenet | — | Unrar cmd. |
| `USENET_CLEANUP` | downloader-native-usenet | — | Cleanup. |
| `USENET_DOWNLOAD_DIR` | downloader-native-usenet | — | Download dir. |
| `USENET_ENGINE` | downloader-native-usenet | — | Engine. |
| `USENET_GRPC_ADDR` | downloader-native-usenet | — | gRPC listen address. |
| `USENET_PAR2` | downloader-native-usenet | — | Par2. |
| `USENET_PAR2_MEM_MB` | downloader-native-usenet | — | Par2 mem mb. |
| `USENET_PAR2_THREADS` | downloader-native-usenet | — | Par2 threads. |
| `USENET_SEVENZIP_CMD` | downloader-native-usenet | — | Sevenzip cmd. |
| `USENET_UNPACK` | downloader-native-usenet | — | Unpack. |
| `USENET_UNRAR_CMD` | downloader-native-usenet | — | Unrar cmd. |

### downloader-qbittorrent

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `QBIT_DOWNLOAD_ROOTS` | downloader-qbittorrent | download dir | Allowed roots for `save_path` passed to qBittorrent (RULE-VAL-1). |
| `QBIT_FIXTURE` | downloader-qbittorrent, media-automation | — | Qbit fixture. |
| `QBIT_GRPC_ADDR` | downloader-qbittorrent | `MUXCORE_GRPC_ADDR_OVERRIDE` | gRPC listen address. |
| `QBIT_HTTP_ADDR` | downloader-qbittorrent | `MUXCORE_HTTP_ADDR` | HTTP listen address. |
| `QBIT_PASSWORD` | downloader-qbittorrent | `QBITTORRENT_PASSWORD` | Credential (secret). |
| `QBIT_URL` | downloader-qbittorrent | `QBITTORRENT_URL` | Qbit url. |
| `QBIT_USERNAME` | downloader-qbittorrent | `QBITTORRENT_USERNAME` | Qbit username. |

### downloader-sabnzbd

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SABNZBD_API_KEY` | downloader-sabnzbd | — | API key. |
| `SABNZBD_FIXTURE` | downloader-sabnzbd | — | Fixture. |
| `SABNZBD_URL` | downloader-sabnzbd | — | Url. |

### emby

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `EMBY_CATALOG_SYNC_SEC` | emby | — | Catalog sync sec. |
| `EMBY_DATA_DIR` | emby | — | Module data directory. |
| `EMBY_GRPC_ADDR` | emby | — | gRPC listen address. |
| `EMBY_HTTP_ADDR` | emby | — | HTTP listen address. |
| `EMBY_SESSIONS_POLL_SEC` | emby | — | Sessions poll sec. |
| `EMBY_SSE_SECRET` | emby | — | Credential (secret). |
| `EMBY_TOKEN` | emby, muxcore-installer | — | Credential (secret). |
| `EMBY_URL` | emby, muxcore-installer | — | Url. |
| `EMBY_WEBSOCKET` | emby | — | Websocket. |

### encryption-aesgcm

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `ENCRYPTION_GRPC_ADDR` | encryption-aesgcm | — | gRPC listen address. |
| `ENCRYPTION_KEY_FILE` | encryption-aesgcm | — | Key file. |
| `ENCRYPTION_MASTER_KEY` | encryption-aesgcm | — | Master key. |
| `ENCRYPTION_MODULE_TOKEN` | encryption-aesgcm | — | Mesh module token. |
| `ENCRYPTION_TLS_CA` | encryption-aesgcm | — | CA bundle for mesh TLS verification. |
| `ENCRYPTION_TLS_CERT` | encryption-aesgcm | — | TLS certificate file for the module's mesh client/server. |
| `ENCRYPTION_TLS_DIR` | encryption-aesgcm | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `ENCRYPTION_TLS_KEY` | encryption-aesgcm | — | TLS private key file. |

### executor-shell

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `EXECUTOR_ALLOWED_COMMANDS` | executor-shell | — | Allowed commands. |
| `EXECUTOR_GRPC_ADDR` | executor-shell | — | gRPC listen address. |
| `EXECUTOR_TLS_CA` | executor-shell | — | CA bundle for mesh TLS verification. |
| `EXECUTOR_TLS_CERT` | executor-shell | — | TLS certificate file for the module's mesh client/server. |
| `EXECUTOR_TLS_DIR` | executor-shell | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `EXECUTOR_TLS_KEY` | executor-shell | — | TLS private key file. |

### feature-flags-file

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `FEATURE_FLAGS_FILE` | feature-flags-file | — | Flags file. |
| `FEATURE_FLAGS_TLS_CA` | feature-flags-file | — | CA bundle for mesh TLS verification. |
| `FEATURE_FLAGS_TLS_CERT` | feature-flags-file | — | TLS certificate file for the module's mesh client/server. |
| `FEATURE_FLAGS_TLS_DIR` | feature-flags-file | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `FEATURE_FLAGS_TLS_KEY` | feature-flags-file | — | TLS private key file. |

### health-monitor

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `HEALTH_MONITOR_GRPC_ADDR` | health-monitor | — | gRPC listen address. |
| `HEALTH_MONITOR_HTTP_ADDR` | health-monitor | — | HTTP listen address. |
| `HEALTH_MONITOR_INTERVAL` | health-monitor | — | Interval. |
| `HEALTH_MONITOR_TLS_CA` | health-monitor | `MUXCORE_TLS_CA` | CA bundle for mesh TLS verification. |
| `HEALTH_MONITOR_TLS_CERT` | health-monitor | `MUXCORE_TLS_CERT` | TLS certificate file for the module's mesh client/server. |
| `HEALTH_MONITOR_TLS_DIR` | health-monitor | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `HEALTH_MONITOR_TLS_KEY` | health-monitor | `MUXCORE_TLS_KEY` | TLS private key file. |

### indexer-piratebay

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `INDEXER_FIXTURE` | indexer-piratebay | — | Fixture. |
| `PIRATEBAY_API_BASE` | indexer-piratebay | — | Api base. |
| `PIRATEBAY_CACHE_TTL` | indexer-piratebay | — | Cache ttl. |
| `PIRATEBAY_ENRICH_DETAILS` | indexer-piratebay | — | Enrich details. |
| `PIRATEBAY_ENRICH_LIMIT` | indexer-piratebay | — | Enrich limit. |
| `PIRATEBAY_GRPC_ADDR` | indexer-piratebay | — | gRPC listen address. |
| `PIRATEBAY_MIN_INTERVAL` | indexer-piratebay | — | Min interval. |

### indexer-torznab

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `PROWLARR_API_KEY` | indexer-torznab | — | API key. |
| `TORZNAB_API_KEY` | indexer-torznab | — | API key. |
| `TORZNAB_GRPC_ADDR` | indexer-torznab | — | gRPC listen address. |
| `TORZNAB_NAME` | indexer-torznab | — | Name. |
| `TORZNAB_TIMEOUT` | indexer-torznab | — | Timeout. |

### input-validate-jsonschema

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `VALIDATE_DATA_DIR` | input-validate-jsonschema | — | Module data directory. |
| `VALIDATE_GRPC_ADDR` | input-validate-jsonschema | — | gRPC listen address. |
| `VALIDATE_TLS_CA` | input-validate-jsonschema | — | CA bundle for mesh TLS verification. |
| `VALIDATE_TLS_CERT` | input-validate-jsonschema | — | TLS certificate file for the module's mesh client/server. |
| `VALIDATE_TLS_DIR` | input-validate-jsonschema | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `VALIDATE_TLS_KEY` | input-validate-jsonschema | — | TLS private key file. |

### jellyfin

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUTH_LOCAL_GRPC_ADDR` | userdata-local | — | gRPC listen address. |
| `JELLYFIN_TRUST_CALLER_HEADER` | jellyfin | — | Dev only: with `MUXCORE_INSECURE_DISABLE_TLS=true`, `1` accepts header-only user identity on `/userdata/from-muxcore` (ADR-0019); otherwise a bearer token is required. |

### logging-file

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `LOG_ALLOWED_ROOT` | logging-file | — | Log allowed root. |
| `LOG_FILE_PATH` | logging-file | — | Log file path. |
| `LOG_FLUSH_MS` | logging-file | — | Log flush ms. |
| `LOG_GRPC_ADDR` | logging-file | — | gRPC listen address. |
| `LOG_LEVEL` | logging-file | — | Log level. |
| `LOG_MAX_BACKUPS` | logging-file | — | Log max backups. |
| `LOG_MAX_SIZE_MB` | logging-file | — | Log max size mb. |
| `LOG_MODULE_TOKEN` | logging-file | — | Mesh module token. |
| `LOG_STDOUT` | logging-file | — | Log stdout. |
| `LOG_TLS_CA` | logging-file | — | CA bundle for mesh TLS verification. |
| `LOG_TLS_CERT` | logging-file | — | TLS certificate file for the module's mesh client/server. |
| `LOG_TLS_DIR` | logging-file | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `LOG_TLS_KEY` | logging-file | — | TLS private key file. |

### media-audiobooks

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUDIOBOOKS_DATA_DIR` | media-audiobooks | — | Module data directory. |
| `AUDIOBOOKS_IMAGE_DIR` | media-audiobooks | — | Image dir. |
| `AUDIOBOOKS_LIBRARY_DIR` | media-audiobooks | — | Library dir. |

### media-automation

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AUTOMATION_DB_PATH` | media-automation | — | SQLite database file path. |
| `AUTOMATION_DEAD_TORRENT_PENALTY` | media-automation | — | Dead torrent penalty. |
| `AUTOMATION_DOWNLOADER_TORRENT` | media-automation | discovered | Pin the torrent downloader module ID used for dispatch. |
| `AUTOMATION_DOWNLOADER_USENET` | media-automation | discovered | Pin the usenet downloader module ID used for dispatch. |
| `AUTOMATION_DOWNLOAD_DIR` | media-automation | — | Download dir. |
| `AUTOMATION_EVENT_SUBSCRIBE_DELAY` | media-automation | — | Event subscribe delay. |
| `AUTOMATION_GRPC_ADDR` | media-automation | — | gRPC listen address. |
| `AUTOMATION_KEEP_STALLED_PARTIALS` | media-automation | — | Keep stalled partials. |
| `AUTOMATION_MAX_RELEASE_GB` | media-automation | — | Max release gb. |
| `AUTOMATION_SERIES_OVERRIDES_JSON` | media-automation | — | Series overrides json. |
| `AUTOMATION_STALL_AUTO_MODE` | media-automation | — | Stall auto mode. |
| `AUTOMATION_STALL_LOOP_MINUTES` | media-automation | — | Stall loop minutes. |
| `AUTOMATION_STALL_TIMEOUT_MINUTES` | media-automation | — | Stall timeout minutes. |
| `AUTOMATION_USENET_GRAB_BONUS` | media-automation | — | Usenet grab bonus. |

### media-books

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `BOOKS_DATA_DIR` | media-books | — | Module data directory. |
| `BOOKS_IMAGE_DIR` | media-books | — | Image dir. |
| `BOOKS_LIBRARY_DIR` | media-books | — | Library dir. |

### media-comics

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `COMICS_DATA_DIR` | media-comics | — | Module data directory. |
| `COMICS_IMAGE_DIR` | media-comics | — | Image dir. |
| `COMICS_LIBRARY_DIR` | media-comics | — | Library dir. |

### media-custom-formats

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `FORMATS_TRASH_GUIDES_PATH` | media-custom-formats | — | Trash guides path. |
| `FORMATS_TRASH_GUIDES_ROOTS` | media-custom-formats | — | Path-list (`:` on Linux) of roots a local TRaSH guides_path must resolve inside. Empty refuses local guides paths. Official sentinels are unchanged. |
| `FORMATS_TRASH_IMPORT_PROFILES` | media-custom-formats | — | Trash import profiles. |
| `FORMATS_TRASH_OFFICIAL` | media-custom-formats | — | Trash official. |
| `FORMATS_TRASH_SCORE_SET` | media-custom-formats | `default` | Trash score set. |
| `FORMATS_TRASH_SERVICES` | media-custom-formats | `radarr,sonarr` | Trash services. |
| `FORMATS_TRASH_SYNC` | media-custom-formats | — | Trash sync. |

### media-dlna

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `DLNA_FRIENDLY_NAME` | media-dlna | — | Friendly name. |
| `DLNA_GRPC_ADDR` | media-dlna | — | gRPC listen address. |
| `DLNA_HEALTH_HTTP_ADDR` | media-dlna | — | HTTP listen address. |
| `DLNA_HTTP_ADDR` | media-dlna | — | HTTP listen address. |
| `DLNA_MEDIA_PATH` | media-dlna | — | Media path. |
| `DLNA_PROBE_CACHE_PATH` | media-dlna | — | Probe cache path. |

### media-ffprobe

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `FFPROBE_ALLOWED_ROOTS` | media-ffprobe | — | Path-list (`:` on Linux) of roots Analyze may read. Empty refuses every file_path. |
| `FFPROBE_BIN` | media-ffprobe | — | Bin. |
| `FFPROBE_DB_PATH` | media-ffprobe | — | SQLite database file path. |
| `FFPROBE_GRPC_ADDR` | media-ffprobe | — | gRPC listen address. |
| `FFPROBE_TIMEOUT` | media-ffprobe | — | Timeout. |

### media-graph

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `GRAPH_AUTO_LINK` | media-graph | — | Auto link. |
| `GRAPH_DB_PATH` | media-graph | — | SQLite database file path. |
| `GRAPH_DEFAULT_REL` | media-graph | — | Default rel. |
| `GRAPH_FIXTURE_PATH` | media-graph | — | Fixture path. |
| `GRAPH_GRPC_ADDR` | media-graph | — | gRPC listen address. |
| `GRAPH_INGEST_ENABLED` | media-graph | — | Ingest enabled. |
| `GRAPH_INGEST_INTERVAL` | media-graph | — | Ingest interval. |
| `GRAPH_INGEST_PAGE_SIZE` | media-graph | — | Ingest page size. |
| `MUXCORE_TOKEN` | media-graph, muxcorectl-cli | — | Credential (secret). |

### media-intro-outro

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `INTRO_MAX_SECONDS` | media-intro-outro | — | Intro max seconds. |
| `INTRO_OUTRO_DATA_DIR` | media-intro-outro | — | Module data directory. |
| `OUTRO_MAX_SECONDS` | media-intro-outro | — | Max seconds. |

### media-library-maintainer

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `DELUGE_PASSWORD` | media-library-maintainer | — | Credential (secret). |
| `JUSTWATCH_API_URL` | media-library-maintainer | — | Justwatch api url. |
| `MAINTAINER_PLEX_MOVIE_SECTION` | media-library-maintainer | — | Plex movie section. |
| `MDBLIST_BASE_URL` | media-library-maintainer | — | Mdblist base url. |
| `PLEX_TOKEN` | media-library-maintainer, muxcore-installer, plex | — | Credential (secret). |
| `PLEX_URL` | media-library-maintainer, muxcore-installer, plex | — | Plex url. |
| `RADARR_API_KEY` | media-library-maintainer | — | API key. |
| `RADARR_URL` | media-library-maintainer | — | Radarr url. |
| `SONARR_API_KEY` | media-library-maintainer | — | API key. |
| `SONARR_URL` | media-library-maintainer | — | Sonarr url. |
| `TRAKT_API_KEY` | media-library-maintainer | — | API key. |
| `TRANSMISSION_PASSWORD` | media-library-maintainer | — | Credential (secret). |
| `TRANSMISSION_USERNAME` | media-library-maintainer | — | Transmission username. |
| `USERDATA_LOCAL_DATA_DIR` | media-library-maintainer | — | Module data directory. |

### media-music

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUSIC_IMAGE_DIR` | media-music | — | Image dir. |

### media-rename

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `RENAME_ALLOWED_ROOTS` | media-rename | — (refuse all) | Roots (`:`-separated) that rename sources, destinations and batch directories must resolve inside (RULE-VAL-1). |

### media-root-folders

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `ROOTS_ALLOWED_PREFIXES` | media-root-folders | — (refuse create) | Prefixes (`:`-separated) a root folder may be created/probed under; set to the library mounts (RULE-VAL-1). |

### media-scanner

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SCANNER_ALLOWED_ROOTS` | media-scanner | — | Extra allowed roots (`:`-separated) for watch dirs and libraries besides `SCANNER_*_ROOT` and registered roots. |
| `SCANNER_DB_PATH` | media-scanner | — | SQLite database file path. |
| `SCANNER_DEFAULT_WATCH_DIR` | media-scanner | — | Default watch dir. |
| `SCANNER_GRPC_ADDR` | media-scanner | — | gRPC listen address. |
| `SCANNER_IMPORT_MODE` | media-scanner | — | Import mode. |
| `SCANNER_LIBRARY_ROOT` | media-scanner | — | Library root. |
| `SCANNER_MIN_VIDEO_BYTES` | media-scanner | — | Min video bytes. |
| `SCANNER_MUSIC_LIBRARY_ROOT` | media-scanner | — | Music library root. |
| `SCANNER_SAFETY_RESCAN` | media-scanner | — | Safety rescan. |
| `SCANNER_SAMPLE_MAX_BYTES` | media-scanner | — | Sample max bytes. |
| `SCANNER_TV_LIBRARY_ROOT` | media-scanner | — | Tv library root. |
| `SCANNER_USE_MESH_STORAGE` | media-scanner | — | Use mesh storage. |

### media-subtitles

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SUBS_MEDIA_ROOTS` | media-subtitles | — (refuse) | Library roots (`:`-separated) that `RegisterSidecar` / `DetectEmbedded` paths must resolve inside. |

### media-tagging

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `TAGGING_DATA_DIR` | media-tagging | — | Module data directory. |
| `TAGGING_DEFAULT_CATEGORY` | media-tagging | — | Default category. |
| `TAGGING_EVENTS_ENABLED` | media-tagging | — | Events enabled. |
| `TAGGING_GRPC_ADDR` | media-tagging | — | gRPC listen address. |

### media-transcoder

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `TRANSCODER_ALLOW_URL_SOURCES` | media-transcoder | off | `1` allows http(s) `src` for playback transcodes (restricted by `TRANSCODER_URL_SOURCE_HOSTS`). |
| `TRANSCODER_HTTP_ALLOWED_CALLERS` | media-transcoder | `media-ui` | Mesh certificate CNs allowed to call the playback HTTP API in household mode. |
| `TRANSCODER_HTTP_TOKEN` | media-transcoder | — | Dev only: bearer token required when the playback HTTP API binds a non-loopback address. |
| `TRANSCODER_MEDIA_ROOTS` | media-transcoder | — (refuse all) | Library roots (`:`-separated) for playback `src` and job inputs (RULE-VAL-1). |
| `TRANSCODER_OUTPUT_DIR` | media-transcoder | `<db dir>/output` | Allowed location for `Enqueue` outputs. |
| `TRANSCODER_URL_SOURCE_HOSTS` | media-transcoder | — | host:port allow-list for URL `src` (e.g. `media-movies:9430,media-tvshows:9450`). |

### media-transcoder-pool

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MEDIA_TRANSCODER_ADDR` | media-transcoder-pool | — | Transcoder addr. |

### metadata-musicbrainz

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `METADATA_GRPC_ADDR` | metadata-musicbrainz, metadata-tmdb | — | gRPC listen address. |
| `MUSICBRAINZ_BASE_URL` | metadata-musicbrainz | — | Base url. |
| `MUSICBRAINZ_FIXTURE` | metadata-musicbrainz | — | Fixture. |
| `MUSICBRAINZ_USER_AGENT` | metadata-musicbrainz | — | User agent. |

### metadata-tmdb

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUXCORE_CFG_TMDB_API_KEY` | metadata-tmdb | — | API key. |
| `TMDB_API_KEY` | metadata-tmdb, muxcore-installer | — | API key. |
| `TMDB_BASE_URL` | metadata-tmdb | — | Base url. |
| `TMDB_BURST` | metadata-tmdb | — | Burst. |
| `TMDB_CACHE_MAX` | metadata-tmdb | — | Cache max. |
| `TMDB_CACHE_TTL_DETAILS` | metadata-tmdb | — | Cache ttl details. |
| `TMDB_CACHE_TTL_LIST` | metadata-tmdb | — | Cache ttl list. |
| `TMDB_CACHE_TTL_SEARCH` | metadata-tmdb | — | Cache ttl search. |
| `TMDB_FIXTURE` | metadata-tmdb | — | Fixture. |
| `TMDB_RATE` | metadata-tmdb | — | Rate. |

### metrics-prometheus

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `METRICS_GRPC_ADDR` | metrics-prometheus | — | gRPC listen address. |
| `METRICS_HTTP_ADDR` | metrics-prometheus | — | HTTP listen address. |
| `METRICS_PATH` | metrics-prometheus | — | Path. |
| `METRICS_TLS_CA` | metrics-prometheus | — | CA bundle for mesh TLS verification. |
| `METRICS_TLS_CERT` | metrics-prometheus | — | TLS certificate file for the module's mesh client/server. |
| `METRICS_TLS_DIR` | metrics-prometheus | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `METRICS_TLS_KEY` | metrics-prometheus | — | TLS private key file. |

### muxcore-installer

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `GH_TOKEN` | muxcore-installer | — | Credential (secret). |
| `GITHUB_TOKEN` | muxcore-installer | — | Credential (secret). |
| `INSTALL_RUNTIME` | muxcore-installer | — | Install runtime. |
| `MUXCORE_BIND_ALL` | muxcore-installer | — | Bind all. |
| `MUXCORE_DB_BACKEND` | muxcore-installer | `sqlite` | Installer database backend (`sqlite`|`postgres`); replaces the legacy `MUXCORE_PROFILE=sqlite|postgres`. |
| `MUXCORE_DRY_RUN` | muxcore-installer | — | Dry run. |
| `MUXCORE_GITHUB_ORG` | muxcore-installer | — | Github org. |
| `MUXCORE_GITHUB_TOKEN` | muxcore-installer | — | Credential (secret). |
| `MUXCORE_GITHUB_TOKEN_FILE` | muxcore-installer | — | Github token file. |
| `MUXCORE_INSTALL_DIR` | muxcore-installer | — | Install dir. |
| `MUXCORE_I_AGREE` | muxcore-installer | — | I agree. |
| `MUXCORE_KEEP_MODE` | muxcore-installer | — | Keep mode. |
| `MUXCORE_LIBRARIES` | muxcore-installer | — | Libraries. |
| `MUXCORE_METADATA_LANGUAGE` | muxcore-installer | — | Metadata language. |
| `MUXCORE_NONINTERACTIVE` | muxcore-installer | — | Noninteractive. |
| `MUXCORE_PLAYBACK` | muxcore-installer | — | Playback. |
| `MUXCORE_REGISTRY` | muxcore-installer | — | Registry. |
| `MVP_IMPORT_DIR` | muxcore-installer | — | Mvp import dir. |

### muxcorectl-cli

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `MUXCORE_ADMIN_TOKEN` | muxcorectl-cli | — | Credential (secret). |
| `MUXCORE_AUTH_URL` | muxcorectl-cli | discovered | auth-local HTTP base URL for invite commands. |
| `MUXCORE_PLAYBACK_MONITOR_URL` | muxcorectl-cli | discovered | playback-monitor HTTP base URL for stream/notification commands. |
| `MUXCORE_REQUEST_URL` | muxcorectl-cli | discovered | request-media HTTP base URL. |
| `MUXCORE_TOKEN_FILE` | muxcorectl-cli | — | Muxcore token file. |
| `SCHEDULER_URL` | muxcorectl-cli | — | Scheduler url. |

### notification-apprise

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `APPRISE_URLS` | notification-apprise | — | Urls. |
| `DISCORD_WEBHOOK` | notification-apprise, notification-default | — | Discord webhook. |
| `MUXCORE_DATA` | notification-apprise | — | Muxcore data. |
| `NOTIFY_GRPC_ADDR` | notification-apprise, notification-default | — | gRPC listen address. |
| `NOTIFY_MODULE_TOKEN` | notification-apprise | — | Mesh module token. |
| `SLACK_WEBHOOK` | notification-apprise, notification-default | — | Slack webhook. |
| `WEBHOOK_URL` | notification-apprise, notification-default | — | Webhook url. |

### notification-default

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `EMAIL_FROM` | notification-default | — | Email from. |
| `EMAIL_TO` | notification-default | — | Email to. |
| `NOTIFICATION_TLS_CA` | notification-default | — | CA bundle for mesh TLS verification. |
| `NOTIFICATION_TLS_CERT` | notification-default | — | TLS certificate file for the module's mesh client/server. |
| `NOTIFICATION_TLS_DIR` | notification-default | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `NOTIFICATION_TLS_KEY` | notification-default | — | TLS private key file. |
| `SMTP_HOST` | notification-default | — | Smtp host. |
| `SMTP_PASS` | notification-default | — | Smtp pass. |
| `SMTP_PORT` | notification-default | — | Smtp port. |
| `SMTP_USER` | notification-default | — | Smtp user. |

### playback-guard

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `PLAYBACK_GUARD_MODULE_TOKEN` | playback-guard | — | Mesh module token. |

### plex

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `PLEX_CATALOG_SYNC_SEC` | plex | — | Catalog sync sec. |
| `PLEX_DATA_DIR` | plex | — | Module data directory. |
| `PLEX_GRPC_ADDR` | plex | — | gRPC listen address. |
| `PLEX_HTTP_ADDR` | plex | — | HTTP listen address. |
| `PLEX_HTTP_SECRET` | plex | — | Credential (secret). |
| `PLEX_SESSIONS_POLL_SEC` | plex | — | Sessions poll sec. |
| `PLEX_SSE` | plex | — | Sse. |
| `PLEX_SYNC_LIST_POLL_SEC` | plex | — | Sync list poll sec. |

### publish-policy-default

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `PUBLISH_POLICY_AUDIT_PATH` | publish-policy-default | — | Policy audit path. |
| `PUBLISH_POLICY_FILE` | publish-policy-default | — | Policy file. |
| `PUBLISH_POLICY_GRPC_ADDR` | publish-policy-default | — | gRPC listen address. |
| `PUBLISH_POLICY_REGISTRY_MATCH` | publish-policy-default | — | Policy registry match. |
| `PUBLISH_POLICY_REGISTRY_SYNC_DELAY` | publish-policy-default | — | Policy registry sync delay. |
| `PUBLISH_POLICY_TLS_CA` | publish-policy-default | — | CA bundle for mesh TLS verification. |
| `PUBLISH_POLICY_TLS_CERT` | publish-policy-default | — | TLS certificate file for the module's mesh client/server. |
| `PUBLISH_POLICY_TLS_DIR` | publish-policy-default | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `PUBLISH_POLICY_TLS_KEY` | publish-policy-default | — | TLS private key file. |

### ratelimit-tokenbucket

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `RATELIMIT_BURST` | ratelimit-tokenbucket | — | Burst. |
| `RATELIMIT_ENABLED` | ratelimit-tokenbucket | — | Enabled. |
| `RATELIMIT_GRPC_ADDR` | ratelimit-tokenbucket | — | gRPC listen address. |
| `RATELIMIT_RATE` | ratelimit-tokenbucket | — | Rate. |
| `RATELIMIT_TLS_CA` | ratelimit-tokenbucket | — | CA bundle for mesh TLS verification. |
| `RATELIMIT_TLS_CERT` | ratelimit-tokenbucket | — | TLS certificate file for the module's mesh client/server. |
| `RATELIMIT_TLS_DIR` | ratelimit-tokenbucket | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `RATELIMIT_TLS_KEY` | ratelimit-tokenbucket | — | TLS private key file. |

### request-media

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `REQUEST_AUTO_APPROVE_USERS` | request-media | — | Auto approve users. |
| `REQUEST_DATA_DIR` | request-media | — | Module data directory. |
| `REQUEST_GRPC_ADDR` | request-media | — | gRPC listen address. |
| `REQUEST_HTTP_ADDR` | request-media | — | HTTP listen address. |
| `REQUEST_MAX_PENDING_PER_USER` | request-media | — | Max pending per user. |
| `REQUEST_MAX_PER_WEEK` | request-media | — | Max per week. |
| `REQUEST_MODULE_PRINCIPALS` | request-media | `media-library-maintainer` | Module IDs (verified mesh certificate CN) allowed to list/deny requests without a user token (ADR-0020). |
| `REQUEST_PREFER_WORKFLOW` | request-media | — | Prefer workflow. |
| `REQUEST_REQUIRE_APPROVAL` | request-media | — | Require approval. |
| `REQUEST_TRUST_CALLER_HEADER` | request-media | — | Dev only: with `MUXCORE_INSECURE_DISABLE_TLS=true`, `1` trusts `X-Caller-Id` without a bearer token (ADR-0019). |

### scheduler-cron

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SCHEDULER_CATCH_UP` | scheduler-cron | — | Catch up. |
| `SCHEDULER_HTTP_ADDR` | scheduler-cron | — | HTTP listen address. |
| `SCHEDULER_STORE_PATH` | scheduler-cron | — | Store path. |
| `SCHEDULER_TZ` | scheduler-cron | — | Tz. |

### secrets-file

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SECRETS_GRPC_ADDR` | secrets-file, secrets-vault | — | gRPC listen address. |
| `SECRETS_KEY_FILE` | secrets-file | — | Key file. |
| `SECRETS_MASTER_KEY` | secrets-file | — | Master key. |
| `SECRETS_STORE` | secrets-file | — | Store. |
| `SECRETS_TLS_CA` | secrets-file | — | CA bundle for mesh TLS verification. |
| `SECRETS_TLS_CERT` | secrets-file | — | TLS certificate file for the module's mesh client/server. |
| `SECRETS_TLS_DIR` | secrets-file | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `SECRETS_TLS_KEY` | secrets-file | — | TLS private key file. |

### secrets-vault

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `AWS_SECRETS_REGION` | secrets-vault | — | Aws secrets region. |
| `AZURE_KEY_VAULT_URL` | secrets-vault | — | Azure key vault url. |
| `GCP_PROJECT_ID` | secrets-vault | — | Gcp project id. |
| `INFISICAL_API_URL` | secrets-vault | — | Infisical api url. |
| `INFISICAL_CLIENT_ID` | secrets-vault | — | Infisical client id. |
| `INFISICAL_CLIENT_SECRET` | secrets-vault | — | Credential (secret). |
| `INFISICAL_ENV` | secrets-vault | — | Infisical env. |
| `INFISICAL_PROJECT_ID` | secrets-vault | — | Infisical project id. |
| `INFISICAL_TOKEN` | secrets-vault | — | Credential (secret). |
| `SECRETS_BACKEND` | secrets-vault | — | Backend. |
| `SECRETS_PREFIX` | secrets-vault | — | Prefix. |
| `VAULT_ADDR` | secrets-vault | — | Addr. |
| `VAULT_KV_MOUNT` | secrets-vault | — | Kv mount. |
| `VAULT_ROLE_ID` | secrets-vault | — | Role id. |
| `VAULT_SECRET_ID` | secrets-vault | — | Secret id. |
| `VAULT_SKIP_VERIFY` | secrets-vault | — | Skip verify. |
| `VAULT_TOKEN` | secrets-vault | — | Credential (secret). |

### serialization-safe

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SERIALIZATION_GRPC_ADDR` | serialization-safe | — | gRPC listen address. |
| `SERIALIZATION_MAX_PAYLOAD_BYTES` | serialization-safe | — | Max payload bytes. |
| `SERIALIZATION_TLS_CA` | serialization-safe | — | CA bundle for mesh TLS verification. |
| `SERIALIZATION_TLS_CERT` | serialization-safe | — | TLS certificate file for the module's mesh client/server. |
| `SERIALIZATION_TLS_DIR` | serialization-safe | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `SERIALIZATION_TLS_KEY` | serialization-safe | — | TLS private key file. |

### spool-resolver-http

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `SPOOL_RESOLVER_ALLOWED_HOSTS` | spool-resolver-http | — | Resolver allowed hosts. |
| `SPOOL_RESOLVER_ALLOW_HTTP` | spool-resolver-http | — | Resolver allow http. |
| `SPOOL_RESOLVER_ALLOW_PRIVATE` | spool-resolver-http | — | Resolver allow private. |
| `SPOOL_RESOLVER_GRPC_ADDR` | spool-resolver-http | — | gRPC listen address. |
| `SPOOL_RESOLVER_TIMEOUT` | spool-resolver-http | — | Resolver timeout. |

### storage-ceph

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `CEPH_ACCESS_KEY` | storage-ceph | — | Credential (secret). |
| `CEPH_BUCKET` | storage-ceph | — | Bucket. |
| `CEPH_CEPHFS_ROOT` | storage-ceph | — | Cephfs root. |
| `CEPH_KEYRING` | storage-ceph | — | Keyring. |
| `CEPH_MONITORS` | storage-ceph | — | Monitors. |
| `CEPH_PATH_STYLE` | storage-ceph | — | Path style. |
| `CEPH_POOL` | storage-ceph | — | Pool. |
| `CEPH_PREFIX` | storage-ceph | — | Prefix. |
| `CEPH_RGW_CA` | storage-ceph | — | Rgw ca. |
| `CEPH_RGW_CLIENT_CERT` | storage-ceph | — | Rgw client cert. |
| `CEPH_RGW_CLIENT_KEY` | storage-ceph | — | Rgw client key. |
| `CEPH_RGW_ENDPOINT` | storage-ceph | — | Rgw endpoint. |
| `CEPH_SECRET_KEY` | storage-ceph | — | Credential (secret). |
| `CEPH_STORAGE_BACKEND` | storage-ceph | — | Storage backend. |
| `CEPH_USER` | storage-ceph | — | User. |
| `CEPH_USE_SSL` | storage-ceph | — | Use ssl. |
| `STORAGE_CEPH_GRPC_ADDR` | storage-ceph | — | gRPC listen address. |
| `STORAGE_CEPH_HTTP_ADDR` | storage-ceph | — | HTTP listen address. |

### storage-overlay

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `OVERLAY_AES_KEY_HEX` | storage-overlay | — | Aes key hex. |
| `OVERLAY_ALLOW_MEMORY` | storage-overlay | — | Allow memory. |
| `OVERLAY_BACKEND` | storage-overlay | — | Backend. |
| `OVERLAY_COMPRESS` | storage-overlay | — | Compress. |
| `OVERLAY_DEDUP` | storage-overlay | — | Dedup. |
| `OVERLAY_ENCRYPT` | storage-overlay | — | Encrypt. |
| `OVERLAY_PREFIX` | storage-overlay | — | Prefix. |
| `OVERLAY_S3_ACCESS_KEY` | storage-overlay | `S3_ACCESS_KEY` | Credential (secret). |
| `OVERLAY_S3_BUCKET` | storage-overlay | `S3_BUCKET` | S3 bucket. |
| `OVERLAY_S3_ENDPOINT` | storage-overlay | `S3_ENDPOINT` | S3 endpoint. |
| `OVERLAY_S3_PATH_STYLE` | storage-overlay | `S3_PATH_STYLE` | S3 path style. |
| `OVERLAY_S3_PREFIX` | storage-overlay | `S3_PREFIX` | S3 prefix. |
| `OVERLAY_S3_REGION` | storage-overlay | `S3_REGION` | S3 region. |
| `OVERLAY_S3_SECRET_KEY` | storage-overlay | `S3_SECRET_KEY` | Credential (secret). |
| `OVERLAY_S3_USE_SSL` | storage-overlay | `S3_USE_SSL` | S3 use ssl. |
| `OVERLAY_UPSTREAM` | storage-overlay | — | Upstream. |
| `S3_ACCESS_KEY` | storage-overlay, storage-s3 | `S3_ACCESS_KEY` | Credential (secret). |
| `S3_BUCKET` | storage-overlay, storage-s3 | `S3_BUCKET` | S3 bucket. |
| `S3_ENDPOINT` | storage-overlay, storage-s3 | `S3_ENDPOINT` | S3 endpoint. |
| `S3_PATH_STYLE` | storage-overlay, storage-s3 | `S3_PATH_STYLE` | S3 path style. |
| `S3_PREFIX` | storage-overlay, storage-s3 | `S3_PREFIX` | S3 prefix. |
| `S3_REGION` | storage-overlay, storage-s3 | `S3_REGION` | S3 region. |
| `S3_SECRET_KEY` | storage-overlay, storage-s3 | `S3_SECRET_KEY` | Credential (secret). |
| `S3_USE_SSL` | storage-overlay, storage-s3 | `S3_USE_SSL` | S3 use ssl. |
| `STORAGE_OVERLAY_GRPC_ADDR` | storage-overlay | — | gRPC listen address. |
| `STORAGE_OVERLAY_HTTP_ADDR` | storage-overlay | — | HTTP listen address. |

### storage-s3

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `STORAGE_S3_GRPC_ADDR` | storage-s3 | — | gRPC listen address. |
| `STORAGE_S3_HTTP_ADDR` | storage-s3 | — | HTTP listen address. |

### tracing-otlp

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | tracing-otlp | — | Otel exporter otlp endpoint. |
| `OTEL_EXPORTER_OTLP_INSECURE` | tracing-otlp | — | Otel exporter otlp insecure. |
| `TRACING_GRPC_ADDR` | tracing-otlp | — | gRPC listen address. |
| `TRACING_TLS_CA` | tracing-otlp | — | CA bundle for mesh TLS verification. |
| `TRACING_TLS_CERT` | tracing-otlp | — | TLS certificate file for the module's mesh client/server. |
| `TRACING_TLS_DIR` | tracing-otlp | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `TRACING_TLS_KEY` | tracing-otlp | — | TLS private key file. |

### userdata-local

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `USERDATA_LOCAL_DB_PATH` | userdata-local | — | SQLite database file path. |
| `USERDATA_LOCAL_GRPC_ADDR` | userdata-local | — | gRPC listen address. |
| `USERDATA_LOCAL_HTTP_ADDR` | userdata-local | — | HTTP listen address. |
| `USERDATA_TLS_CA` | userdata-local | — | CA bundle for mesh TLS verification. |
| `USERDATA_TLS_CERT` | userdata-local | — | TLS certificate file for the module's mesh client/server. |
| `USERDATA_TLS_DIR` | userdata-local | — | Directory holding `tls.crt`/`tls.key`/`ca.crt`. |
| `USERDATA_TLS_KEY` | userdata-local | — | TLS private key file. |

### worker-pool-memory

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `WORKER_POOL_HTTP_ADDR` | worker-pool-memory | — | HTTP listen address. |

### workflow-tapestry

| Env Var | Module | Default | Description |
|---------|--------|---------|-------------|
| `WORKFLOW_GRPC_ADDR` | workflow-tapestry | — | gRPC listen address. |
| `WORKFLOW_MAX_PARALLEL` | workflow-tapestry | — | Max parallel. |

<!-- END GENERATED: check-env-docs -->

---

## Related documentation

- [`AGENTS.md`](../AGENTS.md) — deployment, vault SSH, public URLs
- [`_mvp/PORTS.md`](../_mvp/PORTS.md) — default listen ports
- [`_mvp/.env.example`](../_mvp/.env.example) — annotated local dev template
- [`admin-ui/docs/SETTINGS-COVERAGE.md`](../admin-ui/docs/SETTINGS-COVERAGE.md) — admin UI settings ↔ env mapping
- Per-module `AGENTS.md` — build and test commands

---

*Drift-checked by `scripts/check-env-docs.mjs` (see its header for scan scope: Go `os.Getenv`/`os.LookupEnv`/`*env*` helpers in active modules, plus `MVP_*`/`SMOKE_*` references in `_mvp` shell and compose files).*
