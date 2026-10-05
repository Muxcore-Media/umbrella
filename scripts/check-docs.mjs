#!/usr/bin/env node
// Validate the umbrella documentation set:
//  - relative Markdown links resolve to existing files (and heading anchors, for .md targets)
//  - every FR-*/NFR-* requirement ID cited anywhere is defined in docs/specs/SRS.md
//  - every ADR-NNNN cited exists in docs/adr/
// Scope: root *.md and docs/** (docs/history is historical and skipped).
import { readFileSync, readdirSync, statSync, existsSync } from "node:fs";
import { join, dirname, resolve, relative } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const SKIP_DIRS = new Set(["history", "node_modules"]);

export function listDocs(root = ROOT) {
  const out = readdirSync(root)
    .filter((f) => f.endsWith(".md"))
    .map((f) => join(root, f));
  const walk = (dir) => {
    for (const name of readdirSync(dir)) {
      const p = join(dir, name);
      if (statSync(p).isDirectory()) {
        if (!SKIP_DIRS.has(name)) walk(p);
      } else if (name.endsWith(".md")) out.push(p);
    }
  };
  if (existsSync(join(root, "docs"))) walk(join(root, "docs"));
  return out;
}

// GitHub-style heading slug.
export function slugify(heading) {
  return heading
    .trim()
    .toLowerCase()
    .replace(/<[^>]+>/g, "")
    .replace(/[`*_~]/g, "")
    .replace(/[^\p{L}\p{N}\s-]/gu, "")
    .replace(/\s/g, "-");
}

export function anchorsOf(text) {
  const anchors = new Set();
  const counts = new Map();
  let inFence = false;
  for (const line of text.split("\n")) {
    if (/^\s*```/.test(line)) inFence = !inFence;
    if (inFence) continue;
    const m = /^#{1,6}\s+(.*)$/.exec(line);
    if (!m) continue;
    const base = slugify(m[1].replace(/\[([^\]]*)\]\([^)]*\)/g, "$1"));
    const n = counts.get(base) ?? 0;
    counts.set(base, n + 1);
    anchors.add(n === 0 ? base : `${base}-${n}`);
  }
  return anchors;
}

export function linksOf(text) {
  const links = [];
  let inFence = false;
  for (const line of text.split("\n")) {
    if (/^\s*```/.test(line)) inFence = !inFence;
    if (inFence) continue;
    for (const m of line.matchAll(/\]\(([^)\s]+)\)/g)) links.push(m[1]);
  }
  return links;
}

export function definedRequirementIds(srsText) {
  const ids = new Set();
  for (const m of srsText.matchAll(/^\|\s*((?:N?FR)-[A-Z0-9]+-\d{3})\s*\|/gm)) ids.add(m[1]);
  return ids;
}

export function citedRequirementIds(text) {
  return [...text.matchAll(/\b((?:N?FR)-[A-Z0-9]+-\d{3})\b/g)].map((m) => m[1]);
}

// Submodule paths from .gitmodules; links into a submodule that is not checked
// out (e.g. CI without --recurse-submodules) cannot be verified and are skipped.
export function submodulePaths(root = ROOT) {
  const f = join(root, ".gitmodules");
  if (!existsSync(f)) return [];
  return [...readFileSync(f, "utf8").matchAll(/^\s*path\s*=\s*(.+)$/gm)].map((m) => m[1].trim());
}

function uncheckedSubmodule(root, dest, subs) {
  const rel = relative(root, dest);
  const sub = subs.find((s) => rel === s || rel.startsWith(`${s}/`));
  if (!sub) return false;
  const dir = join(root, sub);
  return !existsSync(dir) || readdirSync(dir).filter((n) => n !== ".git").length === 0;
}

export function check(root = ROOT) {
  const errors = [];
  const subs = submodulePaths(root);
  const srsPath = join(root, "docs/specs/SRS.md");
  const defined = existsSync(srsPath) ? definedRequirementIds(readFileSync(srsPath, "utf8")) : new Set();
  const adrDir = join(root, "docs/adr");
  const adrs = existsSync(adrDir)
    ? new Set(readdirSync(adrDir).map((f) => /^(\d{4})-/.exec(f)?.[1]).filter(Boolean))
    : new Set();
  const anchorCache = new Map();
  const anchorsFor = (p) => {
    if (!anchorCache.has(p)) anchorCache.set(p, anchorsOf(readFileSync(p, "utf8")));
    return anchorCache.get(p);
  };
  for (const file of listDocs(root)) {
    const text = readFileSync(file, "utf8");
    const rel = relative(root, file);
    for (const link of linksOf(text)) {
      if (/^[a-z]+:/i.test(link) || link.startsWith("mailto:")) continue;
      const [target, anchor] = link.split("#");
      const dest = target ? resolve(dirname(file), decodeURIComponent(target)) : file;
      if (!existsSync(dest)) {
        if (uncheckedSubmodule(root, dest, subs)) continue;
        errors.push(`${rel}: broken link ${link}`);
        continue;
      }
      if (anchor && dest.endsWith(".md") && !anchorsFor(dest).has(anchor)) {
        errors.push(`${rel}: missing anchor ${link}`);
      }
    }
    if (defined.size) {
      for (const id of new Set(citedRequirementIds(text))) {
        if (!defined.has(id)) errors.push(`${rel}: undefined requirement ${id}`);
      }
    }
    for (const m of text.matchAll(/\bADR-(\d{4})\b/g)) {
      if (!adrs.has(m[1])) errors.push(`${rel}: unknown ADR-${m[1]}`);
    }
  }
  return errors;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const errors = check();
  for (const e of errors) console.error(e);
  console.log(errors.length ? `check-docs: ${errors.length} problem(s)` : "check-docs: ok");
  process.exit(errors.length ? 1 : 0);
}
