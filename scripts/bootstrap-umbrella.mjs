#!/usr/bin/env node
/**
 * Convert the MuxCore workspace folder into an umbrella git repo with GitHub
 * modules registered as submodules (gitlinks). Safe to re-run: skips if .git exists.
 */
import { execFileSync } from 'node:child_process';
import { existsSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const SKIP_DIRS = new Set([]);

export const COMMIT_MESSAGE = `chore: initialize MuxCore umbrella workspace

Register module checkouts as submodules and track workspace
tooling (.cursor/skills, scripts/app-fanout, AGENTS.md) at the root.`;

export const OWNED_PATHS = [
  '.gitignore',
  '.cursor',
  'scripts',
  'AGENTS.md',
  'README-UMBRELLA.md',
  'platform-quality-fanout.tsx',
];

const scriptDir = dirname(fileURLToPath(import.meta.url));
export const defaultRoot = resolve(scriptDir, '..');

export function gitAt(cwd, args, { encoding = 'utf8' } = {}) {
  return execFileSync('git', ['-C', cwd, ...args], {
    encoding,
    stdio: ['ignore', 'pipe', 'pipe'],
  }).trim();
}

export function hasGitCommand() {
  try {
    execFileSync('git', ['--version'], { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

export function discoverModules(
  root,
  {
    skipDirs = SKIP_DIRS,
    readdir = readdirSync,
    exists = existsSync,
    gitRemote = (dir) => gitAt(dir, ['remote', 'get-url', 'origin']),
  } = {},
) {
  const modules = [];
  for (const dirent of readdir(root, { withFileTypes: true })) {
    if (!dirent.isDirectory() || dirent.name.startsWith('.')) continue;
    if (skipDirs.has(dirent.name)) continue;
    const dir = join(root, dirent.name);
    if (!exists(join(dir, '.git'))) continue;
    let url;
    try {
      url = gitRemote(dir);
    } catch {
      continue; // not a usable git checkout (e.g. invalid gitfile)
    }
    if (!url) continue;
    modules.push({ path: dirent.name, url });
  }
  return modules.sort((a, b) => a.path.localeCompare(b.path));
}

export function buildGitmodulesContent(modules) {
  const lines = [];
  for (const { path, url } of modules) {
    lines.push(`[submodule "${path}"]`);
    lines.push(`\tpath = ${path}`);
    lines.push(`\turl = ${url}`);
  }
  return lines.length ? `${lines.join('\n')}\n` : '';
}

export function listRootMarkdown(root, { readdir = readdirSync, exists = existsSync } = {}) {
  const files = [];
  for (const name of readdir(root)) {
    if (name.endsWith('.md') && exists(join(root, name))) {
      files.push(name);
    }
  }
  return files.sort();
}

export function stageOwnedPaths(root, runGit, { exists = existsSync } = {}) {
  for (const rel of OWNED_PATHS) {
    if (exists(join(root, rel))) {
      runGit(['add', rel], { allowFail: true });
    }
  }
  for (const name of listRootMarkdown(root, { readdir: readdirSync, exists })) {
    runGit(['add', name], { allowFail: true });
  }
  if (exists(join(root, '.github'))) {
    runGit(['add', '.github'], { allowFail: true });
  }
}

export async function main(
  argv,
  {
    root = defaultRoot,
    log = console.log,
    error = console.error,
    exists = existsSync,
    hasGit = hasGitCommand,
    discover = discoverModules,
    runGit: runGitInjected,
  } = {},
) {
  if (exists(join(root, '.git'))) {
    log('Umbrella already initialized (.git exists). Nothing to do.');
    return 0;
  }

  if (!hasGit()) {
    error('git is required');
    return 1;
  }

  const modules = discover(root);
  if (modules.length === 0) {
    error(`No module git repos found under ${root}`);
    return 1;
  }

  const runGit = runGitInjected ?? ((args, { allowFail = false } = {}) => {
    try {
      execFileSync('git', ['-C', root, ...args], {
        stdio: allowFail ? ['ignore', 'ignore', 'ignore'] : 'inherit',
      });
    } catch (err) {
      if (!allowFail) throw err;
    }
  });

  log(`Initializing umbrella with ${modules.length} submodules…`);
  runGit(['init', '-b', 'main']);

  writeFileSync(join(root, '.gitmodules'), buildGitmodulesContent(modules), 'utf8');

  runGit(['add', '.gitmodules']);
  runGit(['submodule', 'absorbgitdirs']);

  for (const { path } of modules) {
    runGit(['add', path]);
  }

  stageOwnedPaths(root, runGit, { exists });

  try {
    execFileSync('git', ['-C', root, 'diff', '--cached', '--quiet'], {
      stdio: 'ignore',
    });
    error('Nothing staged — check for errors');
    return 1;
  } catch {
    // diff --quiet exits 1 when there are staged changes
  }

  execFileSync('git', ['-C', root, 'commit', '-m', COMMIT_MESSAGE], {
    stdio: 'inherit',
  });

  log('Umbrella ready on branch main.');
  log('Next: git remote add origin https://github.com/Muxcore-Media/umbrella.git && git push -u origin main');
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
