# Developer Setup

How to build DualClone, run the tests and use the device scripts.

## Prerequisites

* JDK 17 or newer (CI uses 21). Point `JAVA_HOME` at it.
* Android SDK with platform 36 and a recent build-tools package. Set `ANDROID_HOME` or create `local.properties` with `sdk.dir`.
* Gradle is provided by the wrapper (8.13). Do not install it separately.
* Python 3.10 or newer with Pillow for the device scripts (`pip install pillow`).
* `adb` from Android platform-tools on `PATH`.

Do not put machine specific paths such as `org.gradle.java.home` in the repository's `gradle.properties`. Put them in `~/.gradle/gradle.properties`.

## Build

```bash
./gradlew assembleDebug
```

On Windows use `.\gradlew.bat`. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Its application id is `com.mohitarora.dualclone.debug`, so it installs next to a release build.

The full local gate is the same as CI:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
python scripts/release/scan_repo.py
python scripts/release/check_version.py
python scripts/release/check_network_imports.py
```

Enable the pre-commit hook once per clone. It runs the secret guard, the scan, the version check and the network import check.

```bash
git config core.hooksPath .githooks
```

## Release build

A release build is signed only when a key is configured. Create `keystore.properties` (gitignored) or set `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. See [docs/RELEASING.md](docs/RELEASING.md) and [scripts/release/README.md](scripts/release/README.md).

```bash
./gradlew assembleRelease
python scripts/release/verify_release.py app/build/outputs/apk/release/app-release.apk
```

## Prepare a device

1. Enable Developer options. Open Settings, About phone, Software information and tap Build number seven times.
2. Enable USB debugging, and optionally Wireless debugging.
3. Connect and list devices:

```bash
adb devices
```

For wireless debugging use `adb pair <host>:<port>` once, then `adb connect <host>:<port>`.

4. Start Shizuku. Install it, then start it from the Shizuku app with Wireless debugging, or from a computer with the start script shown in the Shizuku app.
5. Create a target profile.
   * Samsung: Settings, Advanced features, Dual Messenger, enable any app once. One UI creates the Dual Messenger user.
   * Any device: create a work profile with Island, or on an emulator run `adb shell pm create-user --profileOf 0 --managed Work`.

Set `ANDROID_SERIAL` once so you do not repeat `--serial`:

```bash
export ANDROID_SERIAL=<serial>
```

## Install and launch

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -n com.mohitarora.dualclone.debug/com.mohitarora.dualclone.ui.MainActivity
```

## Tests

### Unit tests

```bash
./gradlew testDebugUnitTest
```

### Instrumented e2e tests (emulator)

These run on an emulator with a managed profile and use a UiAutomation shell gateway instead of Shizuku. CI runs them in `.github/workflows/e2e.yml`. Locally, start an API 34 emulator, then:

```bash
sh scripts/ci/run_e2e.sh
```

The script creates the managed profile, starts it and runs `connectedDebugAndroidTest`. The tests use the instrumentation APK itself as the throwaway package, so nothing on the device is cloned or removed.

The tests replace the production Hilt module `BindingsModule` with `E2eGatewayModule`, which binds `ShizukuGateway` to the UiAutomation gateway. If `BindingsModule` gains or loses bindings, mirror them in `app/src/androidTest/java/com/mohitarora/dualclone/e2e/E2eGatewayModule.kt`.

### Device suite

```bash
python scripts/device/e2e_test.py --serial <serial>
```

Options:

| Option | Meaning |
| :--- | :--- |
| `--package` | Application id under test. Default is the debug build. Use `com.mohitarora.dualclone` with `--apk` to test a minified release build |
| `--apk` | APK to install. Default is the Gradle output for the package |
| `--skip-install` | Test what is already installed |
| `--probe-package` | A throwaway app installed for User 0 that may be cloned and removed. Enables TC18 |
| `--user` | Secondary profile id. Default is the first Dual Messenger or managed profile |
| `--out-dir` | Report folder. Default is `.e2e/<timestamp>` |

The suite switches system animations off for the run, because animations make `uiautomator dump` fail, and restores them afterwards. Reports are written to `report.md`, and a failing case keeps its screen dump next to it.

| ID | Check |
| :--- | :--- |
| TC01 | Preflight Check |
| TC02 | APK Installation |
| TC03 | Permission Allow-List |
| TC04 | App Launch and Focus |
| TC05 | Top App Bar Title |
| TC06 | Shizuku Status Badge |
| TC07 | Segmented Filter Live Counts |
| TC07B | App Title Visibility |
| TC08 | Filter Chips Row |
| TC08B | Target Profile Selector Chips |
| TC08C | Target Profile Switching |
| TC08D | Alphabet Scrollbar Navigation |
| TC09 | Secondary Profile Package List |
| TC10 | Search Filtering |
| TC11 | Clear Search |
| TC11B | AppOps Special Access Sheet |
| TC11C | AppOps Status Badges |
| TC12 | Settings Navigation |
| TC13 | Developer Diagnostics Unlock (7 taps on the version row) |
| TC14 | Pull To Refresh |
| TC15 | No Fatal Exceptions |
| TC16 | Screenshot |
| TC17 | Single Launcher Activity |
| TC18 | Ghost Probe Cycle (needs `--probe-package`) |
| TC19 | Paused Profile Indicator (skipped when no profile is paused) |
| TC20 | Self Exclusion |

Cases that cannot apply on a device are skipped, not failed.

### Ghost probe

Diagnoses a stale launcher icon after a clone is removed. Use a throwaway package.

```bash
python scripts/device/ghost_probe.py --serial <serial> --user <profile id> --package <throwaway> cycle
python scripts/device/ghost_probe.py --serial <serial> refresh-launcher
```

### Screen audit

```bash
python scripts/device/comprehensive_screen_audit.py --serial <serial> --output-dir .e2e/audit
```

Captures every screen, sheet and dialog for visual review. Output stays in the gitignored `.e2e` folder. Never publish captures from a personal device. README screenshots use demo data, see the README screenshots section of [CONTRIBUTING.md](CONTRIBUTING.md).

## Regenerate icons

```bash
python scripts/device/generate_icons.py
```

Run it with `--help` first to see the source and output options.

## Troubleshooting

* Shizuku permission denied: confirm Shizuku is running, then in Shizuku open Authorized applications and enable DualClone.
* Device offline or unauthorized: run `adb disconnect`, reconnect and accept the prompt on the device.
* Secure Folder: expected behavior. Knox blocks shell access, so DualClone shows guidance instead.
* `uiautomator dump failed`: a system animation or a busy device. The suite disables animations, rerun after the screen settles.
* Release build fails on signing: set the four signing variables or create `keystore.properties`.
