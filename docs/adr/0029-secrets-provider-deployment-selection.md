# ADR-0029: Select the secrets provider explicitly in deployment configuration

- **Status:** Accepted
- **Date:** 2026-10-07
- **Decided by:** Delegated

## Context

T-M5-07 / issue #151 provides the external secrets option in the registry stack.
Both `secrets-file` and `secrets-vault` advertise `secrets`, but that capability is
not in core's exclusive-security-capability list. The existing compose profile
adds `secrets-vault` while leaving the default file service selected, and the
host runner likewise starts both when the optional flag is set. Discovery order
must not decide where a household's secrets are read or written.

## Decision

1. Keep `secrets-file` as the default provider. The supported external-provider
   compose path appends `docker-compose.secrets-vault.yml` last, after the source
   or registry stack and any dev override. It removes `secrets-file` using
   `!reset null`, clears the external provider's optional profile and requires an
   explicit `SECRETS_BACKEND` only when that overlay is selected.
2. Require Compose `!reset` support for this overlay path; it is verified with
   Docker Compose 2.40.3. Earlier versions and Podman Compose remain unverified;
   the existing dev override's separate `!override` feature does not establish
   the minimum for service removal. Preserve existing module IDs, enrollment
   variables, identities and volumes. The raw
   `--profile secrets-vault` path remains additive and unsupported for exclusive
   selection; it must not be documented as equivalent to the overlay.
3. The dev host runner's existing `MVP_ENABLE_SECRETS_VAULT=1` selects the external
   provider instead of the file provider. Before stopping or starting services,
   preflight rejects a missing backend, attempts to restart an unselected
   provider, or an already running opposing provider.
4. Selection is a deployment invariant, not new core registry enforcement.
   Operators must explicitly stop the old provider before switching or rolling
   back. Compose `up` alone does not stop an orphaned service removed by an
   override. Existing volumes are retained; moving secrets between backends is
   manual and must be completed before dependent modules are restarted.
5. Render/launcher fixtures verify supported-path selection, missing-backend
   rejection and preservation of default file behavior. They do not establish
   live external-provider connectivity, migrate real secrets, or satisfy the
   full household stack gate.

## Alternatives

- **Use the existing profile alone:** starts both providers and leaves discovery
  ambiguous; documenting a manual stop without fixing selection is insufficient.
- **Make secrets globally exclusive in core:** changes the mesh contract and
  provider lifecycle beyond the existing deployment task.
- **Silently choose a default external backend:** can start the wrong provider
  after an incomplete configuration or switch.
- **Automatically delete the old service's data on switch:** makes rollback and
  manual migration destructive and is unnecessary for provider selection.

## Consequences

FR-INS-008 traces this existing optional deployment scope. File-backed installs
retain their default behavior. External-provider operators use an explicit
overlay/flag, deliberate stop-switch-start procedure and backend-specific
credentials; no paid service or hosted dependency becomes required.
