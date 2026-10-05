#!/usr/bin/env node
// File-based coordination bus for concurrent role agents (docs/agents/COORDINATION.md, ADR-0021).
//
// State lives in MUXCORE_COORD_DIR (default <umbrella>/.coord, git-ignored):
//   items/<id>.md      one work item per file; only its owner role edits it after creation
//   events/<ts>-*.json append-only event files (never edited), one per change or message
//   cursor/<role>      name of the last event file the role has read
//   locks/<key>/       claim on a module checkout (mkdir is atomic)
// Every write creates or replaces a whole file owned by one role, so agents on one
// filesystem never race, and a git-backed coord dir (`sync`) never conflicts.
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { execFileSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const ROLES_FILE = join(ROOT, 'scripts', 'coord.roles.json');

export const STATUSES = ['todo', 'in-progress', 'blocked', 'review', 'done', 'dropped'];
const RESOLVED = new Set(['done', 'dropped']);
const ID_RE = /^[A-Za-z0-9][A-Za-z0-9._-]*$/;

export function loadRoles(file = ROLES_FILE) {
  return JSON.parse(readFileSync(file, 'utf8')).roles;
}

export function coordDir(env = process.env) {
  return resolve(env.MUXCORE_COORD_DIR || join(ROOT, '.coord'));
}

function ensureLayout(dir) {
  for (const sub of ['items', 'events', 'cursor', 'locks'])
    mkdirSync(join(dir, sub), { recursive: true });
}

function writeAtomic(path, text) {
  const tmp = `${path}.${process.pid}.${randomBytes(3).toString('hex')}.tmp`;
  writeFileSync(tmp, text);
  renameSync(tmp, path);
}

function checkId(id) {
  if (!id || !ID_RE.test(id))
    throw new UsageError(`invalid item id ${JSON.stringify(id)} (use e.g. T-M4-01 or T-M4-01.bff)`);
}

function checkRole(role, roles) {
  if (!role) throw new UsageError('no role: pass --as <role> or set MUXCORE_ROLE');
  if (!roles[role])
    throw new UsageError(`unknown role ${role} (known: ${Object.keys(roles).join(', ')})`);
}

export class UsageError extends Error {}

const list = (v) =>
  (v ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

// ---- items -----------------------------------------------------------------

const ITEM_FIELDS = [
  'id',
  'title',
  'owner',
  'status',
  'requested_by',
  'blocked_by',
  'modules',
  'evidence',
  'updated',
];

export function formatItem(item) {
  const head = ITEM_FIELDS.map(
    (k) => `${k}: ${Array.isArray(item[k]) ? item[k].join(', ') : (item[k] ?? '')}`,
  );
  return `---\n${head.join('\n')}\n---\n${item.body ?? ''}`;
}

export function parseItem(text) {
  const m = /^---\n([\s\S]*?)\n---\n?([\s\S]*)$/.exec(text);
  if (!m) throw new Error('item file has no header');
  const item = { body: m[2] };
  for (const line of m[1].split('\n')) {
    const i = line.indexOf(':');
    if (i < 0) continue;
    const k = line.slice(0, i).trim();
    const v = line.slice(i + 1).trim();
    item[k] = k === 'blocked_by' || k === 'modules' ? list(v) : v;
  }
  return item;
}

const itemPath = (dir, id) => join(dir, 'items', `${id}.md`);

export function readItem(dir, id) {
  const p = itemPath(dir, id);
  return existsSync(p) ? parseItem(readFileSync(p, 'utf8')) : null;
}

export function readItems(dir) {
  const d = join(dir, 'items');
  if (!existsSync(d)) return [];
  return readdirSync(d)
    .filter((f) => f.endsWith('.md'))
    .map((f) => parseItem(readFileSync(join(d, f), 'utf8')))
    .sort((a, b) => a.id.localeCompare(b.id));
}

// Unresolved blockers of an item; a blocker id with no item file counts as unresolved.
export function openBlockers(dir, item) {
  return item.blocked_by.filter((b) => !RESOLVED.has(readItem(dir, b)?.status));
}

// ---- events ----------------------------------------------------------------

export function emit(dir, event) {
  const ts = new Date().toISOString();
  const name = `${ts.replace(/[:.]/g, '')}-${event.from}-${randomBytes(3).toString('hex')}.json`;
  writeAtomic(join(dir, 'events', name), `${JSON.stringify({ ts, ...event })}\n`);
  return name;
}

export function readEvents(dir, after = '') {
  const d = join(dir, 'events');
  if (!existsSync(d)) return [];
  return readdirSync(d)
    .filter((f) => f.endsWith('.json') && f > after)
    .sort()
    .map((f) => ({ file: f, ...JSON.parse(readFileSync(join(d, f), 'utf8')) }));
}

// An event concerns a role when it is addressed to the role (or to all), when it
// changes an item the role requested, or when it resolves an item that blocks one
// of the role's own items.
export function concerns(dir, role, ev) {
  if (ev.from === role) return false;
  if (ev.to === role || ev.to === 'all') return true;
  if (!ev.item) return false;
  const target = readItem(dir, ev.item);
  if (target?.requested_by === role) return true;
  if (ev.type === 'status' && RESOLVED.has(ev.status)) {
    return readItems(dir).some((it) => it.owner === role && it.blocked_by.includes(ev.item));
  }
  return false;
}

const cursorPath = (dir, role) => join(dir, 'cursor', role);
const readCursor = (dir, role) =>
  existsSync(cursorPath(dir, role)) ? readFileSync(cursorPath(dir, role), 'utf8').trim() : '';

export function inbox(dir, role, { advance = true } = {}) {
  const events = readEvents(dir, readCursor(dir, role));
  if (advance && events.length) writeAtomic(cursorPath(dir, role), `${events.at(-1).file}\n`);
  return events.filter((ev) => concerns(dir, role, ev));
}

// ---- git transport (optional) ---------------------------------------------

function isGitDir(dir) {
  try {
    return (
      execFileSync('git', ['-C', dir, 'rev-parse', '--show-toplevel'], {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'ignore'],
      }).trim() === dir
    );
  } catch {
    return false;
  }
}

// Commit local changes, rebase onto the remote (remote wins on a conflicting lock), push.
export function sync(dir) {
  if (!isGitDir(dir)) return false;
  const git = (...args) =>
    execFileSync('git', ['-C', dir, ...args], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    });
  git('add', '-A');
  if (git('status', '--porcelain').trim()) git('commit', '-q', '-m', 'coord: update');
  if (git('remote').trim()) {
    git('pull', '-q', '--rebase', '-X', 'ours');
    git('push', '-q');
  }
  return true;
}

