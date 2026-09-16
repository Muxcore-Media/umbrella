#!/usr/bin/env node
/**
 * Guard: module repos must not ship AGENTS.md.
 * Unique notes live in docs/agents/; shared sidecar rules in docs/agents/sidecar.md.
 */
import { readdirSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

/** Appendix H dumps — not modules; AGENTS.md there is ignored. */
export const POLLUTED_DUMPS = new Set([
  'muxcorectl',
  'media-ui',
  'cache-memory',
  'custom-scripts',
  'media-jellyfin',
]);

/** Directory names not walked when hunting leftover AGENTS.md. */
export const SKIP_DIR_NAMES = new Set([
  ...POLLUTED_DUMPS,
  '.git',
  'node_modules',
  'vendor',
  'dist',
  'dist-app',
  '.worktrees',
]);

const scriptDir = dirname(fileURLToPath(import.meta.url));
export const defaultRoot = resolve(scriptDir, '..');

export function shouldSkipDir(name) {
  if (SKIP_DIR_NAMES.has(name)) return true;
  if (name.startsWith('.') && name !== '.') return true;
  return false;
}

/**
 * Relative paths of AGENTS.md files that belong in a module (or cookiecutter
 * template). The umbrella root AGENTS.md is allowed.
 */
export function listForbiddenAgents(
  root,
  {
    readdir = readdirSync,
  } = {},
) {
  const found = [];

  function walk(absDir) {
    let entries;
    try {
      entries = readdir(absDir, { withFileTypes: true });
    } catch {
      return;
    }
    for (const entry of entries) {
      const name = entry.name;
      const abs = join(absDir, name);
      if (entry.isDirectory()) {
        if (shouldSkipDir(name)) continue;
        walk(abs);
        continue;
      }
      if (name !== 'AGENTS.md') continue;
      const rel = relative(root, abs).replaceAll('\\', '/');
      if (rel === 'AGENTS.md') continue;
      found.push(rel);
    }
  }

  walk(root);
  return found.sort();
}

export function checkNoModuleAgents(root, opts = {}) {
  const forbidden = listForbiddenAgents(root, opts);
  return { ok: forbidden.length === 0, forbidden };
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
    error = console.error,
    readdir,
  } = {},
) {
  const opts = parseArgs(argv);
  if (opts.help) {
    log(`Usage: generate-module-agents.mjs [--root <workspace>]

Fail if any module (or cookiecutter template) still has AGENTS.md.
Module agent notes belong in docs/agents/ in the umbrella.`);
    return 0;
  }

  const root = rootInjected ?? opts.root;
  const { ok, forbidden } = checkNoModuleAgents(root, { readdir });
  if (!ok) {
    error('module AGENTS.md is forbidden; move notes to docs/agents/:');
    for (const p of forbidden) error(`  ${p}`);
    return 1;
  }
  log('ok: no module AGENTS.md');
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
