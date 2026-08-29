# userdata-local

Durable per-user progress / favorites / prefs / playlists / queue for the household loop.

- Capability: `userdata` / `userdata.local`
- HTTP: `GET|PUT /userdata?user_id=&tenant_id=` (also `X-User-ID` / `X-Tenant-ID`)
- Storage: JSON files under `USERDATA_LOCAL_DATA_DIR` (default `/var/lib/muxcore-userdata`)
- Merge: entry-level for progress (newer `updatedAt` wins); prefs/queue/playlists take PUT body when present
- Tenant: when `TENANT_MODE=1`, files live under `tenants/{id}/{user}.json` (per-tenant data subdirs)

The media BFF (`mediauiprox`) embeds the same `store` package locally and may proxy via `USERDATA_LOCAL_URL`
(prefer mesh unless `USERDATA_PREFER_MESH=0`). The `jellyfin` bridge can pull/push progress and favorites
when `USERDATA_SYNC=1` (see `jellyfin` module README).
