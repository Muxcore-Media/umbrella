# ADR-0023: Mesh CA and module identities are not backed up

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated
- **Extends:** [ADR-0013](0013-backup-coverage-model.md), [ADR-0017](0017-mesh-module-identity.md)

## Context

In the `household` profile, core keeps its mesh CA on the `core-ca` volume. Each module keeps its enrolled certificate and key on a `<svc>-id` volume, and the public CA is exported to `mesh-ca`. ADR-0013 backs up state volumes. Archiving key material would put long-lived private keys into every backup. Restoring an older CA would also invalidate certificates that modules enrolled after the backup was taken.

## Decision

1. `core-ca`, `mesh-ca` and every `<svc>-id` volume are listed under `excluded_volumes` in the household manifest, as key material. backup-local never mounts them.
2. A restore replaces module data only. Mesh identities and the CA stay on their volumes, so after a restore every module reconnects with its existing certificate. The compose restore drill verifies this.
3. Disaster recovery onto a new host, where all volumes are lost, starts from a fresh CA:
   - The operator regenerates enrollment tokens with `_mvp/scripts/gen-enrollment.sh` (`muxcored enroll reset <id>` for reused IDs) and starts the stack.
   - Modules enroll again.
   - The data restore then proceeds as in ADR-0013.
4. `secrets-file`'s master key (`SECRETS_MASTER_KEY`) likewise stays outside archives (operator environment).

## Consequences

Backups contain no private mesh keys. Losing the CA volume needs re-enrollment, documented in `_mvp/docs/RESTORE.md`, but no data loss.
