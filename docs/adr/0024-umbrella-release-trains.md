# ADR-0024: Umbrella release-train tags and deterministic notes

- **Status:** Accepted
- **Date:** 2026-10-07
- **Decided by:** Delegated

## Context

T-M7-01 and TDD §5 call for coordinated `train-YYYY.MM.N` umbrella releases.
`release-train.env` already names a train and the shared install pins (FR-INS-006),
but the umbrella release workflow and CONTRIBUTING still support only `v*`
tags. Generic previous-tag selection can mix semver releases and train releases.
The old workflow also commits a generated changelog to `main` after tagging,
so the published tag does not contain that update and protected branches can
silently prevent the push.

## Decision

1. Coordinated umbrella releases use `train-YYYY.MM.N` and must match
   `MUXCORE_TRAIN` in `release-train.env` at the tagged commit. Module semver
   versioning is unchanged (ADR-0011); historical umbrella `v*` releases remain
   supported. Published tags remain immutable (TDD §3).
2. `scripts/prepare-release-train.sh` is an offline, read-only preparation step.
   It reads the four manifest values and the gitlinks from the selected commit,
   validates the train and pins, and rejects an existing train tag that points
   elsewhere. It never executes the manifest, fetches submodules, creates tags,
   pushes refs, or declares the train verified.
3. Train notes contain Conventional Commit entries since the preceding train on
   the first-parent history, followed by version pins, the umbrella commit and
   all submodule gitlink SHAs. Semver notes select preceding `v*` tags separately.
   With no preceding tag, notes include the entire history including its root
   commit. Full umbrella Git history is required for train preparation.
4. The tag-triggered GitHub-hosted release workflow runs offline tooling fixtures,
   validates the train against committed data and publishes the notes using its
   `contents: write` workflow token. It does not push changes to `main` or require
   submodule or package access. Changelog changes follow the normal review path.
5. Train releases are GitHub prereleases during the pre-1.0 beta (BRD BC-6).
   Generating or publishing notes does not satisfy T-M7-03 or authorize skipping
   the fixture smoke release gate (ADR-0014). Promotion to a v1.0 release remains
   subject to that checklist. No GHCR workflow is added by this decision.

## Alternatives

- **Keep only `v*` umbrella tags:** conflicts with the canonical roadmap and the
  existing train manifest; module semver tags already serve module releases.
- **Fetch module histories to aggregate their changelogs:** adds private-repo
  credentials and missing-history failure modes to notes generation. Umbrella
  Conventional Commits and the exact gitlink inventory are sufficient for this
  task; module releases retain their own notes.
- **Continue a post-tag changelog commit to `main`:** makes the tag and changelog
  diverge and introduces an avoidable protected-branch write from the release job.

## Consequences

- Notes reproduce from an immutable umbrella commit without checking out modules.
  Dirty working-tree changes do not affect a tagged release's notes.
- The release operator still checks complete install pins and records passing
  smoke/CI evidence before creating and pushing the reviewed tag. Missing-module
  warnings from `check-pins.sh` are not complete validation.
- GitHub Actions availability can still block publication (roadmap B-2); local
  preparation and fixture verification remain available independently.
