# MuxCore — agent instructions

This is the authoritative agent-instruction file for the MuxCore umbrella. Read it first, then the roadmap.

## Start here: plan, specs, and decisions

1. **Read [`MASTER-ROADMAP.md`](MASTER-ROADMAP.md) before planning or implementing anything.** It is the canonical source for scope, priorities, milestones, tasks (`T-M*-*`), status, and completion criteria. Find the task you are working on and its referenced requirement IDs.
2. **Read the linked specifications for that task** — at minimum the referenced `FR-*`/`NFR-*` rows in [`docs/specs/SRS.md`](docs/specs/SRS.md) and the design section the task cites.
3. **Who owns what** (one home per subject; reference by ID, never restate):

| Subject | Owner document |
|---------|----------------|
| Scope, priority, milestones, task status, risks, doc map | [`MASTER-ROADMAP.md`](MASTER-ROADMAP.md) |
| Business outcomes, constraints, success measures (`BO-*`, `BC-*`, `SM-*`) | [`docs/specs/BRD.md`](docs/specs/BRD.md) |
| Personas, capabilities, journeys, non-goals (`P-*`, `CAP-*`, `J-*`, `NG-*`) | [`docs/specs/PRD.md`](docs/specs/PRD.md) |
| Workflows, business rules, state machines, permissions (`WF-*`, `RULE-*`) | [`docs/specs/FRD.md`](docs/specs/FRD.md) |
| **Requirements** (`FR-*`, `NFR-*`) with status and verification | [`docs/specs/SRS.md`](docs/specs/SRS.md) |
| Components, trust boundaries, topology, data flows | [`docs/architecture/HLD.md`](docs/architecture/HLD.md) |
| Design patterns, collaboration, interface responsibilities | [`docs/architecture/SDD.md`](docs/architecture/SDD.md) |
| Engineering choices: build, deps, CI, packaging, testing, security approach | [`docs/architecture/TDD.md`](docs/architecture/TDD.md) |
| Subsystem detail (schemas, algorithms, state machines) | [`docs/architecture/lld/`](docs/architecture/lld/README.md) |
| Architectural decisions | [`docs/adr/`](docs/adr/README.md) |

4. **Keep docs and status in sync with code.** A PR that changes behaviour must, in the same PR: update the affected SRS rows (status/evidence), update the roadmap task status (and the validation log when you ran a check), and fix any FRD/LLD text it invalidates. Do not mark anything `Done` or `Impl` without having run its verification.
5. **Record architecture changes as ADRs.** Changes to module boundaries, contracts, data ownership, security model, build/release/CI model, or product commitments need a new ADR in `docs/adr/` (rules in its README). Supersede, never rewrite, an accepted ADR. Mark who decided: `Owner`, `Delegated`, or `Historical` — never imply owner approval you do not have.
6. **Conflicts and assumptions.** If code and a doc disagree, or two docs disagree: do not silently pick one. Resolve using, in order, explicit owner decisions (ADRs marked `Owner`), this file, accepted ADRs, then implementation evidence; record the resolution in the roadmap §4 conflict table (or a new ADR if architectural). State any assumption you rely on in the PR and, if it affects scope, add it to roadmap §4 "Assumptions". Superseded docs keep a banner linking to their replacement — do not delete history.
7. **Hard constraints** (details in BRD §5): GitHub (`github.com/Muxcore-Media`) is the **only** origin, tracker, and CI host ([ADR-0002](docs/adr/0002-github-sole-origin.md)); acquisition is fixture-only ([ADR-0008](docs/adr/0008-fixture-only-acquisition-gate.md)); no paid services; never work in archived dumps (`media-ui`, `media-jellyfin`, `cache-memory`, `custom-scripts`, `muxcorectl`).

## Umbrella workspace

This checkout is an **umbrella repo**: the root git tracks the roadmap, specs, ADRs, `.cursor/skills`, `scripts/`, and **gitlink SHAs** for every module under `.gitmodules` (all `https://github.com/Muxcore-Media/<module>.git`). Module source lives in each submodule directory and pushes to its own GitHub repo; the umbrella records the integrated, known-good SHAs.

