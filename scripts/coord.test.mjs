import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { execFileSync } from 'node:child_process';
import { main, formatItem, parseItem, laneOf, loadRoles } from './coord.mjs';

function harness() {
  const dir = mkdtempSync(join(tmpdir(), 'coord-'));
  const run = async (role, ...argv) => {
    const lines = [];
    const code = await main([...argv, '--as', role], {
      env: { MUXCORE_COORD_DIR: dir },
      out: (l) => lines.push(l),
    });
    return { code, text: lines.join('\n') };
  };
  return { dir, run, done: () => rmSync(dir, { recursive: true, force: true }) };
}

test('item header round-trips', () => {
  const item = {
    id: 'T-M4-01',
    title: 'parental BFF',
    owner: 'frontend',
    status: 'todo',
    requested_by: 'integrator',
    blocked_by: ['T-M3-06'],
    modules: ['_mvp:cmd/mediauiprox'],
    evidence: '',
    updated: 'x',
    body: 'notes\n',
  };
  assert.deepEqual(parseItem(formatItem(item)), item);
});

test('every active lane module is in exactly one lane', () => {
  const roles = loadRoles();
  const seen = new Map();
  for (const [r, { modules }] of Object.entries(roles))
    for (const m of modules) {
      assert.ok(!seen.has(m), `${m} in ${seen.get(m)} and ${r}`);
      seen.set(m, r);
    }
  assert.equal(laneOf(roles, 'core'), 'platform');
  assert.equal(laneOf(roles, 'media-ui'), null, 'archived dumps belong to no lane');
});

test('a blocked item becomes ready and the waiter wakes when its blocker is done', async () => {
  const h = harness();
  try {
    await h.run('platform', 'new', 'T-M3-06', '--title', 'identity');
    await h.run(
      'integrator',
      'new',
      'T-M4-01',
      '--owner',
      'frontend',
      '--title',
      'parental',
      '--blocked-by',
      'T-M3-06',
    );

    let r = await h.run('frontend', 'next');
    assert.equal(r.code, 1);
    assert.match(r.text, /wait on blockers/);

    r = await h.run('frontend', 'wait', 'T-M3-06', '--timeout', '0');
    assert.equal(r.code, 2);

    const waiting = h.run('frontend', 'wait', 'T-M3-06', '--timeout', '5', '--interval', '0.05');
    await assert.rejects(h.run('platform', 'set', 'T-M3-06', 'done'), /evidence/);
    await h.run('platform', 'set', 'T-M3-06', 'done', '--evidence', 'go test ./... green');
    r = await waiting;
    assert.equal(r.code, 0);
    assert.match(r.text, /T-M3-06: done/);

    r = await h.run('frontend', 'next');
    assert.equal(r.code, 0);
    assert.match(r.text, /T-M4-01/);
  } finally {
    h.done();
  }
});

test('inbox shows only events that concern the role, once', async () => {
  const h = harness();
  try {
    await h.run('platform', 'new', 'X-1', '--title', 'contract change');
    await h.run('services', 'new', 'X-2', '--title', 'consume contract', '--blocked-by', 'X-1');
    await h.run(
      'integrator',
      'new',
      'X-3',
      '--owner',
      'platform',
      '--title',
      'requested by integrator',
    );
    await h.run('integrator', 'send', '--to', 'frontend', 'unrelated');

    await h.run('platform', 'set', 'X-1', 'done', '--evidence', 'tests');
    await h.run('platform', 'set', 'X-3', 'in-progress');

    let r = await h.run('services', 'inbox');
    assert.match(r.text, /X-1 -> done/);
    assert.doesNotMatch(r.text, /X-3|unrelated/);
    r = await h.run('services', 'inbox');
    assert.match(r.text, /inbox empty/);

    r = await h.run('integrator', 'inbox');
    assert.match(r.text, /X-3 -> in-progress/);
    assert.doesNotMatch(r.text, /X-1/);

    r = await h.run('platform', 'inbox');
    assert.match(r.text, /new item X-3 for platform/);

    const waiting = h.run('frontend', 'wait', '--inbox', '--timeout', '5', '--interval', '0.05');
    assert.match((await waiting).text, /unrelated/);
  } finally {
    h.done();
  }
});

test('only the owner edits an item', async () => {
  const h = harness();
  try {
    await h.run('platform', 'new', 'Y-1', '--title', 't');
    await assert.rejects(
      h.run('frontend', 'set', 'Y-1', 'done', '--evidence', 'e'),
      /owned by platform/,
    );
  } finally {
    h.done();
  }
});

test('locks are exclusive and lane-checked', async () => {
  const h = harness();
  try {
    await assert.rejects(h.run('frontend', 'lock', 'core'), /platform lane/);
    await assert.rejects(h.run('frontend', 'lock', 'media-ui'), /no lane/);
    await h.run('integrator', 'lock', '_mvp');
    await h.run('frontend', 'lock', '_mvp:cmd/mediauiprox');
    await assert.rejects(h.run('platform', 'lock', '_mvp', '--force'), /locked by integrator/);
    await assert.rejects(h.run('platform', 'unlock', '_mvp'), /locked by integrator/);
    await h.run('integrator', 'unlock', '_mvp');
    const r = await h.run('integrator', 'locks');
    assert.match(r.text, /mediauiprox\s+frontend/);
  } finally {
    h.done();
  }
});

test('git-backed coord dirs exchange state through a remote', async () => {
  const base = mkdtempSync(join(tmpdir(), 'coord-git-'));
  const git = (cwd, ...a) => execFileSync('git', a, { cwd, stdio: 'pipe' });
  try {
    git(base, 'init', '-q', '--bare', '-b', 'main', 'remote.git');
    for (const c of ['a', 'b']) {
      git(base, 'clone', '-q', 'remote.git', c);
      git(join(base, c), 'config', 'user.email', 't@example.invalid');
      git(join(base, c), 'config', 'user.name', 't');
    }
    git(join(base, 'a'), 'commit', '-q', '--allow-empty', '-m', 'root');
    git(join(base, 'a'), 'push', '-q', 'origin', 'HEAD:main');
    git(join(base, 'b'), 'pull', '-q', 'origin', 'main');
    git(join(base, 'b'), 'branch', '-q', '--set-upstream-to=origin/main');

    const run = (c, role, ...argv) =>
      main([...argv, '--as', role], { env: { MUXCORE_COORD_DIR: join(base, c) }, out: () => {} });
    await run('a', 'platform', 'new', 'G-1', '--title', 'over git');
    await run('b', 'services', 'new', 'G-2', '--title', 'after', '--blocked-by', 'G-1');
    await run('a', 'platform', 'set', 'G-1', 'done', '--evidence', 'ok');
    const lines = [];
    const code = await main(['wait', 'G-1', '--timeout', '1', '--interval', '0.05'], {
      env: { MUXCORE_COORD_DIR: join(base, 'b') },
      out: (l) => lines.push(l),
    });
    assert.equal(code, 0, lines.join('\n'));
  } finally {
    rmSync(base, { recursive: true, force: true });
  }
});
