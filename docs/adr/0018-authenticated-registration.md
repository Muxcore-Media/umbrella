# ADR-0018: Authenticated registration and exclusive security capabilities

- **Status:** Superseded by [ADR-0022](0022-registration-rules-dev-restarts.md)
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

`Register` and `Unregister` are open methods, so any mesh peer can unregister `auth-local` and register an impostor. Wiring of security capabilities takes `entries[0]` from a Go map iteration, which is effectively random (NFR-SEC-002).

## Decision

| Rule | dev | household |
|---|---|---|
| Certificate CN ≠ registering module ID | reject | reject |
| No certificate when registering `call.policy`, `publish.policy`, `authorizer`, `identity` or `auth` | warn | reject |
| No certificate for other capabilities | allow | stage 1: allow with warning; stage 2: reject |
| A second provider (different ID) of an exclusive security capability | warn; first-registered provider kept | reject `FailedPrecondition` |
| The same verified ID re-registers | atomic replace | atomic replace |
| `Unregister` | open | requires CN == ID |

Wiring uses the recorded provider, not map order. Core dials sidecars with the core CA roots and its own client certificate, and checks that the sidecar's CN equals the registered ID.

## Consequences

Impersonating security providers requires a certificate the core CA issued for that module ID. The `_mvp/cmd/unregistermodule` tool works only in `dev`, or for the owning module.
