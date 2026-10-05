#!/usr/bin/env node
// Env-var drift check (NFR-OPS-005, roadmap T-M2-07): keep docs/config-reference.md
// in sync with the environment variables the code actually reads.
//
// Scope decision (derived from the doc's own scope statement and its `_mvp` section):
//  - Go: non-test *.go of every ACTIVE module (scripts/module-sets.sh, i.e. archived and
//    historical submodules are never read). Variables are collected from
//      * os.Getenv / os.LookupEnv,
//      * any helper whose name contains "env" (case-insensitive): envOr, getenv, envInt,
//        envBool, envTruthy, parseDurationEnv, ... (first argument; ALL arguments for
//        variadic "first"-style helpers such as firstEnv / envFirst),
//      * string literal OR identifier arguments; identifiers are resolved through
//        `name = "ENV_NAME"` constants declared in the same module,
//      * `for _, k := range []string{"A", "B"}` single-line loops in files that read env.
//    Writers (Setenv, Unsetenv) and ExpandEnv/Environ are ignored.
//  - `_mvp` stack launcher: shell scripts, compose files and Dockerfiles are scanned for
//    `$VAR` / `${VAR...}` but ONLY for the documented `MVP_*` and `SMOKE_*` namespaces
//    (the doc's _mvp section); other shell variables are ordinary shell locals.
//  - Non-Go runtimes (e.g. Node `process.env` in SPAs) are out of scope of the doc.
//  - Prefix concatenation (`os.Getenv("BACKUP_" + name)`) cannot be resolved to names; the
//    prefix is recorded and any documented variable with that prefix counts as read.
//
// Rules (exit 1 on any violation):
//  (a) documented (first table column, or an "Alias: `X`" mention) but not read: phantom;
//  (b) read in code but undocumented, unless listed in scripts/check-env-docs.allow
//      (`NAME  # reason`; reserved for internal/test-only variables).
// Stale allowlist entries (documented or no longer read) only warn.
//
// Usage: node scripts/check-env-docs.mjs [--write] [--rows]
//   --write  (re)generate the marked "Additional variables" block of the doc for every
//            variable read in code but not documented by hand; existing rows are kept.
//            Descriptions are derived from code comments/name patterns, so review them.
//   --rows   print skeleton table rows for undocumented variables.
import { readFileSync, writeFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, dirname, resolve, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, '..');
const SKIP_DIRS = new Set(['.git', 'node_modules', 'vendor', 'testdata', 'dist', 'dist-app']);
const ENV_NAME = /^[A-Z][A-Z0-9_]+$/;
const SHELL_PREFIXES = ['MVP_', 'SMOKE_'];
const SHELL_EXT = /\.(sh|ya?ml)$|(^|\/)Dockerfile[^/]*$|\.Dockerfile$/;

function walk(dir, accept, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    const st = statSync(p);
    if (st.isDirectory()) {
      if (!SKIP_DIRS.has(name)) walk(p, accept, out);
    } else if (accept(p)) out.push(p);
  }
  return out;
}

const isGoSource = (p) => p.endsWith('.go') && !p.endsWith('_test.go');
const isShellTest = (p) => /_test\.sh$/.test(p);

// Active Go module directories, via the shared bash helper.
export function activeModules(root = ROOT) {
  const out = execFileSync(
    'bash',
    ['-c', 'source "$MUXCORE_ROOT/scripts/module-sets.sh"; muxcore_active_go_modules'],
    { env: { ...process.env, MUXCORE_ROOT: root }, encoding: 'utf8' },
  );
  return out
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean);
}

