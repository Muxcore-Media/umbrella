# MuxCore — Technical Design Document (TDD)

> **TDD here means Technical Design Document**, not test-driven development.

| | |
|---|---|
| **Status** | Active — v1.0 (2026-10-05) |
| **Owns** | Concrete engineering choices and trade-offs, cross-cutting technical concerns (build, dependency management, CI, packaging, testing, security hardening approach, data migration), implementation strategy and migration considerations |
| **Does not own** | Requirements (→ [SRS](../specs/SRS.md)), structure (→ [HLD](HLD.md)), collaboration patterns (→ [SDD](SDD.md)), per-subsystem detail (→ [LLD](lld/README.md)). Decisions with long-lived consequences are recorded as [ADRs](../adr/README.md); this document applies them. |

---

## 1. Technology stack (current, retained)

| Area | Choice | Evidence |
|------|--------|----------|
| Backend language | Go 1.26.x (`go` directive 1.26.4–1.26.6 across modules; one at 1.25.0) | `*/go.mod` |
| RPC | gRPC + protobuf (`google.golang.org/grpc` v1.8x), generated stubs committed | `core/proto`, `contracts-*/` |
| Persistence | SQLite (modernc, CGO-free) per module; optional Postgres (pgx) | `database-sqlite`, module stores |
| Operator UI | Go `net/http` + templ + HTMX + Tailwind (CSS embedded via `go:embed`) | `admin-ui` |
| Consumer UI | React 18, Vite 5, Tailwind 4, react-router 6, hls.js; Vitest + Testing Library | `media-ui-app/package.json` |
| BFF | Go (`_mvp/cmd/mediauiprox`) | ~39.6 k LOC |
| Native | SwiftUI (iOS, tvOS) with xcodegen/SwiftPM | `muxcore-ios`, `media-tvos-app` |
| Packaging | OCI images (`_mvp/dockerfiles/*`), compose, GitHub release binaries | `_mvp` |
| Toolchain on dev boxes | Any of: system Go/Node, `mise`, or Nix shells — the repo must not require a specific one | (agent rules are held outside this repository) |

No language or framework change is proposed. Rationale: the problems found in the audit are build hygiene, security defaults, and missing product surface — not stack limitations.

## 2. Repository model

- **Module-per-repository** under `github.com/Muxcore-Media` (≈ 113 repos), aggregated by the **umbrella** repo as git submodules at pinned SHAs. Each module releases independently with its own semver tags.
- **The umbrella is the integration point**: it pins a known-good set, hosts the canonical roadmap/specs/ADRs, workspace scripts, and integration CI.
- **GitHub is the only origin** ([ADR-0002](../adr/0002-github-sole-origin.md)). Every submodule URL is `https://github.com/Muxcore-Media/<repo>.git`.

### 2.1 Archived and empty repositories

| Repo | State (2026-10-05) | Decision |
|------|--------------------|----------|
| `media-ui`, `media-jellyfin`, `cache-memory`, `custom-scripts`, `muxcorectl` | Archived dumps (NG-7) | Keep pinned for history; mark archived on GitHub; exclude from CI and sweeps |
| `playback-contract` | **Empty on GitHub**; `jellyfin`, `plex`, `emby`, `media-jellyfin` `replace` it | Recreate from the consumers' required API surface (it is a small shared Go package), or inline the package into `contracts-playback` and repoint consumers. **Chosen:** fold into `contracts-playback` (which already owns playback events) — fewer repos, matches NG-6 (no second backend contract yet). |
| `metadata-musicbrainz` | **Empty on GitHub**; `media-music` `replace`s it | Rebuild a minimal MusicBrainz client implementing `contracts-metadata` music RPCs with a fixture corpus (FR-MM-001) |
| `media-ui-tv`, `muxcore-android` | **Empty on GitHub** | Source unrecoverable from GitHub. Remove from umbrella pins until source exists; keep requirement FR-NAT-003 as Blocked. `media-ui-tv`'s role is covered by `media-tvos-app`. |

## 3. Dependency management ([ADR-0011](../adr/0011-go-workspace-and-published-versions.md))

### 3.1 Problem found

At the pinned SHAs, 46 of 97 Go modules failed `go build` in the umbrella (validation log in the roadmap):

