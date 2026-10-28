"""DualClone end-to-end regression suite for a physical device or an emulator over ADB.

The suite drives the installed app through uiautomator and checks the package manager state with shell commands.
Test case ids match the list in SETUP.md.

Usage:
    python scripts/device/e2e_test.py --serial <serial>
    python scripts/device/e2e_test.py --serial <serial> --package com.mohitarora.dualclone --apk DualClone-v1.0.0.apk
    python scripts/device/e2e_test.py --serial <serial> --probe-package <throwaway.package> --user 10

Notes:
    --package selects the application id under test. The default is the debug build. Use the release id with a release APK to
    test the minified build. --probe-package must be an app that is installed for User 0 and is safe to clone and remove.
    Without it the clone and ghost probe phases are skipped. Output goes to .e2e/<timestamp>/ unless --out-dir is given.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
import traceback
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Callable

sys.path.insert(0, str(Path(__file__).resolve().parent))
from adbx import Device, Node, PreflightError  # noqa: E402
from ghost_probe import snapshot as probe_snapshot  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
DEBUG_PACKAGE = "com.mohitarora.dualclone.debug"
RELEASE_PACKAGE = "com.mohitarora.dualclone"
ACTIVITY_CLASS = "com.mohitarora.dualclone.ui.MainActivity"

# NOTE: UI strings live in one place so a copy change in strings.xml is a one-line fix here.
TEXT = {
    "app_title": "DualClone",
    "settings_desc": "Settings",
    "settings_titles": ("Settings", "Settings & About"),
    "special_access_desc": "Special Access",
    "special_access_header": "Special Access",
    "special_access_op": "All Files Access",
    "special_access_badges": ("Allowed", "Denied", "System Default"),
    "dev_screen": "Developer Diagnostics",
    "paused_chip": "Paused",
    "version_row": "Version",
}
DEV_UNLOCK_TAPS = 7
FLAG_MANAGED = 0x20
FLAG_QUIET = 0x80
KNOX_USER = 150


@dataclass
class UserInfo:
    user_id: int
    name: str
    flags: int

    @property
    def managed(self) -> bool:
        return bool(self.flags & FLAG_MANAGED)

    @property
    def paused(self) -> bool:
        return self.managed and bool(self.flags & FLAG_QUIET)


def load_allowed(package: str) -> set[str]:
    """Reads the release allow-list. A leading dot means the application id is prepended."""
    allowed = set()
    for line in (ROOT / "scripts" / "release" / "allowed_permissions.txt").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            allowed.add(package + line if line.startswith(".") else line)
    return allowed


class Skip(Exception):
    """Raised inside a case when the precondition for it is not met on this device."""


class Report:
    def __init__(self, out_dir: Path, device_label: str, package: str):
        self.out_dir = out_dir
        self.device_label = device_label
        self.package = package
        self.rows: list[tuple[str, str, str, str]] = []

    def add(self, check_id: str, name: str, status: str, detail: str) -> None:
        self.rows.append((check_id, name, status, detail))
        print(f"[{status}] {check_id} {name}: {detail}", flush=True)

    @property
    def failed(self) -> bool:
        return any(row[2] == "FAIL" for row in self.rows)

    def write(self, header: str) -> Path:
        counts = {s: sum(1 for r in self.rows if r[2] == s) for s in ("PASS", "FAIL", "SKIP")}
        lines = [
            f"# {header}",
            "",
            f"> Executed at: {datetime.now().isoformat(timespec='seconds')}",
            f"> Target: {self.device_label}",
            f"> Package: {self.package}",
            f"> Result: {counts['PASS']} pass, {counts['FAIL']} fail, {counts['SKIP']} skip",
            "",
            "| ID | Check | Result | Detail |",
            "| --- | --- | --- | --- |",
        ]
        lines += [f"| {cid} | {name} | {status} | {detail.replace('|', '/')} |" for cid, name, status, detail in self.rows]
        path = self.out_dir / "report.md"
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        return path


class Suite:
    def __init__(self, dev: Device, package: str, out_dir: Path, report: Report, probe_package: str | None, user: int | None):
        self.dev = dev
        self.package = package
        self.activity = f"{package}/{ACTIVITY_CLASS}"
        self.out_dir = out_dir
        self.report = report
        self.probe_package = probe_package
        self.requested_user = user
        self.screen = (1080, 2400)

    # Helpers

    def case(self, check_id: str, name: str, body: Callable[[], str]) -> None:
        """Runs one case. The body returns a detail string on success, raises AssertionError on failure and Skip to skip."""
        try:
            self.report.add(check_id, name, "PASS", body())
        except Skip as exc:
            self.report.add(check_id, name, "SKIP", str(exc))
        except AssertionError as exc:
            self.save_dump(check_id)
            self.report.add(check_id, name, "FAIL", str(exc))
        except Exception as exc:  # noqa: BLE001
            self.save_dump(check_id)
            self.report.add(check_id, name, "FAIL", f"{type(exc).__name__}: {exc}")
            traceback.print_exc()

    def save_dump(self, label: str) -> None:
        """Keeps the failing screen for diagnosis. A failed dump is ignored on purpose."""
        try:
            raw = self.dev.sh("cat /sdcard/window_dump.xml", check=False)
            if raw.lstrip().startswith("<?xml"):
                (self.out_dir / f"{label}_dump.xml").write_text(raw, encoding="utf-8")
        except Exception:  # noqa: BLE001
            pass

    def nodes(self) -> list[Node]:
        return self.dev.dump_ui()

    def wait_nodes(self, predicate: Callable[[list[Node]], bool], timeout: float = 8.0) -> list[Node]:
        deadline = time.time() + timeout
        last: list[Node] = []
        while time.time() < deadline:
            last = self.nodes()
            if predicate(last):
                return last
            time.sleep(0.6)
        return last

    def tap(self, node: Node, pause: float = 0.8) -> None:
        self.dev.tap(*node.center)
        time.sleep(pause)

    def list_users(self) -> list[UserInfo]:
        users = []
        for match in re.finditer(r"UserInfo\{(\d+):([^:]*):([0-9a-fA-F]+)\}", self.dev.sh("pm list users")):
            users.append(UserInfo(int(match.group(1)), match.group(2), int(match.group(3), 16)))
        return users

    def secondary_user(self) -> UserInfo:
        """The profile used for package checks. User 95 and managed profiles qualify. Knox and full users do not."""
        candidates = [u for u in self.list_users() if u.user_id != 0 and u.user_id != KNOX_USER and (u.user_id == 95 or u.managed)]
        if self.requested_user is not None:
            for user in self.list_users():
                if user.user_id == self.requested_user:
                    return user
            raise Skip(f"User {self.requested_user} does not exist")
        if not candidates:
            raise Skip("No Dual Messenger or managed profile on this device")
        return candidates[0]

    def package_list(self, user: int) -> set[str]:
        out = self.dev.sh(f"pm list packages --user {user}", check=False)
        return {line.split(":", 1)[1].strip() for line in out.splitlines() if line.startswith("package:")}

    def go_home(self) -> None:
        for _ in range(4):
            nodes = self.nodes()
            if any(n.text == TEXT["app_title"] for n in nodes) and any(n.text.startswith("All (") for n in nodes):
                return
            self.dev.back()
            time.sleep(0.6)

    def set_animations(self, enabled: bool) -> dict[str, str]:
        """Animations make uiautomator dump fail with an idle-state error. They are switched off for the run."""
        previous = {}
        for key in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
            previous[key] = self.dev.sh(f"settings get global {key}", check=False).strip()
            value = "1" if enabled else "0"
            self.dev.sh(f"settings put global {key} {value}", check=False)
        return previous

    # Cases

    def run(self, skip_install: bool, apk: Path | None, min_idle: int) -> None:
        dev = self.dev
        try:
            dev.preflight(min_idle_seconds=min_idle)
            self.report.add("TC01", "Preflight Check", "PASS", "Device awake, unlocked, not streaming")
        except (PreflightError, RuntimeError) as exc:
            self.report.add("TC01", "Preflight Check", "FAIL", str(exc))
            return

        size = re.search(r"(\d+)x(\d+)", dev.sh("wm size", check=False))
        if size:
            self.screen = (int(size.group(1)), int(size.group(2)))
        previous_animations = self.set_animations(enabled=False)
        try:
            if not self.install(skip_install, apk):
                return
            self.phases()
        finally:
            for key, value in previous_animations.items():
                dev.sh(f"settings put global {key} {value if value and value != 'null' else '1'}", check=False)

    def install(self, skip_install: bool, apk: Path | None) -> bool:
        dev = self.dev
        if skip_install:
            self.report.add("TC02", "APK Installation", "SKIP", "Skipped by --skip-install")
            return True
        if apk is None:
            name = "debug/app-debug.apk" if self.package == DEBUG_PACKAGE else "release/app-release.apk"
            apk = ROOT / "app" / "build" / "outputs" / "apk" / name
        if not apk.exists():
            self.report.add("TC02", "APK Installation", "FAIL", f"APK not found at {apk}")
            return False
        try:
            out = dev.adb("install", "--user", "0", "-r", str(apk), timeout=240)
            ok = "Success" in out
            self.report.add("TC02", "APK Installation", "PASS" if ok else "FAIL", out.strip())
            return ok
        except Exception as exc:  # noqa: BLE001
            self.report.add("TC02", "APK Installation", "FAIL", str(exc))
            return False

    def phases(self) -> None:
        dev = self.dev
        pkg = self.package

        def permission_allow_list() -> str:
            dump = dev.sh(f"dumpsys package {pkg}")
            requested = dump.split("requested permissions:", 1)[-1].split("install permissions:", 1)[0]
            names = set(re.findall(r"^\s+([A-Za-z0-9_.]+)(?::.*)?$", requested, re.MULTILINE))
            allowed = load_allowed(pkg)
            extra = sorted(n for n in names if n not in allowed and "." in n)
            assert not extra, f"Permissions not on the allow-list: {extra}"
            return f"{len(names)} requested permissions, all on the allow-list"

        self.case("TC03", "Permission Allow-List", permission_allow_list)

        def launch() -> str:
            dev.force_stop(pkg)
            dev.sh(f"am start -n {self.activity}")
            time.sleep(2.5)
            allow = next((n for n in self.nodes() if n.text in ("Allow all the time", "Allow")), None)
            if allow:
                self.tap(allow, 1.5)
            focus = dev.focus()
            assert pkg in focus, f"Unexpected focus: {focus}"
            return f"Focus: {focus}"

        self.case("TC04", "App Launch and Focus", launch)
        state: dict[str, list[Node]] = {"nodes": self.nodes()}

        def title() -> str:
            state["nodes"] = self.wait_nodes(lambda ns: any(n.text == TEXT["app_title"] for n in ns))
            assert any(n.text == TEXT["app_title"] for n in state["nodes"]), "Title not found"
            return "Top bar title found"

        self.case("TC05", "Top App Bar Title", title)

        def shizuku_badge() -> str:
            nodes = state["nodes"]
            grant = next((n for n in nodes if n.text == "Grant"), None)
            if grant:
                self.tap(grant, 1.0)
                button = next((n for n in self.nodes() if "Grant Shizuku Permission" in n.text), None)
                if button:
                    self.tap(button, 1.5)
                    allow = next((n for n in self.nodes() if n.text in ("Allow all the time", "Allow")), None)
                    if allow:
                        self.tap(allow, 1.5)
                dev.back()
                time.sleep(0.8)
                state["nodes"] = self.nodes()
            texts = [n.text for n in state["nodes"]]
            ready = any("Ready" in t for t in texts)
            assert ready, f"Shizuku is not Ready (seen: {[t for t in texts if t in ('Grant', 'Setup', 'Stopped', 'Checking')]})"
            return "Shizuku badge reports Ready"

        self.case("TC06", "Shizuku Status Badge", shizuku_badge)

        def counts() -> str:
            texts = [n.text for n in state["nodes"]]
            found = {label: any(t.startswith(f"{label} (") for t in texts) for label in ("All", "Uncloned", "Cloned")}
            assert all(found.values()), f"Tabs found: {found}"
            return f"Tabs with live counts: {found}"

        self.case("TC07", "Segmented Filter Live Counts", counts)

        def titles() -> str:
            skip = {TEXT["app_title"], "Ready", "Grant", "Setup", "Stopped", "Checking", "Uncloned", "#", "Clone", "Remove"}
            names = [n.text for n in state["nodes"] if len(n.text) > 1 and n.text not in skip and not re.match(r"^(All|Uncloned|Cloned|System) \(", n.text)]
            assert not any(t in ("A...", "...") for t in names), "Squashed titles detected"
            assert any(len(t) > 3 for t in names), "No readable app names on screen"
            return f"Readable titles, sample: {names[:3]}"

        self.case("TC07B", "App Title Visibility", titles)

        def chips() -> str:
            texts = [n.text for n in state["nodes"]]
            assert any(t.startswith("All (") for t in texts) and any(t.startswith("Uncloned (") for t in texts), "Filter tabs missing"
            return "Filter tabs present"

        self.case("TC08", "Filter Chips Row", chips)

        profile_words = ("Dual Messenger", "Island", "Work", "Secure Folder")

        def profile_chips() -> str:
            found = [n for n in state["nodes"] if any(w in n.text for w in profile_words)]
            assert found, "No profile chips detected"
            return "Profile chips: " + ", ".join(n.text for n in found)

        self.case("TC08B", "Target Profile Selector Chips", profile_chips)

        def switch_profile() -> str:
            found = [n for n in state["nodes"] if any(w in n.text for w in profile_words)]
            if len(found) < 2:
                raise Skip("Only one profile chip")
            first, second = found[0].text, found[1]
            self.tap(second, 1.2)
            back = next((n for n in self.nodes() if first in n.text), None)
            assert back is not None, f"Chip {first} not found after switching"
            self.tap(back, 1.0)
            return f"Switched to {second.text} and back to {first}"

        self.case("TC08C", "Target Profile Switching", switch_profile)

        def scrollbar() -> str:
            width, height = self.screen
            x = width - 30
            for y in (int(height * 0.28), int(height * 0.85), int(height * 0.28)):
                dev.sh(f"input tap {x} {y}")
                time.sleep(0.7)
            return "Tapped the rail top, bottom and top again"

        self.case("TC08D", "Alphabet Scrollbar Navigation", scrollbar)

        def secondary_packages() -> str:
            user = self.secondary_user()
            packages = self.package_list(user.user_id)
            assert packages, f"User {user.user_id} lists no packages"
            return f"User {user.user_id} ({user.name}) lists {len(packages)} packages"

        self.case("TC09", "Secondary Profile Package List", secondary_packages)

        def search() -> str:
            field = next((n for n in self.nodes() if "Search" in n.text or "Search" in n.desc), None)
            if field is None:
                raise Skip("Search field not found")
            self.tap(field, 0.5)
            dev.sh("input text samsung")
            time.sleep(1.0)
            matched = any("samsung" in n.text.lower() for n in self.nodes())
            assert matched, "No row matched the query"
            return "Query 'samsung' filtered the list"

        self.case("TC10", "Search Filtering", search)

        def clear_search() -> str:
            clear = next((n for n in self.nodes() if n.desc == "Clear"), None)
            if clear:
                self.tap(clear, 0.5)
                return "Cleared with the clear button"
            dev.back()
            return "Left search with the back key"

        self.case("TC11", "Clear Search", clear_search)

        sheet: dict[str, list[Node]] = {}

        def special_access_sheet() -> str:
            button = next((n for n in self.nodes() if n.desc == TEXT["special_access_desc"]), None)
            if button is None:
                raise Skip("No cloned app with a Special Access button on this tab")
            self.tap(button, 0.8)
            nodes = self.wait_nodes(lambda ns: any(TEXT["special_access_op"] in n.text for n in ns), timeout=8)
            sheet["nodes"] = nodes
            header = any(TEXT["special_access_header"] in n.text for n in nodes)
            ops = any(TEXT["special_access_op"] in n.text for n in nodes)
            assert header and ops, f"Sheet content missing (header={header}, ops={ops})"
            return "Sheet opened with header and operations list"

        self.case("TC11B", "AppOps Special Access Sheet", special_access_sheet)

        def special_access_badges() -> str:
            if "nodes" not in sheet:
                raise Skip("Sheet did not open")
            texts = [n.text for n in sheet["nodes"]]
            badge = any(b in texts for b in TEXT["special_access_badges"])
            storage = any("isolated profile sandbox" in t for t in texts)
            dev.back()
            time.sleep(0.8)
            assert badge, "No status badge in the sheet"
            return f"Status badge found, storage card={storage}"

        self.case("TC11C", "AppOps Status Badges", special_access_badges)

        def settings_navigation() -> str:
            self.go_home()
            button = next((n for n in self.nodes() if n.desc == TEXT["settings_desc"]), None)
            assert button is not None, "Settings icon not found"
            self.tap(button, 1.0)
            nodes = self.wait_nodes(lambda ns: any(n.text in TEXT["settings_titles"] for n in ns))
            state["settings"] = nodes
            assert any(n.text in TEXT["settings_titles"] for n in nodes), "Settings title not found"
            return "Settings screen open"

        self.case("TC12", "Settings Navigation", settings_navigation)

        def developer_unlock() -> str:
            if "settings" not in state:
                raise Skip("Settings did not open")
            row = next((n for n in state["settings"] if n.text.startswith(TEXT["version_row"])), None)
            if row is None:
                row = next((n for n in state["settings"] if re.search(r"\b\d+\.\d+\.\d+\b", n.text)), None)
            assert row is not None, "Version row not found"
            for _ in range(DEV_UNLOCK_TAPS):
                dev.sh(f"input tap {row.center[0]} {row.center[1]}")
                time.sleep(0.15)
            time.sleep(1.0)
            unlocked = any(TEXT["dev_screen"] in n.text for n in self.nodes())
            assert unlocked, f"Developer diagnostics not visible after {DEV_UNLOCK_TAPS} taps"
            return f"Unlocked after {DEV_UNLOCK_TAPS} taps"

        self.case("TC13", "Developer Diagnostics Unlock", developer_unlock)
        self.go_home()

        def pull_to_refresh() -> str:
            width, height = self.screen
            dev.sh(f"input swipe {width // 2} {int(height * 0.3)} {width // 2} {int(height * 0.6)} 300")
            time.sleep(1.5)
            assert self.package in dev.focus(), "App lost focus after the gesture"
            return "Gesture completed and the app kept focus"

        self.case("TC14", "Pull To Refresh", pull_to_refresh)

        def fatal_sweep() -> str:
            log = dev.sh("logcat -d -b crash", check=False)
            lines = [l for l in log.splitlines() if "FATAL EXCEPTION" in l or f"Process: {self.package}" in l]
            assert not lines, f"Crash in logcat: {lines[0][:120]}"
            return "No crash entries for the app"

        self.case("TC15", "No Fatal Exceptions", fatal_sweep)

        def screenshot() -> str:
            path = self.out_dir / "home_screen.png"
            path.write_bytes(dev.adb("exec-out", "screencap", "-p", binary=True))
            return f"Saved {path.name}"

        self.case("TC16", "Screenshot", screenshot)

        def single_launcher() -> str:
            out = dev.sh("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER", check=False)
            entries = [l for l in out.splitlines() if l.strip().startswith(f"{self.package}/")]
            assert len(entries) == 1, f"Expected one launcher activity, found {len(entries)}"
            assert entries[0].strip().endswith(ACTIVITY_CLASS) or ACTIVITY_CLASS in entries[0], f"Unexpected component {entries[0].strip()}"
            return f"Exactly one launcher activity: {entries[0].strip()}"

        self.case("TC17", "Single Launcher Activity", single_launcher)

        def ghost_probe() -> str:
            if not self.probe_package:
                raise Skip("No --probe-package given")
            user = self.secondary_user()
            pkg_ = self.probe_package
            assert pkg_ in self.package_list(0), f"{pkg_} is not installed for User 0"
            if pkg_ in self.package_list(user.user_id):
                raise Skip(f"{pkg_} is already installed in User {user.user_id}, refusing to remove a pre-existing clone")
            if user.paused:
                raise Skip(f"User {user.user_id} is paused, resume it first")
            out = dev.sh(f"pm install-existing --user {user.user_id} {pkg_}", check=False)
            time.sleep(2)
            cloned = probe_snapshot(dev.serial, user.user_id, pkg_, "after-clone")
            assert cloned["listed"], f"Clone not visible after install-existing: {out.strip()}"
            dev.sh(f"pm uninstall --user {user.user_id} {pkg_}", check=False)
            time.sleep(3)
            removed = probe_snapshot(dev.serial, user.user_id, pkg_, "after-remove")
            assert not removed["listed"], "Package manager still lists the clone after uninstall"
            assert not removed["launcher_entry"], "Launcher activity still resolvable after uninstall"
            return f"Clone and remove verified in User {user.user_id}. Check the launcher visually for a stale icon"

        self.case("TC18", "Ghost Probe Cycle", ghost_probe)

        def paused_profile() -> str:
            paused = [u for u in self.list_users() if u.paused]
            if not paused:
                raise Skip("No paused managed profile on this device")
            self.go_home()
            nodes = self.wait_nodes(lambda ns: any(TEXT["paused_chip"] in n.text for n in ns))
            assert any(TEXT["paused_chip"] in n.text for n in nodes), f"No Paused indicator for User {paused[0].user_id}"
            return f"Paused indicator shown (User {paused[0].user_id})"

        self.case("TC19", "Paused Profile Indicator", paused_profile)

        def self_exclusion() -> str:
            users = [u for u in self.list_users() if u.user_id != 0 and u.user_id != KNOX_USER and (u.user_id == 95 or u.managed)]
            if not users:
                raise Skip("No secondary profile")
            own = {DEBUG_PACKAGE, RELEASE_PACKAGE, self.package}
            for user in users:
                listed = self.package_list(user.user_id) & own
                assert not listed, f"DualClone is installed in User {user.user_id}: {sorted(listed)}"
            return f"DualClone is absent from {len(users)} secondary profile(s)"

        self.case("TC20", "Self Exclusion", self_exclusion)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"), required=not os.environ.get("ANDROID_SERIAL"), help="ADB serial (or set ANDROID_SERIAL)")
    parser.add_argument("--package", default=DEBUG_PACKAGE, help="Application id under test (debug or release)")
    parser.add_argument("--apk", type=Path, help="APK to install. Defaults to the Gradle output for the package")
    parser.add_argument("--skip-install", action="store_true", help="Test the build that is already installed")
    parser.add_argument("--probe-package", help="Throwaway package for the clone and ghost probe phase")
    parser.add_argument("--user", type=int, help="Secondary profile to use. Defaults to the first Dual Messenger or managed profile")
    parser.add_argument("--min-idle", type=int, default=0, help="Minimum idle seconds before the run")
    parser.add_argument("--out-dir", type=Path, help="Report folder. Defaults to .e2e/<timestamp>")
    args = parser.parse_args()

    out_dir = args.out_dir or ROOT / ".e2e" / datetime.now().strftime("%Y%m%d-%H%M%S")
    out_dir.mkdir(parents=True, exist_ok=True)
    dev = Device(args.serial)
    model = dev.sh("getprop ro.product.model", check=False).strip() or "unknown device"
    report = Report(out_dir, model, args.package)
    print(f"=== DualClone E2E on {model} ({args.package}) ===", flush=True)

    suite = Suite(dev, args.package, out_dir, report, args.probe_package, args.user)
    suite.run(args.skip_install, args.apk, args.min_idle)
    path = report.write("DualClone E2E Report")
    print(f"\nReport written to: {path}", flush=True)
    return 1 if report.failed else 0


if __name__ == "__main__":
    sys.exit(main())
