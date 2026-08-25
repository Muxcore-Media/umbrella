import assert from 'node:assert/strict';
import { join } from 'node:path';
import { dirname } from 'node:path';
import { describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';
import {
  findPythonOnPath,
  isExecutable,
  main,
  resolveMainScript,
  resolvePythonPath,
  resolveVenvPython,
  resolveWorkspaceRoot,
} from './muxidx.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const workspaceRoot = resolveWorkspaceRoot(here);

describe('resolveWorkspaceRoot', () => {
  it('resolves the MuxCore root from the muxidx script directory', () => {
    assert.equal(resolveWorkspaceRoot(here), workspaceRoot);
    assert.match(resolveWorkspaceRoot(here), /MuxCore$/);
  });
});

describe('resolveMainScript', () => {
  it('points at main.py beside the launcher', () => {
    assert.equal(resolveMainScript(here), join(here, 'main.py'));
  });
});

describe('resolveVenvPython', () => {
  it('uses Unix venv layout by default', () => {
    assert.equal(
      resolveVenvPython('/repo', 'linux'),
      join('/repo', '.venv', 'bin', 'python3'),
    );
  });

  it('uses Windows venv layout on win32', () => {
    assert.equal(
      resolveVenvPython('/repo', 'win32'),
      join('/repo', '.venv', 'Scripts', 'python.exe'),
    );
  });
});

describe('resolvePythonPath', () => {
  it('prefers the workspace venv when it is executable', () => {
    const venvPy = resolveVenvPython('/repo', 'linux');
    const access = (path, mode) => {
      if (path === venvPy && mode) return;
      throw new Error(`unexpected access: ${path}`);
    };
    assert.equal(
      resolvePythonPath({
        workspaceRoot: '/repo',
        scriptDir: '/repo/scripts/muxidx',
        access,
        execFile: () => {
          throw new Error('should not search PATH');
        },
        platform: 'linux',
      }),
      venvPy,
    );
  });

  it('falls back to python3 on PATH when venv is missing', () => {
    const access = () => {
      throw new Error('not executable');
    };
    assert.equal(
      resolvePythonPath({
        workspaceRoot: '/repo',
        scriptDir: '/repo/scripts/muxidx',
        access,
        execFile: () => '/usr/bin/python3\n',
        platform: 'linux',
      }),
      '/usr/bin/python3',
    );
  });

  it('throws when no python interpreter is available', () => {
    assert.throws(
      () =>
        resolvePythonPath({
          workspaceRoot: '/repo',
          scriptDir: '/repo/scripts/muxidx',
          access: () => {
            throw new Error('not executable');
          },
          execFile: () => {
            throw new Error('missing');
          },
          platform: 'linux',
        }),
      /python3 is required to run muxidx/,
    );
  });
});

describe('findPythonOnPath', () => {
  it('returns the first line from which/where output', () => {
    assert.equal(
      findPythonOnPath(() => '/opt/python3\n/opt/python3.12\n', 'linux'),
      '/opt/python3',
    );
  });
});

describe('isExecutable', () => {
  it('returns false when access throws', () => {
    assert.equal(
      isExecutable('/missing', () => {
        throw new Error('ENOENT');
      }),
      false,
    );
  });
});

describe('main', () => {
  it('spawns python on main.py with forwarded argv', async () => {
    const calls = [];
    const venvPy = resolveVenvPython(workspaceRoot, process.platform);
    const code = await main(['search', 'hello'], {
      scriptDir: here,
      workspaceRoot,
      access: (path) => {
        if (path === venvPy) return;
        throw new Error(`unexpected access: ${path}`);
      },
      execFile: () => venvPy,
      spawn: (python, args) => {
        calls.push({ python, args });
        return { status: 0 };
      },
    });

    assert.equal(code, 0);
    assert.equal(calls.length, 1);
    assert.equal(calls[0].python, venvPy);
    assert.deepEqual(calls[0].args, [join(here, 'main.py'), 'search', 'hello']);
  });

  it('returns 1 when python cannot be resolved', async () => {
    const code = await main([], {
      scriptDir: here,
      workspaceRoot,
      access: () => {
        throw new Error('missing');
      },
      execFile: () => {
        throw new Error('missing');
      },
    });
    assert.equal(code, 1);
  });
});
