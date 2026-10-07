# ADR-0026: Administrative session management belongs to the auth provider

- **Status:** Accepted
- **Date:** 2026-10-07
- **Decided by:** Delegated

## Context

T-M5-11 / FR-AUTH-007 requires administrators to list and revoke all active user
sessions. Admin-ui currently lists its own app sessions, while auth-local owns
the bearer sessions used by multiple clients. Reading another module's database
would violate ADR-0009. The existing bearer-based Revoke RPC is not an appropriate
administrative listing contract because listing must not disclose credentials.

## Decision

1. Add `AuthService.ListSessions` and `AuthService.RevokeSession` in core's auth
   proto. The auth provider remains the data owner and enforcement point. Existing
   Validate/Revoke methods retain their wire contracts. Providers that have not
   implemented the new methods return `Unimplemented`.
2. Both administrative methods require a current, fully authenticated end-user
   session in `x-auth-token`, and the provider must recheck that user's current
   admin role. A verified module certificate alone is insufficient. Missing,
   expired or partial authentication returns `Unauthenticated`; a valid non-admin
   principal returns `PermissionDenied`.
3. Session management IDs are independently generated opaque values. Neither IDs
   nor pagination tokens expose a bearer, its hash, or a value accepted by the
   authentication methods. Lists contain only active, fully authenticated `full`
   and `api-token` sessions and may filter by user.
4. List pagination defaults to 100 entries and permits at most 500. Opaque tokens
   are bound to the user filter; ordering is creation time then session ID.
   Pagination is exclusive and evaluated against current state, not a snapshot,
   including when the last item of the preceding page is revoked or expires.
5. Revoke requires both user ID and session ID and atomically matches that exact
   pair. An unknown or previously revoked pair succeeds without affecting another
   user's session. Revocation invalidates the bearer at its next provider
   validation; it does not revoke an API key that originally minted the session.
   Apps with local cookies must revalidate the upstream bearer to enforce it.
6. Deliver the contract and compatibility fixtures first, then the provider
   persistence/RPCs, then admin-ui and BFF/session consumers, with published
   dependency versions in that order (ADR-0011). The initial contract checkpoint
   alone does not implement FR-AUTH-007.

## Alternatives

- **Aggregate local app-session files:** misses native and other client sessions
  and breaks auth-provider ownership.
- **Use bearer tokens or hashes as list/revoke identifiers:** unnecessarily
  exposes credential material and couples UI records to authentication secrets.
- **Authorize with the BFF's module certificate only:** confuses trusted service
  identity with the current end user's right to administer sessions.

## Consequences

Compatibility tests cover rebuilt providers embedding the unimplemented server
and providers running the previous service descriptor. Provider authorization,
persistence, pagination, scoped revoke and consumer revalidation require separate
implementation tests before the requirement can be marked implemented. OIDC and
other providers may remain unsupported and consumers must handle that explicitly.
