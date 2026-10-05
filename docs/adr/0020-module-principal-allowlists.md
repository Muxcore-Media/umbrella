# ADR-0020: Module-to-module calls without a user

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated
- **Extends:** [ADR-0019](0019-end-user-identity-propagation.md)

## Context

ADR-0019 makes modules authenticate end users by forwarding a bearer token. Some calls run on a module's own behalf, with no user. One example is media-library-maintainer listing and denying requests in request-media. Accepting a client-supplied module ID would bring back the spoofing that ADR-0019 removed.

## Decision

1. A module call without a user token is accepted only if the caller presents a mesh client certificate whose chain verifies against the core CA (ADR-0017).
2. The certificate's CN must be on an allowlist kept by the serving module, with an explicit set of RPCs per allowed principal. For request-media this is `REQUEST_MODULE_PRINCIPALS`, which defaults to `media-library-maintainer` and allows list and deny only.
3. Any other RPC still requires a user token. Unverified or plaintext callers never become module principals, except under the `dev`-only legacy flags of ADR-0019.

## Consequences

- Module-to-module calls without a user need mesh client certificates, which a module gets through ADR-0017 enrollment. Until media-library-maintainer dials request-media with its certificate, its list and deny calls stay unauthenticated, as they already were.
- Each serving module documents its allowlist and its default.
