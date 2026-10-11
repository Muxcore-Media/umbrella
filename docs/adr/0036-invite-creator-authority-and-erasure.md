# ADR-0036: Bind user-created invites to current creator authority and serialize creation with erasure

- **Status:** Accepted
- **Date:** 2026-10-11
- **Decided by:** Delegated (root review and acceptance, 2026-10-11, under the standing project authority). Not owner-approved.
- **Refines:** [ADR-0035](0035-user-erasure-ledger.md) invite cleanup and creation during erasure; its accepted text is unchanged.
- **Extends:** [ADR-0019](0019-end-user-identity-propagation.md), [ADR-0020](0020-module-principal-allowlists.md) and [ADR-0026](0026-administrative-session-contract.md) for the invite-creation authority selected below.
- **Tracks:** T-M4-07 E2, FR-AUTH-003 and NFR-DATA-003.

## Context

The E2 pin review found two invite gaps in current auth-local `master`/`v0.1.20`
`6d8723e9f4e6ae5e45284c3be0e0a5d45c81e925`: purge exempts the literal creator
label `deleted-user`, so a real user with that username keeps unredeemed invites;
and invite creation can commit after deletion using a previously resolved creator.
Invites persist `created_by` as a label, without an immutable creator ID. Resolving
the username again can incorrectly bind a stale request to a replacement account.

A supplied end-user bearer can also coexist with an admitted mesh certificate.
Treating the certificate as a fallback after bearer failure loses the caller's
selected authority. HTTP's second authentication lookup must not replace a failed
lookup with the literal creator label `admin`. These are source-review findings;
the registered correction still needs its implementation and verification gates.

## Decision

### 1. Select authority before creating an invite

For invite creation, a supplied bearer selects **end-user authority**, even when
the request also has a verified, otherwise admitted mesh certificate. Require a
fully authenticated, currently valid admin user. Invalid, partial or non-admin
supplied bearer authentication refuses the request without module fallback and
without inserting an invite. Preserve the provider's existing authentication and
authorization error distinctions.

When no bearer is supplied, retain existing verified-module admission and creator
attribution. This decision neither creates a new module-only exception nor widens
the existing one. Identity headers and unverified certificates supply no authority.

For HTTP, failure of the second authenticated-user lookup refuses the request;
there is no literal `admin` fallback. Carry the resolved immutable user ID into
the store operation. Preserve existing invite role, tenant, use and expiry rules.

### 2. Serialize user-created invites with deletion

Add an internal store operation `CreateInviteForUser` that uses the existing
`BEGIN IMMEDIATE` `writeTx`. In that same transaction, require that the immutable
creator ID still exists, is not tombstoned, and currently holds the admin role;
derive its username there and insert the invite before committing. No username
lookup may substitute another user's ID. A failed check or write rolls back.

Creation and erasure therefore have two permitted orderings: if creation commits
first, the subsequent erasure revokes/anonymizes the invite; if erasure commits
first, creation refuses. A same-name replacement account does not revive authority
held by the erased ID. Keep the raw-label store method only for the existing
module-only adapter and legacy fixtures, not user-authorized HTTP/gRPC creation.

This slice adds no schema, proto or dependency change and does not redesign invite
storage. It does not change list/revoke authority or unrelated administrative APIs.

### 3. Purge a real marker-named creator and restore safely

Remove the `deleted-user` marker-name exemption in the existing user purge
transaction. A surviving user row with that actual username receives the same
invite revocation/anonymization as any other deleted creator. Retain transaction
rollback/retry behavior. The existing startup restore purge applies this rule
when the restored user row is still present; no new worker or purge path is added.

### 4. State the legacy provenance boundary

The current `created_by` label cannot distinguish a username from an equal module
CN or distinguish different users who reused a username. Already orphaned invite
rows can also lack a surviving user row from which to recover attribution. This
proposal makes no claim that same-text unrelated invites can be preserved exactly,
and does not infer or broadly purge orphan provenance. These cases remain an
explicit limitation requiring operator reconciliation; a durable creator-kind/ID
migration would need a separate decision and compatibility plan.

## Alternatives

- **Check the username before the transaction:** leaves a deletion race and can
  select a same-name replacement. Check the immutable creator ID inside the write.
- **Use a mesh certificate after a supplied bearer fails:** changes the selected
  principal and can grant a rejected user the module's authority. Refuse instead.
- **Reserve the `deleted-user` username or exempt that label:** fails existing
  users/restores and cannot identify legacy provenance reliably.
- **Add creator-kind/ID columns or purge all unresolved labels now:** may address
  the remaining ambiguity but requires migration and retention decisions outside
  this bounded correction.

## Verification and acceptance

Before source acceptance, run the applicable complete auth-local gates and obtain
independent review. If local dependencies are unavailable, record that limitation
and inspect the exact candidate's actual hosted build, vet, full race, lint and
generated-code results. Required regressions include:

- Normal and literal `deleted-user` creator cleanup with unrelated bystanders.
- A deterministic HTTP request paused after creator resolution, followed by
  erasure and same-name account replacement, then refusal on resume.
- Both create/erase transaction orderings, with no live invite owned by an erased
  creator once erasure commits.
- An injected purge-trigger failure proving rollback, then successful retry.
- Supplied admin/invalid/partial/non-admin bearer cases alongside admitted mTLS,
  plus the unchanged absent-bearer module case and HTTP second-lookup failure.
- Startup restore with a surviving tombstoned marker-named user row.

Acceptance of this ADR authorizes only the registered source scope. It does not
accept the implementation, repair the umbrella pin by itself, close NFR-DATA-003,
or establish the E12 household deletion/restore journey or deployment.
