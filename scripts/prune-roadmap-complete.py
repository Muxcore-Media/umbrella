#!/usr/bin/env python3
"""Remove completed checklists from MASTER-ROADMAP.md (open work only)."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "MASTER-ROADMAP.md"
text = path.read_text()
lines = text.splitlines()


def is_module_header(line: str) -> bool:
    return line.startswith("### Module: `")


def is_major_header(line: str) -> bool:
    return line.startswith("## ") and not line.startswith("### ")


out: list[str] = []
i = 0
while i < len(lines):
    line = lines[i]

    if re.match(r"^- \[x\] ", line):
        i += 1
        continue

    if "| [x]" in line and line.strip().startswith("|"):
        i += 1
        continue

    if line.strip().startswith("<!-- pruned"):
        i += 1
        continue

    if is_module_header(line):
        block = [line]
        i += 1
        while i < len(lines) and not (is_module_header(lines[i]) or is_major_header(lines[i])):
            block.append(lines[i])
            i += 1
        block_text = "\n".join(block)
        if "**Status:** COMPLETE" in block_text:
            continue
        if "<!-- pruned" in block_text and "**Status:** OPEN" not in block_text and "[ ]" not in block_text:
            continue
        cleaned = [l for l in block if not l.strip().startswith("<!-- pruned")]
        out.extend(cleaned)
        continue

    out.append(line)
    i += 1

text = "\n".join(out)
text = re.sub(r"### P1 — Platform engineering\n\n", "", text)

text = re.sub(
    r"## Appendix B: Radarr / Sonarr operator parity\n\n## Radarr / Sonarr → MuxCore operator parity\n\n\*\*Goal:\*\*[^\n]+\n\n\*\*Canonical modules:\*\*[^\n]+\n\nLegend:[^\n]+\n\n---\n\n### A\. Shared \(\*Arr\) operator surfaces\n\n\| \*Arr surface \| MuxCore target \| Status \|\n\|[-| ]+\|\n\n---\n\n### B\. Radarr \(movies\)[^\n]+\n\n\| Radarr surface \| MuxCore target \| Status \|\n\|[-| ]+\|\n\| Calendar \| N/A \(movies\) \| N/A \|\n\n---\n\n### C\. Sonarr \(TV\)[^\n]+\n\n\| Sonarr surface \| MuxCore target \| Status \|\n\|[-| ]+\|\n\n---\n",
    "## Appendix B: Radarr / Sonarr operator parity\n\nNo open checklist rows. Import lists and one-shot migrate shipped (`LISTSYNC-PARITY.md`, admin-ui `/migrate`).\n\n---\n",
    text,
    flags=re.DOTALL,
)

text = re.sub(r"\*\*Shipped \(removed from open checklist\):\*\*[^\n]+\n\n", "", text)
text = re.sub(
    r"\*\*Shipped operator surfaces \(removed\):\*\*[^\n]+\n\nNo open Jellyfin-dashboard checklist rows[^\n]+\n\n",
    "No open Jellyfin-dashboard checklist rows for near-term ops.\n\n",
    text,
)

text = re.sub(
    r"## Appendix D: Bazarr → media-subtitles parity\n\n\*\*Done\*\* —[^\n]+\n\n",
    "## Appendix D: Bazarr → media-subtitles parity\n\nShipped — see [`BAZARR-PARITY.md`](./BAZARR-PARITY.md).\n\n",
    text,
)

module_names = re.findall(r"^### Module: `([^`]+)`", text, re.MULTILINE)
idx_start = text.find("### Index\n")
idx_end = text.find("\n### Module: `", idx_start)
if idx_start != -1 and idx_end != -1:
    new_index = "### Index\n\n"
    new_index += "Open module audits only. Completed wave1 records: `_wave1/module-todos/<module>.md`.\n\n"
    for name in module_names:
        anchor = name.replace("_", "-")
        new_index += f"- [`{name}`](#module-{anchor})\n"
    new_index += "\n"
    text = text[:idx_start] + new_index + text[idx_end + 1 :]

text = re.sub(
    r"One wave of read-only audits:[^\n]+\n\nSkip Appendix H[^\n]+\n\nSource files:[^\n]+\n",
    "Open module audits from the 2026-08-28 fanout. Completed modules are removed from this appendix; history lives in `_wave1/module-todos/<module>.md`.\n\n"
    "Skip Appendix H dumps (`media-ui`, `media-jellyfin`, `cache-memory`, `custom-scripts`, `muxcorectl`) and scratch dirs.\n\n",
    text,
)

text = re.sub(r"\n{4,}", "\n\n\n", text)
path.write_text(text.rstrip() + "\n")
print(f"Wrote {path} ({len(text.splitlines())} lines, {len(module_names)} open modules)")
