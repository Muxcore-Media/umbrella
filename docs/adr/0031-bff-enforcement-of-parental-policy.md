# ADR-0031: BFF enforcement of the authoritative parental policy

- **Status:** Accepted
- **Date:** 2026-10-08
- **Decided by:** Delegated
- **Builds on:** [ADR-0030](0030-authoritative-parental-policy-resource.md), [ADR-0019](0019-end-user-identity-propagation.md), [ADR-0009](0009-per-module-data-ownership.md), [ADR-0027](0027-transcoder-http-mesh-boundary.md)

## Context

The ADR-0030 provider is merged on userdata-local `main` as `d07d419`; its tree is identical to the verified candidate `b199a59` (checked 2026-10-08), so citations to `b199a59:` below read the merged code.

FR-PLAY-007 requires server-side restriction on every list, search, detail and playback API.
ADR-0030 added the typed provider resource `GET`/`PUT /api/parental-policy` in userdata-local
(`b199a59:internal/server/parental.go:16-75`). It also added the public `parental` package for
the schema, normalization and strict decoding (`b199a59:parental/policy.go`). Nothing consumes
either yet. Reading the code shows the following about today's consumer path (C-28):

- **Playback check:** this is the only server check. `handlePlaybackResolve`
  (`_mvp/cmd/mediauiprox/playback.go:94-149`) reads `tags`, `parental_rating` and `unrated` from
  the query string (`:107-109`) and the self-scoped userdata blob (`:105-106`).
  - `parentalBlocksPlayback` (`parental.go:15-43`) returns *allow* on an empty blob, a missing
    `parental` key or JSON errors (`:16-30`). It ignores `rating` (`:41`) and allowed tags. It
    matches blocked tags by **substring** (`:37`).
  - The check is skipped entirely when `s.userdata == nil` (`playback.go:104`).
  - The SPA calls resolve with **only** `src` (`media-ui-app/src/api/client.ts:3291-3294`). In
    practice, therefore, the check never blocks anything.
- **Policy is self-editable and falls back silently:**
  - `PUT /api/userdata` lets any session write its own `prefs.parental` (`userdata.go:245-266`).
  - `load` falls back to the local file store on *any* proxy failure, including 401, 403, 503
    and decode errors (`userdata.go:96-103`, `:151-156`).
  - `save` writes locally when the proxy write fails (`:110-117`).
- **No other route checks.** The movie/TV list and detail handlers (`main.go:943-1039`) call
  the media modules and return every item. `/stream/movies/` and `/stream/tv/` are bare reverse
  proxies (`main.go:739-740`).
- **The admin source is protected but loose.** admin-ui stores `parental.json` (0600 under the
  admin data dir; `handler/parity_actions.go:822-850`, `handler/paths.go:52-54`), keyed by user ID
  with no tenant.
  - Writes go through `POST /users/{id}/parental` (`handler/handler.go:390-391`) behind
    `requireAuth`, which checks `admin.access` (`handler/handler.go:480-512`, `:534-560`).
  - The schema is a free-form `max_parental_rating` string (`parity_actions.go:807-820`),
    comma-joined `blocked_tags`/`allowed_tags`, `allow_unrated`, `kids_mode` and `pin_hash`
    (`:775-783`).
  - After each save, admin-ui also copies the whole struct, including `pin_hash`, into the
    user's writable blob (`handler/userdata_parental.go:54-133`).
  - muxcorectl-cli `users parental set` rewrites the same file directly. Its struct has no
    `kids_mode` or `pin_hash`, so a `set` erases both (`muxcorectl-cli/internal/cli/users_parental.go:13-18,76-87`).
