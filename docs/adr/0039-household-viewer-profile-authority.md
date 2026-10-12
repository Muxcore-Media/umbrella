# ADR-0039: Household viewer profile authority

- **Status:** Proposed
- **Date:** 2026-10-12
- **Decided by:** Delegated proposal under standing project authority; not accepted or owner-approved.
- **Tracks:** T-M4-02, FR-AUTH-010; task remains Todo and requirement Plan.
- **Related:** [ADR-0019](0019-end-user-identity-propagation.md), [ADR-0026](0026-administrative-session-contract.md), [ADR-0030](0030-authoritative-parental-policy-resource.md), [ADR-0031](0031-bff-enforcement-of-parental-policy.md), [ADR-0033](0033-userdata-http-mesh-transport.md), [ADR-0035](0035-user-erasure-ledger.md), [ADR-0038](0038-additive-household-userdata-mutations.md).

This proposal defines the authority boundary to review before profile/PIN work.
It authorizes no source change, modifies no accepted ADR and establishes no
implementation, release or household acceptance. The roadmap records the exact
documentation scope and the separate decision/implementation gates.

## Context and source evidence

Read-only reviews cover the selected draft sources, not a deployed stack.
An account role, a selected profile label and an authorizing credential are
different things. Adding a profile ID to an existing account bearer cannot
remove the authority of that bearer at another route or origin.

| Source anchor | Consequence for the proposal |
|---|---|
| `auth-local` `9f9ec32`, `internal/store/session_identity.go:17–41`, `internal/server/server.go:267–319`; core auth proto `Validate`/`Can` | Validation returns current account identity/roles; Can reloads roles by user ID. The existing contract has no viewer kind, profile or selection generation. |
| `auth-local` `9f9ec32`, `internal/webapp/login.go:180–204,227–267`, `internal/store/store.go:393–411`, `internal/webapp/ratelimit.go:13–32,71–85` | Login codes are short-lived, one-use, in-memory exchanges, not an auth-origin session cookie; API keys can mint new account sessions. Existing web attempt limits are in-memory and cannot supply a durable PIN budget. |
| `_mvp` `5cf783ed`, `cmd/mediauiprox/session_store.go:59–74`, `session_revalidate.go:88–145`, `session_auth.go:54–79` | Bearer-backed BFF sessions revalidate account identity and refresh roles used by operator gates. Bearerless Quick Connect/legacy sessions skip provider validation. Local role stripping or a profile annotation cannot restrict a retained provider credential or retire that independent local authority. |
| `_mvp` `5cf783ed`, `cmd/mediauiprox/parental_routes.go:223–232`, `totp.go:25–34`, `invites.go:153–170` | Current parental exemptions include account-self/MFA routes, which can forward the account bearer. Profile support needs a separate exhaustive credential-kind route policy. |
| `_mvp` `5cf783ed`, `cmd/mediauiprox/parental_gate.go:172–176,226–233,298–299` | Policy identity and cache scope currently use account bearer/user/tenant. A profile label cannot select or isolate policy. |
| `_mvp` `5cf783ed`, `cmd/mediauiprox/userdata.go:97–126`, `playback_session.go:74–75`, `session_auth.go:9`, `http_hardening.go:254–273` | Userdata/telemetry scope lacks profiles; a cookie takes precedence over native bearer input. Logout retires only presented local tokens, not provider authority. |
| `userdata-local` `b0a3abe2`, `internal/store/store.go:67`, `parental.go:17`, `erasure_recovery_test.go:91–103`, `erasure.go:33,74,160–197` | Blob keys are account IDs; policy keys are tenant/account. The fixture treats `victim~p~child` as a literal independent account and preserves it when `victim` is erased. Synthetic keys cannot prove profile ownership or participate correctly in account erasure. |
| `userdata-local` `b0a3abe2`, `internal/auth/conditional.go:41–51`, `internal/store/conditional.go:65,110–123` | ADR-0038's v2 guard requires exact account-self access and empty tenant; preconditions bind account/authority/revision, not profile. |

The shipped cosmetic PIN flow (FR-PLAY-012) verifies a self-editable preference
locally. It grants no backend authority and is not a migration source for an
authoritative PIN, policy or grant. ADR-0030 explicitly excludes PIN fields;
ADR-0038 remains an account-scoped, empty-tenant userdata contract.

## Proposed target

### 1. Account, profile and credential are separate identities

Auth owns a durable profile catalogue, ownership and selection authority; an
existing auth user remains the account. A profile has an opaque, never-reused
ID, one verified owning account and the account's verified tenant. Usernames,
display names, client selectors and caller headers never establish ownership.
Auth must resolve ownership and existence on the server before selecting a
profile or authorizing another module to use its scope.

Account-management credentials and viewer credentials have distinct kinds.
A viewer grant binds account ID, profile ID, tenant, credential kind, expiry
and revocation generation. It cannot acquire the parent's roles or be accepted
as an account token by legacy Validate, Can, ExtractIdentity or self-service
operations. The exact new contract and unsupported-provider behavior require
review before core publication. An old consumer that cannot understand the
kind/scope must reject the viewer credential, never reduce it to account identity.

