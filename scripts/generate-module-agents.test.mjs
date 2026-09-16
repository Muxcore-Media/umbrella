import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, describe, it } from 'node:test';
import {
  POLLUTED_DUMPS,
  SKIP_DIR_NAMES,
  checkNoModuleAgents,
  listForbiddenAgents,
  main,
  shouldSkipDir,
} from './generate-module-agents.mjs';

const temps = [];

function tempRoot() {
  const dir = mkdtempSync(join(tmpdir(), 'generate-module-agents-'));
  temps.push(dir);
  return dir;
}

after(() => {
  for (const dir of temps) {
    rmSync(dir, { recursive: true, force: true });
  }
});

describe('shouldSkipDir', () => {
  it('skips polluted dumps and VCS/build dirs', () => {
    for (const name of ['muxcorectl', 'media-ui', 'cache-memory', '.git', 'node_modules']) {
      assert.equal(shouldSkipDir(name), true, name);
      assert.ok(SKIP_DIR_NAMES.has(name), name);
    }
    for (const name of POLLUTED_DUMPS) {
      assert.equal(shouldSkipDir(name), true, name);
    }
  });

  it('walks real module directories', () => {
    assert.equal(shouldSkipDir('auth-local'), false);
    assert.equal(shouldSkipDir('media-ui-app'), false);
  });
});

describe('listForbiddenAgents', () => {
  it('allows umbrella root AGENTS.md', () => {
    const root = tempRoot();
    writeFileSync(join(root, 'AGENTS.md'), '# umbrella\n');
    assert.deepEqual(listForbiddenAgents(root), []);
  });

  it('flags module root AGENTS.md', () => {
    const root = tempRoot();
    const modDir = join(root, 'auth-local');
    mkdirSync(modDir);
    writeFileSync(join(modDir, 'AGENTS.md'), '# leftover\n');
    assert.deepEqual(listForbiddenAgents(root), ['auth-local/AGENTS.md']);
  });

  it('ignores polluted dumps', () => {
    const root = tempRoot();
    const dump = join(root, 'media-ui');
    mkdirSync(dump);
    writeFileSync(join(dump, 'AGENTS.md'), '# dump\n');
    assert.deepEqual(listForbiddenAgents(root), []);
  });

  it('flags cookiecutter template AGENTS.md', () => {
    const root = tempRoot();
    const cc = join(root, 'muxcore-module-starter/cookiecutter/{{cookiecutter.module_slug}}');
    mkdirSync(cc, { recursive: true });
    writeFileSync(join(cc, 'AGENTS.md'), '# template\n');
    assert.deepEqual(listForbiddenAgents(root), [
      'muxcore-module-starter/cookiecutter/{{cookiecutter.module_slug}}/AGENTS.md',
    ]);
  });
});

describe('checkNoModuleAgents', () => {
  it('is ok when no module AGENTS.md exist', () => {
    const root = tempRoot();
    writeFileSync(join(root, 'AGENTS.md'), '# umbrella\n');
    mkdirSync(join(root, 'docs/agents'), { recursive: true });
    writeFileSync(join(root, 'docs/agents/sidecar.md'), '# sidecar\n');
    const result = checkNoModuleAgents(root);
    assert.equal(result.ok, true);
    assert.deepEqual(result.forbidden, []);
  });

  it('is not ok when a module still has AGENTS.md', () => {
    const root = tempRoot();
    mkdirSync(join(root, 'admin-ui'));
    writeFileSync(join(root, 'admin-ui/AGENTS.md'), '# no\n');
    const result = checkNoModuleAgents(root);
    assert.equal(result.ok, false);
    assert.deepEqual(result.forbidden, ['admin-ui/AGENTS.md']);
  });
});

describe('main', () => {
  it('prints help and returns 0', async () => {
    const logs = [];
    const code = await main(['--help'], { log: (line) => logs.push(line) });
    assert.equal(code, 0);
    assert.match(logs.join('\n'), /docs\/agents/);
  });

  it('returns 0 when clean', async () => {
    const root = tempRoot();
    writeFileSync(join(root, 'AGENTS.md'), '# umbrella\n');
    const logs = [];
    const code = await main([], {
      root,
      log: (line) => logs.push(line),
    });
    assert.equal(code, 0);
    assert.equal(logs.at(-1), 'ok: no module AGENTS.md');
  });

  it('returns 1 and lists leftover paths', async () => {
    const root = tempRoot();
    mkdirSync(join(root, 'core'));
    writeFileSync(join(root, 'core/AGENTS.md'), '# leftover\n');
    const logs = [];
    const errors = [];
    const code = await main([], {
      root,
      log: (line) => logs.push(line),
      error: (line) => errors.push(line),
    });
    assert.equal(code, 1);
    assert.match(errors.join('\n'), /core\/AGENTS\.md/);
    assert.match(errors.join('\n'), /docs\/agents/);
  });
});