- **No authoritative rating exists for media:**
  - `MovieItem` (`media-movies/proto/mgmntv1/movies.proto:117-142`) and `TVSeries`
    (`media-tvshows/proto/tvmgmtv1/tvshows.proto:137-164`) have no certification field. In both
    modules, "rating" means `vote_average` (`media-movies/internal/module.go:1463-1464`).
  - The metadata contracts carry only the TMDB `adult` flag (`contracts-metadata/.../metadata.proto:99`).
    metadata-tmdb drops adult results (`metadata-tmdb/internal/module.go:204,803`). No module
    persists any certification.
  - The only trusted classification-like data is the operator's **item tags**
    (`GetItemTags`/`SetItemTags`; `movies.proto:18-20`, `tvshows.proto:19-21`). In the BFF,
    only admin/manager can write them (`tags.go:304-309`, `:375-380`).
  - The SPA expects a `content_rating` field (`media-ui-app/src/types/index.ts:23-24,412-413`)
    that the BFF never sends (`main.go:1102-1208`). The SPA filter is documented as fail-open
    and treats NR/unknown as permissive (`media-ui-app/src/lib/parental.ts:1-8,20-30`).
- **Identity and bearer:**
  - Sessions store the auth-local bearer (`session_store.go:307-324`, `auth_exchange.go:114`,
    `tv_auth.go:94,177`).
  - **Quick Connect sessions do not.** They are created without a bearer or roles
    (`livetv_qc.go:410`; auth LLD §7 notes this too).
  - The BFF already forwards the bearer under ADR-0019 (`upstream_identity.go:60-80`).
- **Other modules:** playback-guard handles sharing and concurrency rules, and playback-monitor
  handles analytics and history. Neither has parental semantics or a request-path hook (READMEs).
  Neither is an enforcement point.

## Decision

### 1. Enforcement point and route inventory

1. **The consumer BFF is the single enforcement point.** One `parentalGate` component sits in
   `_mvp/cmd/mediauiprox` and has four parts:
   - a policy resolver (item 3),
   - a classification resolver (item 2),
   - one evaluator (`parental.Evaluate`, added to the userdata-local public `parental` package
     next to the schema so the semantics have a single home),
   - a **route-class table keyed by the exact registered pattern**.

   A test extends `routes_inventory_test.go` and fails when any pattern registered by
   `registerRoutes` (`main.go:435`) or `library_api.go:29-83`/`requests.go:12-19` has no class.
   A path-prefix middleware alone is rejected, because item identity arrives in the path, in
   `src` or in `mux_id` depending on the route.

