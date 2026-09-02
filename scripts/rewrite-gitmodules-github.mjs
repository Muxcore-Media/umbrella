#!/usr/bin/env node
/**
 * Rewrite .gitmodules submodule URLs to GitHub (Muxcore-Media/*).
 * Forgejo repo tail → GitHub repo name (when they differ).
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const ROOT = join(import.meta.dirname, '..');
const GITMODULES = join(ROOT, '.gitmodules');

/** submodule path → GitHub repo name */
const GITHUB_REPO = {
  'media-tvos-app': 'muxcore-tvos',
  _mvp: 'mvp',
  _wave1: 'wave1',
  'core.wiki': 'core.wiki',
  muxcorectl: 'muxcorectl-cli',
  'media-ui': 'media-ui-app',
  'media-jellyfin': 'jellyfin',
};

function forgejoRepoFromUrl(url) {
  const m = url.match(/\/muxcore\/([^/]+)\.git$/);
  if (!m) throw new Error(`unexpected forgejo url: ${url}`);
  return m[1];
}

function githubUrl(path, forgejoUrl) {
  const tail = forgejoRepoFromUrl(forgejoUrl);
  const repo = GITHUB_REPO[path] ?? tail.replace(/^core-wiki$/, 'core.wiki');
  return `https://github.com/Muxcore-Media/${repo}.git`;
}

const raw = readFileSync(GITMODULES, 'utf8');
const blocks = raw.trimEnd().split(/\n(?=\[submodule )/);
const out = [];

for (const block of blocks) {
  const nameM = block.match(/^\[submodule "([^"]+)"\]/);
  const pathM = block.match(/\n\tpath = (.+)/);
  const urlM = block.match(/\n\turl = (.+)/);
  if (!nameM || !pathM || !urlM) {
    out.push(block);
    continue;
  }
  const path = pathM[1];
  const url = urlM[1];
  const gh = githubUrl(path, url);
  out.push(
    `[submodule "${nameM[1]}"]\n\tpath = ${path}\n\turl = ${gh}`,
  );
}

writeFileSync(GITMODULES, `${out.join('\n')}\n`);
console.log(`updated ${out.length} submodule URLs in .gitmodules`);
