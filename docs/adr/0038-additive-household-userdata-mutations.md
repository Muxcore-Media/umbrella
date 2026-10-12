# ADR-0038: Additive household userdata mutations

- **Status:** Accepted
- **Date:** 2026-10-12
- **Decided by:** Delegated under standing project authority; root and independent contract review accepted on 2026-10-12. Not owner-approved.
- **Tracks:** T-M4-09, FR-PLAY-004; requirement remains Part.
- **Extends:** [ADR-0033](0033-userdata-http-mesh-transport.md), solely by admitting the new fixed HTTP path for `media-ui`.
- **Related:** [ADR-0009](0009-per-module-data-ownership.md), [ADR-0011](0011-go-workspace-and-published-versions.md), [ADR-0015](0015-upgrade-compatibility-window.md), [ADR-0030](0030-authoritative-parental-policy-resource.md), [ADR-0035](0035-user-erasure-ledger.md), [Proposed ADR-0037](0037-conditional-userdata-mutations.md).

This bounded decision defines a first provider HTTP operation. ADR-0037 remains
Proposed for progress/watched ordering, client integration, migration and
legacy-writer enforcement. No source implementation, published release, consumer
support or runtime acceptance is established by this decision. The exact provider
file and test scope is registered and approved in the roadmap; implementation
starts only after root freezes the accepted documentation in a local commit and
releases that scope. Remote documentation publication is not an additional
implementation prerequisite.

## Context and boundary

Read-only provider, BFF and SPA review found that a first useful conditional API
can support preferences, favorites, want-to-watch, playlists and queue without
deciding progress ordering or migrating every client. Provider baseline
`25de169` is code-identical to accepted `33dceaf1`; it already has an integer
row revision and serialized write/erasure guards. Legacy typed reads/writes lose
unknown JSON members, omission cannot express favorite removal, and stale
full-map writes can overwrite newer state. BFF fallback/migration and SPA
whole-blob pushes remain separate work described in ADR-0037.

The first contract is additive and HTTP-only. Every legacy writer remains
enabled. A later legacy write can restore a removed favorite or discard unknown
fields, so conditional support does not mean coordinated removal, full device
durability or migration is complete. Parental-policy authority remains separate
under ADR-0030; raw `prefs.parental` never grants policy or PIN authority.

## Decision

### 1. Operation, caller and identity scope

Expose `GET` and `PUT /api/userdata/v2` as contract `userdata.conditional.v2`.
The only supported mutation families are `prefs`, `favorites`, `wantToWatch`,
`playlists` and `queue`. Reject `progress`, watched/unwatched operations and any
other mutation family, including when mixed with supported fields; no part of
such a request is applied. Existing progress is preserved as raw stored data.
There is no new gRPC operation or changed legacy endpoint semantics.

Extend ADR-0033's method/path admission only for this new path:

| Verified module CN | `GET /api/userdata/v2` | `PUT /api/userdata/v2` |
|---|---|---|
| `media-ui` | admitted | admitted |
| Any other CN, including `admin-ui`, `userdata-local`, `jellyfin` and `muxcore` | denied | denied |

Retain enrolled mTLS provider/caller identity, exact provider service SAN/CN,
fixed-origin/no-redirect client behavior and module-refusal handling. Existing
path admission entries are unchanged. Admission does not authorize a user.

Use a new scoped authorization method with the existing fresh
`PolicyIdentityProvider` bearer validation. Require exactly one `Authorization`
header containing a bearer and exactly one `X-MuxCore-User-Id` selector. Require
the selector to equal the freshly verified user ID exactly: administrators also
may act only on themselves. Require the verified tenant to be the empty string;
a nonempty tenant is unsupported. Do not infer an empty tenant from
`contracts.Session`, map it to `default`, use a browser tenant header or accept
module-only/`x-auth-token` authority. Unsupported or unavailable identity
resolution fails closed. Existing `ValidateScope` validates the user identity
and bounds it to 256 bytes. This API does not establish platform tenancy.

### 2. Authority, revisions and reads

Each successfully opened `Store` owns one cryptographically random authority:
32 bytes encoded as unpadded base64url (43 characters). Generation failure fails
startup. The authority stays stable for that Store's lifetime and changes on
every reopen/restart, including reopening an unchanged database. It is not
persisted, a bearer credential or a durable provider identity.

