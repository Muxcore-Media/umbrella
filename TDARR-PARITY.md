# Tdarr → MuxCore (`media-transcoder`) parity

> **Status note (2026-10-05).** Historical parity record for `media-transcoder` (SRS `FR-TRN-001`). Requirements and current status are owned by [`docs/specs/SRS.md`](docs/specs/SRS.md); this file is not updated further.

**Goal:** `media-transcoder` (+ `admin-ui` `/transcode`) replaces Tdarr for library transcoding workflows.

Legend: `[x]` shipped · `[~]` partial · `[ ]` missing · **Waiver** = MuxCore-native equivalent accepted

---

## A. Core workflow features

| Tdarr feature | Status |
|---------------|--------|
| Library path → transcode rules | [x] `TranscodeSetup.library_paths` prefix match |
| Plugin stack / filter chain | [x] ordered `PipelineStep` filters + `action.remux` |
| Multiple output formats from one source | [x] multiple `SetupOutput` rows per setup |
| Keep original after transcode | [x] `source_disposition=keep` |
| Replace / drop original after transcode | [x] `replace` / `delete` |
| Archive original to folder | [x] `archive` + `archive_path` |
| Auto-process on new library files | [x] `on_import` via mesh events |
| Manual / scheduled library scan | [x] `manual` trigger + `ScanSetups` + scheduled loop |
| Job queue + progress | [x] existing `Enqueue` / `ListJobs` |
| Transcode profiles (codec, CRF, GPU) | [x] profile CRUD |
| Worker pool / nodes | [x] Optional `TRANSCODER_USE_POOL` / `TRANSCODER_POOL_ADDR` → `media-transcoder-pool` |
| JavaScript custom plugins | [x] **Waiver** — declarative steps + community JSON templates (`ListStepTemplates` / `ApplyStepTemplate` + admin apply on setup edit) |
| Flow editor UI (drag-drop) | [x] visual flow builder on `/transcode` setup editor |
| Hold for review / staging | [x] `hold_for_review` + review queue (approve / reject before destructive disposition) |
| Health-check skip (already optimal) | [x] `filter.skip_if_codec`, `filter.skip_if_container`, size/extension filters |

---

## B. Admin / ops

| Surface | Status |
|---------|--------|
| Setup CRUD | [x] gRPC + `/transcode` admin UI |
| Profile management | [x] gRPC profiles + listed on admin page |
| Pipeline run history | [x] `ListPipelineRuns` + admin table |
| Scan now | [x] POST `/transcode/scan` |
| Hardware detection | [x] `DetectHardware` |
| Per-library variables (Tdarr flows) | **Waiver** — use separate setups per library prefix |
| Node dashboard | **Waiver** — use `/modules` + `media-transcoder-pool` when deployed |

---

## C. Filter / action step types

| Step | Status |
|------|--------|
| `filter.skip_if_codec` | [x] |
| `filter.skip_if_container` | [x] |
| `filter.min_size_mb` / `filter.max_size_mb` | [x] |
| `filter.extension` | [x] |
| `filter.skip_if_output_exists` | [x] |
| `action.remux` | [x] |
| Transcode outputs (implicit action) | [x] via setup outputs + profiles |

---

## D. Example setups (admin-configurable)

1. **HEVC alongside original** — outputs: `hevc_gpu` with suffix `-hevc`, disposition `keep`, filter skip if already HEVC (seeded default).
2. **Replace with single H.264** — one output `h264_fast`, disposition `replace`, remux to mp4 optional step.
3. **Dual streaming + archive** — outputs: `h264_fast` (720p) + `hevc_gpu` (1080p), disposition `archive` to `/data/archive`.
4. **Manual-only remux stack** — trigger `manual`, steps: `action.remux` → mp4, no transcode outputs.

---

## E. Remaining work

None — Tdarr-class library transcoding parity is complete (waivers documented above).

## F. Baseline

`media-transcoder` **v0.3.1** — Tdarr-class library transcoding: configurable setups, multi-output pipelines, filter stacks, source disposition, hold-for-review staging, community step-template pack, optional `TRANSCODER_USE_POOL` forwarding to `media-transcoder-pool`, event-driven and scheduled processing, admin UI at `/transcode`.