Viewing permissions come from an explicit viewer permission set and current
profile policy. Even an adult or unrestricted profile remains a viewer, with
no operator or account-security authority. Account management still requires
fresh account authentication. This proposal does not expand ADR-0030's current
admin-only authority to write parental policy; any profile-owner management
permissions need an explicit reviewed permission table.

### 2. Credential custody is a blocking decision

Entering kids use must leave no usable privileged credential accessible from
that browser context, including other tabs, app origins, delayed exchanges and
copied app bearers. The BFF viewer session must contain only the viewer grant;
encrypting or hiding an account bearer in that same session does not remove its
authority. Clearing one cookie, local storage or UI state is insufficient.

| Custody option | Required proof and cost |
|---|---|
| Account-wide privilege retirement on entry | Atomically advance an account privilege epoch and retire existing full/partial, exchange/refresh and API grants before issuing the viewer grant. Other devices must reauthenticate and retired API credentials need replacement. Every late issuance path must check the captured epoch at commit. This is a concrete conservative candidate, not an accepted revocation policy. |
| Browser-family custody | Bind every account credential and challenge/exchange to a provider-managed browser family, retire that family's privilege on entry, and address pre-family legacy tokens. Separate origins and app cookies need a proven binding; current account IDs alone cannot identify that family. |
| Separate browser/device context | Require an independently isolated viewer context with no accessible privileged session. This changes the supported household journey and cannot be assumed merely because origins differ. |

No option is accepted here. The chosen model must enumerate provider, BFF,
admin-ui, native, API-key, login, MFA, exchange and recovery paths, plus the
effect on existing sessions. Bearerless Quick Connect and legacy local sessions
must be explicitly retired, migrated through a reviewed authority transition,
or refused before kids-ready acknowledgment; a provider epoch cannot revoke
credentials the provider does not own. A PIN must never resurrect a retired account
credential. Fresh password/passkey authentication with configured MFA is the
proposed path back to account authority; late work started before retirement
must not mint or restore it. Until this is decided and verified, profile/PIN
consumer activation is blocked.

### 3. Profile data and policy are verified scopes

The target scope is the verified tuple `(tenant_id, account_id, profile_id)`.
`userdata-local` owns separate profile userdata and parental-policy resources;
auth owns the mapping, PIN verifier and grants. Every profile read/write must
validate a current viewer or explicitly authorized management credential and
resolve its exact scope. No account-role shortcut, arbitrary selector, trusted
identity header, legacy self-editable blob or provider-failure fallback may
substitute for that resolution.

A profile-aware versioned interface must keep its response scope, revision and
authority bound to the verified profile. Do not reinterpret ADR-0038's `user_id`
as a profile ID or let an old writer address the new namespace. The precise
HTTP/proto schema, method-admission table and tenant support remain decisions;
ADR-0033's certificate checks and fresh end-user authorization still apply.
Do not concatenate account/profile IDs into legacy keys: existing account IDs
can collide with that syntax, and current erasure guards do not resolve it.
Separate policies retain unconfigured versus explicitly unrestricted state,
revision checks and fail-closed lookup/classification. Missing profiles, policy
or provider support cannot fall back to the account's unrestricted state.
The recommended, unaccepted policy target aliases the verified default profile
to its existing account policy. Each additional profile needs its own explicit
policy, intersected with the account policy: both must permit the operation;
neither can weaken the other. A missing, malformed or unavailable required
policy denies access. Additional-profile storage uses separate account/profile
columns and independent rows, not synthetic legacy keys. Exact schema and
administrative edit permissions remain for decision review.

BFF caches, local mirrors, pending mutations, migration receipts and playback
bindings must include verified profile scope and session generation. SPA/native
caches and pending work must likewise be scoped; an old response cannot populate
the newly selected profile. Inventory progress writers, bridges and legacy
blob/HTTP/gRPC consumers before enabling the new namespace. Transport acceptance
and an additive endpoint alone do not establish profile isolation.

### 4. Default migration, deletion and restore

Propose one stable default-profile mapping per existing live account, created
idempotently from auth-owned IDs. Cover database upgrade, ordinary user creation,
bootstrap and invite redemption, including its direct user insertion path.
Creation and mapping must be atomic, uniqueness enforced, retries safe, and
erased accounts excluded. Never derive the ID from a reusable username or create
a new mapping as a side effect of a viewer read.
The proposed dormant foundation would backfill once; later startup must fail
on a missing live mapping rather than silently regenerate identity. Its exact
schema, restore validation and writable-downgrade limits need a separate bounded
decision and registration; this document does not authorize that foundation.

Mapping does not move data, create policy, issue a grant or enable switching.
A later migration must preserve existing account userdata and trusted policy
for the verified default profile, retain raw unknown data and revision/restore
semantics, and prevent concurrent legacy writes from crossing into another
profile. The recommended, unaccepted default alias resolves the verified
ownership/default mapping and preserves the original raw row, revision and
absence without copying; equality of account/profile IDs is not authority.
Decision review must confirm this recommendation and its mixed-writer behavior
before activation. Missing policy remains unconfigured; new profiles do not
inherit an unrestricted policy or a cosmetic PIN. Legacy-writer retirement,
rollback and supported downgrade remain unresolved. A dormant mapping alone
activates no alias, API or policy.

