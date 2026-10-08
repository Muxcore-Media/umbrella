# ADR-0033: Userdata HTTP authenticates both provider and calling module

- **Status:** Accepted
- **Date:** 2026-10-08
- **Decided by:** Delegated (standing project authority; root and independent source review, 2026-10-08; source implementation and transport acceptance pending)
- **Extends:** [ADR-0017](0017-mesh-module-identity.md), [ADR-0020](0020-module-principal-allowlists.md), [ADR-0030](0030-authoritative-parental-policy-resource.md), [ADR-0031](0031-bff-enforcement-of-parental-policy.md)
- **Supersedes in part:** [ADR-0016](0016-security-profiles.md) Decision 2's core-only profile enforcement, solely for the direct userdata HTTP listener and its clients. Profile names/inference and the existing core/gRPC model remain unchanged.

## Context and source evidence

T-M4-01/S9 closes the transport gap for FR-PLAY-007 and NFR-SEC-003. A fresh user
bearer authorizes a policy operation, but plaintext HTTP lets the network observe
that bearer or substitute an unrestricted response. Server authentication alone
also leaves any bearer holder able to reach the provider without a trusted
calling module. These are independent boundaries.

This decision is based on source inspection, not a runtime reproduction. The
baseline is umbrella `af8ea20`, userdata-local `e544b35` (`v0.1.5`), core
`8e04fc2` (`v0.6.16`), SDK module `v0.6.6` (`b9437bb`), BFF `_mvp` `41a18c0`
(merged, unpinned) and admin-ui `6d0eb55` (the integrated S6 source). Current
BFF session and admin classification workers have separate ownership; their
uncommitted changes are not evidence for this decision. Jellyfin evidence below
comes from the locally cached published `v0.3.5`, not its uninitialized checkout.

| Evidence | Consequence |
|---|---|
| userdata-local `internal/module.go:139-177` starts a plain `http.Server.Serve`; `internal/server/server.go:34-40` registers `/api/userdata`, `/api/parental-policy`, `/health` | Secure the whole listener, including probes; protecting only the policy client leaves other bearer traffic exposed |
| userdata-local `internal/grpctls/tls.go:43-94` can generate a separate CA and uses `VerifyClientCertIfGiven`; HTTP does not use that configuration | Reusing this configuration unchanged does not establish required mesh identity |
| userdata-local `internal/auth/guard.go:28-50` requires a bearer and matching user/admin for blobs; `internal/auth/parental.go:39-112` requires fresh user/tenant resolution and admin writes | Preserve those checks after module admission; no certificate becomes an end-user or admin credential |
| core `internal/enroll/csr.go:101-130` adds module ID plus shared localhost/loopback SANs; `internal/grpcmesh/enrollment.go:69-92` sets CN to module ID and both client/server EKUs | A valid core chain and loopback SAN alone cannot identify userdata-local |
| SDK `meshid.Ensure` exports `MUXCORE_TLS_CERT/KEY/CA`; `sdk/go/module/module.go:156` enrolls before initialization. Published `meshtls` is gRPC-only, with optional client certs and no expected-CN check | Reuse identity material, but explicitly implement the HTTP identity checks |
| core `internal/grpcmesh/meshtls.go:154-202` checks the expected server CN; media-transcoder `eab357c` `internal/playback_auth.go:89-173` gates HTTP with verified caller CNs | Existing identity model and HTTP precedent; these private packages are not consumer APIs |

### Direct HTTP caller inventory