// Remove full-line comments and block comments so commented-out code is not counted.
function stripComments(src) {
  const blank = (m) => m.replace(/[^\n]/g, ' ');
  return src.replace(/\/\*[\s\S]*?\*\//g, blank).replace(/^[ \t]*\/\/.*$/gm, blank);
}

// Best-effort description for a read site: trailing comment, else the comment
// block directly above the statement (offsets match because stripping preserves length).
function commentFor(orig, index) {
  const lines = orig.split('\n');
  const ln = orig.slice(0, index).split('\n').length - 1;
  const trailing = /\/\/\s*(.+)$/.exec(lines[ln].replace(/"(?:[^"\\]|\\.)*"/g, '""'));
  if (trailing) return trailing[1].trim();
  const above = [];
  for (let i = ln - 1; i >= 0 && /^\s*\/\/\s?/.test(lines[i]); i--) {
    above.unshift(lines[i].replace(/^\s*\/\/\s?/, '').trim());
  }
  return above.join(' ').trim();
}

// Parse the argument text of a call starting just after "(": returns the list of
// top-level argument strings (string-literal aware).
function parseArgs(src, start) {
  const args = [];
  let depth = 0;
  let cur = '';
  for (let i = start; i < src.length; i++) {
    const c = src[i];
    if (c === '"' || c === '`') {
      let j = i + 1;
      while (j < src.length && src[j] !== c) j += c === '"' && src[j] === '\\' ? 2 : 1;
      cur += src.slice(i, j + 1);
      i = j;
      continue;
    }
    if (c === '(' || c === '[' || c === '{') depth++;
    else if (c === ')' || c === ']' || c === '}') {
      if (depth === 0) {
        if (cur.trim()) args.push(cur.trim());
        return args;
      }
      depth--;
    } else if (c === ',' && depth === 0) {
      args.push(cur.trim());
      cur = '';
      continue;
    }
    cur += c;
  }
  return args;
}

const CALL = /(?<![\w.])((?:[A-Za-z_]\w*\.)*)([A-Za-z_]\w*)\(/g;
const WRITER = /^(Setenv|Unsetenv|ExpandEnv|Environ|Clearenv)$/;

function isEnvFunc(name) {
  if (WRITER.test(name)) return false;
  return name === 'Getenv' || name === 'LookupEnv' || /env/i.test(name);
}

// Scan one module's Go sources. Returns { reads: Map<name, Set<source>>, prefixes: Map<prefix, Set<source>> }.
export function scanGoModule(modDir, modName) {
  const origs = new Map();
  const files = walk(modDir, isGoSource).map((p) => {
    const orig = readFileSync(p, 'utf8');
    origs.set(p, orig);
    return [p, stripComments(orig)];
  });
  const consts = new Map();
  for (const [, src] of files) {
    for (const m of src.matchAll(/\b([A-Za-z_]\w*)\s+(?:string\s*)?=\s*"([A-Z][A-Z0-9_]+)"/g)) {
      if (!consts.has(m[1])) consts.set(m[1], new Set());
      consts.get(m[1]).add(m[2]);
    }
  }
  const reads = new Map();
  const prefixes = new Map();
  const info = new Map();
  const note = (name, m, args, orig) => {
    if (!info.has(name)) info.set(name, { defaults: new Set(), comments: new Set() });
    const i = info.get(name);
    const d = args[1] && /^(?:"[^"]*"|-?\d+(?:\.\d+)?|true|false)$/.exec(args[1].trim());
    if (d && args.length === 2) i.defaults.add(d[0].replace(/^"|"$/g, ''));
    const c = commentFor(orig, m.index);
    if (c) i.comments.add(c);
  };
  const add = (map, key, where) => {
    if (!map.has(key)) map.set(key, new Set());
    map.get(key).add(where);
  };
  for (const [path, src] of files) {
    const where = `${modName}:${relative(modDir, path).split(sep).join('/')}`;
    let readsEnv = false;
    // File-local assignments (`envKey := "X"`, later `envKey = "Y"`) resolve identifier args.
    const locals = new Map();
    for (const m of src.matchAll(/\b([A-Za-z_]\w*)\s*:?=\s*"([A-Z][A-Z0-9_]+)"/g)) {
      if (!locals.has(m[1])) locals.set(m[1], new Set());
      locals.get(m[1]).add(m[2]);
    }
    for (const m of src.matchAll(CALL)) {
      const fn = m[2];
      if (!isEnvFunc(fn)) continue;
      // Skip declarations: `func envOr(` and `func (r *T) envOr(`.
      if (/func\s+(\([^)]*\)\s*)?$/.test(src.slice(Math.max(0, m.index - 80), m.index))) continue;
      const args = parseArgs(src, m.index + m[0].length);
      const variadic = /first/i.test(fn);
      const considered = variadic ? args : args.slice(0, 1);
      for (const a of considered) {
        const lit = /^"([^"]*)"$/.exec(a);
        if (lit) {
          if (ENV_NAME.test(lit[1])) {
            add(reads, lit[1], where);
            note(lit[1], m, args, origs.get(path));
            readsEnv = true;
          }
          continue;
        }
        const cat = /^"([A-Z][A-Z0-9_]*_)"\s*\+/.exec(a);
        if (cat) {
          add(prefixes, cat[1], where);
          readsEnv = true;
          continue;
        }
        if (/^[A-Za-z_]\w*$/.test(a) && (consts.has(a) || locals.has(a))) {
          for (const v of new Set([...(consts.get(a) ?? []), ...(locals.get(a) ?? [])])) {
            add(reads, v, where);
            note(v, m, args, origs.get(path));
          }
          readsEnv = true;
        }
      }
    }
    if (readsEnv || /\b(Getenv|LookupEnv)\(/.test(src)) {
      for (const m of src.matchAll(/range\s+\[\]string\{([^}\n]*)\}/g)) {
        for (const l of m[1].matchAll(/"([A-Z][A-Z0-9_]+)"/g)) add(reads, l[1], where);
      }
    }
  }
  return { reads, prefixes, info };
}