Clone/recurse/bump workflow: [`README-UMBRELLA.md`](README-UMBRELLA.md). Agent skills: [`.cursor/skills/README.md`](.cursor/skills/README.md). Commit conventions: [`CONTRIBUTING.md`](CONTRIBUTING.md).

### Agent authorization

All agents are authorized to commit, push, and create pull requests (PRs) for their changes to anything they work on; there is no need to ask permission.

## Subagent routing and orchestration

The primary agent acts as an **orchestrator**, coordinating work through context-scoped subagents rather than performing coding and edits directly. This division of labor optimizes reasoning effort and keeps the main conversation focused on coordination and decision-making.

When delegating to subagents, route work based on task complexity and requirements. Models are listed from cheapest to most capable; prices are Anthropic first-party API rates per million input/output tokens as of 2026-09-25 (see the Anthropic pricing page for current rates):

| Model | Price (in/out per MTok) | Route here |
|-------|-------------------------|------------|
| **Haiku 4.5** | $1 / $5 | Cheapest and fastest. Simple, well-defined tasks: file reads, pattern matching, routine edits, small scripts, exploratory searches, quick lookups. |
| **Sonnet 5.5** | $2 / $10 | Balanced speed and capability for general-purpose work: multi-file refactoring, feature implementation, moderate analysis, code review. **Default choice for most substantive tasks.** |
| **Opus 5.5** | $4 / $20 | Strong reasoning for complex, high-stakes work: architecture decisions, cross-system design, deep codebase analysis, security review, intricate bug diagnosis. Use when Sonnet's output quality is insufficient. |
| **Fable 5.1** | $10 / $50 | Anthropic's most capable widely released model and the most expensive. Turns on hard tasks can run for many minutes. Reserve for the most demanding reasoning and long-horizon agentic work where Opus falls short. |

**Orchestrator responsibilities:** define the task scope, provide context and constraints, verify subagent work, and coordinate across multiple efforts. Route simple, independent subtasks to Haiku or Sonnet to reduce latency and cost. Reserve Opus and Fable for genuinely complex problems requiring deep reasoning or when task failure would be costly.

**Subagent responsibilities:** execute focused work within defined scope — code writes, edits, file operations, targeted analysis. Report results clearly so the orchestrator can verify and coordinate the next step.

### Delegation practices (MuxCore-specific)

- **One module per subagent.** Module boundaries are repo boundaries (each submodule pushes to its own repo), so scope each subagent to a single module directory plus the contracts it consumes. Cross-module changes are sequenced by the orchestrator: `core` / `contracts-*` first, then consumers, then the umbrella gitlink bump.
- **Hand over the spec, not the history.** Give each subagent the roadmap milestone/task ID, the relevant requirement IDs from [`docs/specs/SRS.md`](docs/specs/SRS.md), the module's own `AGENTS.md`/`README.md`, and the exact verification command it must run. Subagents should not need to re-read this whole file.
- **Parallelize only independent work.** Fan out across modules that do not share an edited contract. Two subagents must never edit the same submodule checkout concurrently — use `git worktree` inside the module if parallel branches are needed.
- **Verify before accepting.** The orchestrator re-runs (or inspects the output of) the subagent's stated test/lint command, and checks the diff stays inside the agreed scope. A subagent's "done" is a claim, not evidence.
- **Read-only exploration is cheap.** Use Haiku/Explore-type agents for sweeps across many modules (e.g. "which modules implement `HealthChecker`"); escalate to Sonnet/Opus only for the module that needs changing.
- **Escalate on failure, not by default.** If a Sonnet subagent fails the same task twice, re-scope it or escalate the model; do not retry identically.


## Workstation notes (`claude-workstation` container)

Some sessions run inside the `claude-workstation` container on **vault** rather than on `thin`/`desk`:

- **No Nix.** Toolchains come from `mise` (`mise ls`: Go, Node LTS, Python, Rust, uv). Run `go`, `node`, `npm` directly instead of `nix-shell -p …` wrappers. `golangci-lint` may need `mise use -g golangci-lint` (persists under `$HOME`).
- `gh` is authenticated; run `gh auth setup-git` once so `git` and `go` can fetch private `Muxcore-Media` repos over HTTPS.
- **You are on vault's hardware but not in its host namespace** — the MVP stack under `/mnt/fast-storage/appdata/muxcore/mvp` is not mounted here. Use the SSH paths below for deploys; do not assume localhost ports reach the vault stack.
- Only `/home/claude` persists across container restarts.

## Dev toolchain

The repo must not depend on one toolchain manager ([TDD §1](docs/architecture/TDD.md#1-technology-stack-current-retained)). Use whichever the machine provides:

- **Nix workstations** (`thin`, `desk`): do not assume binaries on PATH outside a Nix shell; prefer a module's `shell.nix` (`muxcore-ios`), else `nix-shell -p go golangci-lint --run 'cd core && golangci-lint run ./...'`, `nix-shell -p nodejs --run 'cd media-ui-app && npm ci && npm test'`. Do not install with apt/brew/curl when Nix provides the tool.
- **mise / system toolchains**: run `go`, `node`, `npm` directly.

Read the module's `README.md` / `AGENTS.md` for module-specific invocations before inventing flags.

**Private Go modules** (all `github.com/Muxcore-Media/*` repos are private):

```bash
export GOPRIVATE='github.com/Muxcore-Media/*'
export GONOSUMDB='github.com/Muxcore-Media/*'
export GIT_TERMINAL_PROMPT=0
gh auth setup-git        # HTTPS credentials for git and go
```

**Non-interactive git (agents):** `source scripts/muxcore-git-env.sh` (sets `GIT_TERMINAL_PROMPT=0`, `GH_PROMPT_DISABLED=1`). Push with `gh`-backed HTTPS credentials; if `gh auth status` fails, restore with `gh auth login`.

**Workspace Go modules** ([ADR-0011](docs/adr/0011-go-workspace-and-published-versions.md)): module `go.mod` files require published GitHub tags and must not contain filesystem `replace` directives. For cross-module work, use the umbrella `go.work` (`scripts/gen-go-work.sh`, roadmap T-M1-09). Before pushing contract consumer changes, run `./scripts/check-proto-drift.sh` (also run by `_mvp/scripts/run-script-tests.sh`).

**CI** runs on GitHub-hosted runners from umbrella templates ([ADR-0003](docs/adr/0003-ci-on-github-hosted-runners.md)). Do not add self-hosted runners or mirror-checkout actions.

## Owner reference deployment (vault & desk)

The sections below describe the **owner's own deployment** used for soak testing. They are operator environment notes, not product requirements, and they must not leak into product docs or specs. Source of truth for host config is the owner's NixOS repo `~/Projects/zemdregon/nix-production`.

#### Key hosts (vault & desk)

Two hosts matter most for day-to-day MuxCore work. Both are reachable over **ZeroTier** (network `2ca2fd5d9eab729d`; local interface often `ztoe6fmlyd`). Prefer ZT IPv6; use `-6` with `ssh`/`scp`/`rsync` when needed.

| Host | Role | SSH (as `ender`) |
|------|------|------------------|
| **vault** | **MuxCore dev deployment** — runs `muxcore-host.service`, module binaries | `ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95` |
| **desk** | **DNS & Cloudflare control plane** — `flarectl` (grey-cloud `*.zem.systems` records), `cloudflared` (tunnel cert), `wrangler` | `ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:93b2:af65:48bb` |

Convenience variables (use in scripts):

```bash
VAULT='ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95'
DESK='ender@fd2c:a2fd:5d9e:ab72:9d99:93b2:af65:48bb'
MVP=/mnt/fast-storage/appdata/muxcore/mvp   # vault MVP root
```

**When to use which:**

- **Changed a module that runs on vault?** Build locally (via `nix-shell`), copy artifacts over SSH, restart with `./run-host.sh` — see [Vault dev / soak environment](#vault-dev--soak-environment) and **Agent deploy policy**.
- **New/changed public hostname or DNS?** SSH to **desk**, run `flarectl` with `CF_API_TOKEN` from `~/.config/cloudflare/token` — see [desk — Cloudflare DNS tooling](#desk--cloudflare-dns-tooling). Grey-cloud records point `*.zem.systems` at dawn/dusk; TLS terminates on the edges, not vault.
- **Cloudflare Tunnel changes?** Also on **desk** — `cloudflared` uses `~/.cloudflared/cert.pem`.
- **NixOS / ingress / vault env vars?** Edit `~/Projects/zemdregon/nix-production`, then `nixos-rebuild switch` to vault/dawn/dusk — see [Deploy workflow after nix changes](#deploy-workflow-after-nix-changes).

LAN fallbacks when on the home network: vault `192.168.40.153`, desk `192.168.68.138`.

**IPv6 scp/rsync:** bracket the address in the remote path:

```bash
scp -6 file "ender@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]:/remote/path"
rsync -avz -e 'ssh -6' ./local/ 'ender@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]:/remote/path/'
```

If SSH fails after a host migration: `ssh-keygen -R <zt-address>`.

### Topology

```
Browser → *.zem.systems (grey-cloud DNS RR)
       → dawn or dusk :443 (nginx + Let's Encrypt)
       → vault over ZeroTier (origin services)
```

- **vault** — NAS / soak host. **Primary MuxCore dev deployment target** — runs `muxcore-host.service` (the MVP stack) and module binaries. Does **not** terminate public HTTPS; dawn/dusk do.
- **dawn** — OVH US-East public edge (ingress, ZT controller identity, Gatus).
- **dusk** — OVH US-West public edge (sister ingress + backup MX).
- **desk** — Home LAN workstation. **DNS & Cloudflare control plane** — `flarectl` (grey-cloud DNS), `cloudflared` (tunnels), `wrangler`; not a MuxCore runtime host.
- **bastion** — Legacy name; ZT address alias for dawn (`zt-addrs.nix`).

MuxCore UIs on vault (proxied publicly):

| Public URL | Service | Vault port |
|------------|---------|------------|
| https://admin.zem.systems | admin-ui | `:8082` |
| https://mux.zem.systems | media-ui-app (mediauiprox) | `:5173` |
| https://auth.zem.systems | auth-local HTTP | `:9401` |

Jellyfin remains at **media.zem.systems** (`:8096`) — not the MuxCore consumer SPA.

### ZeroTier addresses

Canonical map: `nix-production/modules/zt-addrs.nix`

| Host | ZT ULA |
|------|--------|
| vault | `fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95` |
| dawn / bastion | `fd2c:a2fd:5d9e:ab72:9d99:932c:a2fd:5d9e` |
| dusk | `fd2c:a2fd:5d9e:ab72:9d99:939b:2e68:dd6d` |
| desk | `fd2c:a2fd:5d9e:ab72:9d99:93b2:af65:48bb` |

Requires local ZeroTier membership on network `2ca2fd5d9eab729d` (interface often `ztoe6fmlyd`).

### SSH access

See [Key hosts (vault & desk)](#key-hosts-vault--desk) for roles and convenience variables. Details below.

#### vault

```bash
# Preferred: ZT IPv6 as ender
ssh ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95

# Root for nixos-rebuild remote deploys
ssh root@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95

# LAN (when on sequoia.lan)
ssh ender@192.168.40.153
```

**IPv6 scp/rsync:** bracket the address in the remote path:

```bash
scp file ender@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]:/remote/path
rsync -avz -e 'ssh -6' ./local/ 'ender@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]:/remote/path/'
```

If SSH fails after a host migration, remove stale keys:

```bash
ssh-keygen -R fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95
```

#### desk

**DNS & Cloudflare control plane** — run `flarectl`, `cloudflared`, and `wrangler` here (not on vault). See [desk — Cloudflare DNS tooling](#desk--cloudflare-dns-tooling).

```bash
ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:93b2:af65:48bb
# LAN fallback: 192.168.68.138 (Host `desk` in ~/.ssh/config may need key refresh)

# Example: list a public MuxCore hostname (run on desk)
ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:93b2:af65:48bb \
  'export CF_API_TOKEN=$(cat ~/.config/cloudflare/token); flarectl dns list --zone zem.systems --name mux.zem.systems'
```

#### dawn / dusk

```bash
ssh root@[fd2c:a2fd:5d9e:ab72:9d99:932c:a2fd:5d9e]   # dawn
ssh root@[fd2c:a2fd:5d9e:ab72:9d99:939b:2e68:dd6d]   # dusk
```

### Vault dev / soak environment

**SSH to vault** (`ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95`) for MuxCore dev deployment — this is where the live soak stack runs, not your local `_mvp/run-host.sh` checkout (same layout, different machine).

Nix module: `nix-production/modules/muxcore-test.nix` (enabled on vault host).

#### Key paths on vault

| Path | Purpose |
|------|---------|
| `/mnt/fast-storage/appdata/muxcore/mvp` | MVP root (`run-host.sh`, `bin/`, `run/`, `data/`) |
| `/mnt/fast-storage/appdata/muxcore/mvp/bin/` | Module binaries + `muxcorectl` |
| `/mnt/fast-storage/appdata/muxcore/mvp/run/admin.token` | Admin API token for CLI |
| `/mnt/fast-storage/appdata/muxcore/media-ui-app/dist-app/` | Built consumer SPA static files |
| `/mnt/fast-storage/appdata/muxcore/secrets.env` | Secrets injected by NixOS |

Systemd unit: **`muxcore-host.service`** — runs `./run-host.sh up` from the MVP dir.

#### Agent deploy policy (vault)

**Default:** after you change a module repo, **rebuild and redeploy on vault** when that module is already part of the vault stack. Do not leave vault running stale binaries when the user asked for a fix or feature to land there.

**Check whether a module is on vault** (run over SSH):

```bash
VAULT='ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95'
MVP=/mnt/fast-storage/appdata/muxcore/mvp

ssh -6 "$VAULT" "ls -1 $MVP/bin/; pgrep -af '$MVP/bin/' | sed 's|.*/bin/||; s/ .*//' | sort -u"
# optional mesh view:
ssh -6 "$VAULT" "export MUXCORE_GRPC_ADDR=127.0.0.1:9090 MUXCORE_INSECURE_DISABLE_TLS=true; $MVP/bin/muxcorectl modules list 2>/dev/null | head"
```

Treat a module as **present** if its binary exists under `$MVP/bin/` **or** it appears in `./run-host.sh` (always-on or env-gated) and is running / expected for the task.

**One-liner (preferred):** from the workspace, `_mvp/scripts/deploy-module-to-vault.sh` builds (via `nix-shell` when Go/Node are not on PATH), copies over SSH, and restarts the matching `run-host.sh` service. Handles `admin-ui`, `media-ui` / `mediauiprox`, `media-ui-app` (SPA rsync), origin-pinned acquisition modules, `core` / `muxcored`, and `muxcorectl`. Full helper inventory: [`docs/history/USABILITY-DEPLOY-LOOP-SUMMARY.md`](docs/history/USABILITY-DEPLOY-LOOP-SUMMARY.md) (historical).

```bash
_mvp/scripts/deploy-module-to-vault.sh admin-ui --verify-all
_mvp/scripts/deploy-module-to-vault.sh media-ui-app
_mvp/scripts/muxcorectl-vault.sh health status
_mvp/scripts/smoke-vault-health.sh
_mvp/scripts/smoke-vault-public.sh
_mvp/scripts/smoke-vault-all.sh
_mvp/scripts/preflight-vault-ssh.sh
MVP_DEPLOY_SSH=ender@192.168.40.153 _mvp/scripts/deploy-module-to-vault.sh request-media
```

Local stack health: `cd _mvp && ./run-host.sh status`.

**Redeploy a Go module binary** (manual path when the script does not cover your case):

```bash
MODULE=admin-ui   # module dir name == run-host.sh service name in most cases
VAULT='ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95'
MVP=/mnt/fast-storage/appdata/muxcore/mvp

# build (adjust path / ldflags / subdir as needed)
cd "$MODULE"
GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o /tmp/"$MODULE"-linux-amd64 .

# copy while stopped — plain `stop` kills the whole stack; prefer stop-one + restart
scp -6 /tmp/"$MODULE"-linux-amd64 "ender@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]:/tmp/$MODULE.new"
ssh -6 "$VAULT" "cd $MVP && ./run-host.sh stop-one $MODULE && mv /tmp/$MODULE.new bin/$MODULE && chmod +x bin/$MODULE && ./run-host.sh restart $MODULE"
```

**Special cases** (still redeploy when changed, different artifact):

| Module | Build / copy | Restart |
|--------|----------------|---------|
| **admin-ui** | `npm ci` + Tailwind → `assets/dist/styles.css`, then `go build` (CSS is `go:embed`) | `./run-host.sh restart admin-ui` |
| **media-ui-app** | `npm ci && npm run build`, rsync `dist-app/` to `/mnt/fast-storage/appdata/muxcore/media-ui-app/dist-app/` | `./run-host.sh restart media-ui` |
| **media-scanner**, **media-automation**, **downloader-native-torrent** | use `_mvp/scripts/install-origin-module.sh` — do **not** scp ad-hoc binaries | per script / `./run-host.sh restart <name>` |
| **muxcorectl** | build `muxcorectl-cli`, scp to `$MVP/bin/muxcorectl` | no restart (CLI only) |

After deploy, smoke the module (health URL, admin page, or `muxcorectl` as appropriate) and at least one public URL when the service is edge-proxied.

**Module not on vault yet:** if your change depends on a module that is **not** deployed, **suggest adding it** when it would unblock the user's goal — but only when it does **not** conflict with an existing module.

| Want to add | Already on vault | Conflict? | Guidance |
|-------------|------------------|-----------|----------|
| **auth-oidc** | **auth-local** (default) | **Yes** — one `AuthProvider` | Do not offer OIDC while auth-local serves login. Swap only if the user explicitly wants to migrate auth. |
| **auth-local** | **auth-oidc** | **Yes** | Same as above. |
| **secrets-vault** | **secrets-file** | **Yes** — one secrets backend | Pick one; vault soak uses `secrets-file`. |
| **database-*** (non-sqlite) | **database-sqlite** | **Usually yes** | Do not add a second DB module without a migration plan. |
| **notification-apprise** | **notification-default** | **No** | Apprise is an optional extra sink (`MVP_ENABLE_NOTIFICATION_APPRISE=1`). |
| **jellyfin** bridge | NixOS Jellyfin at **media.zem.systems** | **Operational** | Bridge is optional (`MVP_ENABLE_JELLYFIN`); standalone Jellyfin is separate. Do not imply they replace each other. |
| Optional peers (list-sync, dlna, qbit, …) | — | **No** if env-gated | Suggest enabling the matching `MVP_ENABLE_*` flag in `.env` / `muxcore-test.nix`, building the binary, and `./run-host.sh restart <name>`. |

When suggesting a new module, name the **env flag**, **binary**, **port** (`_mvp/PORTS.md`), and whether **admin-ui** or **nix-production** config also needs updating.

Check stack health:

```bash
ssh ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95 \
  'systemctl is-active muxcore-host; curl -s -o /dev/null -w "admin:%{http_code}\n" http://127.0.0.1:8082/health; curl -s -o /dev/null -w "media:%{http_code}\n" http://127.0.0.1:5173/; curl -s -o /dev/null -w "auth:%{http_code}\n" http://127.0.0.1:9401/login'
```

#### muxcorectl on vault

Build locally, copy binary, smoke-test:

```bash
cd muxcorectl-cli
GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o /tmp/muxcorectl-linux-amd64 ./cmd/muxcorectl
scp /tmp/muxcorectl-linux-amd64 \
  'ender@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]:/mnt/fast-storage/appdata/muxcore/mvp/bin/muxcorectl'

ssh ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95 bash -s <<'EOF'
export PATH="/mnt/fast-storage/appdata/muxcore/mvp/bin:$PATH"
export MUXCORE_INSECURE_DISABLE_TLS=true MUXCORE_MESH_DIAL_LOCAL=true
export MUXCORE_GRPC_ADDR=127.0.0.1:9090
export MUXCORE_TOKEN="$(cat /mnt/fast-storage/appdata/muxcore/mvp/run/admin.token)"
muxcorectl health status
muxcorectl modules list | head
EOF
```

#### media-ui-app + mediauiprox

Frontend changes: build locally, rsync `dist-app/`, restart the module:

```bash
cd media-ui-app && npm ci && npm run build

rsync -avz --delete \
  -e 'ssh -6 -i ~/.ssh/id_ed25519 -o BatchMode=yes' \
  media-ui-app/dist-app/ \
  'ender@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]:/mnt/fast-storage/appdata/muxcore/media-ui-app/dist-app/'

ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95 \
  'cd /mnt/fast-storage/appdata/muxcore/mvp && ./run-host.sh restart media-ui'
```

BFF changes also require rebuilding `mediauiprox` and copying to `mvp/bin/mediauiprox`, then the same restart.

#### Origin-pinned Go modules (acquisition stack)

Do **not** scp unpublished binaries for `media-scanner`, `media-automation`, or `downloader-native-torrent`. Use:

```bash
cd _mvp
./scripts/install-origin-module.sh media-scanner --verify-only
./scripts/install-origin-module.sh media-automation --dest ./bin
MVP_DEPLOY_SSH=ender@fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95 \
  ./scripts/install-origin-module.sh --verify-live
```

See `_mvp/docs/ACQUISITION.md` for vault acquisition workflow.

#### Local dev stack

Local reference: `_mvp/run-host.sh` (same layout as vault MVP). Ports: `_mvp/PORTS.md`. Mesh gRPC on vault is **not** published to LAN (`:9090` stays on the VPN veth).

### desk — Cloudflare DNS tooling

**SSH to desk first** (`ssh -6 ender@fd2c:a2fd:5d9e:ab72:9d99:93b2:af65:48bb`). Desk is the control plane for public `*.zem.systems` DNS and Cloudflare Tunnel credentials — vault only runs the MuxCore stack behind dawn/dusk ingress.

Cloudflare CLI tools live on **desk** in `~/.nix-profile/bin/`:

| Tool | Use |
|------|-----|
| **flarectl** | Grey-cloud DNS records on `zem.systems` (A/AAAA → dawn + dusk) |
| **cloudflared** | Tunnel management — cert at `~/.cloudflared/cert.pem` |
| **wrangler** / **cfcli** | Workers / general CF API |

API token: `~/.config/cloudflare/token` → export as `CF_API_TOKEN`.

#### Create / inspect DNS (grey-cloud RR to dawn + dusk)

Run on desk (or over SSH to desk):

```bash
export CF_API_TOKEN=$(cat ~/.config/cloudflare/token)

# List
flarectl dns list --zone zem.systems --name mux.zem.systems

# Create A/AAAA for both edges (example IPs — confirm in fleet.yml)
DAWN4=40.160.83.185;  DAWN6=2604:2dc0:121::cf
DUSK4=15.204.123.78;  DUSK6=2604:2dc0:222::d0a

for name in admin mux auth; do
  flarectl dns create --zone zem.systems --name "$name" --type A     --content "$DAWN4"
  flarectl dns create --zone zem.systems --name "$name" --type AAAA  --content "$DAWN6"
  flarectl dns create --zone zem.systems --name "$name" --type A     --content "$DUSK4"
  flarectl dns create --zone zem.systems --name "$name" --type AAAA  --content "$DUSK6"
done
```

Records are **grey-cloud** (`proxied: false` in `fleet.yml`). Clients hit dawn/dusk directly; TLS terminates on the edge nginx.

Inventory mirror: `nix-production/modules/infrahub/objects/fleet.yml` (DNS + service objects).

### dawn / dusk — adding or changing proxy entries

Public reverse proxy config: **`nix-production/modules/roles/ingress.nix`**

Pattern for a new vault-origin vhost:

```nix
"example.zem.systems" = {
  locations."/" = {
    proxyPass = "http://[${vaultZt}]:PORT";
    proxyWebsockets = true;
  };
} // sharedTls;
```

Both **dawn** and **dusk** import this via `modules/roles/edge-public.nix` → `ingress.nix`. Shared names get DNS-01 ACME when `acme/cloudflareEnv` is in sops (`secrets/acme.yaml`).

#### MuxCore-specific env on vault

When public URLs change, also update **`muxcore-test.nix`** env vars:

- `ADMIN_UI_PUBLIC_URL`, `MEDIA_UI_PUBLIC_URL`, `AUTH_HTTP_URL`
- `ADMIN_UI_TRUSTED_PROXIES`, `AUTH_TRUSTED_PROXIES` — must include dawn/dusk/bastion ZT `/128` entries

Then rebuild **vault** so `muxcore-host` picks up new env.

#### Deploy workflow after nix changes

From a machine with Nix (`thin`, `desk`, etc.):

```bash
cd ~/Projects/zemdregon/nix-production
nix flake check -L   # optional sanity

# Edge hosts (nginx + ACME)
nixos-rebuild switch --flake .#dawn --target-host 'root@[fd2c:a2fd:5d9e:ab72:9d99:932c:a2fd:5d9e]'
nixos-rebuild switch --flake .#dusk  --target-host 'root@[fd2c:a2fd:5d9e:ab72:9d99:939b:2e68:dd6d]'

# Vault (MuxCore env + systemd)
nixos-rebuild switch --flake .#vault --target-host 'root@[fd2c:a2fd:5d9e:ab72:9d99:930d:f160:3e95]'
```

Builds can take several minutes. After edge deploy, verify ACME units:

```bash
ssh root@fd2c:a2fd:5d9e:ab72:9d99:932c:a2fd:5d9e \
  'systemctl is-active nginx acme-admin.zem.systems.service acme-mux.zem.systems.service'
```

Verify routing locally on an edge:

```bash
curl -sk -o /dev/null -w '%{http_code}\n' https://127.0.0.1/health -H 'Host: admin.zem.systems'
```

Public smoke:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' https://admin.zem.systems/health
curl -sS -o /dev/null -w '%{http_code}\n' https://mux.zem.systems/
curl -sS -o /dev/null -w '%{http_code}\n' https://auth.zem.systems/login
```

Health checks also in `nix-production/modules/uptime.nix` (Gatus on dawn).

### Common pitfalls (owner deployment)

1. **Stale SSH host keys** after gryffindor→desk or host reinstall — `ssh-keygen -R <addr>`.
2. **IPv6 scp** needs brackets around the address in the remote spec.
3. **Do not put Caddy on vault :443** — dawn/dusk own public HTTPS (`muxcore-test.nix` comment).
4. **Trusted proxies** — auth/admin break behind dawn/dusk if ZT addresses are missing from `*_TRUSTED_PROXIES`.
5. **Acquisition modules** — use `install-origin-module.sh`, not ad-hoc binary drops.
6. **media.zem.systems ≠ mux.zem.systems** — Jellyfin vs MuxCore consumer UI.
7. **`run-host.sh stop`** kills the **entire** stack — use **`stop-one <name>`** + **`restart <name>`** when swapping one binary.
8. **Stale vault binaries** — if you edited a module repo, rebuild and redeploy per **Agent deploy policy** above; do not assume vault picked up local changes.

## Related docs in this repo

- [`MASTER-ROADMAP.md`](MASTER-ROADMAP.md) — canonical plan (§3 is the full documentation map)
- [`docs/specs/`](docs/specs/SRS.md), [`docs/architecture/`](docs/architecture/HLD.md), [`docs/adr/`](docs/adr/README.md) — specifications, design, decisions
- `_mvp/README.md` — local MVP stack
- `_mvp/docs/ACQUISITION.md` — vault acquisition + origin pinning
- `_mvp/PORTS.md` — default module ports
- `muxcorectl-cli/README.md` — CLI usage
