# ADR-0015: Upgrade compatibility window and schema fixtures

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

Modules create their SQLite schemas inline at startup, with `CREATE TABLE IF NOT EXISTS` plus `ALTER TABLE … ADD COLUMN` that tolerates "duplicate column". There is no schema version. Schemas did change between recent tags. NFR-DATA-002 and FR-INS-005 require upgrades to be tested.

## Decision

1. Upgrades are supported from the module versions pinned by the **previous release train** (N-1), and from each module's previous tag. There are no down-migrations; rollback means restoring a backup ([ADR-0013](0013-backup-coverage-model.md)).
2. Each stateful module commits snapshot fixtures under `testdata/upgrade/<tag>.db` (with `<tag>.schema.sql`). Each snapshot is produced by that tag's code, with seed rows, using `scripts/upgrade-fixtures/snapshot.sh`.
3. An `upgrade_test.go` per module opens each snapshot with the current code, twice. It asserts four things:
   - The upgraded schema is a superset of a fresh database's schema.
   - Seeded rows read back correctly.
   - New columns have their defaults.
   - The integrity and foreign-key checks pass.

   Shared helpers live in `core/sdk/go/module/moduletest`.
4. When a module's DDL changes, its next release must add a snapshot of the tag it is upgrading from.

## Consequences

- Fixture databases (kept under 100 KB each) live in the repos.
- An image-level upgrade drill (previous train → current train) follows once two trains of images exist.