| Owner / source | Calls and required disposition |
|---|---|
| `_mvp/cmd/mediauiprox/parental_gate.go:149-163,241-268` | BFF policy GET uses a dedicated default HTTP client; retain its no-redirect, strict-envelope, no-fallback behavior with the new transport |
| `_mvp/cmd/mediauiprox/userdata.go:138-194` | BFF blob GET/PUT uses shared `upstreamClient` and `/userdata`; the actual provider registers only `/api/userdata`. Correct the client path and prove provider persistence instead of mistaking its existing local-file fallback for a successful provider call |
| admin-ui `handler/parental_provider.go:134-195`; `handler/userdata_parental.go:31-51,72-80,137-146` | Form and trusted migration policy GET/PUT, plus PIN-only blob GET/PUT. All require the same provider transport. Discovery currently invents `http://` for bare advertised addresses; remove that household downgrade |
| Jellyfin `v0.3.5` `internal/userdata_sync.go:330-371,506-562`; `internal/module.go:182,580` | Optional background mirror sends PUT `/userdata`, `X-User-ID`, no auth-local bearer, through the client also used for external Jellyfin traffic. It already fails the current provider path/auth contract. Do not silently grant this job module-only blob writes; disposition below |
| `_mvp/scripts/lib/parental-smoke.sh:42-55`, `smoke.sh:391-400` | Direct curl policy GET/PUT with admin bearer; move the household helper into the admin service's authenticated execution context |
| `_mvp/deploy/helm/muxcore/templates/media-stack.yaml:13,89-94` | Bare HTTP `/health` readiness probe must change with the listener. Household compose currently has no userdata HTTP healthcheck; enumerate new probes before release |
| `_mvp/docker-compose.registry.yml`, `docker-compose.yml`, `docker-compose.dev.yml`, `run-host.sh` | URLs for BFF, admin-ui and optional Jellyfin, identity volumes, host publication and profile overrides must move together. Container and host defaults differ; changing one compose file is insufficient |

The SPA calls the BFF's relative `/api/userdata` (`media-ui-app/src/api/client.ts`
and `src/lib/userdata.ts`), not the provider listener. Restore-drill userdata calls
also go through the BFF. The BFF's separate outbound notification to the Jellyfin
bridge is a different listener and is not secured by this decision. Native iOS
and tvOS checkouts are unavailable here: do not infer that they use the BFF;
inventory their configured endpoints before full S9 closure. Undocumented direct
clients must migrate through a user-facing gateway; household browsers/devices
do not receive mesh private keys.

## Decision

### 1. Provider identity and HTTP client

Publish a small provider-specific `httpclient` package in userdata-local's next
version. It owns constructing a client for this HTTP contract, not a new generic
SDK transport framework. Existing consumers already require userdata-local
`v0.1.5`. No core/protobuf contract or new SDK release is needed; all `go.mod`
dependencies remain published tags, without cross-repository replacements.

- Household/staging and secure operation require a usable enrolled
  `MUXCORE_TLS_CERT`, `MUXCORE_TLS_KEY` and explicit `MUXCORE_TLS_CA`. Missing,
  partial, unreadable or malformed material is a configuration error. Do not use
  system roots, generate an HTTP CA, or fall back to plaintext.
- TLS minimum is 1.2. Clients retain normal Go certificate-chain, lifetime, EKU
  and SAN verification with `ServerName: "userdata-local"`, then additionally
  require the verified leaf's **exact CN `userdata-local`** in `VerifyConnection`.
  Enrollment already includes that service-name SAN. Dialing a configured
  loopback/IP/container address does not change the expected identity. Do not
  use `InsecureSkipVerify` or a global `MUXCORE_TLS_SERVER_NAME` override here.
- Present the calling module's own enrolled certificate. Never borrow another
  service's private key or forward a browser-supplied module identity.
- Accept only a configured HTTP(S) **origin**: no credentials, query, fragment,
  or path prefix. HTTPS is mandatory outside explicit insecure dev. Endpoint
  paths are fixed by the client operation. Refuse every redirect, including
  same-origin redirects. Use a dedicated transport without environment proxy
  routing. Bind the transport to that origin and refuse a different request
  origin before dialing, so it cannot present a mesh identity to an external service.
