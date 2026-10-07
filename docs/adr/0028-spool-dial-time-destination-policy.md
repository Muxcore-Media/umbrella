# ADR-0028: Spool HTTP destinations are checked at connection time

- **Status:** Accepted
- **Date:** 2026-10-07
- **Decided by:** Delegated

## Context

NFR-SEC-009 / RULE-VAL-2 includes core's spool fetcher among the remaining outbound
URL guards. An HTTPS scheme and textual host allow-list do not prevent an allowed
name from resolving to a forbidden address, changing resolution between checks
and connection, or redirecting to a forbidden destination. Operators also use
explicitly allowed LAN spools, so rejecting every private address would remove
an existing configuration path.

## Decision

1. Core's spool HTTP transport enforces the URL and destination policy itself,
   without importing the module SDK or creating a core-to-SDK dependency cycle.
   URLs must use HTTPS and cannot contain userinfo or ambiguous IP encodings.
2. `MUXCORE_SPOOL_ALLOWED_HOSTS` continues matching the exact URL `Host`, including
   an explicitly written port. There are no wildcards, case folding or implicit
   port aliases. With an empty list, only public destinations are permitted.
   Explicit entries may resolve to private LAN, loopback or CGNAT addresses.
3. An allow-list entry never permits metadata endpoints, link-local, unspecified,
   multicast or zone-scoped addresses, or the fetcher's blocked special-use
   ranges. Classification includes IPv4
   mapped into IPv6, well-known NAT64 and 6to4 embedded IPv4. Teredo
   (`2001::/32`) and the reserved network-specific NAT64 range
   (`64:ff9b:1::/48`) are refused. Arbitrary operator-defined translation
   prefixes cannot be inferred without a separately configured translation
   policy.
4. Each new connection resolves once, validates the entire DNS answer, and dials
   only literal addresses from that checked answer. A mixed permitted/forbidden
   answer is rejected before any connection attempt. Address-family fallback may
   use another checked address; it must not trigger a second name resolution.
   HTTPS retains certificate verification against the original URL hostname.
5. Redirects repeat HTTPS, exact-host and destination checks, including guarded
   resolution at connection time. Existing request timeout, redirect ceiling and
   response-read bound remain unchanged. In-flight requests retain their original
   host-policy/client snapshot when `SetAllowedHosts` replaces the active policy.
6. This product transport does not use implicit `HTTP_PROXY`/`HTTPS_PROXY` settings:
   proxy-side resolution would defeat the checked-address connection guarantee.
   Operators who need a proxy require a separately designed, explicitly trusted
   proxy contract. This decision does not change the development environment's
   proxy, CA trust or network-access policy.

## Alternatives

- **Resolve before using the default transport:** leaves a second resolution
  between validation and dialing, including proxy-side resolution.
- **Reject every private address:** prevents intentional operator-configured LAN
  spools and changes the existing exact-host allow-list contract unnecessarily.
- **Trust every address behind an allowed name:** exposes metadata/link-local
  destinations and makes DNS changes bypass the intended boundary.
- **Import the module SDK guard into core:** introduces a dependency in the wrong
  direction; core already owns this fetcher and can enforce the same boundary
  with its existing standard-library dependencies.

## Consequences

Spool fetching now has an explicit address policy independent of module Git
repository host checks. DNS/redirect/address/fallback and policy-replacement
fixtures are required; no live spool fetch is needed to test those boundaries.
Completing this core slice does not close the remaining NFR-SEC-009 module audit.
