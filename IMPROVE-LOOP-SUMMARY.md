# Improve Loop Summary — Cycles 124–145

**State:** cycle **144** complete, **145** in progress (`status: running`)  
**Cycles since 124:** **21** completed (124→144); **145** started

Loop restarted after a brief stop at cycle 123. Focus shifted to **mediauiprox BFF hardening**, **acquisition status pipeline**, and finishing the **admin-ui gRPC timeout sweep**.

---

## Deployed to vault

| Cycle | Highlights |
|-------|------------|
| **124** | admin-ui transcode + Jellyfin gRPC timeouts; media-ui PosterCard detail prefetch |
| **125** | mediauiprox auth exchange: 8s upstream timeout + `{error, code}` JSON |
| **126** | mediauiprox shared 8s client for library/subtitle paths |
| **127** | mediauiprox debrid + invites upstream client; request-media HTTP/gRPC hardening |
| **128** | mediauiprox livetv_qc + userdata paths; auth-local security headers (CSP, Permissions-Policy) |
| **129** | mediauiprox playback resolve/stream JSON errors; admin-ui queue triage UX |
| **130** | media-ui VideoPlayer playback error UX; admin-ui automation queue integration tests |
| **131** | media-ui In Progress “Needs attention” for failed/stalled/import_failed *(UI only)* |
| **132** | request-media passes through `import_failed` / `stalled` / `failed` statuses |
| **133** | admin-ui calendar dead-code cleanup |
| **134** | request-media `statusDetail`/`statusLabel`; admin-ui queue status labels; mediauiprox `/api/requests` proxy |
| **135** | media-ui In Progress cards show statusLabel + statusDetail from API |
| **136** | media-ui Queue page a11y (EmptyState, keyboard nav) |
| **137** | auth-local login CSRF cookies `Secure`+`HttpOnly`; admin-ui naming/rename timeouts |
| **138** | admin-ui subtitles gRPC timeouts; media-ui Discover poster prefetch |
| **139** | admin-ui metadata manager timeouts |
| **140** | admin-ui root-folders timeouts; media-ui Settings a11y |
| **141** | admin-ui migrate/import timeouts; media-ui Home hero detail prefetch |
| **142** | admin-ui API Keys + Libraries timeouts; media-ui ErrorBoundary on Layout |
| **143** | admin-ui backups + formats/profiles timeouts |
| **144** | admin-ui playback admin timeouts; media-ui offline-friendly fetch errors |

---

## Blocked on push (origin-pinned modules)

These modules were hardened locally but **cannot vault-deploy** via `install-origin-module.sh` without **commit + push to Forgejo `origin/main`**:

| Module | First blocked | Prepared version | What’s waiting |
|--------|---------------|------------------|----------------|
| **media-scanner** | 131 | v0.1.26 | ImportPath hardening, ffprobe timeout, clearer errors |
| **media-automation** | 133 | v0.1.43 | `status_detail`/`status_label`, 8s peer discovery timeouts |

**Impact:** Acquisition status labels wired through request-media → mediauiprox → media-ui, but **live `statusLabel`/`statusDetail` on vault** requires media-automation (and scanner fixes) on origin.

**Also local-only (not origin-pinned blockers):**
- **indexer-piratebay** — Init HTTP timeout, `go.sum` fix, `Storage.Put` SDK compat (139); not on vault stack
- **media-ui** / **admin-ui** / **mediauiprox** / **request-media** / **auth-local** changes above were deployed via direct binary/rsync

---

## Cycle 145 (in progress)

- admin-ui: PluginsPage, AuthSSOPage, NetworkingPage timeouts in `parity_actions.go`
- Next queued: origin module push when approved

---

## Themes (124–144)

1. **BFF reliability** — mediauiprox upstream timeouts + consistent JSON errors across auth, library, playback, debrid, invites, livetv, userdata
2. **Acquisition visibility** — end-to-end status pass-through (request-media → UI); origin backend deploy pending push
3. **Admin timeout sweep** — near-complete coverage of remaining handlers (subtitles, metadata, roots, migrate, parity_actions, backups, formats, playback)
4. **media-ui polish** — error boundaries, offline hints, prefetch, a11y, In Progress/Queue UX
