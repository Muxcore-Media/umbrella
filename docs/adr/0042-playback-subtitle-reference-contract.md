# ADR-0042: Playback subtitle references are opaque, server-minted and session-bound

- **Status:** Accepted
- **Date:** 2026-10-22
- **Decided by:** Delegated (executive decision by orchestrator)
- **Builds on:** [ADR-0031](0031-bff-enforcement-of-parental-policy.md) (subtitle track binding), [ADR-0009](0009-per-module-data-ownership.md), [ADR-0023](0023-mesh-identity-outside-backups.md)
- **Refs:** NFR-SEC-008, FR-PLAY-003, FRD RULE-VAL-1, roadmap T-M3-10

## Context

Static reading of `_mvp` pin `721af60`, `cmd/mediauiprox/` (no runtime probe):

- `GET /api/playback/subtitles` mints sidecar IDs as `"sc_" + base64url(absPath)`
  (`playback_subtitles.go:200-202`). `GET /api/playback/subtitles/{id}` decodes any `sc_` ID back to a
  path (`decodeSidecarTrackID`, `:204-213`) and serves it (`:302-305`). `serveSubtitleFile` (`:335-357`)
  only checks that the path is not a directory. There is no root, extension, size, symlink or
  "was this ever listed" check.
- The ADR-0031 binding (`parental_subtitles.go:58-187`) applies only to restricted principals
  (`parental_routes.go:591-597`, `parental_subtitles.go:208-219`). Unrestricted principals reach the
  handler directly (`parental_routes.go:577-579`).
- Discovery also trusts module rows: `resolveAbsMediaPath` returns absolute paths unchanged (`:54-60`)
  and episode `file_path` is used raw (`:117-124`).

**Result: any signed-in unrestricted member can read any file the BFF process can open.** In the
registry compose the `media-ui` service mounts `media-ui-id:/data/mesh-id` (the BFF's mesh private key),
`media-ui-data:/data/media-ui` (sessions, userdata, `password-resets.json`) and `mesh-ca`
(`docker-compose.registry.yml:1401-1474`), plus `/etc/*` in the image. Under `run-host.sh` it is the
whole host user's filesystem. Reading the mesh key lets a member impersonate `media-ui` on the mesh.
This is a high-severity arbitrary file read, not a hardening item.

## Decision

1. **Severity.** T-M3-10 is a security fix and takes priority over M4 feature work.
2. **Opaque references.** A sidecar reference never contains or encodes a path. The list route mints
   `sc_` + hex(first 16 bytes of HMAC-SHA-256(k, session ID ‖ resolved path)), where `k` is 32 random
   bytes generated at process start and never persisted. The serve route never decodes an ID; it looks
   the ID up in the server-side binding table. `decodeSidecarTrackID` and the decode branch are deleted.
3. **Binding for every principal.** `subtitleTrackBindings` gains the resolved path per binding and binds
   for every session, restricted or not (same 4 h sliding TTL, LRU and per-session caps). A lookup must
   match both session and ID. Restricted principals keep the per-fetch item reauthorization unchanged.
4. **Confinement**, applied at discovery and again at serve time on the bound path:
   - Paths are confined with `pathguard.Confine` (core `sdk/go/module/pathguard`, already required at
     `v0.6.7`, `_mvp/go.mod:15`): rejects `..` and NUL, resolves symlinks, compares by path component
     (root `/data/movies` does not admit `/data/movies2`).
   - The video path is confined first. A sidecar must sit in the same directory as its video, with a
     matching stem, and pass `sidecarOwnedBy` for every principal.
   - Extensions: `.srt`, `.vtt`, `.ass` only. Regular files only (no directory, FIFO, device, or a path
     that is a symlink after resolution). Size cap 5 MiB, checked on the opened handle.
   - The file is opened through `os.OpenRoot(<confined directory>)`, so a swap between check and read
     cannot leave the directory.
5. **Fail closed.** With no roots configured, sidecars are neither listed nor served; module-held tracks
   still work. A root that fails `Stat` (unmounted) yields an empty sidecar list and 404 on serve, with an
   ID-free, path-free warning log.
6. **Roots variable: new `MEDIA_UI_MEDIA_ROOTS`**, colon-separated absolute paths, parsed in `main.go`.
   At startup the BFF refuses the whole list (sidecars off, error logged) if any root is `/`, or equals,
   contains or sits inside `MUXCORE_TLS_DIR`, the mesh CA directory, `MEDIA_UI_USERDATA_DIR`, the
   directory of `MEDIA_UI_PASSWORD_RESET_FILE`, or `BACKUP_RESTORE_DIR`.
   **`SUBS_MEDIA_ROOTS` is not reused**, because:
   - each process owns its own configuration (ADR-0009) and `check-media-security-wiring_test.sh` already
     maps one roots variable per module;
   - the two processes see different mount namespaces (media-subtitles: `/data/movies:/data/shows`;
     media-ui: no library mount), so one value cannot be correct for both;
   - sharing it would silently widen the BFF's read reach whenever an operator widens subtitle roots.
   `run-host.sh` defaults `MEDIA_UI_MEDIA_ROOTS` to `$LIBRARY_ROOT:$TV_LIBRARY_ROOT` (not music).
7. **Module-held tracks** (IDs without `sc_`) keep proxying to media-subtitles by row ID; media-subtitles
   already confines with `SUBS_MEDIA_ROOTS` (`media-subtitles internal/module.go:117-127`).

## Client impact

- A reference that is unbound, expired, evicted, owned by another session, forged, or lost in a BFF
  restart returns **`404 subtitles.not_found`** for unrestricted principals and the existing
  **`403 parental.blocked`** for restricted principals (no new code, so restricted principals cannot
  tell "never existed" from "blocked"). No file is opened in either case.
- IDs change on BFF restart and differ per session. **Clients must treat IDs as opaque, never persist
  them across sessions, and on a 404 (or 403) for a subtitle fetch re-list tracks once and retry.**
- **SPA:** `media-ui-app` `VideoPlayer.tsx` must relist once on a stale track; if it does not, a paired
  PR ships with the BFF change.
- **iOS / tvOS risk:** `muxcore-ios` and `media-tvos-app` were not checked out in this workspace, so
  their behavior is unverified. If they cache track IDs and do not relist, a stale ID after a restart or
  4 h idle loses subtitles for that playback (video continues). Decision: the BFF fix is **not** held for
  them. A follow-up task is opened in each repo to relist-once on subtitle 404/403, and the release-train
  notes for the BFF change state the limitation until those land.

## Consequences and rollout

- **Compose:** `MEDIA_UI_MEDIA_ROOTS` stays unset in `docker-compose.registry.yml`, so sidecars are off
  there (the container has no library mount today) and the subtitle path can no longer reach
  `/data/mesh-id`, `/data/mesh-ca`, `/data/media-ui` or `/etc`. If library mounts are added later they
  must be read-only and mounted outside `/data` (for example `/media/movies:ro`); the startup overlap
  check in Decision 6 enforces the separation regardless.
- **run-host.sh:** sidecars keep working from the library roots; the BFF's own data and mesh-id
  directories are outside those roots and rejected by the overlap check.
- **Deploy:** normal S10 rollout. Not blocked by ADR-0031's parental ordering, because this only tightens
  access. Operators on affected builds should rotate the `media-ui` mesh identity after upgrading if the
  BFF was reachable by untrusted members (ADR-0017 enrollment).
- **Code:** `_mvp/cmd/mediauiprox/playback_subtitles.go`, `parental_subtitles.go`, `main.go` and their
  tests; `_mvp/run-host.sh`, `_mvp/BFF-API.md:519-520,611`, `_mvp/.env.example:458-460`.
- **Docs (same PR as code):** `docs/config-reference.md` (`MEDIA_UI_MEDIA_ROOTS`), SRS NFR-SEC-008 row,
  FRD RULE-VAL-1 (add playback sidecars), roadmap T-M3-10 status and §9 log, ADR README index row.
- **Required negative tests** (temp-dir fixtures only, with a filesystem spy or sentinel): forged
  `sc_`+base64(`/etc/passwd`), a random `sc_` ID and another session's valid ID each return 404 (403
  restricted) with no open; `../` in a module row or root is refused; a video or sidecar outside the
  roots, or under `/data/movies2` with root `/data/movies`, is not listed; a symlink escaping the root is
  not listed or served, and a post-list swap to a symlink is refused at serve; unset or unmounted roots
  give an empty list and 404 while module tracks still list; disallowed extensions, directories, FIFOs and
  files over 5 MiB are refused; a root overlapping the TLS or userdata directory disables sidecars.
  Regression: adjacent `Movie.en.srt` (converted to VTT) and `Movie.vtt` still list and play for both
  principal kinds, and the ADR-0031 rebinding and 403 tests pass.

