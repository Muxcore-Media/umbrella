# ADR-0025: Rendered accessibility and consumer browser fixture gates

- **Status:** Accepted
- **Date:** 2026-10-07
- **Decided by:** Delegated

## Context

T-M4-05 adds automated accessibility evidence for both UIs; T-M4-06 adds browser
coverage for household journeys and viewport behavior. Existing consumer
`vitest-axe` checks run in jsdom, and admin-ui already has static accessibility
assertions in `templ/a11y_test.go`. Neither establishes WCAG 2.1 AA compliance or
the full fixture-stack journeys required by M4. The generated module CI workflows
must remain synchronized with the umbrella templates (ADR-0003).

## Decision

1. Keep the generated `.github/workflows/ci.yml` files unchanged. Add separate
   GitHub-hosted workflows for UI verification, with cancellation of superseded
   runs and filters that avoid documentation-only work.
2. Admin-ui renders thirteen actual Go template documents into deterministic
   fixtures. Go DOM checks validate names, labels, IDs, ARIA references and
   landmarks. Development-only jsdom 26 and axe-core 4 dependencies audit those
   documents; a negative control proves missing image/button names are detected.
   Only axe `color-contrast` is disabled in this harness because jsdom has no
   layout/paint engine; `region` remains enabled. Separate jsdom keyboard tests
   exercise the shipped navigation script. The workflow uses Node 24 and the
   module's Go version and requires the existing private-module credentials.
3. The consumer SPA uses Playwright 1.63 with Chromium against its production
   build and deterministic local HTTP fixtures. Development BFF proxies are
   disabled. Unknown API or external requests and uncaught browser errors fail
   the tests. No live services, indexers, credentials or household data are used.
4. Browser coverage runs at 375, 768, 1280 and 1920 pixel widths. Assertions cover
   Home/navigation, keyboard browse-to-detail and play-link behavior, personal
   Settings and operator-link absence, and valid/invalid invite UI behavior.
   Screenshots, traces and reports are diagnostic artifacts, retained for seven
   days in CI; screenshots are not visual regression baselines. The workflow
   installs Playwright's pinned Chromium on Node 22. A local executable override
   allows verification with an installed Chromium, with its version recorded.
5. These are complementary gates. They do not close full J-02 login/session,
   J-03 acquisition/import/playback/resume, J-08 server-side parental enforcement,
   or all layouts. Contrast, visual focus, reduced-motion and assistive-technology
   browser checks remain outstanding under NFR-A11Y-001/002. The compose fixture
   release gate (ADR-0014) remains unchanged.

## Alternatives

- **Only attribute assertions:** cannot detect cross-component ID collisions or
  exercise keyboard behavior in the shipped scripts.
- **Only a mocked browser:** provides layout and interaction evidence but cannot
  establish backend authorization, session or acquisition behavior.
- **Modify the generated CI templates for these two modules:** imposes UI-specific
  tools on unrelated modules and risks losing custom steps during template sync.
- **Provision the whole media stack for each UI edit:** useful for final journey
  acceptance but unnecessary for these deterministic UI regression checks.

## Consequences

The UIs acquire development-only test dependencies and short supplementary jobs.
Local passing results and unobserved GitHub runs are recorded separately. Broad
accessibility and journey requirements remain partial until their remaining
checks run; no compliance or household-ready claim follows from these gates.
