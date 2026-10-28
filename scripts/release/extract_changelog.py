"""Print GitHub release notes for one version, cut from CHANGELOG.md.

Usage:
    python scripts/release/extract_changelog.py 1.0.0 --cert-file cert.txt > RELEASE_NOTES.md

The notes hold the changelog section for the version, the signing certificate SHA-256 when given,
and a short verification note. Fails when the version has no dated section.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def section(version: str) -> str:
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    pattern = re.compile(rf"^## \[{re.escape(version)}\] - \d{{4}}-\d{{2}}-\d{{2}}\s*$", re.MULTILINE)
    match = pattern.search(text)
    if not match:
        raise SystemExit(f"CHANGELOG.md has no dated section for {version}")
    rest = text[match.end():]
    nxt = re.search(r"^## \[", rest, re.MULTILINE)
    body = rest[: nxt.start()] if nxt else rest
    body = re.sub(r"^---\s*$", "", body, flags=re.MULTILINE)
    return body.strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("version", help="Version without the v prefix, for example 1.0.0")
    parser.add_argument("--cert-file", type=Path, help="File holding the signer certificate SHA-256")
    args = parser.parse_args()

    parts = [section(args.version)]
    if args.cert_file and args.cert_file.is_file():
        digest = args.cert_file.read_text(encoding="utf-8").strip()
        parts.append(
            "### Verify this release\n\n"
            f"Signing certificate SHA-256: `{digest}`\n\n"
            "Compare it with the fingerprint in the README before installing. "
            "`SHA256SUMS.txt` holds the SHA-256 checksum of the APK. The in-app updater checks the download against it before it offers the install."
        )
    sys.stdout.write("\n\n".join(parts) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