- The package supplies bounded requests and a no-redirect client without adding
  authorization headers itself. Consumers derive the one bearer and target user
  from their current server session, preserving ADR-0030/0031 semantics. Existing
  policy timeouts remain bounded; requests must also honor cancellation.

### 2. Provider listener and method authorization

In secure operation the existing userdata HTTP port serves TLS only, with
`RequireAndVerifyClientCert`, an explicit core CA pool, and the provider's enrolled
certificate/key. Validate all transport configuration before exposing either
listener; a startup failure must clean up any listener already opened. The HTTP
setup must not inherit gRPC's optional certificate or generated-CA behavior.
The provider certificate must identify `userdata-local` and include that SAN.

Wrap the entire HTTP mux with admission based on the verified leaf CN and the
exact method/path table below. Use no identity headers, raw unverified peer
certificate, URL, IP address or bearer claims as the module principal.

| Verified module CN | `/api/parental-policy` | `/api/userdata` | `/health` |
|---|---|---|---|
| `media-ui` | GET | GET, PUT | GET, HEAD |
| `admin-ui` | GET, PUT | GET, PUT | GET, HEAD |
| `userdata-local` | denied | denied | GET, HEAD |
| `health-monitor` | denied | denied | GET, HEAD |
| Any other CN, including `jellyfin` and `muxcore` | denied | denied | denied |

This first release has a fixed least-privilege table, not a wildcard or a
comma-separated allowlist that can accidentally give every admitted caller
policy-write access. Additional principals/methods need an explicit follow-up
contract and tests. The health endpoint returns no user data and needs no user
bearer, but still requires mTLS and admission. HEAD has an empty body.

For data routes, admission is **additional to** existing HTTP user authorization:
policy always validates a fresh bearer, verified tenant, target existence and
role; only an authenticated admin can PUT. Blob self/admin behavior stays as
implemented. Neither `media-ui` with an admin bearer nor `admin-ui` with a member
bearer can write policy through the wrong layer. A certificate alone cannot read
or write either resource. The gRPC userdata principal model is outside S9; do not
expand HTTP permissions by copying its service-account shortcut.

Absent/untrusted client certificates fail the TLS handshake. A verified but
unlisted CN or disallowed method/path receives generic no-store 403 before the
handler, auth lookup or storage, with `Content-Type: application/json` and exactly
`{"code":"userdata.module_forbidden"}` (no caller, path, user or tenant details).
The shared client recognizes that bounded response as a typed provider-unavailable
error (`errors.Is(err, httpclient.ErrUnavailable)`, reason `module_forbidden`),
not an application 403. Consumers must not revoke the user session or report an
admin/tenant denial for it. The authenticated application response
`403 {"code":"policy.forbidden"}` and existing 401/409/503 meanings remain
distinct. A received module-admission refusal proves that request was not applied;
connection loss/timeouts still have an uncertain write outcome. Policy failure never produces unrestricted/unconfigured success or a
local fallback. Admin writes retain uncertain-outcome handling and revision checks.

### 3. Development and compatibility

Explicit insecure dev may retain plaintext and existing user-bearer authorization
with a startup warning; it has **no authenticated module transport** and cannot
earn household acceptance. The HTTP builder and listener each enforce profile
rules directly, not by relying on an earlier gRPC startup call. Household/staging
reject every supported insecure alias. Unset-profile legacy inference follows
ADR-0016, with the same warning; unknown profiles fail configuration validation.
Dev is not a recovery path after a TLS error. Do not reuse transcoder's dev
`Authorization` token mechanism because userdata needs that header for the user.

The server switch intentionally rejects old household plaintext clients. Prepare
and verify all required consumers before deploying the producer. Do not run a
parallel plaintext compatibility listener. The provider may be released first
for consumer development, but it must not be rolled into a running household by
itself. Preserve database files and policy revisions; this is not a schema change.

