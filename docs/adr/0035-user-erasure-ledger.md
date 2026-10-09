# ADR-0035: User erasure is a durable identity-provider ledger reconciled by every personal-data owner

- **Status:** Accepted
- **Date:** 2026-10-09
- **Decided by:** Delegated (standing project authority, 2026-10-05). Not owner-approved.
- **Refines:** the [ADR-0009](0009-per-module-data-ownership.md) consequence "user deletion needs an event fan-out". ADR-0009's decision is unchanged.
- **Extends:** [ADR-0017](0017-mesh-module-identity.md), [ADR-0020](0020-module-principal-allowlists.md), [ADR-0026](0026-administrative-session-contract.md), [ADR-0030](0030-authoritative-parental-policy-resource.md), [ADR-0013](0013-backup-coverage-model.md)/[ADR-0015](0015-upgrade-compatibility-window.md) (restore)

## Context

NFR-DATA-003 (SRS:201, Plan) requires deleting a user to delete or anonymise
their userdata, requests, sessions and playback history. The plan in LLD
data-ownership §3 (`docs/architecture/lld/data-ownership.md:35-37`) is to publish
`identity.user.deleted` on the mesh bus. Draft umbrella PRs #187–#192
(2026-10-05) implement that plan in submodule commits that are not on any pinned
branch and have no tags. The evidence shows a plain event cannot meet the requirement.

| Evidence (source) | Consequence |
|---|---|
| core `internal/events/memory.go:18-20,276-296`: a full 256-slot subscriber channel drops the event and `Publish` still returns nil. There is no ack, retry or dead-letter. `internal/grpcmesh/events.go:157-163` cancels a module's subscriptions when its stream ends | Delivery is at-most-once. A module that is down, restarting or slow misses the deletion for good |
| core WAL is enabled only by `MUXCORE_EVENT_JOURNAL_PATH` (`cmd/muxcored/init.go:132-137`), which no `_mvp` deployment sets. The gRPC `Replay` RPC has no live tail, the proto `Event` has no sequence field, and segments are pruned regardless of consumer progress (`internal/events/wal.go:32-36`) | The bus cannot provide catch-up |
| SDK `sdk/go/client/client.go:501-508` closes the channel when the stream ends, with no resubscribe. The userdata-local and request-media drafts then stop consuming permanently (`userdata-local@b2d3b68 internal/subscribe.go:30-31`, `request-media@c4d7b2c internal/userdeleted.go:42`) | Drafts lose all later deletions after one core restart |
| `events.go:104` copies the publisher-supplied `Source` without checking it. Subscribers have no verified publisher field. Every draft handler acts on the payload alone, e.g. `userdata-local@b2d3b68 internal/userdeleted.go:14-28` | Any module allowed to publish the type could erase any user |
| `publish-policy-default/policies.yaml` allows `auth-local` only `auth.*`, and registry matching is off by default (`internal/policy/policy.go:131-133`; no `PUBLISH_POLICY_REGISTRY_MATCH` in `_mvp`) | In household compose the drafts' publish would be **denied**. Their unit tests used capture publishers and could not see this |
| `EventService/Subscribe` is an open method (`core/internal/grpcmesh/auth.go:58`, `events.go:44-52`) | Event payloads must not carry identifying data |
| auth-local `internal/store/store.go:309-335` deletes `sessions`, `webauthn_credentials`, `api_tokens` and `users` in one transaction. It leaves `totp` (:159-165) and `webauthn_sessions` (:166-171) behind, and foreign keys are not enabled (DSN :91) | The provider cleanup itself is incomplete |
| auth-local `internal/server/server.go:467-478` + `admin_auth.go:59-62`: gRPC `DeleteUser` accepts **any** CN-verified mesh peer as admin, with no self-delete or last-admin check. HTTP `internal/webapp/users.go:170-186` has both checks, but the last-admin count runs outside the delete transaction. admin-ui `handler/users.go:211` sends no user bearer, so it relies on the mesh-peer bypass | The authority to delete is broader than "admin", and there is a time-of-check/time-of-use race between the count and the delete |
| auth-local ids are 128-bit random hex (`store.go:731-735`) and never reused. Usernames are reusable (`UNIQUE` among live rows, :120). auth-oidc uses the raw IdP `sub` (`internal/oidc/provider.go:216`) and returns `Unimplemented` for `DeleteUser` (`internal/server/server.go:154-164`) | Erasure must key on the id, never the username. OIDC ids can return |
| BFF revalidates every bearer-backed session (`_mvp/cmd/mediauiprox/session_revalidate.go:123-126`), and admin-ui does the same (`handler/session_revalidate.go:43-108`). Quick Connect sessions have no bearer and are **never** revalidated (`session_revalidate.go:80-82`) | Revoking at the provider is effective for bearer sessions, but Quick Connect sessions need local erasure |
| backup-local keeps 10 archives with no age limit (`_mvp/docker-compose.registry.yml:942-943`). Restore runs no hooks (`backup-local/internal/module.go:353-418`). ADR-0015 says rollback means restoring a backup | Restore re-introduces deleted users unless the ledger wins |

