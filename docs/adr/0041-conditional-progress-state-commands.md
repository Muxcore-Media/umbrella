# ADR-0041: Conditional progress and state commands

- **Status:** Accepted
- **Date:** 2026-10-12
- **Decided by:** Delegated under standing project authority; root accepted the bounded provider contract on 2026-10-12 after provider, BFF, SPA and independent contract review and root re-read. Not owner-approved.
- **Tracks:** T-M4-09, FR-PLAY-004; requirement remains Part.
- **Related:** [ADR-0011](0011-go-workspace-and-published-versions.md), [ADR-0015](0015-upgrade-compatibility-window.md), [ADR-0033](0033-userdata-http-mesh-transport.md), [Proposed ADR-0037](0037-conditional-userdata-mutations.md), [ADR-0038](0038-additive-household-userdata-mutations.md).

This bounded decision accepts the separate all-family provider contract. It
does not amend accepted ADR-0038, establish source acceptance, publish a producer
or activate consumers. Root re-read the exact Proposed contract already cleared
by provider, BFF, SPA and independent reviewers before accepting it under standing
delegation. The exact provider file/test scope is registered in the roadmap;
implementation starts only after root freezes the accepted documentation and
source registration in an immutable local commit and explicitly releases the
provider owner. Remote documentation publication is not an additional
implementation prerequisite. Consumer cutover choices below remain unresolved.

## Evidence and boundary

Read-only reviews use userdata-local `b0a3abe2`, `_mvp` `5cf783ed` and
media-ui-app `2693058d` (tree-identical to merged `6674c28f`). No runtime tests
or source edits were performed for the contract review.

| Module/source anchor | Consequence for the provider contract |
|---|---|
| Provider `conditional/contract.go:241,251–254`; `contract_test.go:129` | The v2 parser requires the exact five-operation list and tests rejection of added progress. Extending that list in place breaks compatibility. |
| Provider `internal/store/merge.go:16–43,92–99`; `store.go:159–162,201–214` | Legacy progress compares timestamp strings, then Put overwrites the merge winner. Completion inference and ContinueWatching filtering differ; MarkWatched has separate read/write steps. These paths cannot implement the new commands. |
| SPA `src/lib/userdata.ts:491–515,737–871,900–985`; `useProgressReporting.ts:78–104` | Every push sends six cached sections, including progress every four seconds. Omitted removals and stale sibling values can reappear through unrelated writes; all seven semantic families share this writer. |
| SPA `Settings.tsx:403–416`; `VideoPlayer.tsx:402,424,479,484`; `userdata.ts:257–264,352–368,916–985` | Displayed defaults and cached siblings are not dirty intent. Typed JSON round trips lose raw numeric precision; cache keys are unscoped and pending state is not a durable account journal. |
| BFF `cmd/mediauiprox/userdata.go:150–214,295–304,407–428` | Legacy GET can import, writes can fall back or mirror, and typed/generic decoding loses unknown data or number lexemes. A conditional bridge must bypass these helpers. |
| BFF `userdata_migrate.go:96–125,179–212`; `userdata_migration_test.go:258–304` | Markers do not record later fallback dirtiness. Existing tests explicitly replay an uncertain import after a newer local write; a final marker is not proof of a clean cutover. |

An additive provider can be reviewed independently of migration. While legacy
writers remain enabled, they can overwrite conditional results or restore
removed entries. Neither v3 support nor a successful conditional request proves
durable removal, event chronology or cross-device completion.

## Provider decision

### 1. Separate version, unchanged v2

Add `userdata.conditional.v3`, `GET`/`PUT /api/userdata/v3`, with the exact ordered
operations `["prefs", "favorites", "wantToWatch", "playlists", "queue", "progress"]`.
Explicit watched commands belong
to `progress`; they are not a seventh wire operation. Preserve the v2 endpoint,
parser, envelope and exact five-operation list unchanged, including rejection
of progress. Do not infer support from a manifest alone or fall back to legacy
writes when v3 is unavailable.

For v3, reuse ADR-0038's five-family mutation semantics, strict envelope/raw JSON
rules, size limits, no-store responses, fresh self-only bearer/selector binding,
empty verified tenant, authority and decimal-string revisions, request-ID
correlation, atomic CAS and refusal codes. Retain `legacy_writes: true` exactly;
this is no enforcement capability. Use the same row and Store-lifetime
authority, with no DDL. Admit only verified `media-ui` GET/PUT on this new fixed
path under ADR-0033. Add distinct checked-client operations and response parsing
for v3; existing client operations and path admission stay unchanged. There is
no new gRPC path, profile scope or administrative authority.

