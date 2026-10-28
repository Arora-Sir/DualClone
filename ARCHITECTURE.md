# DualClone Architecture

This document describes the layers of DualClone, how a clone or remove travels from a tap to the package manager, and the decisions behind the design. DualClone is a single Gradle module with strict package layering.

## Layers

```text
ui              Compose screens, sheets, dialogs, navigation, view models
core            Shared UI text type and the design system (tokens, typography, components)
domain          Pure Kotlin models, repository and gateway interfaces, parsers, policies, backup codec
data            Room database, repository implementations, preferences
shizuku         ShizukuGateway implementation, process runner, binder lifecycle
update          The only package that uses the network: update check, APK download, SHA-256 check, install hand-off
work, receiver  Background reconcile worker and the runtime package event monitor
di              Hilt modules
```

Dependencies point inward. `ui` depends on `domain` and `core`. `data` and `shizuku` implement `domain` interfaces. `domain` imports nothing from Android UI or from `data` and `shizuku`. `ArchitectureTest` fails the build when `ui` imports the Shizuku transport or the Shizuku API, when the pure `domain` packages import Android types, when `domain` imports `data`, `ui` or `shizuku`, or when anything outside `update` touches the network.

### Package map

| Package | Contents |
| :--- | :--- |
| `core/common` | `UiText`, a string resource or plain text value that view models emit so they never hold a `Context` |
| `core/designsystem` | Spacing, shapes, typography and motion tokens, contrast-safe status colors, buttons, cards, sheets, dialogs, palette grid |
| `domain/model` | `Profile`, `ProfileKind`, `CloneState`, `CloneEntry`, `OpResult`, `ListResult`, `PmErrorCode` |
| `domain/repo` | `ShizukuGateway`, `ProfileRepository`, `CloneRepository`, `AppOpsRepository` and related ports |
| `domain/pm` | `PmOutputParser`, which turns `pm list users`, `pm list packages`, `install-existing` and `uninstall` text into typed results |
| `domain/policy` | `ProfilePolicy`, the single place that decides which profiles are valid clone targets |
| `domain/backup` | Backup JSON codec and the restore planner |
| `data/db` | Room entities and DAOs for the catalog, clone state, AppOps presets and the command log |
| `data/repository` | Implementations of the domain repositories |
| `shizuku` | `ShizukuManager` for binder state, the command runner and gateway implementations |
| `work` | Periodic reconcile worker |
| `update` | Update check, APK download, SHA-256 verification and install hand-off. The only package that uses the network |
| `ui/screen`, `ui/sheet`, `ui/components` | Home, Settings, diagnostics, bottom sheets and cards |
| `ui/viewmodel` | State holders that call repositories |

## Multi-user model

DualClone reads the profile list with `pm list users` and maps each entry to a `Profile`.

| Profile | How it is recognized | Clone target |
| :--- | :--- | :--- |
| Primary (User 0) | User id 0 | Never. It is the source of packages |
| Samsung Dual Messenger (User 95) | User id 95 | Yes |
| Work or other managed profile | `FLAG_MANAGED_PROFILE` (0x20) | Yes |
| Samsung Secure Folder (User 150) | Knox user id 150 | No. Guidance card only, no shell command is sent |
| Other full users | Neither of the above | Hidden unless the advanced switch is on |

A profile with `FLAG_QUIET_MODE` (0x80) is paused. DualClone shows a Paused chip and warns before operating on it.

Cloning runs `pm install-existing --user <id> <package>`. Removing runs `pm uninstall --user <id> <package>`. The APK is shared with User 0, so there is no version drift between a clone and its host.

## Command path

1. A view model calls a use case or repository, for example `CloneRepository.clone`.
2. The repository validates the package name with `PackageValidator` and checks `ProfilePolicy`. DualClone's own package is always refused.
3. The repository calls `ShizukuGateway.run`. The Shizuku implementation starts the process through Shizuku, drains stdout and stderr concurrently so a large output cannot block on a full pipe, and destroys the process at the deadline.
4. `PmOutputParser` turns the raw output into a `PmErrorCode` or success.
5. After a mutation the repository verifies it with `pm path --user <id> <package>`. Only a verified result flips the state to cloned or removed. `OpResult.Success` carries `verified`.
6. The result, user id and exit code are appended to the command log, which is capped at 500 rows.

`ShizukuGateway` is an interface. Production binds the Shizuku implementation. Unit tests use a fake. The emulator test suite binds a gateway built on `UiAutomation.executeShellCommand`, so CI needs no Shizuku.

## Clone state

Clone state is written by one repository with a per-profile mutex. A failed `pm list packages` never overwrites known state with an empty set. It marks the state as stale. Each package in a profile is classified as installed, hidden, suspended or not installed, because a frozen app is missing from the default package list but still exists.

State is refreshed on resume, after every mutation, from dynamic package change broadcasts while the process is alive, and by the reconcile worker. Android 8 and newer do not deliver implicit package broadcasts to manifest receivers, so the process-bound registration is the live source and the periodic worker is the fallback.

## Persistence