1. **`core/pkg/tenant` split** — `pkg/tenant` was a nested module (tag `pkg/tenant/v0.5.8`) and was later folded back into the root `core` module. Consumers still `require github.com/Muxcore-Media/core/pkg/tenant` and/or `replace` it to `../core/pkg/tenant` (no `go.mod` there) → "ambiguous import" or "no such file".
2. **Missing tags on GitHub** — modules require `github.com/Muxcore-Media/core v0.5.8` and `jellyfin v0.3.0`; those tags existed only on the retired self-hosted origin.
3. **Absolute replaces** — `replace … => /home/ender/Projects/MuxCore/…` in at least `playback-monitor` and two others.
4. **Empty dependency repos** (§2.1).
5. **Proto drift** — consumers reference generated types that no longer exist at the pinned producer version (`auth-local` `authv1.InviteInfo`, `circuitbreaker-simple` `circuitbreakerv1.ListRequest`).
6. **Root `integration/`** imports `core/integsupport`, which exists only on `core` `main` (after the umbrella pin).

### 3.2 Chosen approach

1. **Module `go.mod` files require published tags only** and carry **no filesystem `replace`** (NFR-MNT-007). Standalone CI (`go build` in a fresh clone) therefore works with only a token that can read private org repos.
2. **Umbrella `go.work`** (generated by `scripts/gen-go-work.sh` from `.gitmodules`, excluding archived/empty repos) gives local cross-module development and the umbrella integration job. `go.work` overrides published versions with the checked-out siblings, so local edits are picked up without touching `go.mod`.
3. **Release core `v0.6.0`** from `core` `main` on GitHub (minor bump: module-layout change — `pkg/tenant` is a package of the root module; adds `integsupport`). Keep nested modules that still have their own `go.mod` (`pkg/contracts`, `sdk/go/client`, `sdk/go/module`) and tag them consistently (`pkg/contracts/v0.6.0`, `sdk/go/client/v0.6.0`, `sdk/go/module/v0.6.0`).
4. **Bump consumers in dependency order** (core → contracts-* → platform modules → domain modules → UIs/BFF → umbrella pins), removing `core/pkg/tenant` requires/replaces, and tag each touched module with a patch release.
5. **Go version**: standardise the `go` directive at `1.26.6` and CI `go-version: '1.26.x'`.

**Version resolution for historical references (applied 2026-10-05).** Many `go.mod` files (including older tags of siblings that remain in the module graph) reference versions that existed only on the retired origin. Two remedies, in order of preference:
- **Restore the exact tag** when a published `go.sum` hash identifies the commit: `core v0.5.8` was re-tagged at `d732be6`, whose module hash matches `h1:4mwv7…` recorded by consumers.
- **Resolution tags** otherwise: placeholder versions that consumers only ever reached through `replace` (`core v0.1.0`, `pkg/contracts/v0.0.0`, `sdk/go/{client,module}/v0.5.8`, `…/v0.5.9`) are tagged at the `v0.6.0` commit so the module graph resolves; MVS still selects `v0.6.0` for builds. No consumer has a checksum for these, so no `go.sum` conflicts arise.

**Rejected alternatives:** (a) keep sibling `replace` directives everywhere — breaks standalone CI, leaks host paths, and is rejected by core's own marketplace source scan; (b) a monorepo merge — large, irreversible history change, contradicts module-per-repo releases and marketplace builds from source; (c) only bump consumers without fixing historical versions — older sibling tags stay in the graph via cycles and indirect requirements, so resolution would still fail.

## 4. CI/CD on GitHub ([ADR-0003](../adr/0003-ci-on-github-hosted-runners.md))

### 4.1 Per-module CI (generated from one template)

