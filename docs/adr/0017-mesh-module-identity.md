# ADR-0017: Mesh module identity and enrollment

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

Core has a CA that issues certificates with CN = module ID, and it has single-use tokens that nothing issues. It never reads peer certificates. Module identity comes from the client-supplied `x-caller-id` metadata, which core passes to the identity provider. auth-local (≥ v0.1.14) accepts that ID only if the TLS peer is the module itself, but on the core → auth-local connection the peer is core. As a result, module event publishes cannot authenticate in any mode.

## Decision

1. **Module identity is the CN of a peer certificate that verifies against the core CA** (`VerifiedModuleID`). Core treats modules as service principals, authorized by core rules and the call/publish policy. The user identity provider is consulted only for user tokens. Core never forwards a client-supplied caller ID for a caller that presented a certificate.
2. **In `dev` without certificates**, core accepts `x-caller-id` as the module principal and logs a warning. It does not ask the user identity provider.
3. **Enrollment.**
   - `BootstrapRegister` gains `csr_pem` and `dns_names`. The module generates its own key, and core writes no key files.
   - Tokens are `mct_2_<id>_` followed by hex(HMAC-SHA256(`MUXCORE_ENROLL_SECRET`, id)), made single-use by a persisted ledger (`<ca>/enrolled.json`).
   - `muxcored enroll token|reset|list` manages tokens.
   - SANs are the module ID, loopback, and an operator allow-list.
4. **SDK.** `sdk/go/meshid.Ensure`:
   - Reuses certificates already in `MUXCORE_TLS_DIR`.
   - Otherwise enrolls with the token over server-verified TLS and stores the certificate with mode 0600.
   - Exports `MUXCORE_TLS_CERT`, `MUXCORE_TLS_KEY` and `MUXCORE_TLS_CA`.

   `client.Dial` loads TLS settings from the environment by default.
5. **Single-use tokens with a persistent identity volume per service** were chosen over reusable per-module secrets, matching NFR-SEC-002 ("single-use bootstrap token").

## Consequences

- Each compose service needs a small `<svc>-id` volume, and `scripts/gen-enrollment.sh` writes the secret and tokens into `.env`.
- Losing an identity volume requires `muxcored enroll reset <id>`.
