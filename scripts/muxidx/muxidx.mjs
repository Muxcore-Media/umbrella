#!/usr/bin/env node
import { execFileSync, spawnSync } from 'node:child_process';
import { accessSync, constants } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));

export function resolveWorkspaceRoot(scriptDir = SCRIPT_DIR) {
  return join(scriptDir, '..', '..');
}

export function resolveMainScript(scriptDir = SCRIPT_DIR) {
  return join(scriptDir, 'main.py');
}

export function resolveVenvPython(workspaceRoot, platform = process.platform) {
  if (platform === 'win32') {
    return join(workspaceRoot, '.venv', 'Scripts', 'python.exe');
  }
  return join(workspaceRoot, '.venv', 'bin', 'python3');
}

export function isExecutable(path, access = accessSync) {
  try {
    access(path, constants.X_OK);
    return true;
  } catch {
    return false;
  }
}

export function findPythonOnPath(execFile = execFileSync, platform = process.platform) {
  const cmd = platform === 'win32' ? 'where' : 'which';
  try {
    const out = execFile(cmd, ['python3'], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    return out.trim().split(/\r?\n/)[0] || null;
  } catch {
    return null;
  }
}

export function resolvePythonPath({
  workspaceRoot,
  scriptDir = SCRIPT_DIR,
  access = accessSync,
  execFile = execFileSync,
  platform = process.platform,
} = {}) {
  const root = workspaceRoot ?? resolveWorkspaceRoot(scriptDir);
  const venvPy = resolveVenvPython(root, platform);
  if (isExecutable(venvPy, access)) {
    return venvPy;
  }
  const onPath = findPythonOnPath(execFile, platform);
  if (onPath) {
    return onPath;
  }
  throw new Error('python3 is required to run muxidx');
}

export async function main(argv, opts = {}) {
  const scriptDir = opts.scriptDir ?? SCRIPT_DIR;
  const workspaceRoot = opts.workspaceRoot ?? resolveWorkspaceRoot(scriptDir);
  const mainPy = resolveMainScript(scriptDir);

  let python;
  try {
    python = resolvePythonPath({ workspaceRoot, scriptDir, ...opts });
  } catch (err) {
    console.error(err.message || err);
    return 1;
  }

  const spawn = opts.spawn ?? spawnSync;
  const result = spawn(python, [mainPy, ...argv], {
    stdio: 'inherit',
    env: opts.env ?? process.env,
  });

  if (result.error) {
    console.error(result.error.message || result.error);
    return 1;
  }
  return result.status ?? 1;
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
