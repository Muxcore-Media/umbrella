# LLD — Core mesh, module lifecycle, and marketplace deploy

| | |
|---|---|
| **Status** | Active — v1.0; base architecture with verified follow-up checkpoints tracked in the roadmap (core default branch: `master`) |
| **Scope** | `core` (`cmd/muxcored`, `internal/{module,registry,grpcmesh,events,spool,sandbox,audit}`, `pkg/contracts`, `pkg/tenant`, `sdk/go/*`) |
| **Requirements** | FR-EXT-001…003, NFR-REL-002, NFR-SEC-002/003/009, NFR-OPS-001/004 |

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
| 2 | Repo host allow-list | Manager `allowedRepoHosts` (separate from the HTTP connection policy below) |
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

## 8. Spool HTTP destination policy (NFR-SEC-009)

[ADR-0028](../../adr/0028-spool-dial-time-destination-policy.md) governs HTTP
spool-tag requests; it does not add a Git-clone transport guard. The fetcher uses
a core-local client without a module-SDK dependency.

| Boundary | Behavior |
|----------|----------|
| URL | HTTPS, nonempty host, valid port, no userinfo or ambiguous IP literal |
| Host allow-list | Exact URL `Host` equality, including an explicitly written port; no wildcard/case/implicit-port expansion. Empty list permits public destinations only; explicitly listed hosts may use LAN/loopback/CGNAT |
| Universally denied destinations | Metadata names/addresses, link-local, unspecified, multicast, scoped IPv6 and blocked special-use ranges; an exact host entry does not override this denial |
| Address aliases | IPv4-mapped IPv6 and embedded IPv4 in well-known NAT64/6to4 are classified; Teredo `2001::/32` and reserved network-specific NAT64 `64:ff9b:1::/48` are refused. Arbitrary operator-defined translation prefixes cannot be inferred |
| DNS and dialing | Validate every returned address before dialing any; connect to a checked literal address without resolving the name again. Checked-address family fallback retains the same policy |
| HTTPS identity | TLS verifies the original URL hostname, even though dialing uses a literal address |
| Redirects | Each target passes the same URL/host checks and guarded connection path; the existing `len(via) >= 3` ceiling is unchanged |
| Limits | Existing 10-second request timeout and 1 MiB read bound retained; request cancellation ends associated dialing work |
| Policy update | In-flight requests retain their immutable policy/client snapshot; later fetches use the new snapshot |
| Proxy | This product transport ignores implicit HTTP(S) proxy environment variables so proxy-side DNS cannot bypass address checks |

Verification is tracked in the roadmap's NFR-SEC-009 checkpoint. Core's spool
slice does not close the outbound-request audit in other modules.