Only one provider/Store may actively own a database. Restore requires stopping
the provider and reopening the restored database; hot restore is unsupported.
The new authority makes old in-flight preconditions invalid even if the restored
database reuses a revision. A client must preserve unresolved intent across that
refusal; a new authority does not prove whether an earlier request committed.

Revisions are canonical decimal strings in the range `0` through
`9223372036854775807`; reject signs, leading zeros except `"0"`, whitespace,
exponents, fractional forms and JSON numbers. A stored row has a positive
revision. A missing row reads as revision `"0"`, `exists: false`; its `blob` is
an in-memory initial document with default preferences, empty progress/favorite/
want-to-watch maps and empty playlists/queue. GET creates no row and increments
no revision. Absence means only current absence, never proof of never-written
state or permission to import local data.

Every successful read returns the stored raw JSON document without typed/default
normalization, plus contract, authority,
verified `user_id`, empty `tenant_id`, decimal-string revision, `exists`, the
explicit supported-operations list and `legacy_writes: true`. The latter is
always true in this slice. Raw data is not round-tripped through typed `Blob` or
floating-point representations. Preserve unknown members and large raw numeric
values and number lexemes; whitespace and object-member order need not be
byte-identical. Every response is JSON with `Cache-Control: no-store`.

The read envelope has exactly this shape; `blob` below illustrates a stored
document and is not a replacement for existing raw contents:

```json
{
  "contract": "userdata.conditional.v2",
  "authority": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
  "user_id": "alice",
  "tenant_id": "",
  "revision": "7",
  "exists": true,
  "operations": ["prefs", "favorites", "wantToWatch", "playlists", "queue"],
  "legacy_writes": true,
  "blob": {"prefs": {}, "progress": {}, "favorites": {}, "wantToWatch": {}, "playlists": [], "queue": []}
}
```

`operations` is that exact ordered list. Existing documents are read without
adding defaults; the missing-row construction is the sole defaulting case.

### 3. Conditional requests and acknowledgments

PUT requires `contract`, `request_id`, `expected` and `mutation`.
`expected` contains `authority`, `user_id`, `tenant_id` and `revision` from a
confirmed read. Its user/tenant must match the verified scope. `request_id` is
1–128 ASCII characters matching `[A-Za-z0-9._:-]+`; it is only correlation data,
not stored, deduplicated or looked up as a receipt.

```json
{
  "contract": "userdata.conditional.v2",
  "request_id": "prefs-1",
  "expected": {
    "authority": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    "user_id": "alice",
    "tenant_id": "",
    "revision": "7"
  },
  "mutation": {
    "prefs": {"display": {"theme": "light"}},
    "favorites": {"remove": ["movie-1"]},
    "queue": []
  }
}
```

All envelope and expected members are required, including the explicitly empty
`expected.tenant_id`. An absent `expected` differs from a malformed one.

On success, return the full new read envelope with the same `request_id`,
`outcome: "applied"` and `previous_revision`. The new revision is exactly the
expected previous revision plus one, including for valid operations whose
resulting data is unchanged. Validate and encode the bounded success envelope
before the database write. Return success only after the transaction commits.
For the example, the resulting envelope has revision `"8"`, `exists: true`,
the resulting raw `blob`, and the additional members `"outcome": "applied"`,
`"request_id": "prefs-1"`, `"previous_revision": "7"`. GET never includes these
three acknowledgment members.

A consumer can acknowledge only a valid matching PUT response: contract,
request ID, authority, user/tenant, previous revision and exact increment must
all match the dispatched operation. A matching GET is only an observation of
current state. It does not acknowledge a command, identify its committed
revision, produce a receipt or resolve an uncertain previous attempt.

Timeout, connection loss, malformed/mismatched response or ambiguous storage
failure retains uncertainty. The same desired state is not request
deduplication. No automatic rebasing or full-blob replay follows a conflict;
retrying the original precondition can conflict because the first request
committed, and that conflict does not disprove the earlier commit. Durable
pending journals, restart recovery and user conflict resolution remain consumer
work under ADR-0037.