## Decision

### 1. Authority and trigger; revoke first

1. Erasure starts only through the identity provider's `DeleteUser`, gRPC or the
   provider's HTTP equivalent. The provider requires a current, fully
   authenticated **end-user admin bearer** in `x-auth-token`, rechecked as in
   ADR-0026 §2. A verified module certificate alone is insufficient. This
   removes the mesh-peer bypass for this method only.
2. The target must exist in the caller's verified tenant (empty means the
   single household). Self-deletion is rejected on every path. Deleting the
   last user holding `admin` in the tenant is rejected, and that count is taken
   **inside** the same write transaction as the delete. Self-service account
   deletion is out of scope.
3. One provider transaction:
   - deletes the user row, every `sessions` row (full, partial and api-token),
     `api_tokens`, `webauthn_credentials`, `webauthn_sessions` and `totp`;
   - revokes unredeemed invites the user created and replaces their
     `created_by` username with `deleted-user`;
   - inserts a **tombstone** `user_erasures(erasure_id, user_id, tenant_id,
     deleted_at, deleted_by)`. `erasure_id` is random and opaque; there is no
     username.

   After commit, every bearer for the user fails `Validate`. Fan-out never runs
   before this revocation, and the provider never waits on it.
4. `DeleteUserResponse` gains `erasure_id`. Repeating `DeleteUser` for a
   tombstoned id succeeds and returns the same `erasure_id`. A never-seen id
   returns not found.
5. Bearer-backed app cookies are dropped on their next use by the existing
   revalidation. Bearer-less (Quick Connect) sessions and other app-local state
   are erased by §2. Streams that are already established are not terminated
   (see §7).

### 2. Fan-out: provider ledger plus consumer reconciliation (pull)

1. **The ledger is the only authority.** core's auth proto gains:
   - `ListUserErasures(page_token, page_size)` → `{erasure_id, user_id,
     tenant_id, deleted_at, acknowledged_by_caller}`;
   - `AckUserErasure(erasure_id, outcome, detail_code, counts)`;
   - `GetUserErasureStatus(erasure_id | pending-only)`.

   Providers that do not implement them return `Unimplemented`.
2. `ListUserErasures`/`AckUserErasure` admit only a verified mesh certificate
   whose CN is on the provider allowlist `AUTH_ERASURE_CONSUMERS`
   (ADR-0020 pattern). The acknowledging module is the verified CN, never a
   request field. A user bearer is rejected. `GetUserErasureStatus` requires an
   admin bearer (ADR-0026 rule).
3. `AUTH_ERASURE_REQUIRED` lists the modules whose acknowledgement completes an
   erasure. Deployment generates it from the household-manifest `personal: true`
   entries that are enabled.
