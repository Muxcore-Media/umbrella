# playback-contract

Shared protobuf contract for normalized playback session events across MuxCore bridges and `playback-monitor`.

Bridges publish mesh JSON derived from `PlaybackSessionEvent`; `playback-monitor` is the sole session DB writer.

Shared query types live in `query.proto`:

- `Session` — normalized read model
- `SessionFilter` — history/analytics/public API filters
- `StatsRangeQuery`, `ChartQuery`, `HomeStatsQuery`, `ConcurrentStreamsQuery`

## Storage backends

Default: SQLite file at `PLAYBACK_MONITOR_DB_PATH`.

PostgreSQL/Timescale: set `PLAYBACK_MONITOR_DATABASE_URL` or `DATABASE_URL` to a `postgres://` URL. Optional `PLAYBACK_MONITOR_TIMESCALE=1` creates a sessions hypertable. Deploy with spool tag `playback-plus-postgres`.
