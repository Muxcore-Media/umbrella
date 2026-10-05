> **Historical record** (moved to `docs/history/` on 2026-10-05). Superseded by the canonical plan in [`MASTER-ROADMAP.md`](../../MASTER-ROADMAP.md) ([ADR-0001](../adr/0001-canonical-roadmap-and-spec-set.md)). Mentions of the former self-hosted origin are historical ([ADR-0002](../adr/0002-github-sole-origin.md)). Not maintained.

# Usability & Deploy Loop Summary — Cycles 150–167

**State:** cycle **167** complete, loop **running** (`wake: 600s`)  
**Focus:** deployment scripts, vault smoke helpers, docs, CI — not feature code

---

## Deliverables

| Cycle | What shipped |
|-------|----------------|
| **150** | `deploy-module-to-vault.sh`; `run-host.sh status`; AGENTS.md deploy one-liner |
| **151** | `muxcorectl-vault.sh`; `smoke-vault-health.sh`; deploy aliases + `--verify` |
| **152** | `smoke-vault-public.sh`; deploy `--verify-public`; installer homelab README section |
| **153** | deploy `--list`; installer VIEW-ME / finish-screen hints |
| **154** | `run-host.sh help` |
| **155** | `deploy-module-to-vault_test.sh` |
| **156** | `smoke-vault_test.sh`; `run-script-tests.sh` runner |
| **157** | Forgejo CI `script-tests` job in `_mvp/.forgejo/workflows/ci.yml` |
| **158** | `README-UMBRELLA.md` vault deploy table |
| **159** | `muxcorectl-cli` README + AGENTS vault helpers |
| **162** | `smoke-vault-all.sh`; `preflight-vault-ssh.sh`; deploy `--verify-all`; verify SSH target fix |
| **163** | `install-origin-module` ssh `-6` + preflight; README-UMBRELLA refresh |
| **164** | `run-host.sh cleanup-stale`; ACQUISITION vault section |
| **165** | deploy unknown-module guard; `--list` smoke helpers; smoke `--help` |
| **166** | `muxcore-installer` homelab table sync |
| **167** | muxcorectl-cli docs + this summary refresh |

---

## Quick reference (workspace root)

```bash
_mvp/scripts/deploy-module-to-vault.sh --list
_mvp/scripts/deploy-module-to-vault.sh <module> --verify-all
_mvp/scripts/preflight-vault-ssh.sh
_mvp/scripts/smoke-vault-all.sh
_mvp/scripts/muxcorectl-vault.sh health status
_mvp/scripts/smoke-vault-health.sh
_mvp/scripts/smoke-vault-public.sh
_mvp/scripts/run-script-tests.sh
cd _mvp && ./run-host.sh status
cd _mvp && ./run-host.sh cleanup-stale
```

Canonical ops doc: [`AGENTS.md`](AGENTS.md) → **Agent deploy policy**.

---

## Themes

1. **One-command vault deploy** — build (nix-shell-aware), scp, `stop-one` + `restart`, origin-pinned modules, SPA rsync
2. **Smoke from desk** — local HTTP on vault via SSH; public edge via curl to `*.zem.systems`; muxcorectl mesh health
3. **Discoverability** — `--list`, `help`, README tables across umbrella, installer, muxcorectl-cli
4. **Regression guard** — offline bash tests + Forgejo `script-tests` CI job
5. **Fail fast** — SSH preflight, unknown-module guard, stale pidfile cleanup

---

## Next queued

- Maintenance ticks unless vault smoke fails or deploy friction surfaces