Jellyfin background sync remains unauthorized: no admin credential is stored for
background use, no event username becomes an authenticated household principal,
and no new module-only blob-write exception is introduced. For the first S9
household train, explicitly disable its inbound userdata sync (`USERDATA_SYNC=0`)
and do not wire a provider URL to that worker. Report the unsupported optional
feature clearly; do not describe the whole Jellyfin bridge as broken or remove
its playback function. Re-enabling sync needs a separately reviewed narrow
service-operation/user-mapping contract under ADR-0020 and its own module work.
Native/direct-client inventory and this configured disabled-sync behavior are
release gates, not assumptions that those clients do not exist.

### 4. Deployment, smoke and health

Use enrolled identities and persistent volumes already assigned to each service;
do not copy keys, issue a broad smoke-admin service credential, or back identities
up with userdata (ADR-0023). Set BFF/admin origins to
`https://userdata-local:9672` in household compose. Host household operation may
dial loopback while verifying the fixed `userdata-local` server identity. Explicit
dev compose/host defaults stay HTTP. Discovery must normalize a valid advertised
host/port using the selected mode; unspecified bind addresses cannot be dial
targets. Failure to resolve a valid origin is unavailability, never an HTTP retry.

Household smoke performs the existing explicit admin-bearer seed through a helper
executed **inside admin-ui's own service context**, using the published client and
its mounted identity. S9c owns the actual helper executable/subcommand and its
image/host packaging; S9d only invokes it. Host smoke uses the corresponding local
service context; do not export the private key to the smoke runner. Pass the user
bearer through a protected input, mask it and omit it from logs. No plain curl, `-k`, new anonymous
policy route, or automatic unrestricted-account migration is acceptable.

Readiness uses a local executable probe inside userdata-local, using its own
identity and the same checked client for GET `/health`. S9a must ship that
executable/subcommand in the provider image and host artifact; S9d supplies its
invocation. This replaces bare HTTP probes and creates no additional listener.
Both helpers run separately from the daemon and cannot inherit `MUXCORE_TLS_*`
values exported inside the daemon by `meshid.Ensure`. They resolve only existing
configured cert/key/CA or the existing `MUXCORE_TLS_DIR` identity and mounted CA,
validate them, and fail if missing or invalid. A helper never enrolls, consumes a
bootstrap token, generates a CA or starts the main service.

The `health-monitor` allowlist entry permits that identity; it does not implement
a compatible HTTP probe client in that module. Any configured monitor needs its
own verified transport implementation and evidence, or an explicit unsupported
state, before S9 closure. Manifests that cannot provide these identities/probes
must stay explicitly unsupported for S9 instead of claiming household readiness. Do not expose the provider directly through a
public browser ingress; any retained operator host port is TLS only.

## Sequencing and scoped acceptance

These are implementation slices under T-M4-01/S9, not completion claims.
One worker owns each repository; source release and household rollout are separate.

