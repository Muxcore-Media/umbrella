# Contributing to MuxCore

Thank you for contributing to the MuxCore umbrella workspace. This document covers
commit conventions, changelog expectations, and the release process.

## Development setup

See agent rules are held outside this repository for the reading order (roadmap and specs first), toolchain
notes, and private Go module configuration. Submodule modules may have their own `CONTRIBUTING.md`
files with module-specific build steps.

## Commit messages

All commits on `main` and pull requests must use
[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```text
<type>(<optional-scope>): <description>
```

| Type | Use for |
| ---- | ------- |
| `feat` | New user-facing capability |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, no behavior change |
| `refactor` | Code restructure without feature/fix |
| `perf` | Performance improvement |
| `test` | Tests only |
| `build` | Build system or dependencies |
| `ci` | CI configuration |
| `chore` | Maintenance (submodule bumps, tooling) |
| `revert` | Revert a previous commit |

Examples:

```text
feat(auth): add invite expiry policy
fix(media-ui): correct queue sort order
chore: bump media-ui-app submodule
docs: document vault deploy loop
```

### Local commit lint

Before opening a PR, lint commits against `main`:

```bash
./scripts/lint-commits.sh origin/main HEAD
```

CI runs the same check in `.github/workflows/commitlint.yml`.

## Changelog

[`CHANGELOG.md`](CHANGELOG.md) follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). The `[Unreleased]` section
tracks work in progress. Release notes are generated from conventional commits.
The release workflow publishes those notes without committing back to `main`;
changelog edits follow the normal review process
([ADR-0024](docs/adr/0024-umbrella-release-trains.md)).

Preview what the next release notes would look like:

```bash
./scripts/generate-changelog.sh --version Unreleased
```

Generate notes for a specific tag range:

```bash
./scripts/generate-changelog.sh --from v0.1.0 --to v0.2.0 --version v0.2.0
```

Commit types map to changelog sections:

| Commit type | Changelog section |
| ----------- | ----------------- |
| `feat` | Added |
| `fix` | Fixed |
| `perf`, `refactor`, `docs`, `build`, `ci`, `test`, `chore`, `revert`, `style` | Changed |

## Release process

Coordinated umbrella releases use `train-YYYY.MM.N` tags (for example
`train-2026.10.3`), matching `MUXCORE_TRAIN` in the committed
[`release-train.env`](release-train.env). Individual modules keep their own
`vX.Y.Z` semver releases. Historical umbrella `v*` tags remain supported by the
release workflow.

### 1. Prepare

1. Confirm the intended module SHAs and update `release-train.env` and all install
   pins together. Use a new train identifier; published tags are immutable.
2. Run `scripts/check-pins.sh` from a complete checkout. A warning about a missing
   submodule means that path was not checked and is not release evidence.
3. Verify the normal CI checks and the compose fixture smoke described in
   [ADR-0014](docs/adr/0014-end-to-end-gate-placement.md). Record actual results and
   outstanding CI blockers in the roadmap; generated notes do not establish
   release readiness or satisfy the v1.0 RC checklist.
4. Commit the train metadata and gitlinks, then prepare the exact commit to tag:

```bash
release_commit=$(git rev-parse main)
bash scripts/prepare-release-train.sh --ref "$release_commit" --output /tmp/muxcore-release-notes.md
```

Review the notes, train pins, and full module commit table. The helper reads only
committed data and needs full umbrella Git history, but no submodule checkouts or
network. Local uncommitted changes are not included. It creates no tag or release.
The manifest accepts only the four documented unquoted assignments; it is parsed
as data and never executed by this helper.

### 2. Tag and push

```bash
train=$(git show "$release_commit:release-train.env" | sed -n 's/^MUXCORE_TRAIN=//p')
bash scripts/prepare-release-train.sh --ref "$release_commit" --tag "$train" --output /tmp/muxcore-release-notes.md
git tag -a "$train" "$release_commit" -m "$train"
git push origin "refs/tags/$train"
```

Pushed tags trigger `.github/workflows/release.yml`, which:

1. Runs the offline release-tooling fixtures and checks that the tag matches the
   committed `MUXCORE_TRAIN`. Local preparation refuses a conflicting existing
   tag; publish new tags without force, preserving their immutability.
2. Generates notes since the previous train on the first-parent history, ignoring
   unrelated semver tags and tags on merged feature branches. The first train
   includes the initial commit. The notes also record the committed version pins
   and submodule SHAs.
3. Creates a GitHub prerelease on `Muxcore-Media/umbrella`. Train releases stay
   marked as prereleases during the pre-1.0 beta; they do not declare v1.0 readiness.

The workflow uses `GITHUB_TOKEN` with `contents: write` and never changes `main`.
Its notes do not require private submodule access, package permissions, or GHCR.
Historical umbrella `v*` releases generate notes between `v*` tags; a semver tag
containing a prerelease suffix is also marked as a GitHub prerelease.

### 3. Verify

After the workflow completes:

```bash
gh release view "$train" --repo Muxcore-Media/umbrella
```

Confirm the published notes match
`bash scripts/prepare-release-train.sh --ref "$train" --tag "$train"`.

### Submodule-only changes

Most day-to-day work lands in individual module repos. Bump the umbrella gitlink
with a conventional commit:

```bash
cd media-ui-app
# … commit and push module changes …
cd ..
git add media-ui-app
git commit -m "chore: bump media-ui-app"
```

Only cut an umbrella tag when you want a coordinated workspace release.

## Script tests

Offline tests for release automation scripts:

```bash
bash scripts/generate-changelog_test.sh
bash scripts/prepare-release-train_test.sh
bash scripts/lint-commits_test.sh
```

Umbrella CI runs all `scripts/*_test.sh` fixtures; the release workflow also runs
the changelog and release-train fixtures before publishing.

## Pull requests

1. Branch from `main`.
2. Use conventional commits for every commit in the PR.
3. Run relevant module tests and `./scripts/lint-commits.sh`.
4. Open a PR; commit lint runs automatically.
5. After merge, update `[Unreleased]` in `CHANGELOG.md` when the change is user-visible.
