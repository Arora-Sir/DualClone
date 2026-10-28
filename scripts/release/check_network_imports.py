"""Fail when network code appears outside the update package.

DualClone uses the network only to check for and download updates. All of that code lives in
app/src/main/java/com/mohitarora/dualclone/update/. Any other source file that imports or names
java.net, javax.net, okhttp or retrofit fails this check. The same rule is also enforced by a unit test.

Usage:
    python scripts/release/check_network_imports.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "app" / "src"
ALLOWED_DIRS = (
    Path("main/java/com/mohitarora/dualclone/update"),
    Path("test/java/com/mohitarora/dualclone/update"),
)
PATTERN = re.compile(r"\b(?:java\.net|javax\.net|okhttp3?|retrofit2?|com\.squareup\.(?:okhttp3?|retrofit2?))\b")


def main() -> int:
    problems = []
    for path in sorted(SOURCE_ROOT.rglob("*")):
        if path.suffix not in (".kt", ".java") or not path.is_file():
            continue
        rel = path.relative_to(SOURCE_ROOT)
        if any(allowed in rel.parents for allowed in ALLOWED_DIRS):
            continue
        # NOTE: The architecture test names the banned packages on purpose to enforce the rule.
        if rel.as_posix() == "test/java/com/mohitarora/dualclone/arch/ArchitectureTest.kt":
            continue
        for lineno, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
            if PATTERN.search(line):
                problems.append(f"app/src/{rel.as_posix()}:{lineno}: network reference outside the update package")
    for item in problems:
        print(item)
    print(f"check_network_imports: {len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