- Templates live in the umbrella at `scripts/ci-templates/go-module-ci.yml`, `go-module-ci-proto.yml`, and `node-app-ci.yml`. `scripts/sync-ci.sh` writes `.github/workflows/ci.yml` into each module so that all modules stay identical apart from documented parameters. Separate UI verification workflows complement those generated files ([ADR-0025](../adr/0025-ui-verification-gates.md)).
- Runner: `ubuntu-latest` (GitHub-hosted). Self-hosted runners and the `checkout-mirror` action (retired-origin mirror checkout) are removed.
- Private module access: `secrets.MUXCORE_CI_TOKEN` (already present per repo; verified on `admin-ui`) configured via `git config url."https://x-access-token:${TOKEN}@github.com/".insteadOf "https://github.com/"` with `GOPRIVATE=github.com/Muxcore-Media/*`, `GONOSUMDB`/`GONOSUMCHECK` equivalents.
- Jobs: one job running `go build ./...`, `go vet ./...`, `go test -race -count=1 ./...` and `golangci-lint` (pinned version) — a single job to minimise minutes (NFR-COST-001).
- Triggers: `pull_request` and `push` to `main`, `paths-ignore` for docs-only changes, `concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }`.
- No Docker image build in per-module CI (image publishing is a separate, manually dispatched workflow — §5).

### 4.2 Umbrella CI

`umbrella/.github/workflows/ci.yml` (replaces the retired-origin workflow directory):

1. `checkout` with `submodules: recursive` using `MUXCORE_CI_TOKEN`.
2. **Hygiene checks:** retired-origin reference check (NFR-MNT-006), submodule-resolvability check (NFR-MNT-009), no-filesystem-replace check (NFR-MNT-007), secret scan (NFR-SEC-012), docs link check (§9).
3. **Workspace build:** generate `go.work`; `go build ./...` and `go vet ./...` for every active module (matrix in shards of ~20 modules to keep jobs < 30 min).
4. **Script tests:** `node --test scripts/*.test.mjs`, `_mvp/scripts/run-script-tests.sh`, `scripts/*_test.sh`, eslint.
5. **Integration:** `cd integration && go test -tags integration ./...` (after NFR-MNT-004).
6. Conventional-commit lint on PRs (`scripts/lint-commits.sh`).

### 4.3 Native and heavy jobs

macOS (iOS/tvOS) builds and container smoke run via `workflow_dispatch` only (BC-4 cost).

## 5. Packaging and release

- **Binaries:** each module tags `vX.Y.Z`; GitHub Releases carry linux/darwin amd64/arm64 builds and checksums for `core` (`muxcored`), `muxcorectl-cli`, and the installer.
- **Images:** built by `_mvp/scripts/publish-module-images.sh` from the household manifest. A LAN registry (`_mvp/local-registry.sh`, `MUXCORE_REGISTRY=<host>:5000/muxcore`) is available. Core's GHCR workflow has published with its `GITHUB_TOKEN`; central module-image publication still needs the T-M7-02 workflow with `packages: write`, private checkout credentials and a verified run ([ADR-0007](../adr/0007-install-paths-github-only.md), roadmap R-1/C-22). Every module listed in any compose profile must be in the publish set.
- **Release train:** one `MUXCORE_IMAGE_TAG` per umbrella release; installer, operator, starter, Helm, and compose read the same pin (FR-INS-006). The umbrella release is tagged `train-YYYY.MM.N`, matching committed `release-train.env` and pointing at the pinned SHAs ([ADR-0024](../adr/0024-umbrella-release-trains.md)). `scripts/prepare-release-train.sh` prepares deterministic Conventional Commit notes and a gitlink inventory without changing refs; GitHub Actions publishes the notes without writing back to `main`. Train releases are prereleases during the pre-1.0 beta; readiness remains subject to the ADR-0014 smoke gate and T-M7-03 checklist.

## 6. Data and migration strategy

- Keep per-module SQLite ([ADR-0009](../adr/0009-per-module-data-ownership.md)). Continue startup-time idempotent DDL but require each household module to have an **upgrade test**: start against a database created by the previous tagged release (fixture DB committed under `testdata/`) and assert schema + data survive (NFR-DATA-002).
- No down-migrations; rollback = restore the pre-upgrade backup (FR-INS-005, FR-BAK-004). The upgrade procedure therefore always takes a backup first.
- Postgres remains optional; there is no automated SQLite → Postgres migration and none is planned before v1.0.

## 7. Interfaces and documentation of APIs

- gRPC protos are normative; generated code is committed. `scripts/check-proto-drift.sh` detects consumers built against stale generated code and runs in umbrella CI.
- The BFF API is documented in `_mvp/BFF-API.md`; the document must cover every `/api/*` route group the SPA and native clients use (target: generated route inventory test that fails when a registered route is undocumented).
- `api-rest` exposes core only; no OpenAPI exists despite the CHANGELOG claim — an OpenAPI document for `api-rest` is **Could** priority (not required for v1.0).