### 4. Presence and five-family mutation semantics

Reject unknown request-envelope, precondition and top-level mutation members.
Raw unknown document members and raw values inside supported values are
preserved within the following replacement boundaries. Omission leaves a
section or preference member unchanged; it never means deletion.

| Mutation member | Meaning |
|---|---|
| `prefs` | Non-null object; recursively merge object members. At a named member, scalar, array or null is literal replacement, not deletion. A supplied object merges an existing object, or replaces an absent/null/scalar/array member with the recursively built object. Nested `{}` is valid member intent; top-level `prefs: {}` alone supplies no intent. |
| `favorites`, `wantToWatch` | Non-null operation object with `set` object and/or `remove` array. Set values are raw entry objects whose `id` exactly equals their map key. A set replaces the entire entry; callers carry unknown members forward for retained content. Remove names explicit keys and is idempotent even if absent. Reject duplicate removals, a key in both set/remove, malformed entries and unknown operation members. Empty set/remove collections alone supply no intent. |
| `playlists`, `queue` | Non-null arrays replacing the complete named array; `[]` explicitly clears it and counts as intent. Clients preserve raw unknown members in items they retain. There is no provider per-item merge rule. |

Require at least one intentional operation overall. Any member of `prefs`
counts, including a same-value replacement or nested `{}`. Combining an empty
prefs/map-operation object with another valid operation is allowed. Repeated
remove and other valid data no-ops still increment the revision once.

Media/map/removal identifiers follow existing `ValidateScope` identity validation
and its 256-byte limit. Validate only the affected known sections through the
existing typed models; want-to-watch entries require the matching string ID but
otherwise remain raw. Do not serialize typed validation copies or coerce their
raw numbers. Unrelated existing sections, including progress, remain
untouched. A missing target section is initialized as needed; a stored null or
wrong-type top-level `prefs`, `favorites` or `wantToWatch` target is refused as
storage unavailable without overwriting it. Whole-array `playlists`/`queue`
replacement may replace any old value, subject to validation of the supplied
array. Typed validation permits literal null wherever the existing decoder
accepts it; those nulls remain stored literals, never deletion markers.

### 5. Strict JSON, storage and size limits

Reject invalid UTF-8, lone UTF-16 surrogate escapes, duplicate decoded object
member names at every nesting level, trailing data, malformed JSON and nesting
deeper than 64 containers. Unknown raw values obey those syntactic rules.
Known sections still undergo their typed validation; unknown numeric tokens are
preserved without passing through `float64`.

Request bodies are at most 4 MiB. The encoded stored blob is at most 8 MiB; the
separately encoded v2 response is at most 8 MiB plus 16 KiB. Use a shared JSON
encoder without HTML escaping amplification, preserving raw numeric values.
Check the exact encoded blob and success reply before writing. The v2 response
cap is separate from existing client caps; no legacy size constant is widened.

Acquire the Store mutex and a database connection, then `BEGIN IMMEDIATE` with
a bounded request context and busy timeout. Read current raw data and revision,
check the existing erasure write guards, compare authority/scope/revision,
validate/apply the operation and perform a conditional insert/update in that
same transaction. Encode and check limits before persistence, commit once and
increment once. Conflict/refusal changes neither data nor revision. Refuse
revision exhaustion before overflow. No DDL is required.

Use the existing erasure query body unchanged; only its parameter type may be
generalized to the query interface needed by the connection transaction. The
new write must obey both last-seen and durable erasure refusal. This is not
authorization to investigate or alter blocked admin/E7 paths.

### 6. Refusals and uncertainty

Errors use a bounded no-store JSON `code` response and never return a successful
document. Schema/precondition/auth/admission/size/conflict/erasure refusals are
known not applied for this request. Identity unavailability and revision
exhaustion also happen before mutation. Storage-unavailable errors are
conservatively uncertain, even if a particular failure may have occurred before
commit. Transport or unreadable responses cannot manufacture known refusal.

