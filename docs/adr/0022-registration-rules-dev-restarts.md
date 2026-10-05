# ADR-0022: Registration rules, with dev-mode restarts

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated
- **Supersedes:** [ADR-0018](0018-authenticated-registration.md)

## Context

ADR-0018 allowed atomic replacement only when the same *verified* module ID re-registers. In the `dev` profile modules have no certificates. The first real compose run (T-M2-10) showed that any restarted module then looped forever on "already registered", because modules did not unregister on shutdown and core kept the stale entry.

## Decision

ADR-0018's table, with the re-registration row extended for `dev`:

| Rule | dev | household |
|---|---|---|
| Certificate CN ≠ registering module ID | reject | reject |
| No certificate when registering `call.policy`, `publish.policy`, `authorizer`, `identity` or `auth` | warn | reject |
| No certificate for other capabilities | allow | stage 1: allow with warning; stage 2 (`MUXCORE_REQUIRE_MODULE_CERTS`): reject |
| A second provider (different ID) of an exclusive security capability | warn; first-registered provider kept | reject `FailedPrecondition` |
| The same verified ID re-registers | atomic replace | atomic replace |
| The same ID re-registers **without** a certificate | atomic replace with a warning, only if the existing entry was also unverified | reject |
| `Unregister` | open | requires CN == ID |

A replaced entry keeps its original registration order, so ownership of an exclusive capability never moves to a different ID.

**SDK behaviour:**
- `modulesdk.Run` unregisters (best effort) before stopping on SIGINT/SIGTERM.
- A supervision loop re-registers when core no longer knows the module or comes back after a restart.

## Consequences

- Dev stacks recover from module and core restarts without intervention.
- In `dev`, an unverified peer can replace another unverified registration of the same ID. That is consistent with dev trusting `x-caller-id`.
- Verified registrations cannot be displaced by unverified ones in either profile.