// Scan _mvp shell/compose/Dockerfile for MVP_* / SMOKE_* references.
export function scanShell(modDir, modName) {
  const reads = new Map();
  const files = walk(modDir, (p) => SHELL_EXT.test(p) && !isShellTest(p));
  for (const p of files) {
    const src = readFileSync(p, 'utf8')
      .split('\n')
      .filter((l) => !/^\s*#/.test(l))
      .join('\n');
    for (const m of src.matchAll(/\$\{?([A-Z][A-Z0-9_]+)(:?[-=?])?/g)) {
      if (!SHELL_PREFIXES.some((pre) => m[1].startsWith(pre))) continue;
      // A name assigned in the same file (`NAME=...`, `local NAME=`, Dockerfile `ARG NAME=`)
      // is a shell local/build arg unless it self-defaults (`NAME="${NAME:-x}"`).
      const assigned = new RegExp(
        `(^|[\\s;])(export\\s+|local\\s+|readonly\\s+|ARG\\s+|ENV\\s+)?${m[1]}=`,
        'm',
      ).test(src);
      const selfDefault = new RegExp(`${m[1]}="?\\$\\{${m[1]}:?[-=]`).test(src);
      if (assigned && !selfDefault) continue;
      if (!reads.has(m[1])) reads.set(m[1], new Set());
      reads.get(m[1]).add(`${modName}:${relative(modDir, p).split(sep).join('/')}`);
    }
  }
  return reads;
}

// Parse the doc: returns Map<name, {kind: "row"|"alias", module}>.
export function parseDoc(text) {
  const docs = new Map();
  let inFence = false;
  for (const line of text.split('\n')) {
    if (/^\s*```/.test(line)) inFence = !inFence;
    if (inFence) continue;
    const m = /^\|\s*`([A-Z][A-Z0-9_]+)`\s*\|([^|]*)\|([^|]*)\|(.*)\|\s*$/.exec(line);
    if (!m) continue;
    if (!docs.has(m[1])) docs.set(m[1], { kind: 'row' });
    for (const a of m[4].matchAll(/[Aa]lias(?:es)?:?\s*`([A-Z][A-Z0-9_]+)`/g)) {
      if (!docs.has(a[1])) docs.set(a[1], { kind: 'alias' });
    }
  }
  return docs;
}

export function parseAllow(text) {
  const allow = new Map();
  for (const raw of text.split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const m = /^([A-Z][A-Z0-9_]+)\s*#\s*(\S.*)$/.exec(line);
    if (!m) throw new Error(`allowlist line needs "NAME  # reason": ${line}`);
    allow.set(m[1], m[2]);
  }
  return allow;
}

export function scanAll(root, modules) {
  const reads = new Map();
  const prefixes = new Map();
  const info = new Map();
  const merge = (into, from) => {
    for (const [k, v] of from) {
      if (!into.has(k)) into.set(k, new Set());
      for (const s of v) into.get(k).add(s);
    }
  };
  for (const mod of modules) {
    const dir = join(root, mod);
    if (!existsSync(dir)) continue;
    const r = scanGoModule(dir, mod);
    merge(reads, r.reads);
    merge(prefixes, r.prefixes);
    for (const [k, v] of r.info) {
      if (!info.has(k)) info.set(k, { defaults: new Set(), comments: new Set() });
      v.defaults.forEach((d) => info.get(k).defaults.add(d));
      v.comments.forEach((d) => info.get(k).comments.add(d));
    }
    if (mod === '_mvp') merge(reads, scanShell(dir, mod));
  }
  return { reads, prefixes, info };
}

export function compare({ reads, prefixes }, docs, allow) {
  const prefixList = [...prefixes.keys()];
  const isRead = (n) => reads.has(n) || prefixList.some((p) => n.startsWith(p));
  const phantoms = [...docs.keys()].filter((n) => !isRead(n)).sort();
  const undocumented = [...reads.keys()].filter((n) => !docs.has(n) && !allow.has(n)).sort();
  const staleAllow = [...allow.keys()].filter((n) => docs.has(n) || !reads.has(n)).sort();
  return { phantoms, undocumented, staleAllow };
}

export const GEN_BEGIN = '<!-- BEGIN GENERATED: check-env-docs -->';
export const GEN_END = '<!-- END GENERATED: check-env-docs -->';

const SUFFIX_DESC = [
  [/_TLS_CERT$/, "TLS certificate file for the module's mesh client/server."],
  [/_TLS_KEY$/, 'TLS private key file.'],
  [/_TLS_CA$/, 'CA bundle for mesh TLS verification.'],
  [/_TLS_DIR$/, 'Directory holding `tls.crt`/`tls.key`/`ca.crt`.'],
  [/_TLS_SERVER_NAME$/, 'TLS server name override for mesh dial.'],
  [/_GRPC_CLIENT_ADDR$/, 'gRPC address the BFF dials for this module.'],
  [/_GRPC_ADDR$/, 'gRPC listen address.'],
  [/_HTTP_ADDR$/, 'HTTP listen address.'],
  [/_HTTP_URL$/, 'HTTP base URL of the module.'],
  [/_MODULE_TOKEN$/, 'Mesh module token.'],
  [/_API_KEY$/, 'API key.'],
  [/_(TOKEN|PASSWORD|SECRET|SECRET_KEY|ACCESS_KEY)$/, 'Credential (secret).'],
  [/_DB_PATH$/, 'SQLite database file path.'],
  [/_DATA_DIR$/, 'Module data directory.'],
];

function humanize(name, mods) {
  const toks = mods[0]
    .toUpperCase()
    .split(/[^A-Z0-9]+/)
    .filter(Boolean);
  let stem = name;
  // Drop a leading module-name prefix (`media-automation` -> `MEDIA_AUTOMATION_` or `AUTOMATION_`).
  const cands = toks.map((_, i) => toks.slice(i).join('_') + '_').concat(toks[0] + '_');
  for (const pre of cands) {
    if (stem.startsWith(pre) && stem.length > pre.length) {
      stem = stem.slice(pre.length);
      break;
    }
  }
  const words = stem.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1) + '.';
}

export function describe(name, mods, inf) {
  const c = [...(inf?.comments ?? [])].find((x) => x.length >= 12 && !/^(nolint|TODO|#)/.test(x));
  if (c) {
    const sentence = c.split(/(?<=\.)\s/)[0].trim();
    const clipped = sentence.length > 140 ? sentence.slice(0, 137).trimEnd() + '...' : sentence;
    return clipped.replace(/\|/g, '/').replace(/\.?$/, '.');
  }
  for (const [re, d] of SUFFIX_DESC) if (re.test(name)) return d;
  return humanize(name, mods);
}

function rowFor(name, scan) {
  const mods = [...new Set([...scan.reads.get(name)].map((x) => x.split(':')[0]))].sort();
  const inf = scan.info.get(name);
  const defs = [...(inf?.defaults ?? [])];
  const def = defs.length === 1 && defs[0] !== '' ? '`' + defs[0].replace(/\|/g, '/') + '`' : '—';
  return {
    mods,
    row: `| \`${name}\` | ${mods.join(', ')} | ${def} | ${describe(name, mods, inf)} |`,
  };
}

// Split doc text into [before, generatedBody, after]; generatedBody is null when absent.
export function splitGenerated(text) {
  const a = text.indexOf(GEN_BEGIN);
  const b = text.indexOf(GEN_END);
  if (a < 0 || b < a) return [text, null, ''];
  return [text.slice(0, a), text.slice(a + GEN_BEGIN.length, b), text.slice(b + GEN_END.length)];
}

// Regenerate the generated block: variables read in code that are not documented by
// hand and not allowlisted. Existing rows (hand-edited descriptions) are kept verbatim.
export function renderGenerated(text, scan, allow) {
  const [before, body, after] = splitGenerated(text);
  const hand = parseDoc(before + after);
  const existing = new Map();
  for (const l of (body ?? '').split('\n')) {
    const m = /^\|\s*`([A-Z][A-Z0-9_]+)`\s*\|/.exec(l);
    if (m) existing.set(m[1], l);
  }
  const names = [...scan.reads.keys()].filter((n) => !hand.has(n) && !allow.has(n)).sort();
  const groups = new Map();
  for (const n of names) {
    const { mods, row } = rowFor(n, scan);
    const line = existing.get(n) ?? row;
    if (!groups.has(mods[0])) groups.set(mods[0], []);
    groups.get(mods[0]).push(line);
  }
  let out = GEN_BEGIN + '\n\n';
  for (const mod of [...groups.keys()].sort()) {
    out += `### ${mod}\n\n| Env Var | Module | Default | Description |\n|---------|--------|---------|-------------|\n${groups.get(mod).join('\n')}\n\n`;
  }
  return before + out + GEN_END + after;
}

function main() {
  const rows = process.argv.includes('--rows');
  const write = process.argv.includes('--write');
  const docPath = join(ROOT, 'docs/config-reference.md');
  const allowPath = join(HERE, 'check-env-docs.allow');
  const allow = existsSync(allowPath) ? parseAllow(readFileSync(allowPath, 'utf8')) : new Map();
  const scan = scanAll(ROOT, activeModules(ROOT));
  if (write) {
    const next = renderGenerated(readFileSync(docPath, 'utf8'), scan, allow);
    writeFileSync(docPath, next);
    console.log('wrote generated block in docs/config-reference.md');
  }
  const docs = parseDoc(readFileSync(docPath, 'utf8'));
  const { phantoms, undocumented, staleAllow } = compare(scan, docs, allow);

  for (const n of staleAllow)
    console.warn(`warn: allowlist entry ${n} is documented or no longer read`);
  if (rows) {
    for (const n of undocumented) {
      const mods = [...new Set([...scan.reads.get(n)].map((s) => s.split(':')[0]))].sort();
      console.log(`| \`${n}\` | ${mods.join(', ')} | — | TODO |`);
    }
  }
  let fail = false;
  if (phantoms.length) {
    fail = true;
    console.error(`FAIL: ${phantoms.length} documented variable(s) not read by any active module:`);
    for (const n of phantoms) console.error(`  ${n}`);
  }
  if (undocumented.length) {
    fail = true;
    console.error(
      `FAIL: ${undocumented.length} variable(s) read in code but missing from docs/config-reference.md (or scripts/check-env-docs.allow):`,
    );
    for (const n of undocumented)
      console.error(`  ${n}  (${[...scan.reads.get(n)].slice(0, 2).join(', ')})`);
  }
  if (fail) process.exit(1);
  console.log(
    `ok: ${docs.size} documented variables match ${scan.reads.size} read in code (${scan.prefixes.size} dynamic prefixes, ${allow.size} allowlisted)`,
  );
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main();
