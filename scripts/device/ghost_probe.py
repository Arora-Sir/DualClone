"""Diagnose stale launcher icons after a clone is removed from a secondary profile.

Usage:
    python scripts/device/ghost_probe.py --serial <serial> --user 10 --package <pkg> snapshot
    python scripts/device/ghost_probe.py --serial <serial> --user 10 --package <pkg> cycle
    python scripts/device/ghost_probe.py --serial <serial> refresh-launcher

Use a throwaway package that is already installed for User 0. The cycle command runs
pm install-existing, snapshot, pm uninstall, snapshot, so the before and after states can be compared.
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import time


def adb(serial: str, *args: str, timeout: int = 30) -> str:
    cmd = ["adb", "-s", serial, *args]
    res = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    return (res.stdout + res.stderr).strip()


def sh(serial: str, command: str) -> str:
    return adb(serial, "shell", command)


def snapshot(serial: str, user: int, pkg: str, label: str) -> dict:
    listed = pkg in sh(serial, f"pm list packages --user {user}")
    known = pkg in sh(serial, f"pm list packages -u --user {user}")
    path = sh(serial, f"pm path --user {user} {pkg}")
    dump = sh(serial, f"dumpsys package {pkg}")
    state = re.search(rf"User {user}: ceDataInode=\S+ deviceProtectedDataInode=\S+ installed=(\w+) hidden=(\w+) suspended=(\w+)", dump)
    if state is None:
        state = re.search(rf"User {user}:.*?installed=(\w+).*?hidden=(\w+).*?suspended=(\w+)", dump)
    launcher = sh(serial, f"cmd package query-activities --user {user} --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER")
    users = sh(serial, "pm list users")
    flags = re.search(rf"UserInfo\{{{user}:[^:]*:([0-9a-fA-F]+)\}}", users)
    quiet = bool(flags and int(flags.group(1), 16) & 0x80)
    result = {
        "label": label,
        "listed": listed,
        "known_uninstalled_or_installed": known,
        "pm_path": path or "(none)",
        "installed_hidden_suspended": state.groups() if state else None,
        "launcher_entry": pkg in launcher,
        "profile_paused": quiet,
    }
    print(f"[{label}] " + ", ".join(f"{k}={v}" for k, v in result.items() if k != "label"))
    return result


def cmd_cycle(serial: str, user: int, pkg: str) -> None:
    snapshot(serial, user, pkg, "before")
    print(sh(serial, f"pm install-existing --user {user} {pkg}"))
    time.sleep(2)
    snapshot(serial, user, pkg, "after-clone")
    print(sh(serial, f"pm uninstall --user {user} {pkg}"))
    time.sleep(3)
    after = snapshot(serial, user, pkg, "after-remove")
    if after["listed"] or after["launcher_entry"]:
        print("RESULT: package manager still reports the clone, this is not a launcher ghost.")
    else:
        print("RESULT: OS state is clean. Check the Work tab visually, then run refresh-launcher if an icon remains.")


def cmd_refresh_launcher(serial: str) -> None:
    pkg = "com.sec.android.app.launcher"
    if pkg not in sh(serial, "pm list packages"):
        print("Samsung launcher not present on this device.")
        return
    print(sh(serial, f"am force-stop {pkg}") or "launcher force-stopped")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"), required=not os.environ.get("ANDROID_SERIAL"))
    parser.add_argument("--user", type=int, default=10)
    parser.add_argument("--package")
    parser.add_argument("command", choices=["snapshot", "cycle", "refresh-launcher"])
    args = parser.parse_args()
    if args.command != "refresh-launcher" and not args.package:
        parser.error("--package is required")
    if args.command == "snapshot":
        snapshot(args.serial, args.user, args.package, "snapshot")
    elif args.command == "cycle":
        cmd_cycle(args.serial, args.user, args.package)
    else:
        cmd_refresh_launcher(args.serial)
    return 0


if __name__ == "__main__":
    sys.exit(main())