2. **Route classes.** Each class covers the routes below for a restricted principal.
   Registration line, then handler location.

   - **C-LIST: filter items; on policy failure, fail the whole response.**
     - `GET /api/movies` `main.go:511` → `main.go:943`, including `?library=` → `library_paths.go:301`.
     - `GET /api/tv` `:547` → `main.go:993`.
     - `GET /api/collections` `:541` → `collections.go:46`.
     - `GET /api/collections/{id}` `:542,546` → `collections.go:75`.
   - **C-ITEM: check one item; deny the whole response.**
     - Detail:
       - `GET /api/movies/{id}` `:525` → `main.go:972`.
       - `GET /api/tv/{id}` `:559` → `main.go:1018`. Episodes are classified by their series.
     - Per-item reads:
       - Tags: `tags.go:273,344`.
       - Titles: `titles.go:24,124`.
       - History: `item_history.go:90,106`.
       - Artwork: `item_artwork.go:103,118`.
       - Files: `item_files.go:27`.
       - Subtitles: `item_subtitles.go:137,165`.
       - Episode file: `episode_file.go:11` (`:539`).
   - **C-PLAY: check the item behind `src` or the path.**
     - Resolve: `GET /api/playback/resolve` `:570` → `playback.go:94`.
     - Transcode: `GET /stream/transcode` `:665` → `playback.go:357`.
     - HLS index: `GET /stream/hls` `:666` → `playback.go:421`.
     - Trickplay: `GET /stream/trickplay` `:668` → `playback.go:544`.
     - Direct movie stream: `/stream/movies/` `:739`, which today is an unguarded `reverseProxy`
       (`main.go:1263`).
     - Direct episode stream: `/stream/tv/` `:740`.
     - Playback subtitles: `GET /api/playback/subtitles{,/{id}}` `:593-594` → `playback_subtitles.go:249,270`.
     - Segments: `GET /api/playback/segments` `:660` → `playback.go:169`.
     - Chapters: `GET /api/playback/chapters` `:663` → `playback_chapters.go:101`.
     - Analysis: `GET /api/playback/analysis` `:664` → `playback_analysis.go:240`.
     - `GET /stream/hls/{key}/{file}` `:667` → `playback.go:512` needs extra handling. The
       transcoder derives the key deterministically from `src` and parameters
       (`media-transcoder/internal/playback_hls.go:108`), so a key alone proves nothing. The
       BFF binds each key to the item and session when it proxies an authorized index, and
       refuses unbound keys for restricted principals. The exact mechanism must be proven by
       test.
   - **C-DENY: no trusted classification exists. Restricted principals get 403; everyone else
     is unchanged.**
     - External catalogue:
       - Discover: `/api/discover/` `:719` → `discover.go:50`.
       - Search and requests: `/api/search`, `/api/request*`, `requests.go:14-18` → `:21`.
       - Related titles: `GET /api/graph/related` `:718` → `graph.go:9`.
       - Watchlist: `/api/watchlist` `:720` → `watchlist.go:24`.
     - External players:
       - Jellyfin: `/api/jellyfin/play` `:567` → `main.go:1043`.
       - Plex: `/api/plex/play` `:569` → `plex_play.go:22`.
       - Debrid: `/api/debrid/{vfs,stream}` `:704-705` → `debrid.go:38,42`.
     - Live TV: `GET /api/livetv` `:669` → `livetv_qc.go:174`.
     - Non-video libraries:
       - Lists: `GET /api/{music,books,…}` `library_api.go:29` → `:259`.
       - Detail: `library_api.go:99,142,185,332`.
       - Streams: `:86,233,246,386`.
       - Lyrics: `livetv_qc.go:464`.
     - Title-revealing feeds with no role gate:
       - Calendar: `calendar.go:37`.
       - Activity: `activity.go:63`.
       - Wanted: `:167`.
       - Missing: `missing.go:18`.
       - Playback history: `sessions.go:17`.
       - Active sessions: `sessions.go:158,224`.
       - Item watch stats: `watch_stats.go:15`.
       - Watch together: `watch_together.go:153`.
       - Media issues: `media_issues.go:91`.
       - Segment media list: `playback.go:295`.

     The role-gate check on these feeds was a scan of each handler's first 25 lines. Treat it
     as indicative until the inventory test classifies each one.
   - **C-EXEMPT: routes that return no catalogue content.** Session, login and logout,
     `/api/userdata` writes, `POST /api/playback/session` telemetry, health, and operator
     routes that are already gated to admin/manager.
3. Images under `/images/*` skip auth today (`main.go:826-829`, `:733-738`). They are **not**
   covered by this decision (see item 6).

### 2. Provenance of media classification