Account erasure must cover its profiles, PIN material, grants and every profile
data owner under ADR-0035; profile-only deletion needs its own durable consumer
and restore disposition. Restoring an older database cannot resurrect revoked
authority, reused profile IDs or erased data. No new deletion/restore acceptance
is claimed, and existing E7/B-8 restrictions remain outside this proposal.

### 5. PIN, grant and switch lifecycle

Propose a provider-verified PIN for entry to an explicitly protected viewer
profile; it selects viewer authority and never unlocks an individual restricted
item, bypasses policy, or grants account/admin authority. PIN creation/reset
requires authorized account management; no self-editable hash is promoted.
Store only a protected verifier, exclude PINs from logs/responses, and preserve
leading zeros. Length, derivation parameters, recovery authority, attempt limits,
cooldown and expiry values must be fixed by the reviewed contract before code.

Failed attempts need an atomic durable budget bound to verified account/profile,
with a broader abuse limit. Restart, multiple BFFs, changing local session IDs
or simultaneous attempts must not reset or exceed it. An unavailable PIN store
or uncertain check cannot issue a grant. A successful verification must consume
the appropriate challenge once and issue only the intended profile grant.

Selection requires the expected generation, rotates the BFF session/generation
and retires its prior viewer grant atomically at the authority; stale selection
is a conflict. All tabs sharing that cookie must converge, while separate
devices keep separate viewer sessions. Every profile mutation must carry its
expected generation: an old tab's queued write can otherwise acquire the new
cookie and corrupt the newly selected profile. Derive its target from verified
credentials and reject a mismatch, never reinterpret the old payload. Reject
late reads, exchanges, acknowledgments and bindings from the prior generation.
PIN reset, profile removal, account erasure, logout and explicit revocation
invalidate the affected grants. Expiry, provider outage and restart/restore
must fail closed without upgrading authority. Persistent grant state and the
exact invalidation bound still require a decision. A child-ready transition must
cancel/drain tracked prior privileged streams; request revalidation alone does
not terminate an established response. Already delivered/downloaded bytes and
the existing public-image residual are not solved by grant revocation.

## Sequence and review gates

These are proposed future slices, not implementation registrations.

| Phase | Owner and prerequisite | Required evidence before the next phase |
|---|---|---|
| Decision | Root plus independent auth/userdata/BFF review | Resolve custody, permission/kind matrix, PIN limits/lifecycle, default-data migration, restore, unsupported providers and rollout boundaries; record accepted decisions without rewriting accepted bodies. |
| Identity contract and provider | `core` first for any new shared contract, then `auth-local`; separately inventory OIDC | Publish reviewed proto/generated artifacts before consumers; provider migration, every creation path, ownership, deny-by-kind, concurrent PIN budgets, stale issuance/revocation and restore tests. A bounded mapping foundation would need its own decision and registration. |
| Profile data and policy | `userdata-local`, after verified auth scope is available | Publish profile-aware contract/client; cross-profile/account/tenant and caller-spoofing negatives, mixed old/new writers, atomic revision conflicts, default migration, erasure, restart and restore fixtures. |
| Boundary and clients | `_mvp`, then SPA/admin/native consumers; serialize edits per module | Credential-kind route inventory, exact forwarding, no account bearer in viewer persistence, cross-tab/late-result tests, legacy/unsupported refusal, scoped caches/mutations and policy/playback binding tests. Preserve browser CSRF and native credential separation. |
| Coordinated activation | Umbrella after every supported writer/consumer is ready | Document rollback/legacy retirement, run real-provider household/restart/restore and multi-device isolation journeys, independent review and exact required hosted checks. Keep native Apple gates and unavailable checks explicit. |

The adversarial matrix must include a child reusing an old parent cookie or
bearer at BFF, admin and auth origins; selecting another account's profile;
replaying a pre-switch mutation or a PIN challenge; delayed login/key issuance;
concurrent attempts across sessions/restarts; and missing/denied/unavailable
identity or policy providers. Prove old bearerless Quick Connect/legacy local
credentials cannot retain privilege or bypass profile policy after kids-ready
acknowledgment, including persisted-session restart. Existing parental classification behavior and
cosmetic PIN behavior must retain separate regression coverage.

## Alternatives and consequences

A profile field on the account session, local role masking, client-only PIN
checks and forwarding the parent bearer all fail to establish the proposed
authority boundary. Separate accounts preserve today's identity model but do
not deliver FR-AUTH-010's multiple profiles per account. A shared mutable
account blob or unverified profile selector cannot supply independent userdata
and policy authority.

The proposed model adds a new identity kind, migration and coordinated rollout;
it cannot be delivered by the profile switcher alone. Credential custody and
the unsupported-client/restore rules remain blockers. Documentation checks and
read-only review supply no runtime evidence: FR-AUTH-010 remains Plan, T-M4-02
Todo, FR-PLAY-007 Part and NFR-DATA-003 Plan.
