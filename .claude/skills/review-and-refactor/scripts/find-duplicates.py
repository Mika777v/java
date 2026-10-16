#!/usr/bin/env python3
"""Lists blocks of code that appear more than once (candidates for extracting a method).

Usage: python3 find-duplicates.py [min_lines] [path ...]
  min_lines  how many consecutive lines must match (default 5)
  path       files or directories to scan (default: main Java code and templates)

Lines are compared after trimming spaces; blank lines, imports, braces-only lines and
comments are ignored. Read-only. A hit is a candidate, not a finding: check the code.
"""
import pathlib
import subprocess
import sys
from collections import defaultdict

root = pathlib.Path(subprocess.check_output(
    ["git", "rev-parse", "--show-toplevel"], cwd=pathlib.Path(__file__).parent, text=True).strip())
app = root / "onboarding-progress-system" / "src" / "main"

args = sys.argv[1:]
min_lines = int(args.pop(0)) if args and args[0].isdigit() else 5
targets = [pathlib.Path(a) for a in args] or [app / "java", app / "resources" / "templates"]

def files():
    for t in targets:
        if t.is_file():
            yield t
        else:
            yield from sorted(p for p in t.rglob("*") if p.suffix in (".java", ".html"))

def meaningful(path):
    for no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = " ".join(raw.split())
        if (not line or line.startswith(("import ", "package ", "//", "*", "/*"))
                or line in ("{", "}", "};", ");", "})", "</div>", "</td>", "</tr>")):
            continue
        yield no, line

windows = defaultdict(list)
lines_of = {}
for f in files():
    lines = list(meaningful(f))
    lines_of[f] = lines
    for i in range(len(lines) - min_lines + 1):
        chunk = tuple(l for _, l in lines[i:i + min_lines])
        windows[chunk].append((f, i))

# Overlapping windows (same files, start index +1) are merged into one longer block
dups = sorted(tuple(sorted(places)) for places in windows.values() if len(places) > 1)
blocks = []  # [places_at_start, length]
by_start = {}
for places in dups:
    prev = tuple((f, i - 1) for f, i in places)
    if prev in by_start:
        block = by_start.pop(prev)
        block[1] += 1
    else:
        block = [places, min_lines]
        blocks.append(block)
    by_start[places] = block

blocks.sort(key=lambda b: (-b[1] * len(b[0]), str(b[0])))
for places, length in blocks:
    print(f"--- {length} lines x {len(places)} places:")
    for f, i in places:
        lines = lines_of[f]
        print(f"    {f.relative_to(root)}:{lines[i][0]}-{lines[i + length - 1][0]}")
    f, i = places[0]
    for _, l in lines_of[f][i:i + min(length, 6)]:
        print(f"      | {l[:110]}")
    if length > 6:
        print("      | ...")
print(f"\n{len(blocks)} duplicated block(s) of {min_lines}+ lines")
