# LLD — Core mesh, module lifecycle, and marketplace deploy

| | |
|---|---|
| **Status** | Active — v1.0 (2026-10-05); describes `core` at the umbrella pin with notes on `main` |
| **Scope** | `core` (`cmd/muxcored`, `internal/{module,registry,grpcmesh,events,spool,sandbox,audit}`, `pkg/contracts`, `pkg/tenant`, `sdk/go/*`) |
| **Requirements** | FR-EXT-001…003, NFR-REL-002, NFR-SEC-002/003, NFR-OPS-001/004 |

## 1. Startup sequence (`cmd/muxcored/main.go`)

1. Load config (`muxcore.json` via `MUXCORE_CONFIG`; ~70 `MUXCORE_*` overrides) → startup checks.
2. Event bus (in-memory; WAL if `MUXCORE_EVENT_JOURNAL_PATH`).
3. gRPC mesh: CA (`~/.muxcore/ca` or `MUXCORE_GRPC_CA_CERT_DIR`), TLS/mTLS unless `MUXCORE_INSECURE_DISABLE_TLS`; interceptors: logging, panic recovery, auth.
4. Storage orchestrator, worker pool, HTTP server (`:8080`), audit (JSONL hash chain).
5. Wire policies and auth by discovery (`bootstrap/wiring.go`).
6. Module manager; fetch spool tag and spawn modules (`LoadAndSpawnModules`).
7. `InitAll` → `StartAll` (dependency order) → serve HTTP + gRPC (`:9090`) → join seeds → heartbeat.

## 2. Module states and health

`registered → starting → running ⇄ degraded → stopping → stopped`. `StartHealthCheckLoop` polls `Health()`; failures mark `degraded`, publish `module.degraded`, and trigger remediation (restart for spawned modules). Sidecars report via `ReportHealth` (health-monitor) and core health RPC.

## 3. Registration (`internal/module/mgr/manager.go`)

`Register(info)` → `MinCoreVersion` check → `SidecarProxy` wrapper → `modMgr.Register` → capability index update → `PostRegisterHook` (re-wire call policy, publish policy, auth) → `module.registered` event. **Security target:** see [auth LLD §5](auth-and-sessions.md#5-mesh-authorisation-core).

## 4. Mesh call path (`grpcmesh/server.go` `Client.Call`)

```
Call(target, method, payload):
  require call.policy provider        → else error (fail closed)
  policy.Allow(caller, target, method) → else PermissionDenied (target: also audit + publish call.policy.denied)
  local in-process handler?           → invoke
  remote: healthy nodes → per-node circuit breaker (5 failures / 60 s reset) → LBStrategy → fallback nodes
```

## 5. Marketplace deploy (`SpoolService.DeployTag`, `internal/spool`)

| Step | Check | Config |
|------|-------|--------|
| 1 | Version string pattern | — |
| 2 | Repo host allow-list | `MUXCORE_SPOOL_ALLOWED_HOSTS` (target default: `github.com`) |
| 3 | `git clone --depth 1 --branch <version>` | — |
| 4 | Contract reconcile (`contracts-reconciler`) | — |
| 5 | Source scan: cgo, `go:generate`, `unsafe`, `go.mod replace` | `ScanPolicy` |
| 6 | `go build ./cmd/module/` → cache `~/.muxcore/modules/<id>/<version>/` | — |
| 7 | SHA-256 checksum | tag `Checksum` |
| 8 | Publisher allow-list | `MUXCORE_SPOOL_ALLOWED_PUBLISHERS` |
| 9 | Signature (ed25519 / minisign) | `MUXCORE_SPOOL_REQUIRE_SIGNATURE`, `_PUBLIC_KEY`, `_TRUSTED_KEYS_DIR` |

Note: step 5 rejects modules whose `go.mod` contains `replace` — another reason for NFR-MNT-007.

## 6. Persistence owned by core

Audit JSONL (`MUXCORE_AUDIT_PATH`; 100 MB × 5 rotation; SHA-256 chain; HMAC available via `SetSigningKey` but not wired — target: `MUXCORE_AUDIT_HMAC_KEY_FILE`), optional event WAL, CA material (0600), module binary cache, local storage provider (`MUXCORE_STORAGE_DIR`, 0700), in-memory task/idempotency/dead-letter stores (file-backed with `--task-dir` etc.).

## 7. Module layout change in v0.6.0 ([ADR-0011](../../adr/0011-go-workspace-and-published-versions.md))

- `pkg/tenant` is a **package of the root module** (`github.com/Muxcore-Media/core/pkg/tenant` resolved via `github.com/Muxcore-Media/core`). The nested-module tag `pkg/tenant/v0.5.8` is obsolete; consumers must drop any `require`/`replace` of `github.com/Muxcore-Media/core/pkg/tenant`.
- Nested modules that remain: `pkg/contracts`, `sdk/go/client`, `sdk/go/module` — tagged `<path>/v0.6.0` alongside `v0.6.0`.
- `integsupport` (test harness helpers) is a package of the root module, used by the umbrella `integration/` suite.
