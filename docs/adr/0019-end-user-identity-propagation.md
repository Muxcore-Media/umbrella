# ADR-0019: End-user identity propagation

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated
- **Amends:** consequences of [ADR-0010](0010-bff-direct-module-access.md)

## Context

request-media takes the user from `X-Caller-Id` / `X-MuxCore-User`, and the BFF copies client headers through, including `X-Caller-Id`. `core/pkg/tenant` trusts `X-Tenant-ID`, and the Jellyfin bridge trusts `X-User-ID` (NFR-SEC-007). userdata-local already validates a forwarded bearer token through auth-local.

## Decision

1. **The BFF and admin-ui** strip client `X-Caller-Id`, `X-MuxCore-*`, `X-Tenant-ID`, `X-Auth-Claims-Tenant`, `Cookie` and `Authorization` headers. They forward `Authorization: Bearer <auth-local token>` for the signed-in user. For one release they also keep sending `X-Caller-Id=<user id>` for compatibility.
2. **Modules resolve the bearer token** through the identity provider (`ExtractIdentity`), caching the result for at most 30 s, keyed by the token's SHA-256. They never trust identity or tenant headers. The one exception is a legacy path, allowed only when `MUXCORE_INSECURE_DISABLE_TLS=true` and a module-specific opt-in (for example `REQUEST_TRUST_CALLER_HEADER=1`) are both set.
3. **`core/pkg/tenant`** honours headers only with `TENANT_TRUST_HEADERS=1`, and gains `WithAuthenticatedTenant`.

## Alternatives

- **Trust headers from allow-listed peers:** needs an mTLS HTTP server in every module and still trusts the BFF completely.
- **Signed assertions:** need key distribution to every module.

Either may be added later as defence in depth.

## Consequences

Spoofed identity headers have no effect. Modules depend on the identity provider for each new token, which the cache amortises.