The read envelope and PUT/ACK fields are those of ADR-0038 with the v3 contract
and operation list. A mutation may atomically combine the five existing families
with the following `progress` command:

```json
{
  "progress": {
    "report": {
      "movie-1": {
        "positionSec": 12,
        "durationSec": 100,
        "metadata": {"kind": "movie", "title": "Title", "href": "/movies/movie-1"},
        "observedAt": "2026-10-12T10:00:00+01:00"
      }
    },
    "set_watched": {"movie-1": false},
    "remove": ["movie-2"]
  }
}
```

This is the `mutation` value, not a complete request. The request still requires
the exact `contract`, `request_id` and original `expected` scope/authority/revision.

### 2. Progress intent and raw preservation

| Command | Meaning |
|---|---|
| `report` | Map of media IDs to non-null report objects with exactly required `positionSec` and `durationSec` plus optional `metadata` and `observedAt`. Position/duration obey the numeric rule below. Duration zero means unknown; position greater than duration is allowed. Metadata is a non-null raw object; observedAt is a timestamp string under §3. A report neither clamps position nor infers watched state at 92% or another threshold. |
| `set_watched` | Map of media IDs to exact booleans. This is explicit watched/unwatched intent, separate from a playback observation. |
| `remove` | Array of explicit media IDs to remove, including absent or malformed entries in a valid progress map. Reject duplicate removals or overlap with either other command. Omission never removes an entry. |

`progress` must be a non-null object with only these three optional members;
present report/set_watched must be non-null objects and remove a non-null array.
Malformed commands, aliases or unknown command members yield
`userdata.invalid_body`; `userdata.mutation_unsupported` is reserved for an
unsupported top-level family. Empty collections add no intent; progress containing
no nonempty suboperation, combined with another valid family, does not read,
validate or initialize stored progress. At least one intentional operation overall is required. Nonempty
remove against a missing map creates `progress: {}` and increments once; a
null/nonobject stored map refuses as `userdata.storage_unavailable`.

Position/duration are strict JSON numbers with mathematical value at least zero.
Float64 parsing must succeed with a finite result; reject a mathematically
nonzero value that rounds/underflows to zero, including negative tiny values.
Signed mathematical zero, ordinary decimals such as `0.1` and finite subnormals
are allowed. Exact binary representability is not required. Preserve validated
incoming numeric lexemes; only final watched=true deliberately replaces position
with canonical `0`. Unknown metadata numbers retain ADR-0038's arbitrary raw
semantics. No maximum beyond this numeric rule is introduced.

Report and set_watched may target the same ID in one transaction. Apply reports,
then explicit watched values; for every affected retained entry, final
`watched: true` forces `positionSec` to zero. False preserves the reported/current
position. Rewind is an explicit report of position zero plus `set_watched: false`.
A client completion rule can deliberately send report plus `set_watched: true`;
the provider does not invent that intent. Preserving or changing the current
client completion/rewatch policy is a separate migration decision. An explicit
unwatched entry near the end may still be hidden by the unchanged legacy
ContinueWatching ratio rule.

Metadata assigns named entry members shallowly; omission preserves an existing
member, including unknown raw values and numeric lexemes. Reserve `id`,
`positionSec`, `durationSec`, `watched`, `updatedAt` and `observedAt`, including
aliases, from metadata. Known metadata `kind`, `title`, `href`,
`poster_url` and `stream_url` must use canonical spelling and string values;
their aliases are also invalid. Alias equivalence is exactly Go
`strings.EqualFold` (Unicode simple case folding), with no trimming or underscore
rewriting; only exact canonical spelling is accepted for known members. Truly
unknown members remain raw; do not serialize a typed validation copy.

IDs obey the existing `ValidID`/256-byte bounds. New entries start only with
`id` equal to the key, position zero and duration zero, plus supplied intent;
report-only creation omits watched and inherits no sibling data. Nonempty report
or watched intent initializes a missing map/entry. An existing target must be a
non-null object without any noncanonical alias of the eleven known fields above.
Existing id, if present, must be a non-null string equal to the map key; fill a
missing id from that key. Aliases, wrong-type entries and ID mismatch refuse
before overlays as `userdata.storage_unavailable`, even if a command would
otherwise replace them.

