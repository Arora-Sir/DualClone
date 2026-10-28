"""Fail when tracked or staged files contain private data or style violations.

Checks private IP addresses, personal local paths, secret-looking strings,
dash, curly-quote and spaced double-hyphen style violations in docs and sources.
Version consistency lives in check_version.py.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

TEXT_SUFFIXES = {".kt", ".kts", ".xml", ".md", ".py", ".yml", ".yaml", ".toml", ".pro", ".properties", ".json", ".sh", ".txt"}
SKIP_FILES = {"LICENSE", "scripts/release/scan_repo.py"}

PRIVATE_PATTERNS = {
    "private IP": re.compile(r"\b(?:100\.(?:6[4-9]|[7-9]\d|1[01]\d|12[0-7])\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3})\b"),
    "personal path": re.compile(r"(?:[A-Za-z]:\\Users\\Mohit|[A-Za-z]:/Users/Mohit|D:\\Study|D:/Study|/\.gemini/|\\\.gemini\\)"),
    "personal email": re.compile(r"(?i)\b[\w.+-]+@(?:gmail\.com|oxaam\.com)\b"),
    "agent shell prefix": re.compile(r"^\s*(?:\$ )?rtk\s"),
    "LAN address": re.compile(r"\b(?:10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(?:1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3})\b"),
    "secret": re.compile(r"(?i)(?:ghp_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|(?:store|key)Password\s*=\s*[\"'][^\"']+[\"'])"),
}
STYLE_PATTERNS = {
    "em or en dash": re.compile("[\u2014\u2013]"),
    "curly quote": re.compile("[\u201c\u201d\u2018\u2019]"),
}
# NOTE: A spaced double hyphen is used as a dash substitute in prose. Command flags never have spaces around the hyphens.
PROSE_PATTERNS = {"spaced double hyphen": re.compile(r"\s--\s")}


def tracked_files() -> list[str]:
    out = subprocess.run(["git", "ls-files", "--cached", "--others", "--exclude-standard"], cwd=ROOT, capture_output=True, text=True, check=True)
    return [line for line in out.stdout.splitlines() if line]


def main() -> int:
    problems: list[str] = []
    for rel in tracked_files():
        path = ROOT / rel
        if rel in SKIP_FILES or path.suffix not in TEXT_SUFFIXES or not path.is_file():
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for lineno, line in enumerate(text.splitlines(), 1):
            patterns = {**PRIVATE_PATTERNS, **STYLE_PATTERNS, **(PROSE_PATTERNS if path.suffix == ".md" else {})}
            for name, pattern in patterns.items():
                # NOTE: Vector path data in XML drawables contains number runs that look like addresses.
                if name == "LAN address" and path.suffix == ".xml":
                    continue
                if pattern.search(line):
                    problems.append(f"{rel}:{lineno}: {name}")

    for item in problems:
        print(item)
    print(f"scan_repo: {len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
