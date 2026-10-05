# ADR-0013: Backup coverage model for the household install

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated

## Context

FR-BAK-002 requires every stateful module in the household manifest to be covered by backup. FR-BAK-004 requires an automated restore drill. Two mechanisms exist today:

- **`contracts.Backupable.ExportState`:** backup-local only calls it for peers in the same process. The sidecar binary has no peers, and no mesh-callable backup service exists.
- **Directory archives:** backup-local archives the directories listed in `BACKUP_SOURCE_DIRS`.

In the registry compose, several stateful modules wrote their databases into the image filesystem with no volume, and nothing declared which state each module owns.

## Decision

1. **Declaration in the manifest.** `_mvp/household-manifest.yaml` is the source of truth for state. A `state:` map gives each stateful module its named volume, mount path, the env vars that point into it, its kind (sqlite/files/json), and whether it holds personal data. A `stateless:` list and an `excluded_volumes:` list cover the rest; library media (movies, shows, downloads) is not state. Optional per-entry fields:
   - `backup: false` — state that must not be archived, such as the encryption key file.
   - `shares` — a volume also mounted by another service, such as admin-ui reading media-ui's data.
   - `service` — the compose service name, when it differs from the module ID.
2. **Coverage means directory backup.** A module is covered when its compose service stores its state on the declared volume and backup-local mounts that volume read-only under `/source/<module-id>`. The volume must also be in backup-local's default `BACKUP_SOURCE_DIRS`. `_mvp/scripts/check-state-coverage.sh` enforces this in CI.
3. **Consistency.** Writers must be quiesced (their services stopped) for a consistent v1.0 backup. The restore drill does this. `ExportState` stays an in-process mechanism.
4. **Restore.** Archive entries are placed back into the module volumes, using the manifest mapping. backup-local can re-index archives copied into a fresh install.

## Alternatives

- **Per-module `muxcore.json` state declarations:** these need every module checkout, so the check can't run in the public mvp CI before B-1. They can be cross-checked later.
- **Mesh-callable `Backupable` gRPC service (the owning module runs `VACUUM INTO`):** gives consistent online backups, but needs a contract change in every stateful module. Deferred; revisit at M4.

## Consequences

- Backups require a short service stop, which is acceptable for a household install.
- New stateful modules must be added to the manifest, or the coverage check fails.
