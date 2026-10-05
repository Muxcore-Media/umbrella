# ADR-0003: CI on GitHub-hosted runners from a shared template

- **Status:** Accepted
- **Date:** 2026-10-05
- **Decided by:** Delegated (follows Owner [ADR-0002](0002-github-sole-origin.md))

## Context

Module workflows use `runs-on: self-hosted` and a `Muxcore-Media/mvp/.github/actions/checkout-mirror` action that checks out via the Forgejo mirror. On 2026-10-05 recent runs were `queued` or `cancelled` (e.g. `media-automation`, `media-ui-app`), i.e. no runner is serving them. The organisation is on the GitHub Free plan with private repositories (BC-4), so hosted minutes are limited. A per-repo secret `MUXCORE_CI_TOKEN` exists for private-module fetches.

## Decision

- All repositories use **GitHub-hosted `ubuntu-latest`** runners with plain `actions/checkout`.
- Module workflows are **generated from templates in the umbrella** (`scripts/ci-templates/`) by `scripts/sync-ci.sh`, so they stay uniform.
- One job per module run (build + vet + test + lint), `concurrency` cancel-in-progress, docs-only changes skipped, macOS and container-smoke jobs `workflow_dispatch` only.
- The umbrella runs workspace-level checks (hygiene, `go.work` build, script tests, integration).

## Alternatives

- **Keep self-hosted runners** — rejected: no runner is online; ties CI to homelab hardware the owner asked to stop focusing on.
- **Reusable workflow in a central repo** — viable later; requires changing each private repo's Actions access level. Templates + sync script work today with no settings changes.

## Consequences

- CI minutes are consumed from the Free-plan quota; risk BR-1 is tracked. If the quota is exhausted, the fallback is to run per-module CI only on PRs and rely on the umbrella workspace job for `main`.
- Image publishing and native builds are not part of default CI.

## Revisit when

Minutes are routinely exhausted, the org upgrades its plan, or a self-hosted runner is reintroduced on GitHub.
