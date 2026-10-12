# Architecture Decision Records

**Owns:** individual records of architecturally significant decisions — context, alternatives, rationale, consequences, status, and supersession.

## Rules

1. One decision per file: `NNNN-kebab-title.md`, numbered sequentially. Never renumber or delete.
2. **Status** is one of `Proposed`, `Accepted`, `Superseded by ADR-NNNN`, `Deprecated`. To change a decision, write a new ADR that supersedes the old one and update only the old one's status line and a "Superseded by" link — do not rewrite its body.
3. **Decided by** distinguishes who made the call:
   - `Owner` — the project owner (Zemdregon) decided it; cite the source (roadmap section, chat instruction with date).
   - `Delegated` — made by an agent under the owner's standing delegation ("you are the final authority", 2026-10-05). Not owner-approved; the owner may overturn it with a superseding ADR.
   - `Historical` — recorded retroactively from existing implementation; no decision record existed before.
4. Write an ADR when a change affects module boundaries, contracts, data ownership, security model, build/release/CI model, or a product commitment. Do not write ADRs for minor implementation preferences.

## Index

| ADR | Title | Status | Decided by |
|-----|-------|--------|-----------|
| [0001](0001-canonical-roadmap-and-spec-set.md) | Canonical roadmap location and specification set | Accepted | Delegated (2026-10-05) |
| [0002](0002-github-sole-origin.md) | GitHub is the sole origin; Forgejo / git.zem.systems retired | Accepted | Owner (2026-10-05) |
| [0003](0003-ci-on-github-hosted-runners.md) | CI on GitHub-hosted runners from a shared template | Accepted | Delegated (2026-10-05) |
| [0004](0004-modular-sidecar-architecture.md) | Core runtime + capability modules as sidecar processes over gRPC | Accepted | Historical |
| [0005](0005-consumer-ui-replaces-jellyfin-web.md) | `media-ui-app` replaces Jellyfin web; Jellyfin handoff near-term | Accepted | Owner (MASTER-ROADMAP §1, pre-2026-10) |
| [0006](0006-registry-compose-install-path.md) | Registry compose (Path A) is the household install path | Superseded by 0007 | Owner (MASTER-ROADMAP §1) |
| [0007](0007-install-paths-github-only.md) | Install paths with GitHub as the only origin | Accepted | Delegated (2026-10-05), follows Owner ADR-0002 |
| [0008](0008-fixture-only-acquisition-gate.md) | Fixture-only acquisition gate | Accepted | Owner (Appendix E) |
| [0009](0009-per-module-data-ownership.md) | Per-module data ownership (SQLite per module) | Accepted | Historical |
| [0010](0010-bff-direct-module-access.md) | BFF dials modules directly instead of via `ModuleMesh` | Accepted | Historical |
| [0011](0011-go-workspace-and-published-versions.md) | Published versions in `go.mod`, umbrella `go.work` for local dev; core v0.6.0 | Accepted | Delegated (2026-10-05) |
| [0012](0012-reproducible-module-builds-for-spool-checksums.md) | Reproducible module builds for spool checksums | Accepted | Delegated (2026-10-05) |
| [0013](0013-backup-coverage-model.md) | Backup coverage model for the household install | Accepted | Delegated (2026-10-05) |
| [0014](0014-end-to-end-gate-placement.md) | Placement of the end-to-end release gate | Accepted | Delegated (2026-10-05) |
| [0015](0015-upgrade-compatibility-window.md) | Upgrade compatibility window and schema fixtures | Accepted | Delegated (2026-10-05) |
| [0016](0016-security-profiles.md) | Security profiles (`dev` / `household`) | Accepted | Delegated (2026-10-05) |
| [0017](0017-mesh-module-identity.md) | Mesh module identity and enrollment | Accepted | Delegated (2026-10-05) |
| [0018](0018-authenticated-registration.md) | Authenticated registration and exclusive security capabilities | Superseded by 0022 | Delegated (2026-10-05) |
| [0019](0019-end-user-identity-propagation.md) | End-user identity propagation | Accepted | Delegated (2026-10-05) |
| [0020](0020-module-principal-allowlists.md) | Module-to-module calls without a user | Accepted | Delegated (2026-10-05) |
| [0021](0021-module-version-single-source.md) | A module's version has one source — its `muxcore.json` | Accepted | Delegated (2026-10-05) |
| [0022](0022-registration-rules-dev-restarts.md) | Registration rules, with dev-mode restarts | Accepted | Delegated (2026-10-05) |
| [0023](0023-mesh-identity-outside-backups.md) | Mesh CA and module identities are not backed up | Accepted | Delegated (2026-10-05) |
| [0024](0024-umbrella-release-trains.md) | Umbrella release-train tags and deterministic notes | Accepted | Delegated (2026-10-07) |
| [0025](0025-ui-verification-gates.md) | Rendered accessibility and consumer browser fixture gates | Accepted | Delegated (2026-10-07) |
| [0026](0026-administrative-session-contract.md) | Administrative session management belongs to the auth provider | Accepted | Delegated (2026-10-07) |
| [0027](0027-transcoder-http-mesh-boundary.md) | Transcoder HTTP uses the BFF's verified mesh identity | Accepted | Delegated (2026-10-07) |
| [0028](0028-spool-dial-time-destination-policy.md) | Spool HTTP destinations are checked at connection time | Accepted | Delegated (2026-10-07) |
| [0029](0029-secrets-provider-deployment-selection.md) | Select the secrets provider explicitly in deployment configuration | Accepted | Delegated (2026-10-07) |
| [0030](0030-authoritative-parental-policy-resource.md) | Parental policy has a separate authenticated provider resource | Accepted | Delegated (2026-10-07) |
| [0031](0031-bff-enforcement-of-parental-policy.md) | The BFF is the single enforcement point for the authoritative parental policy | Accepted | Delegated (2026-10-08) |
| [0032](0032-fail-closed-paginated-classification.md) | Reject inconsistent paginated classification responses; narrowly supersedes 0031 §2.1 for paginated results | Accepted | Delegated (2026-10-08) |
| [0033](0033-userdata-http-mesh-transport.md) | Userdata HTTP authenticates both provider and calling module; narrowly supersedes 0016 Decision 2 for direct userdata HTTP | Accepted | Delegated (2026-10-08) |
| [0035](0035-user-erasure-ledger.md) | User erasure is a durable identity-provider ledger reconciled by every personal-data owner (0034 is reserved for the request-only permission model) | Accepted | Delegated (2026-10-09) |
| [0036](0036-invite-creator-authority-and-erasure.md) | Bind user-created invites to current creator authority and serialize creation with erasure | Accepted | Delegated (2026-10-11) |
| [0037](0037-conditional-userdata-mutations.md) | Conditional userdata mutations and coordinated writer migration | Proposed | Delegated proposal (2026-10-11; not accepted) |
| [0038](0038-additive-household-userdata-mutations.md) | Additive household userdata mutations: bounded five-family provider HTTP contract | Accepted | Delegated (2026-10-12; root and independent contract review) |
