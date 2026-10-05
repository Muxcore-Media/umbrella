# ADR-0009: Per-module data ownership (SQLite per module)

- **Status:** Accepted
- **Date:** recorded 2026-10-05
- **Decided by:** Historical — observed in every stateful module (SQLite WAL, `SetMaxOpenConns(1)`, inline DDL)

## Decision (as implemented)

Each module owns a private store; no shared database; cross-module links are string IDs and events. `database-sqlite` / `database-postgres` are optional capability services, not the store most modules use.

## Alternatives

Shared Postgres with per-module schemas (stronger consistency, harder independent releases and backups); event sourcing via core WAL (not built).

## Consequences

- Simple single-host operation and independent module upgrades.
- Backups must enumerate every module's data (FR-BAK-002); user deletion needs an event fan-out (NFR-DATA-003); no cross-module transactions — workflows must be idempotent (NFR-REL-004).

## Revisit when

Multi-node deployment becomes a product goal, or Postgres is made the default backend.
