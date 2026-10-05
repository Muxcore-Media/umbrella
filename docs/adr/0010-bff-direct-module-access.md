# ADR-0010: BFF dials modules directly instead of via `ModuleMesh`

- **Status:** Accepted
- **Date:** recorded 2026-10-05
- **Decided by:** Historical — `_mvp/cmd/mediauiprox` dials typed gRPC clients and HTTP ports directly; `AGENT-MODULE-AUDIT-2026-08-21.md` calls this intentional

## Decision (as implemented)

The consumer BFF uses capability discovery to find modules, then calls their typed gRPC/HTTP APIs directly, instead of routing through `ModuleMesh.Call`.

## Rationale

Typed contracts, streaming endpoints (`/stream/*`, HLS), and lower latency than opaque JSON mesh calls.

## Consequences

- Call policy does not mediate BFF → module traffic; the BFF must enforce its own authorisation (session roles, server-side parental filtering — FR-PLAY-007) and must forward the user's auth token so modules/core can authorise.
- Module endpoints the BFF calls must require authenticated callers (NFR-SEC-007).

## Revisit when

Multi-node deployments require load-balanced/remote module calls from the BFF.
