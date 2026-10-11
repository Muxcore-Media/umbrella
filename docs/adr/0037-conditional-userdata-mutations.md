# ADR-0037: Conditional userdata mutations and coordinated writer migration

- **Status:** Proposed
- **Date:** 2026-10-11
- **Decided by:** Delegated proposal under standing project authority; not accepted or owner-approved.
- **Tracks:** T-M4-09, FR-PLAY-004; requirements remain Part.
- **Related:** [ADR-0009](0009-per-module-data-ownership.md), [ADR-0011](0011-go-workspace-and-published-versions.md), [ADR-0015](0015-upgrade-compatibility-window.md), [ADR-0030](0030-authoritative-parental-policy-resource.md), [ADR-0033](0033-userdata-http-mesh-transport.md).

This draft proposes a direction and records unresolved contract choices. It does
not authorize implementation, change an accepted ADR or establish runtime
acceptance. Decision number `0034` remains reserved for the separate request-permission work.

## Context and source evidence

Three independent module reviews are read-only: no tests, probes or source edits.
Provider source is userdata `25de169`, code-identical to accepted `33dceaf1`
(metadata-only successor); BFF source is retained by `_mvp` `e98c46ba`; SPA source
is reviewed `7354e43`, tree-identical to accepted `fdfa25ff`. Existing transport
acceptance does not establish durable cross-device mutation semantics.

| Source anchor | Observed limitation |
|---|---|
| userdata `internal/store/merge.go:62,107`; public `store/store.go:114` | Favorite maps merge by key, so omission does not remove a favorite. Provider parsing supplies omitted preferences and empty arrays; the fallback preserves omitted sections. |
| userdata `internal/models/models.go:80` | Typed decoding loses unknown members, except explicitly raw fields. Unknown top-level fields are also discarded. |
| userdata `internal/store/merge.go:35`; `internal/store/store.go:151,209` | Progress compares timestamp strings, then a later write overwrites that freshness result. An internal favorite toggle exists but is not a network removal contract and is unsuitable for retry intent. |
| userdata `internal/server/server.go:145` and gRPC/store revision handling | A stored revision exists. HTTP omits it and network writes have no expected-revision precondition. Store serialization alone cannot detect stale client intent. |
| BFF `cmd/mediauiprox/userdata.go:181,278,295,369,407` | Typed blobs discard revision/unknown data; provider failure can succeed through local merge, and successful provider responses are merged into the local mirror. |
| BFF `cmd/mediauiprox/userdata_migrate.go:179–212` | An attempted migration marker permits another unconditional full local-blob import after an uncertain result, including intervening local changes. Existing tests preserve this behavior. |
| SPA `src/lib/userdata.ts:491,737,847,957`; progress hook | Every push sends six cached sections, including progress every four seconds. Favorite/wanted removals become omitted keys; unrelated writes can replay stale maps. |
| SPA `Settings.tsx:403`, `VideoPlayer.tsx:399`; `userdata.ts:257,362,946`; `api/client.ts:628` | Preference callers send displayed sections or spread cached siblings. Local revision guards are not server revisions, keys are not account-scoped, capability decoding is insufficient, and BFF success is not provider durability. |

A stale legacy favorite map and an intentional legacy re-add are indistinguishable.
An additive endpoint alone therefore cannot promise that removal survives later
unconditional legacy writes. Unknown fields already lost cannot be recovered by
this proposal.

## Proposed direction

### 1. Separate identifiable conditional operations

Introduce a separately identifiable versioned read/mutation contract carrying an
expected revision and explicit intent. Do not reinterpret legacy PUT fields or
silently let an old server ignore new fields and report success. Precise HTTP
routes, gRPC shape, envelope, revision encoding and error codes remain open.
Unsupported contracts, missing preconditions, conflicts and provider unavailability
must remain distinguishable from successful durable acknowledgments.

Bind revisions and responses to the confirmed provider authority and supported
user/tenant scope. A bare integer is insufficient if switching or restoring an
authority can reuse revisions. The authority identifier and its lifecycle are
unresolved; this draft does not invent a tenant mapping or repair tenant mode.
Keep existing identity checks and ADR-0030's separate parental-policy authority.
Any new HTTP operation needs an explicit ADR-0033 admission-table extension and
published fixed-operation client support before consumers use it; no widening of
module-only permissions is proposed.