// ---- locks -----------------------------------------------------------------

const lockKey = (module) => module.replace(/[/:]/g, '~');

export function laneOf(roles, module) {
  return Object.keys(roles).find((r) => roles[r].modules.includes(module)) ?? null;
}

export function lock(dir, role, module, note = '') {
  const p = join(dir, 'locks', lockKey(module));
  try {
    mkdirSync(p);
  } catch (e) {
    if (e.code !== 'EEXIST') throw e;
    const held = readLock(dir, module);
    if (held?.role === role) return held;
    throw new UsageError(
      `${module} is locked by ${held?.role ?? 'unknown'} since ${held?.since ?? '?'}${held?.note ? ` (${held.note})` : ''}`,
    );
  }
  const held = { module, role, since: new Date().toISOString(), note };
  writeFileSync(join(p, 'owner.json'), `${JSON.stringify(held)}\n`);
  return held;
}

export function readLock(dir, module) {
  const f = join(dir, 'locks', lockKey(module), 'owner.json');
  return existsSync(f) ? JSON.parse(readFileSync(f, 'utf8')) : null;
}

export function unlock(dir, role, module, { force = false } = {}) {
  const held = readLock(dir, module);
  if (held && held.role !== role && !force)
    throw new UsageError(
      `${module} is locked by ${held.role}, not ${role} (use --force only if that agent is gone)`,
    );
  rmSync(join(dir, 'locks', lockKey(module)), { recursive: true, force: true });
}

