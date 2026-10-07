# ADR-0027: Transcoder HTTP uses the BFF's verified mesh identity

- **Status:** Accepted
- **Date:** 2026-10-07
- **Decided by:** Delegated (records the shipped server boundary and its BFF wiring)

## Context

The transcoder's playback HTTP API can fetch source URLs and read media files.
Its server already restricts household callers to verified mesh certificates,
with `TRANSCODER_HTTP_ALLOWED_CALLERS` defaulting to the `media-ui` module CN.
The BFF previously used plain HTTP and accepted broader playback source strings.
The security fan-out therefore could not be integrated into the household stack
without completing both transport and source confinement (NFR-SEC-003/008/009).

## Decision

1. In household mode, the BFF connects to the transcoder over HTTPS using its
   enrolled mesh certificate/key and trusted CA, shared with its gRPC TLS
   configuration (ADR-0017). All four transcoder HTTP proxy paths and the
   capability health probe use this transport. Plain HTTP is rejected outside
   explicitly insecure dev operation.
2. The server's verified-certificate CN allow-list authorizes the calling module;
   client-supplied identity headers do not. The BFF strips browser identity and
   bearer headers from the proxied request. This service boundary complements
   the BFF's end-user authorization; it does not delegate user policy to the
   transcoder or make arbitrary user credentials service credentials.
3. Dev containers may use plain HTTP with an operator-supplied shared
   `TRANSCODER_HTTP_TOKEN` on the BFF and transcoder. The dev compose requires it,
   and the clean-room script generates and masks a random token. Loopback host
   development retains its existing loopback listener. No token is shipped.
4. Browser playback sources must be relative `/stream/movies/...` or
   `/stream/tv/...` paths. External URLs, local paths, query/fragment additions,
   traversal and encoded separator variants are rejected before proxying or
   direct fallback. The BFF resolves the accepted path against configured library
   origins; the transcoder's URL host allow-list and media-root confinement are
   also configured from the deployment's mounts and service origins.
5. An explicitly empty `TRANSCODER_HTTP_URL` disables this optional peer, including
   Kubernetes deployments that do not deploy a transcoder. Security wiring is
   added only for modules actually present in those deployment manifests.

## Alternatives

- **Forward the browser bearer to the transcoder:** confuses the end-user and
  module trust boundaries and bypasses the established mesh identity model.
- **Disable certificate verification to restore playback:** violates household
  TLS requirements and defeats caller authentication.
- **Allow arbitrary source strings because the server has guards:** unnecessarily
  broadens the BFF API and leaves fallback behavior outside its intended contract.

## Consequences

The BFF must enroll before household transcoder access succeeds, and dev container
operators must provide the shared token. Local handler/transport tests and compose
renders establish the wiring, while a real household/dev fixture smoke is still
required before a new train is described as deployable (ADR-0014).
