# Changelog

All notable changes to DualClone are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.7] - 2026-10-30

### Changed
* Upgraded the alphabet scrollbar to an expandable fast-scroll rail. A sleek 4dp thumb rests quietly on the edge when idle and expands smoothly into the full alphabet rail with haptic feedback and a floating letter bubble upon touch and drag.
* Removed the permanent 28dp list rail padding. App cards now expand to full screen width with symmetrical margins, eliminating app title and package name truncation.
* Removed the diagnostic status line from the home screen header. Moved the quiet reconcile timestamp to the list footer at the very end of the app list.
* Removed the persistent sticky section letter header from the list. App cards now start immediately beneath the segmented filter tabs, freeing vertical screen space.
* Updated fast-scroll letter index mapping to accurately jump to the first app of each letter without header offset.

---

## [1.0.6] - 2026-10-29

### Changed
* Refresh Home Launcher after Clone removal is now available in Settings (off by default). When enabled, removing a clone from a work profile automatically restarts the home screen once, debounced after the last removal in a batch, clearing ghost icons left by Samsung One UI Home.
* Removed the tall launcher refresh banner from the home screen. When automatic refresh is turned off, a bottom snackbar message offers a Refresh launcher action after work profile removals.
* Launcher refresh hints and automatic restarts are now scoped strictly to work profiles. Dual Messenger and primary user removals never trigger launcher restarts.
* Filter tabs (All, Uncloned, Cloned) now preserve their scroll positions independently, preventing tab switches from resetting or clamping scroll offsets.
* Removed the redundant Clone into heading above profile chips for a cleaner home screen layout.
* Added a centered Knox guidance dialog with inline actions when tapping the Secure Folder chip, explaining system restrictions without shifting the home screen layout.

### Fixed
* Fixed a one-second banner flash that occurred when removing an app with automatic launcher refresh enabled.
* Fixed scroll position loss when switching between filter tabs on the home screen.
* Fixed a brief guidance card flash during cold start by applying a grace period before displaying the empty profiles state.

---

## [1.0.5] - 2026-10-29

### Added
* A Follow releases with Obtainium row under Updates and about. One tap adds DualClone to Obtainium or ObtainX, which then tell you about new releases.

### Changed
* The Hilt navigation helper moved to its new package, which removes the deprecation warnings.

---

## [1.0.4] - 2026-10-29

### Changed
* The Pure AMOLED black row now has a divider when it is on. Tap the left part to open Surface lift and use the switch on the right to turn it off. With Pure black off the whole row toggles as before.

---

## [1.0.3] - 2026-10-29

### Added
* Surface lift under Pure AMOLED black. Tap the row to open a ten step pill that moves cards and raised surfaces from flat black to clearly lifted, with Recommended at step 5 and a reset action. It is included in backups.

### Fixed
* The update sheet showed release notes as raw Markdown (hash signs and backticks). It now shows plain text with bullets.

---

## [1.0.2] - 2026-10-29

### Fixed
* Apps installed or removed while DualClone was closed never showed up or disappeared in the list. The app list now refreshes on every start and with the manual refresh.
* Messages such as Cloned to Dual Messenger drew as a light bar in dark mode. Snackbars now follow the theme in light, dark and Pure black.
* The filter tabs flashed zero counts for one frame at startup. The counts now fade in once the list has loaded.

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