4. **Every personal-data owner runs a reconciler.** It runs at startup, every
   `ERASURE_SWEEP_INTERVAL` (default 5 min, jittered), and on an operator or
   CLI trigger. Each run:
   - lists the **whole** ledger (paged; household scale, no cursor);
   - for each tombstone not in its local `erasure_applied` record, applies its
     disposition (§3) **in one local transaction that also records the
     application**;
   - checks the post-condition (no remaining row carries the id);
   - acknowledges, retrying until accepted. Acknowledgement is idempotent.
5. A consumer reaches the ledger only through the provider of the exclusive
   `identity` capability (household requires a certificate:
   `core/internal/module/mgr/manager.go:196-218`). It verifies the server's
   leaf CN equals that provider's module id (ADR-0033 §1 pattern). No ledger
   response from any other source is acted on.
6. A shared SDK helper owns discovery, CN check, paging, sweep scheduling and
   acknowledgement, so this security path is not copied into seven modules. Each
   owner supplies only `Apply(ctx, tombstone) (counts, error)`.
7. An optional later hint event (`identity.erasure.recorded`, empty payload,
   publish rule `auth-local: identity.*`) may trigger an early sweep. It is
   never authority, and a spoofed hint costs one extra `List` call.

### 3. Per-owner disposition

The tombstone's `user_id` is globally unique, so deletion is by id. Where the
owner stores a tenant, it also checks that the tenant matches the tombstone.

| Owner | Data keyed by/containing the user | Disposition |
|---|---|---|
| auth-local | user, sessions, tokens, passkeys, passkey sessions, TOTP | **Delete** in the §1 transaction |
| auth-local | invites `created_by` (username via `internal/server/invites.go:39-48`; column `internal/store/invites.go:61`) | Revoke unredeemed invites; **anonymise** `created_by` |
| auth-local | `user_erasures` tombstone (id, tenant, times, deleting admin id) | **Retain with reason:** this is the erasure authority and protects restores. Never pruned. Contains no username |
| userdata-local | `user_blobs` (`internal/store/store.go:43-48`, no tenant column) | **Delete** |
| userdata-local | `parental_policies` `(tenant_id,user_id)` (`internal/store/parental.go:18-26`) | **Delete** the user's row. Rows where `updated_by` is the user: **anonymise** `updated_by` |
| userdata-local | Viewer-profile siblings | **None exist at pins:** no `~p~` key in any pinned source. T-M4-02 must key per-profile data under the owning account so this disposition covers it |
| request-media | `requests` with `requested_by` (`internal/reqstore/store.go:55-78`) | `watchlisted`, `pending` and `denied` rows: **delete**, along with their `request_ready_notified` rows. `approved`/`processing`/`available` rows are a household acquisition record: **anonymise** `requested_by` to `deleted-user` |
| request-media | `request-policy.json` `auto_approve_users` (`internal/policy.go:25-30`) | **Remove** the id. Quotas are computed, not stored. Votes/comments do not exist |
| playback-monitor | `sessions` (IP, device, geo), `server_users`, `user_identities` (`internal/db.go:108-165`) | **Delete** in one transaction: the `server_users` row `(muxcore-native, id)`, its identity, every session of that identity (an operator merge asserted it is the same person), the identity's other links, and any `muxcore-native` sessions with `user_id = id` |
| playback-monitor | External media-server accounts not merged with the MuxCore identity | **Retain with reason:** they are keyed by a different id space (`internal/identity.go:15-21`) and nothing links them to this user. Operators use `DeleteUserHistory` or a merge |
| playback-guard (optional) | `violations`, `trust_scores`, `user_aliases` (`internal/db.go:36-60`) | **Delete** rows for the id and aliases whose canonical user is the id. Rows keyed by `name:` or an external id with no link: same retention reason as playback-monitor |
| BFF (`_mvp` mediauiprox) | `sessions.json`, including Quick Connect; `quickconnect.json` codes; fallback blobs `<dir>/<id>.json` and `tenants/<t>/<id>.json`; `.provider-migration` markers; `watch-together.json` rooms hosted by the user | **Delete** |
| BFF | `media-issues.json` `reportedBy` (a username, `media_issues.go:116-129`) | **Anonymise.** New entries also store the reporter id; legacy entries with an unresolvable username are anonymised |
| admin-ui | `sessions.json` | **Delete** the user's sessions. A persist failure fails the application |
| admin-ui | `password-resets.json` (username-keyed, shared with the BFF) | **Delete.** New entries store the user id resolved at request time; legacy entries whose username no longer resolves are purged. admin-ui is the single eraser for this file |
| admin-ui | `parental.json` legacy entry (`handler/parental.go:68`) | **Delete** (ADR-0030 §7 migration source only) |
| core | Audit JSONL (actor/resource ids; `Details` must not carry PII) | **Retain with reason:** it is a hash-chained, tamper-evident log (NFR-OPS-004). Retention is by size rotation (100 MB × 5) |
| media-library-maintainer (optional) | `candidates.criteria_json` holding `RequestedBy` and per-user watch maps (`internal/scan.go:418`) | **Delete** the affected candidates; they are regenerated on the next scan |
| jellyfin bridge (optional) | `userdata_user_map` entries pointing to the id (`internal/module.go:105`) | **Delete** the mapping |
| notification-default, media-tagging, media-list-sync, emby, plex, ai-* | No household-user-keyed persistence found | No action. They are not consumers |