Apply overlays, then validate the resulting retained known values without typed
serialization: position/duration, if present, obey the numeric rule; missing
ones on existing entries stay missing. Watched may be absent, null (unset) or
boolean. Present known metadata fields must be non-null strings. Retained
observedAt must satisfy §3, but watched-only intent preserves its valid original
raw string. Old updatedAt is always replaced and need not have been valid.
Explicit replacements can repair malformed canonical values, such as a report
replacing bad duration or removing bad observedAt; final watched=true replaces
bad position with zero. Any remaining malformed retained value refuses as
storage unavailable with no changes. Remove bypasses per-entry validation;
unrelated entries stay raw. Valid no-ops increment once.

### 3. CAS order and timestamps

Successful compare-and-swap commit order defines state order. A client with an
older observation and a current precondition can replace a newer observation;
this API does not reconstruct physical event chronology or automatically
reconcile stale events. Concurrent commands with one expected revision cannot
both succeed. No conflict triggers automatic rebase of progress, watched intent
or any other family.

Sample one provider receipt timestamp after precondition/command checks and store
it as `updatedAt` on affected retained entries, canonical UTC with exactly nine
fractional digits. It is informational wall-clock metadata: it may repeat or
regress and never selects a winner. Optional observedAt accepts only ASCII
`YYYY-MM-DDTHH:MM:SS`, an optional `.fraction`, then `Z`, `+HH:MM` or `-HH:MM`:
four-digit Gregorian year
0000–9999, valid date, hours 00–23, minutes/seconds 00–59 (no leap seconds),
optional dot plus 1–9 fractional digits, and offset hours 00–23/minutes 00–59.
Reject lowercase separators, space/comma, excessive precision, null, empty and
invalid values. Normalize supplied values to UTC with exactly nine fractional
digits and require the resulting UTC year to stay 0000–9999; never truncate
extra precision silently.
Equivalent offsets normalize equally; valid old, equal or future observations
are admitted only by matching CAS, with no clock/skew winner or cutoff. A report
without observedAt removes the previous report's observation timestamp;
watched-only intent preserves it. Untouched legacy data and legacy timestamp
sorting are unchanged.

Use one raw transaction for scope/erasure/precondition validation, application,
encoding/size checks and persistence; do not invoke legacy ParseBlob, MergeBlob,
Put or MarkWatched. Mixed invalid commands, conflict or erasure refusal change
nothing. Every valid mutation commits one revision increment, including no-ops;
success is returned only after commit. Revision exhaustion refuses before write.

### 4. Acknowledgment and uncertainty

Only a validated PUT acknowledgment matching the immutable dispatched contract,
request ID, scope, authority, previous revision and exact increment confirms
intent. A matching GET is current observation, not a receipt. Retrying the
original precondition may conflict after an earlier commit; that refusal cannot
resolve the earlier uncertainty. Request IDs do not deduplicate or persist.

Retain ADR-0038's known-refusal/uncertainty distinction. Storage unavailability,
transport loss and malformed/mismatched ACKs remain uncertain. Among checked
client `Unavailable` reasons, only `ReasonModuleForbidden` independently proves
transport admission refused application. Provider restart rotates authority;
do not replace an old expected authority or discard the unresolved command.

## Consumer and cutover decisions still required

