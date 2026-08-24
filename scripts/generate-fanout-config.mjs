#!/usr/bin/env node
/**
 * Regenerate scripts/app-fanout.config.json module lists from the workspace.
 * Run from MuxCore root: node scripts/generate-fanout-config.mjs
 */
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const CONFIG_PATH = join(ROOT, 'scripts', 'app-fanout.config.json');

const FRONTENDS = new Set([
  'media-ui-app',
  'admin-ui',
  'muxcore-docs',
  'media-ui',
  'media-tvos-app',
  'muxcore-ios',
  'muxcore-android',
]);
const PLATFORM = new Set([
  'core',
  '_mvp',
  'muxcorectl-cli',
  'muxcorectl',
  'muxcore-operator',
  'muxcore-installer',
  'muxcore-module-starter',
  'workflow-tapestry',
  'custom-scripts',
]);
const SKIP = new Set(['core.wiki', '_wave1']);

const dirs = readdirSync(ROOT, { withFileTypes: true })
  .filter((d) => d.isDirectory() && !d.name.startsWith('.'))
  .map((d) => d.name)
  .filter((name) => existsSync(join(ROOT, name, '.git')))
  .filter((name) => !SKIP.has(name));

const trees = (ids) => ids.sort().map((id) => ({ id, path: id }));
const existing = JSON.parse(readFileSync(CONFIG_PATH, 'utf8'));

existing.workspace = {
  multiRepo: false,
  umbrella: true,
  name: 'MuxCore',
  docs: 'AGENTS.md',
};
existing.skills['frontend-page-accessibility'].trees = trees(dirs.filter((d) => FRONTENDS.has(d)));
existing.skills['frontend-format'].trees = trees(dirs.filter((d) => FRONTENDS.has(d)));
existing.skills['frontend-lint'].trees = trees(dirs.filter((d) => FRONTENDS.has(d)));
existing.skills['backend-format'].trees = trees(
  dirs.filter((d) => !FRONTENDS.has(d) && !PLATFORM.has(d)),
);
existing.skills['backend-lint'].trees = trees(
  dirs.filter((d) => !FRONTENDS.has(d) && !PLATFORM.has(d)),
);
existing.skills['platform-format'].trees = trees(dirs.filter((d) => PLATFORM.has(d)));
existing.skills['platform-lint'].trees = trees(dirs.filter((d) => PLATFORM.has(d)));

writeFileSync(CONFIG_PATH, `${JSON.stringify(existing, null, 2)}\n`);
console.log(
  `Updated ${CONFIG_PATH}: ${existing.skills['frontend-lint'].trees.length} frontends, ${existing.skills['backend-lint'].trees.length} backends, ${existing.skills['platform-lint'].trees.length} platform`,
);
