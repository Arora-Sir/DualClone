"""Audit everything git would publish. Exit code 1 blocks the push.

Run before every push and before creating the first public commit:
    python scripts/release/prepush_audit.py
"""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXPECTED_EMAILS = {"cs.mohitaroraofficial" + "@gmail.com"}
MAX_FILE_BYTES = 2_000_000

FORBIDDEN = re.compile(
    r"(^|/)(release-signing/|secrets/|key[-_]text[^/]*\.txt$|key-backup[^/]*\.txt$|secret[^/]*\.txt$|[^/]*\.b64$|\.e2e/|\.claude/|\.gemini/|private/|keystore\.properties$|local\.properties$|"
    r"HANDOFF\.md$|PROJECT_TRACKER\.md$|FUTURE_WORK\.md$|bugs\.md$|CLAUDE\.md$|GEMINI\.md$|AGENTS\.md$|island\.apk$|\.env($|\.)|"
    r".*\.(jks|keystore|p12|pfx|pem|key|idsig|apk|aab|apks|xapk|hprof|log))"
)


def git(*args: str) -> str:
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=True).stdout


def run_script(name: str) -> bool:
    res = subprocess.run([sys.executable, str(ROOT / "scripts" / "release" / name)], cwd=ROOT, capture_output=True, text=True)
    print(f"[{name}] {res.stdout.strip().splitlines()[-1] if res.stdout.strip() else 'no output'}")
    if res.returncode != 0:
        print(res.stdout)
    return res.returncode == 0


def main() -> int:
    problems: list[str] = []
    tracked = [f for f in git("ls-files").splitlines() if f]
    staged_or_untracked = [f for f in git("ls-files", "--others", "--exclude-standard").splitlines() if f]
    print(f"tracked files: {len(tracked)}, untracked and not ignored: {len(staged_or_untracked)}")

    for f in tracked + staged_or_untracked:
        if FORBIDDEN.search(f):
            problems.append(f"forbidden file would be published: {f}")
        p = ROOT / f
        if p.is_file() and p.stat().st_size > MAX_FILE_BYTES:
            problems.append(f"file larger than 2 MB: {f} ({p.stat().st_size} bytes)")

    email = git("config", "user.email").strip()
    if email not in EXPECTED_EMAILS:
        problems.append(f"git author email is {email}, expected one of {sorted(EXPECTED_EMAILS)}")

    for script in ("scan_repo.py", "check_version.py", "check_network_imports.py"):
        if not run_script(script):
            problems.append(f"{script} failed")

    if shutil.which("sh") and (ROOT / ".githooks" / "guard-secrets.sh").exists():
        res = subprocess.run(["sh", ".githooks/guard-secrets.sh"], cwd=ROOT, input="\n".join(tracked + staged_or_untracked), capture_output=True, text=True)
        if res.returncode != 0:
            problems.append("guard-secrets.sh rejected a file: " + res.stdout.strip().replace("\n", ", "))

    for item in problems:
        print("PROBLEM:", item)
    print("AUDIT FAILED" if problems else "AUDIT CLEAN: nothing private would be published")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
