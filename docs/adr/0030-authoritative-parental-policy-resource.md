# ADR-0030: Parental policy has a separate authenticated provider resource

- **Status:** Accepted
- **Date:** 2026-10-07
- **Decided by:** Delegated

## Context

FR-PLAY-007 requires restrictions for a principal to hold across every list,
search, detail and playback API. The current BFF accepts request-supplied
classification and reads parental preferences from self-editable userdata.
Provider-read failure can fall back to empty local data. Extending those checks
to more routes would preserve the authority problem (roadmap C-28).

`userdata-local` already owns persistent per-user data, while auth owns current
identity, roles and tenant membership. A protected policy resource can use those
owners without changing ordinary progress/preferences or introducing a second
identity source. This decision is made under the user's standing project
decision delegation; it is not a claim of implementation or enforcement.

## Decision

1. Add an independent typed HTTP resource, `GET`/`PUT /api/parental-policy`, in
   `userdata-local`, advertised as `userdata.parental-policy.v1`. No new gRPC or
   core auth contract is required. Policy-specific identity resolution consumes
   the existing `ValidateResponse.TenantId` and `UserInfo.TenantId`; the generic
   auth session type does not acquire a guessed tenant field.
2. Every request validates a current `Authorization: Bearer` through auth.
   A module certificate, caller-supplied identity/tenant headers, or an
   `x-auth-token` fallback does not authorize this HTTP resource. Auth failure or
   unavailability cannot turn into an unrestricted policy or a legacy-data read.
3. `X-MuxCore-User-Id` selects the requested target; it does not establish caller
   identity. A current user may read their own policy; a current admin may read
   or write another user's policy only in the same verified tenant. Writes
   require the current admin bearer even when the target is the caller. Resolve
   another target's existence and current tenant through existing
   `AuthService.ListUsers`; reject cross-tenant access. Tenant scope always comes
   from verified identity, not the target selector or a browser tenant header.
   An empty verified tenant means the single-household scope; it is not rebound
   to a client-supplied default.
4. Store `parental_policies` separately from `user_blobs`, keyed by
   `(tenant_id, user_id)`. Preserve existing blob/progress API semantics and do
   not automatically promote any parental-looking fields from those blobs.
   A missing record returns `state: "unconfigured"`, `revision: 0`,
   `policy: null`. A stored record returns `state: "configured"`, a positive
   revision, typed policy and `updated_at`. Absence and an explicitly unrestricted
   policy are distinct states. Responses also include the verified `user_id`
   and `tenant_id` so a consumer can check scope. A consumer must not infer
   unrestricted access from absence, unsupported capability, failed lookup or
   invalid data.
5. PUT accepts `{expected_revision, policy}` and atomically compares/increments
   the revision; stale writers receive HTTP 409. The policy has `version: 1`,
   explicit `mode: "restricted" | "unrestricted"` and a strict rules schema.
   `expected_revision` is required (zero creates a record); restricted rules are
   a complete replacement, and unrestricted mode requires `rules: null`.
   There is no DELETE route: disabling restriction is a deliberate,
   revision-checked write of explicit unrestricted mode.
6. Restricted rules cover `kids_mode`, a supported `max_rating` token, exact
   normalized `blocked_tags`/`allowed_tags`, and `allow_unrated`. Blocked tags win.
   A nonempty `allowed_tags` set requires at least one exact normalized match;
   that match cannot override rating or unrated restrictions. Kids
   mode defaults to PG when no explicit threshold is configured. Unknown or
   malformed classifications never imply unrestricted access. The provider
   validates/stores this contract; later consumers must enforce these semantics
   against trusted media metadata, not caller-supplied ratings or tags.
7. The strict policy schema has no PIN or PIN-hash fields. Profile selection,
   PIN-unlock grants and their migration remain separate work. Later policy
   migration may read only the protected admin-ui `parental.json` and must use
   authenticated, revision-checked provider writes. No legacy self-editable blob
   or local fallback file becomes an authority by being copied into this store.
8. This first producer is unused by the existing BFF/admin flows. Follow-up
   consumer work must migrate the trusted administrative source, obtain current
   provider policy, derive media classification from authoritative sources and
   define failure behavior before enforcing every route. FR-PLAY-007 remains
   partial until that end-to-end boundary and its negative cases are verified.
9. All responses use `Cache-Control: no-store`. Distinguish missing/invalid
   credentials (401), forbidden role/user/tenant access (403), an absent admin
   target (404), revision conflict (409), invalid schema/body/query (400), a body
   larger than 32 KiB (413), and unavailable auth/storage (503). Auth gRPC
   `Unauthenticated`/`PermissionDenied` map to 401/403; transient, unsupported or
   internal auth errors map to 503 without exposing provider details. An error
   response cannot masquerade as unconfigured or unrestricted success.

## Alternatives

- **Keep policy inside the writable userdata blob:** allows the restricted
  principal to change the authority the server relies on.
- **Copy all existing preference blobs automatically:** upgrades untrusted input
  into privileged policy and loses the distinction between missing and explicitly
  unrestricted configuration.
- **Add BFF middleware before defining authority:** repeats the current trust
  gaps across more endpoints without establishing enforcement.
- **Use a local UI file as the permanent multi-consumer authority:** duplicates
  ownership and cannot give BFF/native consumers a shared typed provider boundary.
- **Add a core session/tenant contract only for this resource:** unnecessary;
  existing auth responses carry verified tenant and current user information.

## Consequences and verification

The producer needs strict-schema and normalization fixtures, fresh-bearer
authorization tests, same-tenant self/admin access tests, target-existence checks,
atomic concurrent/stale-write tests, persistence and absence/unrestricted tests,
and regression checks that ordinary userdata/progress are unchanged. Tests must
prove legacy blobs, identity headers and unavailable auth cannot authorize a
policy write or manufacture an unrestricted policy.

Provider tests alone do not establish parental protection. Trusted migration,
consumer route coverage, metadata provenance, failure behavior and profile/PIN
grant semantics retain their own implementation and acceptance work.
The tenant checks in this decision apply only to this resource; they do not
establish platform-wide multi-tenant isolation or change NG-1.
