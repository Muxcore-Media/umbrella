#!/usr/bin/env python3
"""Remove named Appendix I module blocks from MASTER-ROADMAP.md and rebuild index."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "MASTER-ROADMAP.md"
remove = set(sys.argv[1:])
if not remove:
    print("usage: remove-roadmap-modules.py <module> [<module> ...]", file=sys.stderr)
    sys.exit(1)

lines = path.read_text().splitlines()


def is_module_header(line: str) -> bool:
    return line.startswith("### Module: `")


def is_major_header(line: str) -> bool:
    return line.startswith("## ") and not line.startswith("### ")


out: list[str] = []
i = 0
while i < len(lines):
    line = lines[i]
    if is_module_header(line):
        name = line.split("`")[1]
        block = [line]
        i += 1
        while i < len(lines) and not (is_module_header(lines[i]) or is_major_header(lines[i])):
            block.append(lines[i])
            i += 1
        if name in remove:
            continue
        out.extend(block)
        continue
    out.append(line)
    i += 1

text = "\n".join(out)
module_names = re.findall(r"^### Module: `([^`]+)`", text, re.MULTILINE)
idx_start = text.find("### Index\n")
idx_end = text.find("\n### Module: `", idx_start)
if idx_start != -1:
    if module_names and idx_end != -1:
        new_index = "### Index\n\n"
        new_index += "Open module audits only. Completed wave1 records: `_wave1/module-todos/<module>.md`.\n\n"
        for name in module_names:
            anchor = name.replace("_", "-")
            new_index += f"- [`{name}`](#module-{anchor})\n"
        new_index += "\n"
        text = text[:idx_start] + new_index + text[idx_end + 1 :]
    else:
        text = text[:idx_start] + text[idx_end + 1 if idx_end != -1 else idx_start :]
        text = re.sub(
            r"(## Appendix I:[^\n]+\n\n)Open module audits[^\n]+\n\n",
            r"\1All module audits from the 2026-08-28 fanout are complete. History: `_wave1/module-todos/<module>.md`.\n\n",
            text,
            count=1,
        )

path.write_text(text.rstrip() + "\n")
print(f"Removed {len(remove)} modules; {len(module_names)} remain ({len(text.splitlines())} lines)")
