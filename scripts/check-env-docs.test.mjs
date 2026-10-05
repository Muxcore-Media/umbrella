import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import {
  scanAll,
  parseDoc,
  parseAllow,
  compare,
  renderGenerated,
  GEN_BEGIN,
  GEN_END,
} from './check-env-docs.mjs';

function fixture(files) {
  const root = mkdtempSync(join(tmpdir(), 'check-env-docs-'));
  for (const [rel, body] of Object.entries(files)) {
    const p = join(root, rel);
    mkdirSync(join(p, '..'), { recursive: true });
    writeFileSync(p, body);
  }
  return root;
}

const GO = `package m
import "os"
const envToken = "MOD_TOKEN"
func envOr(k, d string) string { if v := os.Getenv(k); v != "" { return v }; return d }
func f() {
	_ = os.Getenv("MOD_ADDR")
	_ = envOr("MOD_DIR", "/data")
	_ = os.Getenv(envToken)
	_ = os.Getenv("MOD_" + name)
	os.Setenv("MOD_WRITTEN", "x")
	// os.Getenv("MOD_COMMENTED")
}
`;

const DOC = (names) => names.map((n) => `| \`${n}\` | mod | — | d |`).join('\n');

test('scanner finds literals, helpers, constants and prefixes; ignores tests, writers, comments', () => {
  const root = fixture({
    'mod/go.mod': 'module x\n',
    'mod/a.go': GO,
    'mod/a_test.go': 'package m\nfunc t() { _ = os.Getenv("MOD_TESTONLY") }\n',
  });
  const { reads, prefixes } = scanAll(root, ['mod']);
  assert.deepEqual([...reads.keys()].sort(), ['MOD_ADDR', 'MOD_DIR', 'MOD_TOKEN']);
  assert.deepEqual([...prefixes.keys()], ['MOD_']);
});

test('phantom documented variable is detected', () => {
  const root = fixture({ 'mod/a.go': GO });
  const scan = scanAll(root, ['mod']);
  const docs = parseDoc(DOC(['MOD_ADDR', 'MOD_DIR', 'MOD_TOKEN', 'BACKUP_EXCLUDE']));
  const r = compare({ ...scan, prefixes: new Map() }, docs, new Map());
  assert.deepEqual(r.phantoms, ['BACKUP_EXCLUDE']);
  assert.deepEqual(r.undocumented, []);
});

test('undocumented variable is detected', () => {
  const root = fixture({ 'mod/a.go': GO });
  const scan = scanAll(root, ['mod']);
  const docs = parseDoc(DOC(['MOD_ADDR', 'MOD_DIR']));
  const r = compare(scan, docs, new Map());
  assert.deepEqual(r.undocumented, ['MOD_TOKEN']);
});

test('allowlist suppresses undocumented, requires a reason, and flags stale entries', () => {
  const root = fixture({ 'mod/a.go': GO });
  const scan = scanAll(root, ['mod']);
  const docs = parseDoc(DOC(['MOD_ADDR', 'MOD_DIR']));
  const allow = parseAllow('# c\nMOD_TOKEN  # internal only\nGONE_VAR  # old\n');
  const r = compare(scan, docs, allow);
  assert.deepEqual(r.undocumented, []);
  assert.deepEqual(r.staleAllow, ['GONE_VAR']);
  assert.throws(() => parseAllow('MOD_TOKEN\n'), /reason/);
});

test('prefix concatenation covers documented variables with that prefix', () => {
  const root = fixture({ 'mod/a.go': GO });
  const scan = scanAll(root, ['mod']);
  const docs = parseDoc(DOC(['MOD_ADDR', 'MOD_DIR', 'MOD_TOKEN', 'MOD_DYNAMIC', 'OTHER_DYNAMIC']));
  const r = compare(scan, docs, new Map());
  assert.deepEqual(r.phantoms, ['OTHER_DYNAMIC']);
});

test('MVP_/SMOKE_ shell references count for _mvp, shell locals do not', () => {
  const root = fixture({
    '_mvp/go.mod': 'module x\n',
    '_mvp/run.sh': 'X="${MVP_FOO:-1}"\nMVP_LOCAL=1\necho "$MVP_LOCAL" "${PATH}" "$SMOKE_URL"\n',
  });
  const { reads } = scanAll(root, ['_mvp']);
  assert.deepEqual([...reads.keys()].sort(), ['MVP_FOO', 'SMOKE_URL']);
});

test('generated block adds rows for undocumented vars and preserves edited rows', () => {
  const root = fixture({ 'mod/a.go': GO });
  const scan = scanAll(root, ['mod']);
  const doc = `# D\n${DOC(['MOD_ADDR'])}\n${GEN_BEGIN}\n| \`MOD_DIR\` | mod | — | hand edited |\n${GEN_END}\n`;
  const out = renderGenerated(doc, scan, new Map());
  assert.match(out, /hand edited/);
  assert.match(out, /`MOD_TOKEN`/);
  const r = compare(scan, parseDoc(out), new Map());
  assert.deepEqual(r.undocumented, []);
});

test('package-qualified constants from another package of the module resolve', () => {
  const root = fixture({
    'mod/internal/enroll/e.go': 'package enroll\nconst (\n\tEnvSecret = "MUXCORE_ENROLL_SECRET"\n\tenvHidden = "MUXCORE_ENROLL_HIDDEN"\n)\n',
    'mod/cmd/main.go':
      'package main\nimport ("os"; "x/internal/enroll")\nfunc f() {\n\t_ = os.Getenv(enroll.EnvSecret)\n\t_ = os.Getenv(enroll.Missing)\n\t_ = os.Getenv(other.EnvSecret)\n}\n',
  });
  const { reads } = scanAll(root, ['mod']);
  assert.deepEqual([...reads.keys()], ['MUXCORE_ENROLL_SECRET']);
});

test('reader helpers without "env" in the name: parameter heuristic and explicit list', () => {
  const root = fixture({
    'mod/pkg/c.go': 'package pkg\nconst EnvDir = "MUXCORE_TLS_DIR"\nconst EnvOther = "OTHER_THING"\n',
    'mod/a.go': `package m
import "os"
func get(getenv func(string) string) {
	pick := func(v, key string) string {
		if v != "" { return v }
		return getenv(key)
	}
	_ = pick("x", pkg.EnvDir)
	_ = pick("y", "MUXCORE_BOOTSTRAP_TOKEN")
	_ = pick("z", pkg.EnvOther)
	_ = lookup("MUXCORE_VIA_HELPER")
	_ = lookup("OTHER_STUFF")
	_ = unrelated("MUXCORE_NOT_A_READER")
}
func lookup(name string) string { return os.Getenv(name) }
func unrelated(s string) string { return s }
`,
  });
  const { reads } = scanAll(root, ['mod']);
  // pick is on the explicit list, so OTHER_THING counts there; lookup is heuristic and
  // therefore prefix-gated; `unrelated` never reads env.
  assert.deepEqual(
    [...reads.keys()].sort(),
    ['MUXCORE_BOOTSTRAP_TOKEN', 'MUXCORE_TLS_DIR', 'MUXCORE_VIA_HELPER', 'OTHER_THING'],
  );
  const wider = scanAll(root, ['mod'], { prefixes: ['MUXCORE_', 'OTHER_'] }).reads;
  assert.ok(wider.has('OTHER_STUFF'));
});