1. **Authority.** The authoritative classification belongs to the item's owning media module
   (ADR-0009). media-movies and media-tvshows add these fields:
   - `content_rating` (string token),
   - `content_rating_source` (`operator` | `tmdb`),
   - `tag_labels` (the item's existing operator tags).

   The fields go on `MovieItem` and `TVSeries` so that list routes need no N+1 calls. The
   modules also add:
   - an admin-only override RPC, `SetContentRating`, which can also record an explicit `NR`;
   - an optional narrowing `classification_filter` on `ListMovies`/`ListTVShows`, so that
     pagination and `total` do not count hidden items. The BFF still re-evaluates every
     returned item and drops any that fail.
2. **Source precedence.** An `operator` value always wins. A `tmdb` value is a later slice
   (contracts-metadata, then metadata-tmdb, then the media modules) using one configured
   certification country. Until that slice lands, only operator-classified items are visible
   to restricted principals.
3. **Inputs that are never trusted:** request query or body fields, SPA `content_rating`,
   userdata blobs, the Jellyfin/Plex bridges, filenames, and media-tagging's separate
   assignment store. Whether media-tagging mirrors its tags into media-module item tags is
   unverified, so it is not consulted.
4. **TV.** Episodes and seasons inherit their series classification. media-tvshows adds a
   `GetEpisode` RPC so the BFF can map an episode ID from `/stream/tv/<episode_id>` to
   `series_id`. No contract does that today (`tvshows.proto:6-26`).
5. **Rating states.** Every item has one of three states:
   - *rated*: a token in the provider's supported set (`b199a59:parental/policy.go:96-100`);
   - *unrated*: an explicit `NR`/`UR` recorded by a source;
   - *unavailable*: empty, an unknown token (for example `15` or `12A`), item missing, or a
     lookup error.
6. **Evaluation in restricted mode** (ADR-0030 item 6):

   | Classification | Result |
   |---|---|
   | Rating *unavailable* | Always denied, whatever `allow_unrated` says |
   | Rating *unrated* | Allowed only when `allow_unrated` is true |
   | Rating *rated* | Compared on a single ladder that mirrors the SPA levels without NR/UR |

   - **Ceiling:** the explicit `max_rating` wins. Otherwise `kids_mode` sets the ceiling to PG.
   - **Tags:** compared by exact normalized match, and blocked tags win. A nonempty
     `allowed_tags` requires a match, and that match cannot override rating denial.
   - **Tag lookup:** if the policy has tag rules and the tag lookup fails, the item is denied.
7. **Trust boundary.** Managers can edit item tags (RULE-AUTH-3), and admins can set rating
   overrides. Both therefore influence enforcement. A restricted policy on an admin or manager
   account is applied, but it **is not a security boundary**. admin-ui must say so.

### 3. Failure behavior

The policy request is
`GET {USERDATA_LOCAL_URL}/api/parental-policy` with:
- headers: exactly one `Authorization: Bearer <session auth token>` and `X-MuxCore-User-Id: <session user>`;
- no query string and no client identity or tenant input. `scopeFromRequest` overrides
  (`userdata.go:60-92`) are **not** used.

The BFF decodes the body with `parental.DecodePolicy` and checks the envelope strictly:
- `state` is either `configured` (revision > 0, valid policy) or `unconfigured` (revision 0,
  null policy);
- the response's `user_id` and `tenant_id` must equal the session's.

| Condition | C-LIST / C-ITEM / C-PLAY / C-DENY | C-EXEMPT |
|---|---|---|
| Configured `unrestricted` | Pass through. No classification lookups. | unchanged |
| Configured `restricted` | Evaluate. A denied item is omitted from lists. Detail and playback return **403 `parental.blocked`**; resolve keeps the alias `playback.parental_blocked`. C-DENY returns **403 `parental.restricted_route`**. | unchanged |
| `unconfigured` | **403 `parental.policy_unconfigured`**. Absence never implies unrestricted. | unchanged |
| Session has no bearer (Quick Connect, legacy sessions) | **403 `parental.policy_unverifiable`** | unchanged |
| Provider 401 | **401 `parental.session_invalid`**. Evict the cache entry. | unchanged |
| Provider 403/404/409/413/5xx, timeout, connection error, missing route, bad JSON, envelope or scope mismatch, unknown state | **503 `parental.policy_unavailable`**. Never fall back to the local store or the blob, never cache, never return an empty 200. | unchanged |
| Media-module lookup fails for the classification | **503 `parental.classification_unavailable`** (fail closed) | n/a |

All of these responses carry `Cache-Control: no-store`. A blocked response carries no item
metadata.

There is **no switch to turn enforcement off** in the household profile. With
`MEDIA_UI_REQUIRE_AUTH=0` (dev only, `main.go:92`) there is no session, so enforcement is
inactive and the BFF logs a startup warning. The current `prefs.parental` evaluation and the
query inputs are deleted (`parental.go`, `playback.go:104-114`).

### 4. Caching and revisions

- **Policy cache:**
  - The key is SHA-256(bearer) plus the session user. Entries last at most **30 s**, matching
    the ADR-0019 bound.
  - Only validated `configured` and `unconfigured` responses are cached. Errors are never
    cached, and a provider 401 evicts the entry.
  - Admin changes therefore reach the BFF within 30 s. There is no cross-process invalidation.
    Stream and segment requests recheck through the cache, so a new restriction stops further
    segments within that bound.
- **Classification cache:** entries are keyed by item ID and last at most 30 s. They are used
  only for C-PLAY lookups such as HLS segments and direct ranges. List and detail responses
  carry their own fields.
- **Revisions:** the BFF records the revision in debug logs and audit context. There is no
  cross-TTL monotonicity rule, because a restore can legitimately lower the revision. The BFF
  never writes policy.

### 5. Trusted migration of admin-ui `parental.json`

1. **Who runs it.** An admin runs it from admin-ui (`POST /users/parental/migrate`, behind
   `requireAuth`). It uses that session's auth-local bearer. Because the provider requires
   admin for PUT (`b199a59:internal/auth/parental.go:61-64`), a non-admin who has
   `admin.access` gets 403 and nothing is written. The only input is the protected
   `parental.json`. Blobs are never read.
2. **Dry run first.** The dry run lists every `AuthService.ListUsers` account with its mapped
   policy and outcome. A write run needs the dry-run digest, which is the SHA-256 of
   `parental.json` plus the plan.
3. **Mapping:**
   - `max_parental_rating` is trimmed and upper-cased.
   - The tag strings are split on commas.
   - `allow_unrated` and `kids_mode` map directly. **`pin_hash` is never sent** (ADR-0030 item 7).
   - An entry with any restriction set becomes `restricted`.
   - An unsupported rating is reported and **skipped**, so the account stays unconfigured and
     fails closed.
   - An all-default entry, or an account with no entry, becomes `unrestricted` **only** when
     the admin ticks an explicit "set unrestricted for N listed accounts" choice. That choice
     is not checked by default, and it is the administrative decision ADR-0030 item 5 requires.
4. **Idempotency.** For each account the tool first GETs the current policy:
   - unconfigured: PUT with `expected_revision: 0`;
   - configured and equal after normalization: skip;
   - configured and different: report a conflict and **do not overwrite**, because the provider
     is now the authority;
   - 409: report as stale;
   - 404 or 403: report the missing or cross-tenant account.

   Re-running the tool is safe. Results are audited (`admin.parental.migrate`) and written to
   `parental-migration.json` in the admin data dir. `parental.json` is retained as legacy
   history and not deleted.
5. **After migration:**
   - The admin-ui form reads and writes **only** the provider (GET revision, then PUT
     `expected_revision`, with a 409 conflict UI).
   - admin-ui stops copying restriction fields into the blob. PIN behaviour stays as it is
     until FR-AUTH-010.
   - muxcorectl-cli `users parental set` must stop reporting success for a file that no longer
     matters.

### 6. Out of scope, and what remains Part

**Out of scope:**
- Profiles, kids profiles, PIN unlock and PIN grants (FR-AUTH-010 / T-M4-02). A client PIN gate
  must never unlock a server denial.
- Native client acceptance (muxcore-ios, media-tvos-app) beyond their use of the same BFF routes.
- Jellyfin and Plex servers' own parental controls. The handoff routes are denied to restricted
  principals instead.
- Classification of music, books, comics, audiobooks and live TV. These are denied rather than
  classified.
- `/images/*`, which stays public.
- Continue-watching titles written into the user's own userdata progress (title, poster and
  href are client-written: `useProgressReporting.ts:8-10,46-48`). Playback of those items is
  still enforced.
