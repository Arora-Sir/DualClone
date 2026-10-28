# Changelog

All notable changes to DualClone are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.1] - 2026-10-28

### Changed
* New installs start with the Blue accent instead of the wallpaper color. Existing choices are kept.
* The Settings title shows the selected color, for example Color palette: Blue. The preset formerly called One UI is now called Blue.
* Build tooling updated (KSP 2.3.12). Dependency update pull requests are now grouped monthly and skip major versions.

---

## [1.0.0] - 2026-10-28

### Added
* One-tap cloning and removal of any installed app into Samsung Dual Messenger (User 95) and Work Profiles (for example Island), driven by Shizuku with no root.
* Profile selector, segmented All, Uncloned and Cloned filter with live count badges, search, sort and bulk clone or remove.
* Alphabet fast-scroller with finger tracking, spring physics bubble and reverse-sort support.
* AppOps special access management for cloned apps.
* Knox Secure Folder (User 150) guidance. DualClone never sends shell commands to that container.
* Theme engine built on Material tonal palettes, with Material You wallpaper colors, ten preset accents and Pure AMOLED Black. The Settings row shows five quick colors and a More colors sheet holds the rest.
* Custom color picker with a draggable saturation and brightness square, a hue strip, hex entry with Reset, and a live preview of the palette it will generate. Screen readers get adjustable controls and actions.
* Local command audit log (capped at 500 entries) and developer diagnostics screen.
* Verified clone and removal. The package manager is checked again before success is reported, removal asks for confirmation, and paused work profiles are flagged first.
* Clones without a host app (orphans) can be listed and removed. System apps and profile owners are never listed or removed.
* Backup and restore to a file with an optional password. Export everything, only the clone list and presets, or only the settings. A restore previews every item first and ends on the same list with the result for each one.
* Reconcile worker that refreshes clone state every 12 hours on battery-friendly conditions, plus runtime package event monitoring.
* Refresh launcher action and banner for stale Samsung One UI home screen icons.
* The Shizuku guidance card appears only after a problem state has held for a moment, so nothing flashes during startup.
* DualClone never clones itself.
* Developer profile, support sheet and open-source attribution sheet.
* In-app updater. It checks the GitHub Releases API when you tap Check for updates, or daily if you switch that on (off by default), downloads the signed APK, verifies its SHA-256 and opens the system installer. This is the only network use.
* Release pipeline with signed APKs, SHA256SUMS.txt, signature and permission allow-list verification, a network containment check, and an emulator end-to-end suite in CI.

### Changed
* Release builds are signed with a dedicated release key.
* Project is licensed under GPL-3.0.

---