| Boundary | Required future decision and verification before activation |
|---|---|
| BFF raw bridge | Use a published fixed v3 client with size-bounded strict parsing and original validated bytes, fresh verified bearer/user/empty tenant and request snapshot. Refuse TENANT_MODE=1, nonempty tenant, bearerless Quick Connect/anonymous callers, selectors and every query including bare `?`. Never use legacy scope/default-tenant helpers, fallback, mirror, migration or Jellyfin push. Pure conditional GET must have no local/provider/marker write side effects. Separate live capability, BFF-only refusal codes and enforcement state remain to be designed; do not append fields to the strict provider envelope. |
| Local artifacts and imports | A final marker, absent provider row or revision zero proves no clean history. Recommended conservative initial cohort requires positively absent local blob **and** marker across all relevant legacy storage layouts after legacy paths are frozen, with exact ownership/path checks; this still is not a new-account or automatic-import claim. Prior TENANT_MODE changes must not hide unpartitioned or tenant/default artifacts. Any existing, attempted, unknown, unreadable, corrupt or ambiguous artifact needs explicit disposition. Strict raw inspection must replace helpers that hide corruption as empty or accept arbitrary marker states. Preserve originals, refuse uncertain replay, and coordinate concurrent writers/erasure. Exact migration/cutover protocol is unresolved. |
| SPA intent and readers | Switch the shared reader/writer and all seven semantic families together: progress, explicit watched, preferences, favorites, wanted, playlists and queue. Legacy progress pushes can restore removed siblings. Capture fixed set/remove intent before cache mutation; require sparse dirty-field commands or an explicitly chosen whole-form conflict policy. Route every legacy GET caller and exported legacy API helper through the new coordinator; never reconstruct requests from presentation defaults/full typed caches. |
| Raw data and pending journal | Preserve raw confirmed values and unsafe numeric lexemes independently from typed presentation, or separately decide provider patch semantics that avoid resending them. Durably retain queued/unsent intent separately from the immutable dispatched envelope, including edits queued behind uncertain work. Persist each exact dispatch/request ID/original precondition before sending; account/server/verified-tenant scope and current epoch must guard every asynchronous completion. Keep unresolved old-authority commands visible and quarantined across restart/restore. Cross-tab atomic journal coordination, conflict UI and recovery policy remain undecided; authority alone is not a journal partition. |
| Every writer and rollback | Inventory/refuse or migrate old SPA tabs, native clients, BFF GET import/PUT/POST/fallback/mirror, direct provider HTTP/gRPC, admin/PIN and operational scripts; optional background sync remains unsupported. BFF-only refusal cannot enforce direct writers. Persistent cutover must survive old-process restart, and rollback must preserve pending intent/artifacts without silently reopening unconditional writes. Native monitor telemetry is separate and cannot acknowledge userdata. |

Evidence for artifact refusal includes public `store/store.go:65–77` hiding read
failure, BFF `userdata_migrate.go:96–115` hiding marker failures, and
`erasure_stores.go:433–466` checking old storage layouts and stored user identity
because sanitized paths can collide. Existing final markers do not record later fallback writes. No
future gate may interpret these helpers' empty results as positive absence.

## Alternatives, sequencing and verification

- Extending v2 in place fails existing strict consumers; keep both versions.
- Client-clock winner selection cannot establish event order and preserves the
  current timestamp ambiguity. CAS makes state transitions explicit but leaves
  conflicts for deliberate resolution; unrelated writes still contend on one row.
- A pure raw bridge or five-family SPA cutover cannot prevent legacy progress
  replay. Provider-only publication is a separate milestone from safe activation.
- Durable receipts or per-entry/event ordering could reduce uncertainty but need
  separate schema/retention/restore decisions; this decision adds neither.

The roadmap registers exact provider source scope and tests before
implementation. Root must commit the accepted decision and registration and
explicitly release that scope before source edits. Require strict parser, raw/numeric/size/alias,
mixed command/no-op, timestamp, concurrency, erasure, real HTTP/mTLS/admission,
cross-version and restart/restore/lost-ACK fixtures plus actual full provider
build/vet/race/lint gates. Keep existing upgrade suites and unchanged DDL; local
SQLite B-3 remains a blocker, not permission for dependency/CI workarounds.

After provider review and a genuine published producer/client tag, separately
decide/register the BFF, all-family client journal and writer cutover. Verify
zero fallback/migration/mirror side effects, strict scope/ACK failures, every
marker/artifact state, uncertain imports, raw numeric preservation, dirty/edit-
revert intent, old-writer refusal and rollback. Interleave every family with
progress and explicit watched, reorder responses across new edits/navigation/
account changes and asynchronous parsing, and cover reload/offline/storage
failure/two-tab journal coordination while preserving existing focus controls.
Finally require real provider/
BFF/client restart, offline/concurrent devices and native runtime evidence.
Current provider manifest `0.1.8` is untagged. No consumer source, tag, dependency,
gitlink or deployment is released here; FR-PLAY-004 stays Part, and ADR-0037's
remaining integration decisions, E7/B-8 and T-M3-10 boundaries stay unchanged.