Any owner that writes on behalf of a user id **without** that user's bearer
(bridge pushes, BFF Quick Connect ingest) must refuse ids in its last-seen
ledger.

### 4. Backups

1. Archives are immutable and checksummed, and are **not rewritten**. A deleted
   user's data stays in archives made before the deletion until retention
   (default 10 archives, no age limit) or the operator removes them. The
   admin-ui delete confirmation and `_mvp/docs/RESTORE.md` say so. Operators
   wanting a time bound set `BACKUP_RETENTION_DAYS`; the default is unchanged.
2. **The tombstone wins over restored data.**
   - Before replacing auth-local state, restore tooling exports the live ledger.
     It imports the ledger after the restore and before the stack serves (an
     `authctl erasures export|import` run on auth-local's own database while it
     is stopped). With no live system, use the newest available auth-local
     archive, never one older than any other restored module.
   - At startup auth-local deletes any user, credential or session row whose id
     is tombstoned, and refuses to create a tombstoned id.
   - Restored consumers carry `erasure_applied` records consistent with their
     own snapshot, so their startup sweep re-erases restored data.

### 5. Completeness and "done"

An erasure is **complete** when every module in `AUTH_ERASURE_REQUIRED` has
acknowledged `ok`. Pending or failed modules, with their last acknowledgement
time, appear in admin-ui `/users` and in `muxcorectl users erasures`.

NFR-DATA-003 moves to Impl only when all of the following hold:
- provider and per-owner tests pass (§Verification);
- a cross-module `integration/` test passes in which one consumer is stopped
  during deletion and catches up after it starts;
- a household clean-room journey reaches "complete" and the owners' APIs and
  stores show no rows for the id;
- the restore drill passes with a pre-deletion archive and no resurrection.

### 6. Failure, re-run and security

1. Failure handling:
   - **Provider down:** deletion fails closed; consumers back off.
   - **Consumer down:** the erasure stays pending, and the consumer catches up
     at startup.
   - **Local failure:** the transaction rolls back and the module acks
     `failed`; the next sweep retries.
   - **Lost acknowledgement:** it is re-sent, and acks are idempotent.
2. Re-applying an applied tombstone is a no-op, so replaying old tombstones is
   harmless.
