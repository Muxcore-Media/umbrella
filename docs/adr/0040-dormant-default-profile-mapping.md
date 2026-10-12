# ADR-0040: Dormant default-profile mapping

- **Status:** Accepted
- **Date:** 2026-10-12
- **Decided by:** Delegated under standing project authority; root and independent exact-candidate review accepted on 2026-10-12. Not owner-approved.
- **Tracks:** T-M4-02, FR-AUTH-010; task remains Todo and requirement Plan.
- **Related:** [ADR-0009](0009-per-module-data-ownership.md), [ADR-0015](0015-upgrade-compatibility-window.md), [ADR-0035](0035-user-erasure-ledger.md), [Proposed ADR-0039](0039-household-viewer-profile-authority.md).

This bounded decision concerns only dormant rows inside auth-local. It does not
accept ADR-0039's viewer-authority model or establish implementation or profile
activation. The exact source scope and gates are registered in the roadmap;
implementation starts only after root commits the accepted decision/registration
and explicitly releases the sole source owner. Remote documentation publication
is not a prerequisite. Design acceptance permits only a Draft implementation
candidate subject to the verification and fixture gates below.

## Context and evidence

Read-only review uses auth-local `v0.1.21`, source `9f9ec32`. Account creation
uses `internal/store/store.go:238–269`; bootstrap calls that path
(`bootstrap.go:33`), while invite redemption inserts users directly inside its
transaction (`invites.go:237–251`). A mapping added only to normal creation would
miss invites. Startup sweeps restored tombstoned users; offline `ImportLedger`
opens/migrates the database separately and commits ledger merge plus sweep
(`erasure.go:320–411,825–855`). Both paths need consistency validation.

The existing upgrade fixtures cover `v0.1.5` and `v0.1.19`; they do not establish
the immediate-predecessor `v0.1.21` or current N-1 release-train gate.

## Decision

### 1. Internal schema and invariant

Use one dedicated table, with explicit constraints:

```sql
CREATE TABLE account_default_profiles (
    user_id TEXT NOT NULL PRIMARY KEY REFERENCES users(id),
    profile_id TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL
);
```

Every extant, non-tombstoned user has exactly one row; every retained row names
such a user. `profile_id` is independent cryptographically random 128-bit data,
encoded as exactly 32 lowercase hexadecimal characters. Check entropy-read
errors; a generation or insertion failure aborts the enclosing transaction.
`created_at` is UTC formatted with Go's `time.RFC3339Nano`; validation requires
parse/UTC-format equality with that canonical representation. Do not normalize IDs, derive
them from usernames, infer equality with account IDs, or reuse deleted mappings.

There is no tenant/name/kids/PIN/default flag or second profile. Ownership is
the exact account ID; tenant remains owned by the current account. Declare the
foreign key without changing the connection's global foreign-key pragma or
relying on cascades. Purge mapping rows explicitly with their owning account.

### 2. Creation and migration

Inside one `BEGIN IMMEDIATE` write transaction, check whether the table exists.
On first introduction only, create it and backfill every live non-tombstoned
user. Commit all rows and DDL together; RNG/SQL failure leaves no partial table
or backfill. Existing unrelated migrations retain their current boundaries.

When the table already exists, never fill a missing mapping or replace an
invalid one. Defer row-consistency validation until tombstoned strays have been
purged, then reject missing live mappings, malformed IDs/timestamps, duplicate
identity, or unknown non-tombstoned orphan rows. Return an error and refuse open;
do not repair by assigning a new identity. Tombstoned rows may be purged even
when their mapping contents are malformed; preserve bystander mappings.

Normal user creation inserts user and mapping in one transaction. Bootstrap
inherits that path. Invite redemption inserts the mapping in its existing
transaction so failure also rolls back the new user and invite use count.
Successful repeated opens and account metadata/tenant changes retain the same
profile ID; no read creates or replaces a mapping.