## 8. Security hardening approach

Ordered by risk; each item maps to an SRS requirement and is delivered in milestone M3 unless noted.

| # | Change | Requirement |
|---|--------|-------------|
| 1 | Remove default credentials; installer/compose generate a random bootstrap password printed once; CI lint for known default literals | NFR-SEC-001, FR-INS-004 |
| 2 | Profiles: `dev` (insecure allowed, loud warning) vs `household` (TLS on mesh with core CA auto-issue; fixture acquisition; signatures required) — the household profile is the compose/installer default | NFR-SEC-003, FR-EXT-003, NFR-SEC-010 |
| 3 | Core: require mTLS client cert or bootstrap token for `Register` of security capabilities; bind CN to module ID; refuse duplicate providers of exclusive security capabilities | NFR-SEC-002 |
| 4 | BFF: CSRF (Origin/Referer check + SameSite), `POST /logout`, `http.Server` timeouts, security headers, persisted encrypted JSON sessions under its data directory | NFR-SEC-004/006, NFR-REL-003 |
| 5 | admin-ui: TLS-aware dial helper replacing 27 `insecure.NewCredentials()` sites; session store under data dir, encrypted bearer, 0600 | NFR-SEC-003/005 |
| 6 | request-media / tenant middleware: derive caller from authenticated context only | NFR-SEC-007 |
| 7 | Loopback-by-default or auth for scheduler-cron, health-monitor `/status`, core `/metrics` | NFR-SEC-011 |
| 8 | Restore/target path allow-list in admin-ui | FR-BAK-003, NFR-SEC-008 |
| 9 | Wire audit HMAC key when configured | NFR-OPS-004 |

## 9. Testing strategy

| Layer | Tooling | Gate |
|-------|---------|------|
| Unit | `go test -race`, Vitest | Per-repo CI |
| Contract drift | `scripts/check-proto-drift.sh` | Umbrella CI |
| Workspace build | `go.work` build/vet of all active modules | Umbrella CI |
| Integration (in-process) | `core/internal/integration` (`-tags integration`), root `integration/` | Umbrella CI |
| Stack smoke (fixture) | `_mvp/smoke.sh` (+ new fixture acquisition assertions, FR-INS-003/SM-1) | Release candidate (manual dispatch) |
| Consumer browser fixtures | Playwright Chromium against the built SPA and strict local HTTP fixtures; selected navigation/settings/invite views, keyboard focus and reduced-motion behavior at 375/768/1280/1920 px (ADR-0025) | Separate `media-ui-app/.github/workflows/browser.yml`; local evidence does not imply a successful GitHub run or full WCAG acceptance |
| Full E2E journeys | **Pending:** Playwright against a real fixture stack for J-02, J-03, J-08; browser HTTP fixtures do not prove auth, acquisition, playback or parental enforcement | Release candidate |
| Accessibility | `vitest-axe` in SPA; Go-rendered admin DOM fixtures + axe + jsdom keyboard tests; retain `templ/a11y_test.go` (ADR-0025) | SPA unit CI plus separate admin `accessibility.yml`; contrast and other browser/manual WCAG checks remain pending |
| Docs | Link/ID checker `scripts/check-docs.mjs` (relative links resolve; every `FR/NFR` ID cited exists in SRS) | Umbrella CI |

## 10. Migration considerations for this plan

- **Retired-origin removal:** delete the old workflow directories, the mirror/CI/registry scripts written for the self-hosted origin, the `checkout-mirror` action, and doc references. Historical records (`_wave1/`, dated audits) are kept but bannered as historical; they are excluded from the reference check by path.
- **Tags:** versions that existed only on the retired origin are either restored on GitHub (when a published `go.sum` hash identifies the exact commit) or given resolution tags ([ADR-0011](../adr/0011-go-workspace-and-published-versions.md)); consumers move to new GitHub tags.
- **Homelab specifics** (vault/desk/dawn/dusk hosts, ZeroTier, NixOS paths) are the owner's operator environment, not product. They are removed from product docs (specs, wiki, install guides). The owner's reference deployment notes stay in the root `AGENTS.md` under "Owner reference deployment", clearly separated from product requirements.
