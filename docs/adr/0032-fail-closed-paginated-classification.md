# ADR-0032: Reject inconsistent paginated classification responses

- **Status:** Accepted
- **Date:** 2026-10-08
- **Decided by:** Delegated
- **Supersedes in part:** [ADR-0031](0031-bff-enforcement-of-parental-policy.md)
  §2.1's instruction to drop failed rechecks, only for already-paginated
  `ListMovies` / `ListTVShows` results.
- **Refs:** FR-PLAY-007, T-M4-01 S5b

## Context

ADR-0031 requires the BFF to request the media module's narrowing
`classification_filter`, then re-evaluate every returned item and drop any that
fail. The media module paginates its filtered results and returns a global
`total`. If a returned row fails the same policy in the BFF, that total is no
longer a reliable count of visible items. Dropping rows from one page cannot
repair a global count: the BFF does not know which unseen rows also fail.
Forwarding the original total can count hidden items, while subtracting only
the rejected rows can still expose an incorrect count.

This decision covers that inconsistency during S5b implementation. It does not
assert that the source work has passed verification.

Existing library paths do not by themselves establish complete enumeration:
the baseline scan caps at ten pages of 100 rows, and tag-based selection uses
the first page and map iteration order. A partial scan cannot support an exact
visible total or stable local pagination.

## Decision

1. For a restricted principal, the BFF requests the published
   `classification_filter` on paginated `ListMovies` and `ListTVShows` calls
   and re-evaluates each returned row under the same policy.
2. If any returned row fails that recheck, including unavailable or malformed
   classification, reject the entire response with HTTP **503** and the generic
   code **`parental.classification_unavailable`**. Set
   **`Cache-Control: no-store`**. Return neither rows nor the upstream total,
   and include no item metadata in the error.
3. If every returned row passes, preserve the module's filtered pagination and
   total. The module remains responsible for filtering the full result set
   before pagination; the BFF recheck does not prove unseen rows or totals.
4. For collections or library results, drop denied items and recompute visible
   totals **before** local pagination only after enumeration has completed.
   Apply deterministic ordering before pagination. If completion cannot be
   established within a safe enumeration bound, fail closed with the same
   generic no-store 503 rather than report a partial count as an exact total.
   This does not assert that existing library scans already enumerate fully.
5. This changes only ADR-0031 §2.1's failed-recheck behavior for already-paginated
   upstream results. The policy, evaluator, classification authority, unrestricted
   behavior, detail/playback denials, other failure handling and cache bounds
   remain governed by ADR-0030/0031. No module contract or release-train change
   is introduced.

## Alternatives

- **Drop failed rows and retain the upstream total:** may count hidden items and
  contradict the visible result set.
- **Subtract the number dropped from this page:** cannot account for rejected
  items on other pages, so it cannot establish the correct total.
- **Fetch every upstream page to recalculate the total:** changes the bounded
  paginated request into a full enumeration and does not provide a consistent
  snapshot across those calls. It is outside this decision.
- **Trust the module without rechecking:** removes ADR-0031's independent BFF
  check of the returned classification.

## Consequences and verification

An inconsistent page becomes a retryable service failure, even if some rows
would be visible. This preserves the response boundary without inventing a
total. Empty filtered pages and correctly narrowed pages remain successful.
FR-PLAY-007 stays Part; S9 transport, rollout, restricted-account integration and
the other ADR-0031 acceptance limits remain open.

Required fixtures must prove:

- Both paginated media calls receive the policy's narrowing filter; successful
  pages retain their filtered items, pagination and total.
- A page containing both allowed and denied rows returns the generic no-store
  503 with neither rows nor total. Unavailable, malformed and tag-denied
  classifications exercise the same boundary; include an upstream total larger
  than the returned page to guard against page-local subtraction.
- Completed collections/library enumerations drop denied rows, recompute totals
  over all visible items and paginate only afterward in deterministic order.
  Incomplete scans, including those stopped at a safe bound, return neither
  rows nor a partial total. Include multi-page and tag-selected fixtures that
  would fail under the baseline cap or first-page-only behavior.
- Unrestricted responses preserve their existing behavior, and policy/provider
  failures retain ADR-0031's failure handling.

Mutation checks must catch retaining the upstream total after dropping a failed
row or replacing the rejection with page-local total subtraction. Passing these
fixtures establishes only this paginated-response behavior, not live rollout or
complete parental enforcement.
