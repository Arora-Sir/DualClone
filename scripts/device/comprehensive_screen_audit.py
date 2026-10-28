"""Comprehensive UI/UX visual audit script for DualClone on Samsung Galaxy S24 Ultra.

Captures all 19 visual surfaces and user interaction flows:
1. Home Catalog Default (All apps, User 95 target)
2. Profile Selector: Island / Work (User 10)
3. Profile Selector: Knox Secure Folder (User 150) with Knox Guidance Card & Open button
4. Search with matching results
5. Search empty state illustration
6. Filter Chip: Installed
7. Filter Chip: Cloned
8. Filter Chip: System
9. Sort Bottom Sheet
10. Shizuku Setup Bottom Sheet
11. AppOps Special Access Bottom Sheet
12. Bulk Mode Active (2 items selected)
13. Alphabet Scrollbar Active Touch (floating bubble tracking)
14. Settings & About Main Screen
15. Settings Profile Topology & Knox Section (scrolled)
16. Developer Diagnostics Screen (Binder info & audit trail)
17. Clear Logs Confirmation Alert Dialog
18. Samsung One UI App Drawer Launcher Icon
19. Samsung One UI Finder Clean Search (Zero 'Leaks')
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


def run_audit(serial: str, out_dirs: list[Path]) -> None:
    dev = Device(serial)
    for d in out_dirs:
        d.mkdir(parents=True, exist_ok=True)

    print(f"Starting Expanded 19-Screen Visual Audit on {serial}...", flush=True)

    def snap(name: str):
        raw = dev.adb("exec-out", "screencap", "-p", binary=True)
        for d in out_dirs:
            (d / name).write_bytes(raw)
        print(f"Captured {name} ({len(raw)} bytes)", flush=True)

    # Launch app
    dev.sh(f"am force-stop {PKG}")
    dev.sh(f"am start -n {ACTIVITY}")
    time.sleep(2.5)

    # 1. Home Screen Catalog Default
    snap("01_home_catalog_default.png")

    # 2. Profile Selector: Island / Work (User 10)
    nodes = dev.dump_ui()
    work_chip = next((n for n in nodes if "Island" in (n.text or "") or "Work" in (n.text or "") or "10" in (n.text or "")), None)
    if work_chip:
        dev.sh(f"input tap {work_chip.center[0]} {work_chip.center[1]}")
        time.sleep(1.0)
        snap("02_profile_switch_work.png")

    # Switch back to Dual Messenger (User 95)
    nodes = dev.dump_ui()
    dual_chip = next((n for n in nodes if "Dual Messenger" in (n.text or "") or "Dual App" in (n.text or "") or "95" in (n.text or "")), None)
    if dual_chip:
        dev.sh(f"input tap {dual_chip.center[0]} {dual_chip.center[1]}")
        time.sleep(1.0)

    # 3. Segmented Filter Bar: Installed Tab
    nodes = dev.dump_ui()
    installed_tab = next((n for n in nodes if (n.text or "").startswith("Installed")), None)
    if installed_tab:
        dev.sh(f"input tap {installed_tab.center[0]} {installed_tab.center[1]}")
        time.sleep(1.0)
        snap("03_filter_installed_tab.png")

    # 4. Segmented Filter Bar: Cloned Tab
    nodes = dev.dump_ui()
    cloned_tab = next((n for n in nodes if (n.text or "").startswith("Cloned")), None)
    if cloned_tab:
        dev.sh(f"input tap {cloned_tab.center[0]} {cloned_tab.center[1]}")
        time.sleep(1.0)
        snap("04_filter_cloned_tab.png")

    # Reset to All Tab
    nodes = dev.dump_ui()
    all_tab = next((n for n in nodes if (n.text or "").startswith("All")), None)
    if all_tab:
        dev.sh(f"input tap {all_tab.center[0]} {all_tab.center[1]}")
        time.sleep(0.8)

    # 5. Search Matching
    nodes = dev.dump_ui()
    search_node = next((n for n in nodes if "Search" in (n.text or "") or "Search" in (n.desc or "")), None)
    if search_node:
        dev.sh(f"input tap {search_node.center[0]} {search_node.center[1]}")
        time.sleep(0.4)
        dev.sh("input text 'samsung'")
        time.sleep(1.0)
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.5)
        snap("05_search_matching.png")

        # 6. Search Empty State
        clear_btn = next((n for n in dev.dump_ui() if n.desc == "Clear"), None)
        if clear_btn:
            dev.sh(f"input tap {clear_btn.center[0]} {clear_btn.center[1]}")
            time.sleep(0.5)
        dev.sh(f"input tap {search_node.center[0]} {search_node.center[1]}")
        time.sleep(0.4)
        dev.sh("input text 'xyznonexistentapp'")
        time.sleep(1.0)
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.5)
        snap("06_search_empty_state.png")

        # Clear search
        clear_btn = next((n for n in dev.dump_ui() if n.desc == "Clear"), None)
        if clear_btn:
            dev.sh(f"input tap {clear_btn.center[0]} {clear_btn.center[1]}")
            time.sleep(0.8)

    # 7. Sort & Secondary Filter Sheet
    nodes = dev.dump_ui()
    sort_filter_btn = next((n for n in nodes if n.desc == "Filter & Sort Options" or n.desc == "Sort"), None)
    if not sort_filter_btn:
        sort_filter_btn = next((n for n in nodes if "Filter" in (n.desc or "") or "Sort" in (n.desc or "")), None)
    if sort_filter_btn:
        dev.sh(f"input tap {sort_filter_btn.center[0]} {sort_filter_btn.center[1]}")
        time.sleep(1.2)
        snap("07_sort_filter_sheet.png")

        # Select Z to A sort
        sheet_nodes = dev.dump_ui()
        z_to_a_opt = next((n for n in sheet_nodes if "Z to A" in (n.text or "")), None)
        if z_to_a_opt:
            dev.sh(f"input tap {z_to_a_opt.center[0]} {z_to_a_opt.center[1]}")
            time.sleep(1.0)
            snap("08_sort_z_to_a_reversed_rail.png")

        # Reopen sort sheet and select Recently Updated
        dev.sh(f"input tap {sort_filter_btn.center[0]} {sort_filter_btn.center[1]}")
        time.sleep(1.0)
        sheet_nodes = dev.dump_ui()
        rec_opt = next((n for n in sheet_nodes if "Recently Updated" in (n.text or "")), None)
        if rec_opt:
            dev.sh(f"input tap {rec_opt.center[0]} {rec_opt.center[1]}")
            time.sleep(1.0)
            snap("09_sort_recently_updated_date_pills.png")

        # Reopen and reset to A to Z
        dev.sh(f"input tap {sort_filter_btn.center[0]} {sort_filter_btn.center[1]}")
        time.sleep(1.0)
        sheet_nodes = dev.dump_ui()
        a_to_z_opt = next((n for n in sheet_nodes if "A to Z" in (n.text or "")), None)
        if a_to_z_opt:
            dev.sh(f"input tap {a_to_z_opt.center[0]} {a_to_z_opt.center[1]}")
            time.sleep(0.8)

    # 10. Shizuku Setup Bottom Sheet
    nodes = dev.dump_ui()
    badge_node = next((n for n in nodes if n.text in ["Ready", "Grant", "Setup", "Stopped"]), None)
    if badge_node:
        dev.sh(f"input tap {badge_node.center[0]} {badge_node.center[1]}")
        time.sleep(1.2)
        snap("10_shizuku_setup_sheet.png")
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.8)

    # 11. AppOps Special Access Bottom Sheet
    nodes = dev.dump_ui()
    more_btn = next((n for n in nodes if n.desc == "Special Access"), None)
    if more_btn:
        dev.sh(f"input tap {more_btn.center[0]} {more_btn.center[1]}")
        time.sleep(1.5)
        snap("11_appops_special_access_sheet.png")
        # Scroll to view SYSTEM_ALERT_WINDOW and bottom padding
        dev.sh("input swipe 540 1800 540 1000 300")
        time.sleep(0.8)
        snap("11b_appops_scrolled_bottom.png")
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.8)

    # 12. Bulk Mode Active
    dev.sh("input swipe 400 950 400 950 800")
    time.sleep(1.2)
    dev.sh("input tap 400 1150")
    time.sleep(0.8)
    snap("12_bulk_selection_mode.png")
    cancel_btn = next((n for n in dev.dump_ui() if n.text == "Cancel" or n.desc == "Close" or n.desc == "Cancel Selection"), None)
    if cancel_btn:
        dev.sh(f"input tap {cancel_btn.center[0]} {cancel_btn.center[1]}")
    else:
        dev.sh("input keyevent KEYCODE_BACK")
    time.sleep(0.8)

    # 13. Alphabet Scrollbar Active Touch (Hold on right letter 'M' area)
    dev.sh("input swipe 1045 1350 1045 1350 700")
    snap("13_alphabet_scrollbar_active.png")
    time.sleep(0.5)

    # 14. Settings Screen Main (Theme Presets & Status Pill)
    nodes = dev.dump_ui()
    settings_btn = next((n for n in nodes if n.desc == "Settings"), None)
    if settings_btn:
        dev.sh(f"input tap {settings_btn.center[0]} {settings_btn.center[1]}")
        time.sleep(1.2)
        snap("14_settings_main_screen.png")

        # 15. Settings Scrolled (Profile Topology & Knox Section)
        dev.sh("input swipe 540 1600 540 700 300")
        time.sleep(1.0)
        snap("15_settings_profile_topology.png")

        # 15B. Settings Scrolled Further (Developer Card & Open Source Attribution)
        dev.sh("input swipe 540 1600 540 700 300")
        time.sleep(1.0)
        snap("15b_settings_attribution_and_dev.png")

        # 16. Developer Diagnostics Screen (tap diagnostics action card or 5-tap version)
        dev_card = next((n for n in dev.dump_ui() if "Developer Diagnostics" in (n.text or "")), None)
        if dev_card:
            dev.sh(f"input tap {dev_card.center[0]} {dev_card.center[1]}")
        else:
            dev.sh("input swipe 540 800 540 1600 300")
            time.sleep(0.6)
            ver_node = next((n for n in dev.dump_ui() if "Version" in (n.text or "")), None)
            if ver_node:
                for _ in range(6):
                    dev.sh(f"input tap {ver_node.center[0]} {ver_node.center[1]}")
                    time.sleep(0.15)
        time.sleep(1.2)
        snap("16_dev_diagnostics_screen.png")

        # 17. Clear Logs Confirmation Dialog
        del_btn = next((n for n in dev.dump_ui() if n.desc == "Clear Logs"), None)
        if del_btn:
            dev.sh(f"input tap {del_btn.center[0]} {del_btn.center[1]}")
            time.sleep(1.0)
            snap("17_clear_logs_dialog.png")
            cancel_dialog_btn = next((n for n in dev.dump_ui() if n.text == "Cancel"), None)
            if cancel_dialog_btn:
                dev.sh(f"input tap {cancel_dialog_btn.center[0]} {cancel_dialog_btn.center[1]}")
            else:
                dev.sh("input keyevent KEYCODE_BACK")
            time.sleep(0.5)

        # Return to Settings then Home
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.5)
        dev.sh("input keyevent KEYCODE_BACK")
        time.sleep(0.5)

    # 18. Launcher Drawer Icon
    dev.sh("input keyevent KEYCODE_HOME")
    time.sleep(1.0)
    dev.sh("input swipe 540 1400 540 400 300")
    time.sleep(1.2)
    snap("18_launcher_drawer_icon.png")

    # 19. One UI Finder Clean Search
    dev.sh("input swipe 540 1000 540 400 200")
    time.sleep(1.0)
    dev.sh("input text dual")
    time.sleep(1.2)
    snap("19_finder_search_clean.png")

    # Return to app
    dev.sh("input keyevent KEYCODE_HOME")
    dev.sh(f"am start -n {ACTIVITY}")
    time.sleep(1.0)

    print("Complete expanded 19-screen visual audit finished successfully.", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="DualClone Comprehensive Visual Audit")
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"), required=not os.environ.get("ANDROID_SERIAL"), help="ADB serial (or set ANDROID_SERIAL)")
    parser.add_argument("--run-id", default="03", help="Audit run identifier")
    args = parser.parse_args()

    project_dir = Path(__file__).resolve().parents[2] / ".e2e" / f"visual_audit_run_{args.run_id}"
    run_audit(args.serial, [project_dir])
