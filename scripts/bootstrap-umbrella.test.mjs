import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, describe, it } from 'node:test';
import {
  buildGitmodulesContent,
  COMMIT_MESSAGE,
  discoverModules,
  listRootMarkdown,
  main,
  OWNED_PATHS,
  SKIP_DIRS,
} from './bootstrap-umbrella.mjs';

const temps = [];

function tempDir(prefix) {
  const dir = mkdtempSync(join(tmpdir(), prefix));
  temps.push(dir);
  return dir;
}

function gitIn(dir, args) {
  return execFileSync('git', ['-C', dir, ...args], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  }).trim();
}

function initBareRemote() {
  const remote = tempDir('bootstrap-remote-');
  gitIn(remote, ['init', '--bare']);
  return remote;
}

after(() => {
  for (const dir of temps) {
    rmSync(dir, { recursive: true, force: true });
  }
});

function initModuleUnder(root, name, remoteUrl) {
  const dir = join(root, name);
  mkdirSync(dir, { recursive: true });
  gitIn(dir, ['init']);
  gitIn(dir, ['config', 'user.email', 'bootstrap-test@example.com']);
  gitIn(dir, ['config', 'user.name', 'Bootstrap Test']);
  writeFileSync(join(dir, 'README'), `${name}\n`);
  gitIn(dir, ['add', 'README']);
  gitIn(dir, ['commit', '-m', 'init']);
  gitIn(dir, ['remote', 'add', 'origin', remoteUrl]);
  return dir;
}

describe('discoverModules', () => {
  it('finds sibling repos with origin URLs and skips configured dirs', () => {
    const root = tempDir('bootstrap-discover-');
    const remoteA = initBareRemote();
    const remoteB = initBareRemote();
    initModuleUnder(root, 'alpha', remoteA);
    initModuleUnder(root, 'beta', remoteB);
    mkdirSync(join(root, 'core.wiki'));
    writeFileSync(join(root, 'core.wiki', '.git'), 'skip\n');
    mkdirSync(join(root, '_wave1'));
    writeFileSync(join(root, '_wave1', '.git'), 'skip\n');
    mkdirSync(join(root, '.hidden'));
    writeFileSync(join(root, '.hidden', '.git'), 'skip\n');
    mkdirSync(join(root, 'plain'));

    const modules = discoverModules(root);
    assert.deepEqual(
      modules.map((m) => m.path),
      ['alpha', 'beta'],
    );
    assert.match(modules[0].url, /bootstrap-remote-/);
  });

  it('respects custom skipDirs', () => {
    const root = tempDir('bootstrap-skip-');
    const remote = initBareRemote();
    initModuleUnder(root, 'keep', remote);

    const modules = discoverModules(root, { skipDirs: new Set(['keep']) });
    assert.equal(modules.length, 0);
  });
});

describe('buildGitmodulesContent', () => {
  it('formats submodule entries with tabs', () => {
    const content = buildGitmodulesContent([
      { path: 'admin-ui', url: 'ssh://git@example/admin-ui' },
      { path: 'core', url: 'ssh://git@example/core' },
    ]);
    assert.equal(
      content,
      `[submodule "admin-ui"]
\tpath = admin-ui
\turl = ssh://git@example/admin-ui
[submodule "core"]
\tpath = core
\turl = ssh://git@example/core
`,
    );
  });
});

describe('listRootMarkdown', () => {
  it('lists only markdown files in the workspace root', () => {
    const root = tempDir('bootstrap-md-');
    writeFileSync(join(root, 'AGENTS.md'), '# agents\n');
    writeFileSync(join(root, 'notes.txt'), 'nope\n');
    mkdirSync(join(root, 'nested'));
    writeFileSync(join(root, 'nested', 'deep.md'), '# deep\n');
    assert.deepEqual(listRootMarkdown(root), ['AGENTS.md']);
  });
});

describe('main', () => {
  it('returns 0 when .git already exists', async () => {
    const root = tempDir('bootstrap-existing-');
    mkdirSync(join(root, '.git'));
    const logs = [];
    const code = await main([], {
      root,
      log: (msg) => logs.push(msg),
      error: () => {},
    });
    assert.equal(code, 0);
    assert.match(logs.join('\n'), /already initialized/);
  });

  it('returns 1 when git is unavailable', async () => {
    const root = tempDir('bootstrap-no-git-');
    const errors = [];
    const code = await main([], {
      root,
      hasGit: () => false,
      log: () => {},
      error: (msg) => errors.push(msg),
    });
    assert.equal(code, 1);
    assert.deepEqual(errors, ['git is required']);
  });

  it('returns 1 when no module repos are found', async () => {
    const root = tempDir('bootstrap-empty-');
    const errors = [];
    const code = await main([], {
      root,
      log: () => {},
      error: (msg) => errors.push(msg),
    });
    assert.equal(code, 1);
    assert.match(errors[0], /No module git repos found/);
  });

  it('initializes umbrella repo with submodules and owned files', async () => {
    const root = tempDir('bootstrap-init-');
    const remote = initBareRemote();
    initModuleUnder(root, 'demo-module', remote);

    writeFileSync(join(root, 'AGENTS.md'), '# agents\n');
    writeFileSync(join(root, '.gitignore'), '*.log\n');

    const prevAuthor = process.env.GIT_AUTHOR_NAME;
    const prevAuthorEmail = process.env.GIT_AUTHOR_EMAIL;
    const prevCommitter = process.env.GIT_COMMITTER_NAME;
    const prevCommitterEmail = process.env.GIT_COMMITTER_EMAIL;
    process.env.GIT_AUTHOR_NAME = 'Bootstrap Test';
    process.env.GIT_AUTHOR_EMAIL = 'bootstrap-test@example.com';
    process.env.GIT_COMMITTER_NAME = 'Bootstrap Test';
    process.env.GIT_COMMITTER_EMAIL = 'bootstrap-test@example.com';

    try {
      const code = await main([], { root, log: () => {} });
      assert.equal(code, 0);
      assert.ok(existsSync(join(root, '.git')));
      assert.ok(existsSync(join(root, '.gitmodules')));
      assert.match(gitIn(root, ['log', '-1', '--format=%s']), /initialize MuxCore umbrella workspace/);
      assert.match(gitIn(root, ['show', 'HEAD:.gitmodules']), /\[submodule "demo-module"\]/);
      assert.match(gitIn(root, ['show', 'HEAD:AGENTS.md']), /agents/);
    } finally {
      if (prevAuthor === undefined) delete process.env.GIT_AUTHOR_NAME;
      else process.env.GIT_AUTHOR_NAME = prevAuthor;
      if (prevAuthorEmail === undefined) delete process.env.GIT_AUTHOR_EMAIL;
      else process.env.GIT_AUTHOR_EMAIL = prevAuthorEmail;
      if (prevCommitter === undefined) delete process.env.GIT_COMMITTER_NAME;
      else process.env.GIT_COMMITTER_NAME = prevCommitter;
      if (prevCommitterEmail === undefined) delete process.env.GIT_COMMITTER_EMAIL;
      else process.env.GIT_COMMITTER_EMAIL = prevCommitterEmail;
    }
  });

  it('exports stable skip and owned path lists', () => {
    assert.deepEqual([...SKIP_DIRS], []);
    assert.ok(OWNED_PATHS.includes('scripts'));
    assert.match(COMMIT_MESSAGE, /Register module checkouts as submodules/);
  });
});
