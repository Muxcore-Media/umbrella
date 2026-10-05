# ADR-0002: GitHub is the sole origin; Forgejo / git.zem.systems retired

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Owner — instruction on 2026-10-05: "disregard git.zem.systems, remove mentions of it, and only focus on github".

## Context

Earlier documents described a self-hosted Forgejo at `git.zem.systems` as the origin and CI host, with GitHub (`github.com/Muxcore-Media`) as a "future public mirror" (`.github/workflows/RETIRED.md`, `MASTER-ROADMAP.md` Appendix E, `_mvp/tls/FORGEJO-RUNNER.md`, ~1,500 references in ~320 files). In practice, issues, PRs, and Dependabot already ran on GitHub (`.project-management.md`; `gh issue list`), `.gitmodules` already pointed at GitHub, and on 2026-10-05 `git.zem.systems` served a GitLab sign-in page, not Forgejo. Some tags (`core v0.5.8`, `jellyfin v0.3.0`) and repository contents (`playback-contract`, `metadata-musicbrainz`, `media-ui-tv`, `muxcore-android`) exist only on the retired host.

## Decision

GitHub (`github.com/Muxcore-Media/*`) is the only source of truth for code, tags, releases, issues, and CI. All references to `git.zem.systems`, Forgejo runners, Forgejo mirrors, and Forgejo-based registries are removed from active content. Historical records that mention them are kept only when bannered as historical.

## Consequences

- `.forgejo/` workflow directories, Forgejo mirror/CI/registry scripts, and the `checkout-mirror` composite action are deleted.
- CI moves to GitHub Actions ([ADR-0003](0003-ci-on-github-hosted-runners.md)).
- Versions and content present only on Forgejo are treated as non-existent; dependants move to new GitHub tags ([ADR-0011](0011-go-workspace-and-published-versions.md)) and empty repos are rebuilt or removed ([TDD §2.1](../architecture/TDD.md#21-archived-and-empty-repositories)).
- The previous "install from Forgejo registry" path is gone ([ADR-0007](0007-install-paths-github-only.md) supersedes [ADR-0006](0006-registry-compose-install-path.md)).
- Appendix E's constraint "Treating `.github/workflows` as origin CI" is reversed.