Room stores the app catalog, clone state, AppOps presets and the command log. The schema is exported to `app/schemas/` and committed. Version 1 is the baseline, so there is no migration test yet. Every schema change must add one. Search is a plain `LIKE` query over the catalog. The catalog is a few hundred to a few thousand rows, so no full-text index is used. Theme settings live in preferences.

## Background work

`ReconcileWorker` runs on a battery-not-low periodic schedule, on package events and on demand. It refreshes the catalog, reconciles clone state with the operating system, prunes orphan rows and re-applies saved AppOps presets. It reports through a status line in the app and posts no notification.

## Knox Secure Folder

Knox blocks shell access to User 150. DualClone never sends a command to it. The profile appears as a chip with a lock badge, and tapping it shows a guidance card with a shortcut that opens Secure Folder.

## Launcher ghosts

After a verified removal in a managed or Dual Messenger profile, Samsung One UI Home can keep a stale icon. The package manager is already clean at that point, so DualClone cannot detect it. Settings offers Refresh launcher, which runs `am force-stop com.sec.android.app.launcher` through Shizuku on devices where that package exists. `scripts/device/ghost_probe.py` reproduces and diagnoses the case.

## Design decisions

* Single module. A multi-module split adds build time and Hilt wiring that a codebase this size does not need. Package layering and an architecture test hold the boundaries.
* `pm` text parsing behind `ShizukuGateway`. A typed binder call to the package manager is possible later and would only change the gateway.
* Backups hold the clone list and AppOps presets. App data is not backed up because shell access cannot read another profile's data directory without root.
* Network contained in one package. `update/` owns every network call and the install hand-off. Nothing else may import `java.net`, `javax.net`, OkHttp or Retrofit. An architecture test and `scripts/release/check_network_imports.py` enforce it. See `docs/UPDATES.md`.
* One launcher entry. LeakCanary was removed because it added a second launcher icon.
* Adaptive icon content sits inside a 52 percent safe zone so Samsung squircle and circle masks never clip it.
* App cards use two lines so the app name gets the full width. The profile badge sits on the second line.
* System apps are filtered on every tab, using both `FLAG_SYSTEM` and `FLAG_UPDATED_SYSTEM_APP`.
* External links resolve the default browser first, so document apps that register for http links do not intercept them.

## Testing

| Suite | Where | What it covers |
| :--- | :--- | :--- |
| Unit tests | `app/src/test` | Parser fixtures, validators, policy, clone state machine, backup codec, migrations |
| Instrumented e2e | `app/src/androidTest/.../e2e` | Clone, verified remove, profile listing and manifest invariants on an emulator with a managed profile, run by `.github/workflows/e2e.yml` |
| Device suite | `scripts/device/e2e_test.py` | UI flow on a real device, including Samsung Dual Messenger, Knox and launcher ghosts |
| Ghost probe | `scripts/device/ghost_probe.py` | Package manager and launcher state around a clone and remove cycle |
| Release checks | `scripts/release/verify_release.py` | Signature, exact permission allow-list, one launcher activity |

See [SETUP.md](SETUP.md) for commands.

## Background and goals

This section explains the platform constraints and design reasoning behind the layers above.

### Background

Android has supported multiple users since 5.0. Samsung One UI uses a lightweight secondary user for Dual Messenger. It is user id 95 on the devices this project was developed on, and it is created the first time you enable Dual Messenger for an app. Cloned apps run in that user with their own data, and One UI marks their launcher icons with a badge.

One UI Settings only offers Dual Messenger for a short list of apps, mostly messengers. The restriction is in the Settings UI. At the package manager level, any app that is installed for the primary user can be installed for the secondary user with `pm install-existing`, which a privileged shell can run.

### Alternatives

* Work profile managers such as Island and Shelter create a managed profile. That works for any app, but it adds work profile UI such as the Work tab, a briefcase badge and a paused state, and it isolates more than some people want.
* Other Shizuku based clone tools exist. DualClone does not benchmark against them or make claims about their behavior.
* Samsung Secure Folder is a separate Knox container and cannot be driven from a shell.

### Design goals

#### Fast, local catalog

The installed app list is cached in a local Room database, so the Home screen has data to show while the package manager is queried. Search runs over that table. No startup time or search latency figure is published because none has been measured for a release build.

#### Local by default, network for updates only

Everything DualClone does with your apps stays on the device. The network is used for one thing, checking GitHub Releases for a newer version and downloading it when you ask. That code sits in one package, and the build fails if network code appears anywhere else. See [docs/UPDATES.md](docs/UPDATES.md).

#### Honest about platform limits

* Knox Secure Folder blocks shell access. DualClone explains that and opens Secure Folder instead of failing with an error.
* A paused work profile cannot receive package changes reliably. DualClone shows the state and warns first.
* Samsung One UI Home can keep a stale icon after a clone is removed. DualClone offers a manual launcher refresh.
* Clones share the APK with the primary user, so there is nothing to update separately. Updating the host app updates every clone.

#### Verified changes

A command that exits with success is not treated as proof. DualClone checks the package manager again after every clone and removal and records the result in a local command log.

#### One launcher entry

Debug tooling must not add icons to the app drawer. The app registers a single launcher activity, and the release check fails if there are more.