export function readLocks(dir) {
  const d = join(dir, 'locks');
  if (!existsSync(d)) return [];
  return readdirSync(d)
    .map((k) => join(d, k, 'owner.json'))
    .filter(existsSync)
    .map((f) => JSON.parse(readFileSync(f, 'utf8')));
}

// ---- commands --------------------------------------------------------------

function parseArgs(argv) {
  const pos = [];
  const opts = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const [k, v] = a.slice(2).split(/=(.*)/s);
      if (v !== undefined) opts[k] = v;
      else if (i + 1 < argv.length && !argv[i + 1].startsWith('--')) opts[k] = argv[++i];
      else opts[k] = true;
    } else pos.push(a);
  }
  return { pos, opts };
}

const fmtItem = (dir, it) => {
  const open = openBlockers(dir, it);
  const blocked = open.length ? `  blocked by ${open.join(', ')}` : '';
  return `${it.id.padEnd(16)} ${it.status.padEnd(11)} ${it.owner.padEnd(10)} ${it.title}${blocked}`;
};

const fmtEvent = (ev) => {
  const what =
    ev.type === 'status'
      ? `${ev.item} -> ${ev.status}${ev.note ? `: ${ev.note}` : ''}`
      : ev.type === 'new'
        ? `new item ${ev.item} for ${ev.to}: ${ev.note}`
        : `${ev.item ? `[${ev.item}] ` : ''}${ev.note}`;
  return `${ev.ts} ${ev.from} ${ev.type}: ${what}`;
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

export async function main(
  argv,
  { env = process.env, out = console.log, rolesFile = ROLES_FILE } = {},
) {
  const { pos, opts } = parseArgs(argv);
  const cmd = pos.shift();
  const roles = loadRoles(rolesFile);
  const dir = coordDir(env);
  const role = opts.as ?? env.MUXCORE_ROLE;
  const pull = () => {
    try {
      sync(dir);
    } catch (e) {
      out(`warning: sync failed: ${e.message.split('\n')[0]}`);
    }
  };
  if (cmd && (cmd !== 'hook-session-start' || roles[role])) ensureLayout(dir);

  switch (cmd) {
    case 'init':
      out(`coord dir ${dir}${isGitDir(dir) ? ' (git-backed)' : ''}`);
      return 0;

    case 'new': {
      const [id] = pos;
      checkId(id);
      checkRole(role, roles);
      const owner = opts.owner ?? role;
      checkRole(owner, roles);
      if (!opts.title) throw new UsageError('new needs --title');
      pull();
      const blockedBy = list(opts['blocked-by']);
      blockedBy.forEach(checkId);
      const item = {
        id,
        title: opts.title,
        owner,
        status: 'todo',
        requested_by: role,
        blocked_by: blockedBy,
        modules: list(opts.modules),
        evidence: '',
        updated: new Date().toISOString(),
        body: opts.body ? `${opts.body}\n` : '',
      };
      try {
        writeFileSync(itemPath(dir, id), formatItem(item), { flag: 'wx' });
      } catch (e) {
        if (e.code === 'EEXIST')
          throw new UsageError(`item ${id} already exists (owner ${readItem(dir, id).owner})`);
        throw e;
      }
      emit(dir, { from: role, to: owner, type: 'new', item: id, note: opts.title });
      pull();
      out(fmtItem(dir, item));
      return 0;
    }

    case 'set': {
      const [id, status] = pos;
      checkId(id);
      checkRole(role, roles);
      pull();
      const item = readItem(dir, id);
      if (!item) throw new UsageError(`no item ${id}`);
      if (item.owner !== role && !opts.force)
        throw new UsageError(
          `${id} is owned by ${item.owner}; ask them (coord send) instead of editing it`,
        );
      if (status && !STATUSES.includes(status))
        throw new UsageError(`status must be one of ${STATUSES.join(', ')}`);
      if (status === 'done' && !opts.evidence && !item.evidence)
        throw new UsageError('done needs --evidence (the verification you ran)');
      if (opts['blocked-by'] !== undefined) {
        item.blocked_by = list(opts['blocked-by']);
        item.blocked_by.forEach(checkId);
      }
      if (opts.owner) {
        checkRole(opts.owner, roles);
        item.owner = opts.owner;
      }
      if (status) item.status = status;
      if (opts.evidence) item.evidence = opts.evidence;
      if (opts.note) item.body += `\n- ${new Date().toISOString()} ${role}: ${opts.note}\n`;
      item.updated = new Date().toISOString();
      writeAtomic(itemPath(dir, id), formatItem(item));
      emit(dir, {
        from: role,
        to: opts.owner,
        type: 'status',
        item: id,
        status: item.status,
        note: opts.note ?? opts.evidence ?? '',
      });
      pull();
      out(fmtItem(dir, item));
      return 0;
    }

    case 'show': {
      const item = readItem(dir, pos[0]);
      if (!item) throw new UsageError(`no item ${pos[0]}`);
      out(formatItem(item).trimEnd());
      const open = openBlockers(dir, item);
      if (open.length) out(`\nopen blockers: ${open.join(', ')}`);
      return 0;
    }

    case 'list':
    case 'board': {
      pull();
      const items = readItems(dir).filter(
        (it) =>
          (!opts.owner || it.owner === opts.owner) &&
          (opts.all || opts.status || !RESOLVED.has(it.status)) &&
          (!opts.status || it.status === opts.status),
      );
      items.forEach((it) => out(fmtItem(dir, it)));
      if (!items.length) out('(no items)');
      return 0;
    }

    case 'next': {
      checkRole(role, roles);
      pull();
      const mine = readItems(dir).filter(
        (it) => it.owner === role && !RESOLVED.has(it.status) && it.status !== 'review',
      );
      // An item marked `blocked` with no blocked_by waits on something outside the bus (owner action).
      const isReady = (it) => it.status !== 'blocked' || it.blocked_by.length > 0;
      const ready = mine.filter((it) => isReady(it) && !openBlockers(dir, it).length);
      ready.forEach((it) => out(fmtItem(dir, it)));
      if (!ready.length) {
        out(
          mine.length
            ? `(nothing ready: ${mine.length} item(s) wait on blockers)`
            : '(no open items for this role)',
        );
        return 1;
      }
      return 0;
    }

    case 'send': {
      checkRole(role, roles);
      const to = opts.to;
      if (to !== 'all') checkRole(to, roles);
      const note = pos.join(' ');
      if (!note) throw new UsageError('send needs a message');
      emit(dir, { from: role, to, type: opts.type ?? 'message', item: opts.item, note });
      pull();
      return 0;
    }

    case 'inbox': {
      checkRole(role, roles);
      pull();
      const evs = inbox(dir, role, { advance: !opts.peek });
      evs.forEach((ev) => out(fmtEvent(ev)));
      if (!evs.length && !opts.quiet) out('(inbox empty)');
      return 0;
    }

    case 'log': {
      const evs = readEvents(dir);
      evs.slice(-Number(opts.n ?? 30)).forEach((ev) => out(fmtEvent(ev)));
      return 0;
    }

    case 'lock': {
      checkRole(role, roles);
      const [module] = pos;
      if (!module) throw new UsageError('lock needs a module path');
      pull();
      const lane = laneOf(roles, module);
      if (lane && lane !== role && !opts.force)
        throw new UsageError(
          `${module} is in the ${lane} lane; ask ${lane} (coord new --owner ${lane}) or pass --force with a reason in --note`,
        );
      if (!lane && !opts.force)
        throw new UsageError(
          `${module} is in no lane (archived or unknown); see scripts/coord.roles.json`,
        );
      lock(dir, role, module, opts.note ?? '');
      pull();
      const held = readLock(dir, module);
      if (held?.role !== role) throw new UsageError(`${module} was claimed first by ${held?.role}`);
      emit(dir, {
        from: role,
        type: 'lock',
        note: `locked ${module}${opts.note ? `: ${opts.note}` : ''}`,
      });
      out(`locked ${module}`);
      return 0;
    }

    case 'unlock': {
      checkRole(role, roles);
      unlock(dir, role, pos[0], { force: !!opts.force });
      emit(dir, { from: role, to: 'all', type: 'unlock', note: `unlocked ${pos[0]}` });
      pull();
      out(`unlocked ${pos[0]}`);
      return 0;
    }

    case 'locks':
      readLocks(dir).forEach((l) =>
        out(`${l.module.padEnd(28)} ${l.role.padEnd(10)} ${l.since} ${l.note}`),
      );
      return 0;

    // Block until every listed item is done/dropped (exit 0), or until the role's
    // inbox has something (--inbox), or until --timeout seconds pass (exit 2).
    // Agents run this as a background command so the harness wakes them on exit.
    case 'wait': {
      const ids = list(pos.join(','));
      ids.forEach(checkId);
      if (!ids.length && !opts.inbox) throw new UsageError('wait needs item ids and/or --inbox');
      if (opts.inbox) checkRole(role, roles);
      const timeout = Number(opts.timeout ?? 1500) * 1000;
      const interval = Number(opts.interval ?? (isGitDir(dir) ? 60 : 10)) * 1000;
      const deadline = Date.now() + timeout;
      for (;;) {
        pull();
        const pending = ids.filter((id) => !RESOLVED.has(readItem(dir, id)?.status));
        if (ids.length && !pending.length) {
          ids.forEach((id) => out(`${id}: ${readItem(dir, id).status}`));
          return 0;
        }
        if (opts.inbox) {
          const evs = inbox(dir, role, { advance: false });
          if (evs.length) {
            evs.forEach((ev) => out(fmtEvent(ev)));
            return 0;
          }
        }
        if (Date.now() >= deadline) {
          out(`timeout; still pending: ${pending.join(', ') || '(inbox empty)'}`);
          return 2;
        }
        await sleep(Math.min(interval, Math.max(0, deadline - Date.now())));
      }
    }

    case 'sync':
      out(sync(dir) ? 'synced' : `${dir} is not a git repo; nothing to sync`);
      return 0;

    // SessionStart hook: print the role's unread inbox and ready items. Silent without a role.
    case 'hook-session-start': {
      if (!role || !roles[role]) return 0;
      pull();
      const evs = inbox(dir, role, { advance: false });
      out(`MuxCore role: ${role} (docs/agents/ROLES.md). Coord dir ${dir}.`);
      if (evs.length) {
        out('Unread coordination events (run `node scripts/coord.mjs inbox` to mark read):');
        evs.forEach((ev) => out(`  ${fmtEvent(ev)}`));
      }
      return 0;
    }

    default:
      out(`usage: coord.mjs <command> [--as <role>]   (role defaults to $MUXCORE_ROLE)
  init                                   create the coord dir layout
  new <id> --title T [--owner R] [--blocked-by A,B] [--modules m1,m2] [--body text]
  set <id> [status] [--note text] [--evidence text] [--blocked-by A,B] [--owner R]
  show <id> | list [--owner R] [--status S] [--all] | next
  send --to <role|all> [--item ID] <message>
  inbox [--peek]                         unread events that concern you
  wait [<id,...>] [--inbox] [--timeout s] [--interval s]   exit 0 when resolved, 2 on timeout
  lock <module> [--note text] | unlock <module> | locks
  log [--n N] | sync
statuses: ${STATUSES.join(', ')}`);
      return cmd ? 64 : 0;
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2))
    .then((code) => process.exit(code))
    .catch((e) => {
      console.error(`coord: ${e.message}`);
      process.exit(e instanceof UsageError ? 64 : 1);
    });
}
