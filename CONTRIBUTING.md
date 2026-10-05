# Contributing to MuxCore

Thank you for contributing to the MuxCore umbrella workspace. This document covers
commit conventions, changelog expectations, and the release process.

## Development setup

See [`AGENTS.md`](AGENTS.md) for the reading order (roadmap and specs first), toolchain
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
tracks work in progress. Release sections are generated from conventional commits.

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

Releases are tagged on the umbrella repo with `v*` semver tags (for example `v0.3.0`).

### 1. Prepare

1. Ensure `main` is green in GitHub Actions (`.github/workflows/ci.yml`).
2. Confirm submodule gitlinks on `main` reflect the intended module SHAs.
3. Review `./scripts/generate-changelog.sh --version vX.Y.Z` output locally.

### 2. Tag and push

```bash
git checkout main
git pull
git tag -a vX.Y.Z -m "vX.Y.Z"
git push origin vX.Y.Z
```

Pushed tags trigger `.github/workflows/release.yml`, which:

1. Generates release notes from conventional commits since the previous tag.
2. Inserts a new section into `CHANGELOG.md` (before `[Unreleased]`).
3. Creates a GitHub Release on `Muxcore-Media/umbrella` with the generated notes.

The workflow uses `GITHUB_TOKEN` with `contents: write`.

### 3. Verify

After the workflow completes:

```bash
gh release view vX.Y.Z --repo Muxcore-Media/umbrella
```

Confirm release notes match `./scripts/generate-changelog.sh --from <prev> --to vX.Y.Z --version vX.Y.Z`.

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
bash scripts/lint-commits_test.sh
```

These are also run from `_mvp/scripts/run-script-tests.sh`.

## Pull requests

1. Branch from `main`.
2. Use conventional commits for every commit in the PR.
3. Run relevant module tests and `./scripts/lint-commits.sh`.
4. Open a PR; commit lint runs automatically.
5. After merge, update `[Unreleased]` in `CHANGELOG.md` when the change is user-visible.