### 3. Erasure, startup and offline import

Extend existing owner-ID cleanup to delete the mapping before the user row,
including stray rows whose owner is already absent but tombstoned. This is
provider-local housekeeping under ADR-0035, not broader erasure acceptance.

Startup's tombstone sweep and mapping-consistency validation share one write
transaction. Offline `ImportLedger` likewise runs ledger merge, tombstone sweep
and consistency validation in its existing write transaction, before commit.
An invalid live mapping or unknown orphan aborts that transaction, including new
ledger entries and deletions; a malformed tombstoned mapping is removed first.
`ImportLedger` performs forward migration before this transaction today. That
schema migration can persist when import later fails; this decision does not
promise rollback of the entire migration/import operation.

### 4. Dormancy, restore and downgrade limits

No ID enters `User`, claims, sessions, `UserInfo`, protobuf/HTTP responses or a
capability advertisement. No profile CRUD, PIN/grant, account-token retirement,
userdata/policy alias or migration, BFF/admin/SPA/native change is included.
Current account authentication, authorization and credential behavior remain.

IDs are stable only while the mapping survives in the database/backup. Table
absence is the first-migration marker and cannot distinguish an old schema
from total loss of that table. Restoring a pre-feature backup can mint new IDs
while dormant; this is not a claim of corruption-proof or active-profile restore
safety. Activation requires a separate preservation/restore decision. A backup
containing valid mappings preserves them on reopen and ledger reconciliation.

An old binary ignores the table and can create unmapped users or orphan rows.
Writable downgrade is unsupported. Rollback follows ADR-0015 by restoring a
compatible backup; do not run an old writer against the migrated database.

## Verification and delivery gates

Future source verification must cover:

- Fresh/normal/bootstrap/invite creation; stable reopen; unchanged account
  claims/session behavior; exact ID/time constraints and checked RNG failure.
- First-migration failure rollback and repeated migration; mapping insertion
  failure rolls back user creation and invite use count; concurrent opens and
  creation cannot violate uniqueness or mint replacements. Inject failures
  through private helper inputs or SQL triggers, not global RNG replacement.
- Missing/malformed live mapping and unknown orphan refusal; tombstoned owner
  and owner-absent cleanup with stable bystanders; startup sweep/validation
  rollback; offline import conflict/validation rollback and its separate
  prior-migration boundary; preserved backup IDs and pre-feature restore limits.
- Retained `v0.1.5`/`v0.1.19` upgrade fixtures: double open, seeded data, defaults,
  schema superset, integrity and foreign-key checks, plus mapping assertions.

Consistency checks cover mapping invariants only, not unrelated orphan-session
fixtures. Update direct test-user seeding and erasure row-count assertions to
include the mapping rather than widening production cleanup.

ADR-0015 requires a genuine immediate-predecessor `v0.1.21` snapshot generated
by source `9f9ec32e89c3ad87e9dfec2f2b45c381575a7c0b`, with recorded provenance,
and verified previous-train (N-1) coverage. B-3 blocks executing that old store
locally; the registered snapshot tool path also lacks `sqlite3`. No genuine new
snapshot has been produced. The historical missing `v0.1.7` note is not current
N-1 proof. Preserve existing released fixture artifacts; do not substitute hand-written SQL,
a fabricated database/provenance record or a CI workaround.

After root commits the decision/source registration and releases the owner, a candidate may
run normal available/hosted build, vet, race, lint and focused checks while kept
Draft. Such results cannot replace fixture provenance or migration verification.
No source acceptance, merge, pin, tag/release or activation until genuine
previous-tag/N-1 evidence, full applicable checks and independent review pass.
The exact source path scope remains owned by the roadmap; this ADR alone releases
none. FR-AUTH-010 stays Plan, T-M4-02 Todo and NFR-DATA-003 Plan; ADR-0039 remains
Proposed, and E7/B-8, T-M3-10 and existing runtime gates are unchanged.
