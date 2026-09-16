import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, describe, it } from 'node:test';
import {
  buildAgentsMarkdown,
  formatCapabilities,
  formatContracts,
  generateModuleAgents,
  main,
  readMuxcoreMeta,
  shouldProcessModule,
  SKIP_DIR_NAMES,
} from './generate-module-agents.mjs';

const temps = [];

function tempRoot() {
  const dir = mkdtempSync(join(tmpdir(), 'generate-module-agents-'));
  temps.push(dir);
  return dir;
}

function writeMuxcore(dir, data) {
  writeFileSync(join(dir, 'muxcore.json'), `${JSON.stringify(data, null, 2)}\n`, 'utf8');
}

after(() => {
  for (const dir of temps) {
    rmSync(dir, { recursive: true, force: true });
  }
});

describe('formatContracts', () => {
  it('returns none declared for empty contracts', () => {
    assert.equal(formatContracts([]), 'none declared');
    assert.equal(formatContracts(null), 'none declared');
  });

  it('formats interface and repo tail', () => {
    const text = formatContracts([
      {
        repo: 'github.com/Muxcore-Media/contracts-scanner',
        interface: 'ScannerService',
      },
    ]);
    assert.equal(text, 'ScannerService (contracts-scanner)');
  });
});

describe('formatCapabilities', () => {
  it('falls back when capabilities are missing or empty', () => {
    assert.equal(formatCapabilities([]), 'see muxcore.json');
    assert.equal(formatCapabilities(undefined), 'see muxcore.json');
  });

  it('joins capability names', () => {
    assert.equal(formatCapabilities(['auth', 'settings']), 'auth, settings');
  });
});

describe('readMuxcoreMeta', () => {
  it('parses muxcore.json fields', () => {
    const payload = JSON.stringify({
      name: 'Auth Local',
      capabilities: ['auth'],
      contracts: [{ repo: 'github.com/Muxcore-Media/contracts-auth', interface: 'Auth' }],
    });
    const meta = readMuxcoreMeta('/tmp/auth-local/muxcore.json', () => payload);
    assert.equal(meta.ok, true);
    assert.equal(meta.modname, 'Auth Local');
    assert.deepEqual(meta.capabilities, ['auth']);
    assert.equal(meta.contracts.length, 1);
  });

  it('returns failure metadata for invalid json', () => {
    const meta = readMuxcoreMeta('bad', () => '{not json');
    assert.equal(meta.ok, false);
    assert.equal(meta.modname, '');
  });
});

describe('shouldProcessModule', () => {
  it('skips listed directory names', () => {
    for (const name of ['muxcorectl', 'media-ui', 'cache-memory']) {
      assert.equal(
        shouldProcessModule(name, { hasMuxcore: true, hasAgents: false }),
        false,
        name,
      );
      assert.match(name, SKIP_DIR_NAMES);
    }
  });

  it('requires muxcore.json', () => {
    assert.equal(shouldProcessModule('auth-local', { hasMuxcore: false, hasAgents: false }), false);
  });

  it('skips media-ui-app when AGENTS.md already exists', () => {
    assert.equal(
      shouldProcessModule('media-ui-app', { hasMuxcore: true, hasAgents: true }),
      false,
    );
    assert.equal(
      shouldProcessModule('media-ui-app', { hasMuxcore: true, hasAgents: false }),
      true,
    );
  });
});

describe('buildAgentsMarkdown', () => {
  it('renders the module template with capabilities and contracts', () => {
    const md = buildAgentsMarkdown({
      modname: 'Media Scanner',
      dirName: 'media-scanner',
      capabilities: ['media.scanner', 'settings'],
      contracts: [
        {
          repo: 'github.com/Muxcore-Media/contracts-scanner',
          interface: 'ScannerService',
        },
      ],
    });
    assert.match(md, /^# AGENTS\.md — Media Scanner/);
    assert.match(md, /`media-scanner`/);
    assert.match(md, /media\.scanner, settings/);
    assert.match(md, /ScannerService \(contracts-scanner\)/);
    assert.match(md, /cd media-scanner/);
    assert.match(md, /Do not add `ROADMAP.md` \/ `TASKS.md` in this repo/);
  });
});

describe('generateModuleAgents', () => {
  it('writes AGENTS.md for modules with muxcore.json', () => {
    const root = tempRoot();
    const modDir = join(root, 'demo-module');
    mkdirSync(modDir);
    writeMuxcore(modDir, {
      name: 'Demo Module',
      capabilities: ['demo.cap'],
      contracts: [],
    });

    const logs = [];
    const written = generateModuleAgents(root, { log: (line) => logs.push(line) });

    assert.equal(written.length, 1);
    assert.equal(written[0].name, 'demo-module');
    const agents = readFileSync(join(modDir, 'AGENTS.md'), 'utf8');
    assert.match(agents, /Demo Module/);
    assert.match(agents, /demo\.cap/);
    assert.match(logs.join('\n'), /wrote .*demo-module\/AGENTS\.md/);
  });

  it('does not overwrite media-ui-app when AGENTS.md exists', () => {
    const root = tempRoot();
    const modDir = join(root, 'media-ui-app');
    mkdirSync(modDir);
    writeMuxcore(modDir, { name: 'Media UI App', capabilities: ['ui'] });
    writeFileSync(join(modDir, 'AGENTS.md'), 'keep-me\n', 'utf8');

    const written = generateModuleAgents(root, { log: () => {} });

    assert.equal(written.length, 0);
    assert.equal(readFileSync(join(modDir, 'AGENTS.md'), 'utf8'), 'keep-me\n');
  });

  it('uses directory name when muxcore.json is invalid', () => {
    const root = tempRoot();
    const modDir = join(root, 'broken-json');
    mkdirSync(modDir);
    writeFileSync(join(modDir, 'muxcore.json'), '{', 'utf8');

    generateModuleAgents(root, { log: () => {} });
    const agents = readFileSync(join(modDir, 'AGENTS.md'), 'utf8');
    assert.match(agents, /# AGENTS\.md — broken-json/);
    assert.match(agents, /Capabilities \| see muxcore\.json/);
    assert.match(agents, /Contracts \| none declared/);
  });
});

describe('main', () => {
  it('prints done and returns 0', async () => {
    const root = tempRoot();
    const modDir = join(root, 'main-test');
    mkdirSync(modDir);
    writeMuxcore(modDir, { name: 'Main Test', capabilities: [], contracts: [] });

    const logs = [];
    const code = await main([], {
      root,
      log: (line) => logs.push(line),
    });

    assert.equal(code, 0);
    assert.equal(logs.at(-1), 'done');
    assert.ok(readFileSync(join(modDir, 'AGENTS.md'), 'utf8').includes('Main Test'));
  });
});
