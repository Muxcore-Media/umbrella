#!/usr/bin/env bash
# Convert the MuxCore workspace folder into an umbrella git repo with Forgejo
# modules registered as submodules (gitlinks). Safe to re-run: skips if .git exists.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -d .git ]]; then
  echo "Umbrella already initialized (.git exists). Nothing to do."
  exit 0
fi

if ! command -v git >/dev/null; then
  echo "git is required" >&2
  exit 1
fi

SKIP_DIRS=(core.wiki _wave1)
declare -A SKIP_MAP=()
for d in "${SKIP_DIRS[@]}"; do SKIP_MAP["$d"]=1; done

modules=()
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  modules+=("$line")
done < <(
  node <<'NODE'
const { readdirSync, existsSync } = require('fs');
const { join } = require('path');
const { execFileSync } = require('child_process');
const ROOT = process.cwd();
const SKIP = new Set(['core.wiki', '_wave1']);
const gitAt = (dir, args) => execFileSync('git', ['-C', dir, ...args], { encoding: 'utf8' }).trim();
for (const name of readdirSync(ROOT, { withFileTypes: true }).filter(d => d.isDirectory() && !d.name.startsWith('.')).map(d => d.name)) {
  if (SKIP.has(name)) continue;
  const dir = join(ROOT, name);
  if (!existsSync(join(dir, '.git'))) continue;
  const url = gitAt(dir, ['remote', 'get-url', 'origin']);
  console.log(`${name}\t${url}`);
}
NODE
)

if ((${#modules[@]} == 0)); then
  echo "No module git repos found under $ROOT" >&2
  exit 1
fi

echo "Initializing umbrella with ${#modules[@]} submodules…"
git init -b main

: > .gitmodules
for entry in "${modules[@]}"; do
  path="${entry%%$'\t'*}"
  url="${entry#*$'\t'}"
  {
    echo "[submodule \"${path}\"]"
    echo "	path = ${path}"
    echo "	url = ${url}"
  } >> .gitmodules
done

git add .gitmodules
git submodule absorbgitdirs

for entry in "${modules[@]}"; do
  path="${entry%%$'\t'*}"
  git add "$path"
done

# Owned umbrella files (not submodules)
git add \
  .gitignore \
  .cursor \
  scripts \
  AGENTS.md \
  README-UMBRELLA.md \
  platform-quality-fanout.tsx \
  2>/dev/null || true

for f in *.md; do
  [[ -f "$f" ]] && git add "$f" || true
done

if [[ -d .github ]]; then
  git add .github
fi

if git diff --cached --quiet; then
  echo "Nothing staged — check for errors" >&2
  exit 1
fi

git commit -m "$(cat <<'EOF'
chore: initialize MuxCore umbrella workspace

Register Forgejo module checkouts as submodules and track workspace
tooling (.cursor/skills, scripts/app-fanout, AGENTS.md) at the root.
EOF
)"

echo "Umbrella ready on branch main."
echo "Next: git remote add origin <forgejo-umbrella-url> && git push -u origin main"