3. Spoofing: no event, header, payload or non-provider response can erase
   data. Only an admin bearer creates a tombstone, and only the
   `identity`-capability provider serves the ledger. In `dev` without
   certificates `x-caller-id` is trusted (ADR-0017 §2), so dev has no household
   acceptance.
4. ID reuse: auth-local ids are random and checked against the ledger at
   creation and import. Usernames are never an erasure key; the legacy
   username-keyed stores in §3 are anonymised or purged.
5. Exposure: the ledger reveals deleted ids only to allowlisted modules. No
   bus event carries an id.

### 7. Out of scope

- Self-service deletion.
- auth-oidc erasure. It needs issuer-namespaced ids plus a tombstoned-subject
  login refusal before it can claim NFR-DATA-003; until then its ledger RPCs
  return `Unimplemented` and status shows `unsupported`.
- Profiles/PIN (T-M4-02).
- Rewriting archives or core audit entries.
- External accounts (Jellyfin/Plex/Emby servers, Trakt, the IdP).
- Terminating streams that are already established.
- Data export or portability.
- Multi-node operation.
- The `database-sqlite` shared capability (unverified whether any household
  module stores user rows there).

## Alternatives

- **At-most-once `identity.user.deleted`, as in draft PRs #187–#192:** misses
  modules that are down, denied by the default publish policy, `Source` can be
  spoofed, and two drafts never resubscribe (Context table).
- **Core WAL replay:** not configured, no remote cursor, pruning ignores
  consumer progress, no live tail.
- **Provider pushes an erase RPC to each owner:** needs a live consumer
  inventory, outbound mTLS to every owner, a new admission row in each
  (e.g. ADR-0033's fixed table) and the same durable retry state. Pull reuses
  each owner's startup and the existing `identity` trust. The ledger allows
  adding push later.
- **Core-hosted EventStore/outbox:** has no gRPC surface for sidecars and would
  make core hold personal data.
- **Provider deleting other modules' rows directly:** violates ADR-0009.
- **Disable the account instead of erasing it:** does not meet NFR-DATA-003.
- **Leave dangling references (RULE-VAL-3):** tolerates deletion but erases nothing.

## Consequences and verification

- **Contracts first:** this is an auth-proto addition plus a core SDK helper,
  released before the provider and the consumers (ADR-0011). Every stateful
  consumer adds a small `erasure_applied` table, which needs ADR-0015 upgrade
  fixtures.
- **Deployment:** must set `AUTH_ERASURE_CONSUMERS`/`REQUIRED` and keep
  them in step with the manifest.
- **Docs:** LLD data-ownership §3 is rewritten. Draft PRs #187–#192 are
  closed as superseded. The documents they edit, e.g.
  `docs/adr/0024-viewer-profiles-in-userdata.md` (umbrella commit `2413540`),
  were never merged.

**Required negative tests:**
- `DeleteUser` rejects each of these: a non-admin bearer, a module certificate
  without a bearer, a cross-tenant target, self-deletion (gRPC and HTTP), and
  the last admin. For two concurrent deletes of the last two admins, exactly
  one succeeds.
- After deletion:
  - `Validate` fails for full and api-token sessions;
  - TOTP and passkey rows and passkey sessions are gone;
  - `CreateUser` or import with a tombstoned id is refused.
- `List`/`Ack` reject a user bearer and a non-allowlisted CN. A forged
  acknowledging module field is ignored.
- A consumer:
  - ignores a published fake `identity.user.deleted` or hint event, with no
    rows changed;
  - rejects a ledger served by the wrong CN;
  - with the provider unreachable, erases nothing.
- Per owner:
  - victim rows are removed or anonymised and a bystander's rows are intact;
  - a second sweep is a no-op;
  - an injected mid-transaction failure rolls back and the next sweep
    completes;
  - the post-condition count is 0.
- A Quick Connect session of a deleted user is gone within one sweep.
- Restoring a pre-deletion archive neither resurrects the user nor leaves
  their data in place.
