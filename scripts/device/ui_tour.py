"""DualClone UI Screen Audit and Tour Script.

Drives Samsung Galaxy S24 Ultra over ADB to capture high resolution screenshots of
all screens and modal states for design verification.
"""

from __future__ import annotations

import argparse
import os
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from adbx import Device

PKG = "com.mohitarora.dualclone.debug"
ACTIVITY = f"{PKG}/com.mohitarora.dualclone.ui.MainActivity"


def capture_all_screens(serial: str, out_dir: Path) -> None:
    dev = Device(serial)
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f"Starting UI Screen Tour on {serial}...", flush=True)

    def snap(name: str):
        path = out_dir / name
        raw = dev.adb("exec-out", "screencap", "-p", binary=True)
        path.write_bytes(raw)
        print(f"Captured {name} ({len(raw)} bytes)", flush=True)

    # 1. Reset and Launch Home Screen
    dev.sh(f"am force-stop {PKG}")
    dev.sh(f"am start -n {ACTIVITY}")
    time.sleep(2.5)

    # Dismiss any Shizuku permission prompt if visible
    nodes = dev.dump_ui()
    allow_node = next((n for n in nodes if "Allow all the time" in n.text or n.text == "Allow"), None)
    if allow_node:
        dev.sh(f"input tap {allow_node.center[0]} {allow_node.center[1]}")
        time.sleep(1.5)

    snap("screen_01_home.png")

    # 2. Search Screen
    nodes = dev.dump_ui()
    search_node = next((n for n in nodes if "Search" in (n.text or "") or "Search" in (n.desc or "")), None)
    if search_node:
        dev.sh(f"input tap {search_node.center[0]} {search_node.center[1]}")
        time.sleep(0.5)
        dev.sh("input text 'samsung'")
        time.sleep(1.0)
        snap("screen_02_search.png")
        # Clear search
        clear_node = next((n for n in dev.dump_ui() if n.desc == "Clear"), None)
        if clear_node:
            dev.sh(f"input tap {clear_node.center[0]} {clear_node.center[1]}")
        else:
            dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.8)

    # 3. Sort & Filter Bottom Sheet
    nodes = dev.dump_ui()
    sort_btn = next((n for n in nodes if n.desc == "Sort"), None)
    if sort_btn:
        dev.sh(f"input tap {sort_btn.center[0]} {sort_btn.center[1]}")
        time.sleep(1.2)
        snap("screen_03_sort_sheet.png")
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.8)

    # 4. Shizuku Setup Bottom Sheet
    nodes = dev.dump_ui()
    badge_node = next((n for n in nodes if n.text in ["Ready", "Grant", "Setup", "Stopped"]), None)
    if badge_node:
        dev.sh(f"input tap {badge_node.center[0]} {badge_node.center[1]}")
        time.sleep(1.2)
        snap("screen_04_shizuku_sheet.png")
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.8)

    # 5. AppOps Special Access Bottom Sheet
    nodes = dev.dump_ui()
    more_btn = next((n for n in nodes if n.desc == "Special Access"), None)
    if more_btn:
        dev.sh(f"input tap {more_btn.center[0]} {more_btn.center[1]}")
        time.sleep(1.5)
        snap("screen_05_appops_sheet.png")
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.8)

    # 6. Bulk Selection Mode Bar
    # Long-press on first app card to trigger bulk selection mode
    dev.sh("input swipe 400 950 400 950 800")
    time.sleep(1.2)
    snap("screen_06_bulk_mode.png")
    # Tap Cancel or back to exit bulk mode
    cancel_btn = next((n for n in dev.dump_ui() if n.text == "Cancel"), None)
    if cancel_btn:
        dev.sh(f"input tap {cancel_btn.center[0]} {cancel_btn.center[1]}")
    else:
        dev.sh("input keyevent KEYCODE_BACK")
    time.sleep(0.8)

    # 7. Settings Screen
    nodes = dev.dump_ui()
    settings_btn = next((n for n in nodes if n.desc == "Settings"), None)
    if settings_btn:
        dev.sh(f"input tap {settings_btn.center[0]} {settings_btn.center[1]}")
        time.sleep(1.2)
        snap("screen_07_settings.png")

        # 8. Developer Diagnostics Unlock (5 taps on Version row)
        settings_nodes = dev.dump_ui()
        ver_node = next((n for n in settings_nodes if "Version" in (n.text or "")), None)
        if ver_node:
            for _ in range(6):
                dev.sh(f"input tap {ver_node.center[0]} {ver_node.center[1]}")
                time.sleep(0.15)
            time.sleep(1.2)
            snap("screen_08_dev_diagnostics.png")

            # Return from dev diagnostics to settings
            dev.sh("input keyevent KEYCODE_BACK")
            time.sleep(0.5)

        # Return from settings to home
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.5)

    print("UI Screen Tour completed successfully.", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="DualClone UI Screen Tour")
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"), required=not os.environ.get("ANDROID_SERIAL"), help="ADB serial (or set ANDROID_SERIAL)")
    parser.add_argument("--out-dir", default=str(Path(__file__).resolve().parents[2] / ".e2e" / "ui_tour"), help="Output directory")
    args = parser.parse_args()

    capture_all_screens(args.serial, Path(args.out_dir))