| Order / slice | Repository | Concrete scope and required verification |
|---|---|---|
| S9a | userdata-local | TLS listener, verified-CN method table, small public HTTP client API, actual local probe executable/subcommand with image/host packaging, existing-identity-only resolution, startup cleanup and local configuration docs. Real TLS fixtures must prove both directions, unchanged bearer/admin/tenant/revision behavior, health isolation and dev/profile handling. Run `go test -race ./...`, `go vet ./...`, `go build ./...`, module lint if configured; inspect diff, then publish the module version before consumer pins. No BFF/admin/deploy edits |
| S9b | `_mvp` BFF | After the existing T-M5-11 worker releases this checkout, consume the published version after enrollment; policy and blob calls share the checked provider client, canonical blob path, no redirects. Preserve the policy cache bound/error mapping and separate ordinary blob fallback semantics. TLS tests cover forged unrestricted policy, 401 eviction, 403 module denial → 503, admin-CN rejection for a spoofed provider, wrong CA, redirects and real-provider blob persistence; never count fallback as provider success. Run focused/full Go checks and `./scripts/run-script-tests.sh` |
| S9c | admin-ui | After classification recovery releases this checkout, consume the published version for form GET/PUT, every migration request and PIN blob GET/PUT, including discovery; ship and package the separate admin-bearer seed helper with existing-identity-only resolution. Test real TLS/member/admin/cross-tenant/stale writes, all redirect types, uncertain write results, dry-run/no-write, and partial migration recovery. Preserve session revalidation. Run full Go race/build/vet/lint and affected interaction/a11y checks |
| S9d | `_mvp` deployment | Serialize with S9b. Update registry/reference/dev compose, host runner, invocation of the packaged admin seed/provider probe helpers, supported Helm/kustomize presence and runbooks. Explicitly disable optional incompatible Jellyfin sync and inventory native/direct clients. Render every affected manifest/profile; run script fixtures and actual authenticated health/seed smoke. No missing-module wiring or acceptance by text inspection alone |
| S9e | umbrella | After published source versions and consumer review, update gitlinks/integration fixtures; verify published-tag builds with `GOWORK=off`, `./scripts/check-proto-drift.sh`, docs/pins checks and fixture household clean-room smoke. Record exact source/tag/train and restricted-account outcomes; only then consider S9 accepted. S10/S11 train/rollout and live acceptance retain separate evidence |

Producer adversarial TLS fixtures must include no client cert; untrusted, expired,
wrong-EKU and non-allowlisted client certificates; forged identity headers; a
same-CA impostor server with the shared loopback SAN; a wrong-CN server that even
has the `userdata-local` SAN; wrong server CA/SAN/EKU/expiry; missing key/CA;
plaintext to secure port; and household/staging plus each insecure flag. Assert that admission 403 maps to
`ErrUnavailable` while application `policy.forbidden` remains an application
denial, in the client and admin migration/form fixtures. Verify
that failures never invoke auth/storage or send a bearer to the wrong server.
Valid `media-ui` plus valid self bearer reads policy; its PUT is denied even with
an admin bearer. Valid `admin-ui` plus member bearer cannot write; valid admin
writes retain same-tenant checks and atomic revision conflicts. Real sockets are
required in addition to handler tests that construct a TLS state manually.

Acceptance must also verify that a newly restrictive admin write reaches an
already-running BFF within ADR-0031's 30-second cache bound, and transport failure
after cache expiry fails closed. A successful provider TLS unit suite alone does
not establish BFF/admin wiring, profile/PIN support, native-client routing, live
household enforcement or release readiness. FR-PLAY-007 remains Part. T-M3-10 is
separately blocked; this work does not investigate or retry subtitle paths.

## Alternatives and consequences

- **Server TLS only:** hides traffic but does not restrict calling modules or
  policy-writing callers. Require both transport directions and retain user auth.
- **Core CA plus URL-host/loopback match only:** another enrolled module shares
  loopback SANs. Bind clients to the service-name SAN and exact verified CN.
- **Reuse gRPC TLS or transcoder HTTP unchanged:** optional client certificates,
  generated/system CA fallback and the dev bearer header are unsuitable here.
- **New SDK-first generic HTTP framework:** possible later, but unnecessary for
  this one provider contract; publication of its small client avoids duplicated
  verification in BFF/admin without widening core's API or blocking on a core release.
- **Plaintext trusted network, a shared household token, or TLS verification off:**
  none authenticates this mesh boundary under ADR-0017; no household fallback.
- **Move policy to gRPC or add a module-only Jellyfin write exception now:** changes
  the accepted HTTP/user-authority contract and increases scope. Preserve the
  current resource and plan optional background authority separately.

The coordinated switch may cause a bounded maintenance outage if versions or
identities mismatch; fail closed and fix that mismatch. Never call a rollback to
plaintext household-secure. Keep the prior train and data recoverable, but require
the fixture gate before any new deployable claim. Work stays on GitHub origins/CI,
uses no paid service, and acquisition validation remains fixture-only.
