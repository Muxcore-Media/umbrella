#!/usr/bin/env node
/**
 * Generate thin AGENTS.md for MuxCore module repos from muxcore.json.
 */
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const SKIP_DIR_NAMES =
  /^(muxcorectl|media-ui|cache-memory|custom-scripts|media-jellyfin|muxcore-module-starter\/cookiecutter)$/;

export const SPECIAL_DIRS = ['core', 'muxcorectl-cli', 'admin-ui', '_mvp'];

const scriptDir = dirname(fileURLToPath(import.meta.url));
export const defaultRoot = resolve(scriptDir, '..');

export function formatContracts(contracts) {
  const cs = contracts ?? [];
  if (cs.length === 0) return 'none declared';
  return cs
    .map((c) => {
      const iface = c?.interface ?? '';
      const repoTail = String(c?.repo ?? '')
        .split('/')
        .pop();
      return `${iface} (${repoTail})`;
    })
    .join('; ');
}

export function formatCapabilities(capabilities) {
  if (!capabilities?.length) return 'see muxcore.json';
  return capabilities.join(', ');
}

export function readMuxcoreMeta(muxcorePath, readFile = readFileSync) {
  try {
    const d = JSON.parse(readFile(muxcorePath, 'utf8'));
    return {
      modname: d.name ?? '',
      capabilities: d.capabilities ?? [],
      contracts: d.contracts ?? [],
      ok: true,
    };
  } catch {
    return {
      modname: '',
      capabilities: [],
      contracts: [],
      ok: false,
    };
  }
}

export function buildAgentsMarkdown({ modname, dirName, capabilities, contracts }) {
  const caps = formatCapabilities(capabilities);
  const contractsText = formatContracts(contracts);
  return `# AGENTS.md — ${modname}

MuxCore sidecar module (\`${dirName}\`). Workspace deploy and SSH: [\`../AGENTS.md\`](../AGENTS.md). Default ports: [\`_mvp/PORTS.md\`](../_mvp/PORTS.md).

## Module identity

| Field | Value |
|-------|-------|
| Directory | \`${dirName}\` |
| Capabilities | ${caps} |
| Contracts | ${contractsText} |

## Agent rules

- Modules run as gRPC sidecars; capabilities are the security boundary.
- TLS required in production (\`MUXCORE_INSECURE_DISABLE_TLS\` is dev-only).
- Match existing Go patterns; run \`gofmt\` and package tests before finishing.
- Cross-module events: prefer \`github.com/Muxcore-Media/contracts-media/events\` over deprecated \`core/pkg/contracts\` aliases.
- Do not edit polluted workspace dumps (see \`MASTER-ROADMAP.md\` Appendix H).
- Roadmaps, task lists, and remaining-work checklists live in workspace [\`MASTER-ROADMAP.md\`](../MASTER-ROADMAP.md) and umbrella GitHub Issues. Do not add \`ROADMAP.md\` / \`TASKS.md\` in this repo.

## Build

\`\`\`bash
cd ${dirName}
go test ./...
\`\`\`
`;
}

export function shouldProcessModule(name, { hasMuxcore, hasAgents }) {
  if (SKIP_DIR_NAMES.test(name)) return false;
  if (!hasMuxcore) return false;
  if (hasAgents && name === 'media-ui-app') return false;
  return true;
}

export function generateModuleAgents(
  root,
  {
    log = console.log,
    exists = (p) => {
      try {
        readFileSync(p);
        return true;
      } catch {
        return false;
      }
    },
    readdir = readdirSync,
    readFile = readFileSync,
    writeFile = writeFileSync,
  } = {},
) {
  const written = [];
  for (const dirent of readdir(root, { withFileTypes: true })) {
    if (!dirent.isDirectory()) continue;
    const name = dirent.name;
    const dir = join(root, name);
    const muxcorePath = join(dir, 'muxcore.json');
    const agentsPath = join(dir, 'AGENTS.md');
    const hasMuxcore = exists(muxcorePath);
    const hasAgents = exists(agentsPath);

    if (!shouldProcessModule(name, { hasMuxcore, hasAgents })) continue;

    const meta = readMuxcoreMeta(muxcorePath, readFile);
    const modname = meta.ok && meta.modname ? meta.modname : name;
    const content = buildAgentsMarkdown({
      modname,
      dirName: name,
      capabilities: meta.ok ? meta.capabilities : [],
      contracts: meta.ok ? meta.contracts : [],
    });
    writeFile(agentsPath, content, 'utf8');
    log(`wrote ${dir}/AGENTS.md`);
    written.push({ dir, name });
  }

  for (const special of SPECIAL_DIRS) {
    if (!exists(join(root, special))) continue;
  }

  return written;
}

function parseArgs(argv) {
  const opts = { root: defaultRoot };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--root' && argv[i + 1]) {
      opts.root = resolve(argv[++i]);
    } else if (arg === '--help' || arg === '-h') {
      opts.help = true;
    }
  }
  return opts;
}

export async function main(
  argv,
  {
    root: rootInjected,
    log = console.log,
    exists,
    readdir,
    readFile,
    writeFile,
  } = {},
) {
  const opts = parseArgs(argv);
  if (opts.help) {
    log('Usage: generate-module-agents.mjs [--root <workspace>]');
    return 0;
  }

  const root = rootInjected ?? opts.root;
  generateModuleAgents(root, { log, exists, readdir, readFile, writeFile });
  log('done');
  return 0;
}

function isDirectRun(metaUrl) {
  const entry = process.argv[1];
  if (!entry) return false;
  return pathToFileURL(resolve(entry)).href === metaUrl;
}

if (isDirectRun(import.meta.url)) {
  main(process.argv.slice(2))
    .then((code) => process.exit(code ?? 0))
    .catch((err) => {
      console.error(err.message || err);
      process.exit(1);
    });
}