### 2. Preserve presence and explicit mutation intent

Propose a raw, presence-preserving patch so omitted sections/members remain
unchanged. Merge preference object members; replace playlists/queue only when
explicitly supplied. Defaults belong to initial document construction or a read
presentation, not partial-request decoding. Preserve unknown members outside
explicitly replaced values. For whole-array replacements, clients must carry raw
unknown fields forward in retained items; a provider-side per-item identity/merge
rule would be a separate contract choice and is not implied by replacement.

Represent favorite set/remove as idempotent desired operations, not toggles or
omission; include an explicit wanted-removal disposition in the client contract.
Reject contradictory set/remove intent for one key. Exact null, empty-value,
unknown-operation and malformed-input rules must be resolved before code. An
explicit empty array must remain distinguishable from an omitted array.

Inside one transaction, read the current document/revision, validate the mutation,
compare its precondition, apply it, persist and increment the revision. Conflict
changes neither data nor revision. A coarse existing row revision is the smallest
initial mechanism; basic compare-and-write need not require DDL. No-op revision
behavior and receipt storage are still contract choices.

Normalize each accepted progress entry exactly once and compare parsed RFC3339
instants, including valid offsets and fractional precision. Do not overwrite that
selection later. Missing, invalid, equal or future timestamps and clock skew need
explicit policies; client timestamps cannot prove real event order. Distinguish
progress reports from explicit watched/unwatched intent.

### 3. Preserve uncertainty and local intent

A conflict is not permission to fetch a new revision and blindly replay a stale
full map. A fresh read may refresh confirmed state while retaining pending intent;
resolution/reapplication rules must be explicit. A timeout or lost acknowledgment
does not prove failure. Idempotent desired state does not itself provide request
deduplication or prove which operation committed.

Choose either a defined receipt/idempotency protocol or retained uncertainty with
reconciliation. If receipts are chosen, retries must reuse the exact operation
identity, payload and original precondition. Receipt scope, lifetime, storage,
restore behavior and matching acknowledgment rules remain unresolved. Even a GET
matching desired state or current revision cannot confirm the original command
or its committed revision. Retrying the original precondition can conflict
because the first attempt committed: conflict is neither proof of failure nor
permission to rebase. Reconciliation may report currently observed state
separately; a GET must not silently complete an uncertain operation.

SPA operations need pending/failed/conflict/uncertain state distinct from local
preference edit revisions, request order and session generation. Preserve those
existing guards. Bind new pending operations and acknowledgments to confirmed
account/session/authority scope. Current preference pending state is memory-only;
a first in-memory slice cannot claim restart durability. A persisted journal needs
an explicit account partitioning and lifecycle design because current local keys
are not account-scoped.

### 4. Give the BFF an authoritative conditional path

The new path must preserve raw fields, revision and authority rather than round
tripping `store.Blob`. It must not acknowledge via the legacy local fallback,
merge a successful result into the legacy mirror, or silently downgrade a
conditional operation to PUT after conflict, timeout or provider failure. A future
local conditional authority would need its own atomic revision contract.

Negotiate an explicit validated capability, default unsupported, including the
supported scope. Existing `favorites` capability remains separate. Conditional
support must not imply all writers are migrated or strong removal is enforced.

Existing completed migration markers are historical decisions, not revision proof.
Preserve unresolved local data; initially refuse conditional writes that require
unfinished migration. Any later automatic import needs provider-confirmed
never-written state under a defined absence/revision contract, not merely empty
collections. Freeze the exact payload and original expected revision. A changed
revision or ambiguous result requires reconciliation; never rebase the full import
blob automatically. Lost-ack replay remains unresolved without the agreed receipt
or uncertainty policy. Conditional reads must not trigger legacy GET migration.

### 5. Migrate all writers before stronger acceptance

Migrate the SPA's entire shared write path: progress, watched/unwatched, favorites,
wanted state, preferences, playlists and queue. Capture intent before constructing
a full cache snapshot. Settings submits a displayed section and player handlers
spread siblings; the transport cannot infer dirty preference fields. Either change
callers to supply dirty fields or document conservative section-level intent and
conflict handling. Preserve existing draft/focus and stale-response protections.

