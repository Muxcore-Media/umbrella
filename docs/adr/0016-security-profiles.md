# ADR-0016: Security profiles (`dev` / `household`)

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

TDD §8 item 2 requires a `dev` profile where insecure operation is allowed (with a loud warning) and a `household` profile that is secure by default. Today 86 repositories read `MUXCORE_INSECURE_DISABLE_TLS` independently, compose and the installer hard-code it, and core has no profile concept. `MUXCORE_PROFILE` is already overloaded: run-host uses `dev|staging`, while the installer uses it to choose the database (`sqlite|postgres`).

## Decision

1. **The variable.** `MUXCORE_PROFILE` takes `dev` or `household`; `staging` is an alias for `household`. The installer's database selector becomes `MUXCORE_DB_BACKEND`. A shim maps `MUXCORE_PROFILE=sqlite|postgres` to it, with a warning.
2. **Enforcement.** Core is the only enforcer: in `household` it accepts no plaintext connections and no unauthenticated module principals. The module SDK reads the profile only to give clear errors (household) or warnings (dev). Modules need no other per-module logic.
3. **What `household` means:**
   - gRPC always uses TLS, with a core CA created automatically under `<data>/ca`.
   - Core exports the public `ca.crt` to `MUXCORE_CA_EXPORT_DIR`.
   - Clients present certificates (`VerifyClientCertIfGiven`), and a verified certificate is required for every method except BootstrapRegister, health checks and read-only discovery.
   - `MUXCORE_INSECURE_DISABLE_TLS` is fatal.
   - Marketplace `DeployTag` and orphan resurrection require signatures. Curated boot tags stay anchored by checksum (ADR-0012).
   - Fixture acquisition is enforced by module defaults, not by core; core stays media-agnostic.
4. **What `dev` means:** insecure operation is allowed. Core prints a warning banner at startup, and `/health` reports `"profile":"dev","insecure":true`.
5. **Migration:**
   - **Phase 0:** if the profile is unset and the insecure flag is set, the profile is `dev`, with a deprecation warning. If the profile is unset and the flag is not set, the profile is `household`.
   - **Phase 1:** compose and the installer switch to `household` once enrollment (ADR-0017) exists and every image in the release train is built on the new SDK. A `docker-compose.dev.yml` override keeps the dev loop, and run-host stays `dev`.
   - **Phase 2:** the inference rule is removed one release train later (ADR-0015 window).

## Consequences

- Installs that set nothing get secure defaults.
- Rollback is `MUXCORE_PROFILE=dev` plus the insecure flag in `.env`.
- Marketplace deploys are effectively disabled in `household` until signed spool entries exist.