- Platform multi-tenancy (NG-1).
- Module-to-module HTTP: any container on the compose network can GET
  `media-movies:9430/stream/movies/<id>` (`docker-compose.registry.yml:407-420`).

**FR-PLAY-007 stays Part after this ADR is implemented until all of the following hold:**
- the household policy call uses an authenticated transport. Today userdata-local HTTP is
  plaintext (`internal/module.go:138-175`; gRPC alone has TLS at `:148-158`), so a spoofed
  response could forge `unrestricted`;
- the live household run passes the negative tests below;
- profile-scoped restrictions exist (FR-AUTH-010);
- the image and userdata-title residuals are resolved or accepted by the owner.

The SRS evidence may then record "BFF movie/TV routes enforced (fixture)".

## Alternatives

- **Keep today's request metadata and blob check, and add more routes to it:** this repeats C-28
  on more routes.
- **Filter only in the SPA:** the SPA is untrusted and documented as fail-open.
- **Enforce in each media module:** this would spread identity and policy resolution into every
  module and still leave the external and BFF-only routes uncovered.
- **Treat unconfigured as unrestricted, or offer an operator switch for it:** this contradicts
  ADR-0030 item 4. It would be a fail-open default.
- **Treat items with no rating as unrated when `allow_unrated` is set:** "unavailable" and
  "unrated" mean different things in ADR-0030. With no certification data stored today, this
  would unlock almost the whole library.