Inventory BFF routes, native clients, admin blob/PIN consumers, migration helpers,
optional synchronization and other direct writers. Each must migrate or be refused
for the participating scope at an explicit enforcement/capability cutover. This
inventory does not authorize blocked admin work or change existing unsupported
Jellyfin admission. Per-scope versus global cutover and compatibility-window rules
remain open. Legacy clients may coexist during an additive phase, but only
conditional acceptance is possible while unrestricted legacy writes remain.
Rollback must not silently restore unconditional writes while retaining a strong
removal claim. Disabling the feature must preserve pending/uncertain work honestly.

## Unresolved decisions before implementation

| Choice | Required resolution |
|---|---|
| Wire and revisions | Versioned paths/envelopes and HTTP/gRPC parity; Go int64 versus JavaScript safe-integer limits; exact parsing, error and acknowledgment shapes. |
| Authority continuity | Scope and lifetime across provider switch, database reset and restore; revision reuse, never-written proof and in-flight operations. |
| Mutation semantics | Null/empty/contradictory input, explicit re-add, no-op revision behavior, preference/array replacement boundaries and unknown data limits. |
| Ordering | Progress validity, equality, missing/future times, offsets and clock skew; interaction with explicit watched intent. |
| Uncertainty | Receipt/deduplication versus retained uncertainty, including migration lost-ack reconciliation; any DDL/retention/restore implications. |
| Enforcement | Complete writer inventory, capability meaning, global/per-scope switch, unsupported legacy behavior, rollback and account-partitioned pending journals. |

## Alternatives and consequences

- **Repair legacy merge functions only:** improves omissions/freshness but cannot
  distinguish stale replay from intended re-add or establish concurrent intent.
- **Explicit operations plus coarse row revision:** smallest proposed storage
  change; frequent progress can cause unrelated preference conflicts. Consumers
  must retain intent and handle those conflicts honestly.
- **Per-section/item revisions:** reduces unrelated conflicts but enlarges the
  wire, storage and migration contract.
- **Persistent removal tombstones/operation ordering:** supports richer offline
  resolution but adds retention, re-add, legacy-write and restore decisions.

Raw preservation and coordinated migration cost more than a merge-only patch.
No DDL is required merely to compare the existing revision; authority continuity,
receipts or persistent modes may require schema changes and ADR-0015 fixtures.

## Ordered future slices and acceptance

These are proposed sequencing gates, not registered source implementation tasks.
One writer per repository; publish actual contracts before dependent edits.

1. Resolve the open choices and review/accept the contract; complete the writer
   inventory and explicit compatibility/rollback boundary.
2. Provider first: HTTP/gRPC parsing, raw presence semantics, atomic preconditions,
   explicit removal and progress rules. Verify omitted/empty/null, unknown fields,
   both orders of concurrent disjoint/conflicting updates, no-change conflicts,
   retry/re-add, reopen and authority continuity under the selected policy.
3. Publish the reviewed transport/protocol operation and source version. Preserve
   ADR-0033 identity/admission tests and ADR-0015 upgrade fixtures as applicable;
   an unavailable tag is a dependency gate, not permission for an unpublished pin.
4. BFF: consume that published operation; preserve revision/authority and typed
   outcomes; prove no fallback, mirror merge, GET migration or silent downgrade.
5. Each client/writer: migrate every mutation family and intent-producing caller;
   verify conflicts/uncertainty, newer edits, account changes, unknown data and
   honest capability status. Native Apple/runtime gates remain separate.
6. Migration/enforcement: verify never-written imports, frozen retries, uncertain
   results, legacy interference, every writer disposition and rollback before
   claiming removal survives mixed-version use.
7. Real provider/BFF/client fixtures: verify acknowledged changes after provider,
   BFF and client restart, second-device reads, offline recovery and concurrent
   devices. Frontend fixtures alone cannot close FR-PLAY-004.

No tests or acceptance results are supplied by this draft. E7/platform and
T-M3-10 blocks, erasure/parental authority, deployment and train scope are unchanged.