| HTTP status | Exact `code` and meaning |
|---|---|
| 400 | `userdata.protocol_unsupported` for an unsupported contract; `userdata.mutation_unsupported` for an unsupported mutation family; `userdata.invalid_body` for invalid request JSON/schema/intent; `userdata.invalid_precondition` for a malformed precondition; `userdata.invalid_query` for any query string, including a trailing bare `?`, on GET or PUT. |
| 428 | `userdata.precondition_required` when `expected` is absent. |
| 401 | `userdata.unauthenticated` for missing/invalid bearer credentials. |
| 403 | `userdata.forbidden` for forbidden user/selector access; `userdata.scope_unsupported` for a verified nonempty tenant. Secure transport admission retains ADR-0033's separate `userdata.module_forbidden`. |
| 405 | `userdata.method_not_allowed`, with `Allow: GET, PUT`, at the handler in dev/non-admission use. Secure admission refuses unsupported methods before user auth with its existing 403. |
| 409 | `userdata.authority_conflict` or `userdata.revision_conflict`; no current revision, blob or acknowledgment is returned. |
| 410 | Existing `userdata.account_erased` for erasure write refusal. |
| 413 | `userdata.too_large` for request/blob/response limits. |
| 503 | `userdata.identity_unavailable` for unavailable/unsupported current identity; `userdata.storage_unavailable` for unavailable/malformed storage or ambiguous storage failure; `userdata.revision_exhausted` before revision overflow. |

For example, a revision refusal is exactly
`{"code":"userdata.revision_conflict"}`. No success/acknowledgment fields or
provider details accompany errors. A new known refusal cannot erase the
uncertainty of an earlier dispatch.

### 7. Compatibility and implementation boundary

All old HTTP/gRPC writers, public fallback and migration behavior remain
unchanged. They can interfere with data written through v2. `legacy_writes:
true` must not be interpreted as an enforcement or durable-removal capability.
The API does not import local data, migrate an account, fix BFF session/blob
tenant agreement or provide progress/watched semantics.

The first provider slice adds only the API, its public `conditional` contract/
parser, scoped
identity method, new fixed client operations/admission and tests. Manifest
changes advertise the new capability only; changelog text is Unreleased. No
version/dependency/protobuf changes, legacy merge rewrite, public fallback,
BFF/SPA/admin changes, gitlinks or deployment belong to it. A published producer
tag and subsequent separately reviewed consumer contract are required before
dependent integration under ADR-0011. No unpublished pin or filesystem replace
substitutes for that release.

The published checked HTTP client adds fixed operations `GetUserdataV2` and
`PutUserdataV2` through its existing `Do` boundary, with the separate v2 response
cap. It does not activate any consumer. The SPA's shared writer cannot safely
switch selected families while continuing stale legacy progress/watched blob
pushes; activation awaits every shared write family's migration and separately
reviewed BFF/provider/client semantics.

## Alternatives and consequences

- Extending unconditional PUT cannot distinguish explicit removal from omitted
  stale data or reject concurrent intent.
- Accepting all ADR-0037 decisions now would require progress event ordering,
  client pending journals, migration and complete writer enforcement. The
  bounded provider operation can be decided independently.
- A persisted authority or receipt journal would support a different continuity
  model, but requires additional schema, retention and restore decisions. This
  first operation instead rotates authority on open and preserves uncertainty.
- A coarse row revision avoids per-section schema changes but allows unrelated
  legacy/progress writes to conflict with preferences. No automatic replay hides
  that tradeoff.

## Verification and remaining gates

The registered provider slice must verify real HTTP and mTLS admission, fresh
self/household identity and refusals; exact strict parsing and size boundaries;
every supported mutation family, mixed unsupported requests and raw unknown
values; both orderings of concurrent writes, no-change conflicts, no-op
increments and exhaustion; uncertain acknowledgments; erasure overlap; and
reopen/restore authority rotation with durable data. Existing legacy behavior
must remain covered. Pure package checks are useful but cannot substitute for
the full provider race/build/vet/lint and hosted gates when local SQLite source
is unavailable under B-3.

Provider-only verification does not establish BFF forwarding, no-fallback
acknowledgment, client journals, every-writer migration, Apple runtime, restart/
second-device persistence or household rollout. FR-PLAY-004 stays Part. ADR-0037
and those integration gates remain open; E7/B-8, T-M3-10, publication limits,
deployment and release train remain unchanged.