## Alternatives

- **Root-confine the decoded `sc_` path:** smallest diff, but any member can still read any subtitle
  file of any item (bypassing per-item checks) and IDs keep leaking server paths.
- **HMAC-signed stateless references carrying the path:** survives restarts, but adds a persistent key
  to rotate and back up (ADR-0013) and a signed path is still a path the server must trust.
- **Plain SHA-256(session ID ‖ path) as the ID** (the plan's form): gives an offline oracle to confirm a
  guessed session ID against a known path. The per-process HMAC key costs nothing.
- **Bind only restricted principals (status quo of ADR-0031):** leaves the hole open for everyone else.
- **Reuse `SUBS_MEDIA_ROOTS`:** rejected in Decision 6.
- **Fetch roots from media-root-folders over gRPC:** a runtime dependency on every fetch for data that is
  static per deployment.
- **Serve all sidecars through media-subtitles:** cleaner ownership, but breaks adjacent SRT/VTT until a
  scanner import hook exists (M6).
- **Add read-only library mounts to compose now:** widens the BFF's filesystem reach to restore a feature
  compose never had; deferred.

## Open items

| Item | Recommended default |
|------|---------------------|
| `.ass` is served today with `Content-Type: text/vtt` and no conversion, which players cannot parse. | Keep `.ass` allowed but serve it as `text/x-ssa; charset=utf-8`; the SPA hides tracks it cannot render. Revisit with ASS→VTT conversion in media-subtitles. |
| Native clients' relist behavior (not verifiable here). | Ship the BFF fix; open relist-once tasks in `muxcore-ios` and `media-tvos-app`; document the gap in release notes. |
| Library mounts and sidecars in the registry compose. | Off. Enable only by operator opt-in with `:ro` mounts outside `/data`, after an owner decision. |
| Whether `.ssa`, `.sub`, `.txt` (accepted by media-subtitles) should be BFF sidecars. | No; they go through media-subtitles. |
| Mesh-identity rotation for existing deployments. | Recommend rotation in release notes; do not force it automatically. |