- **Use the TMDB `adult` flag as the rating:** it is binary, adult results are already filtered
  out, and it is not persisted.
- **Have the migration copy blobs, or set every account unrestricted automatically:** this
  promotes untrusted input or makes the administrative decision implicitly.
- **Filter list pages only in the BFF:** this breaks pagination and leaks hidden counts through
  `total`. It is kept only as a re-check behind the module filter.

## Consequences and verification

- **Rollout order:** provider (userdata-local release), then admin migration, then the BFF.
  If they deploy together, every user gets `policy_unconfigured` until an admin runs the
  migration. Admin-ui itself is unaffected.
- **New accounts:** accounts created by invite or by admin are unconfigured, so they are denied
  until an admin sets a policy. A follow-up should require a policy choice at account creation.
- **Semantic changes:**
  - Blocked tags become exact matches, not substrings.
  - Unknown ratings are denied.
  - The restricted views of discover, search, related titles and non-video libraries are empty
    or 403.
- **Tooling and docs:**
  - `parental_test.go` is replaced.
  - BFF-API.md documents the route classes and codes.
  - FRD WF-07 step 1 says resolve takes an "item ID", but the code takes `src`
    (`playback.go:99`). Correct the FRD.

**Required negative tests** (BFF, unless another module is named):
1. Every registered pattern is classified (inventory test).
2. Ignored inputs:
   - Query `tags`/`parental_rating`/`unrated`, `X-MuxCore-User-Id`, `user_id`, `X-Tenant-ID`
     and edits to blob `prefs.parental` do not change any decision.
   - Their removal does not unlock anything.
3. For a blocked movie, an episode of a blocked series, and an unavailable rating, each of these
   returns 403 and is absent from lists and totals:
   - resolve, `/stream/movies`, `/stream/tv`, transcode, HLS index, trickplay;
   - subtitles, chapters, segments, analysis;
   - detail and the per-item reads.
4. An HLS key computed from a blocked `src`, or never bound to the session, is refused.
5. The rating ladder:
   - kids mode with no max denies PG-13 and allows PG;
   - an explicit max overrides kids mode;
   - `NR` with `allow_unrated` false is denied, and with it true is allowed;
   - an unavailable rating with `allow_unrated` true is still denied;
   - an unknown token is denied.
6. Tags:
   - a blocked tag beats an allowed tag;
   - a nonempty allowed set with no match is denied;
   - an allowed match does not unlock a rating denial;
   - a tag lookup failure is denied;
   - `gore` does not block `gorey`.
7. Provider failures (down, timeout, 5xx, 404 route missing, malformed JSON, unknown field,
   `user_id`/`tenant_id` mismatch, revision 0 with a policy) return 503 on every gated class.
   There is no local or blob fallback, and errors are never cached.
8. A provider 401 returns 401 and evicts the cache entry. Unconfigured returns 403. A session
   with no bearer returns 403.
9. An unrestricted policy reproduces today's responses (regression). A changed policy takes
   effect within 30 s.
10. C-DENY routes return 403 for restricted principals and are unchanged for unrestricted ones.
11. Migration (admin-ui):
    - a dry run writes nothing;
    - a re-run is a no-op;
    - a differing configured policy is reported, not overwritten;
    - a bad rating is skipped;
    - unlisted accounts are untouched without the explicit opt-in;
    - a non-admin causes zero writes;
    - 409 is surfaced;
    - `pin_hash` is never transmitted.
12. muxcorectl-cli `parental set` no longer reports success.
13. Media modules:
    - only admin can set an override;
    - `classification_filter` never widens results;
    - the schema upgrade fixture follows ADR-0015.
