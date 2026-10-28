# Release scripts

| Script | Purpose |
| :--- | :--- |
| `scan_repo.py` | Fails on private IPs, personal paths, emails, secret-looking strings, dashes, curly quotes. Runs in the pre-commit hook and in CI |
| `check_version.py` | Checks `versionName`, README badge and CHANGELOG. With `--tag vX.Y.Z` the tag must equal `v` plus `versionName` and the changelog needs a dated entry |
| `extract_changelog.py` | Prints the release notes for one version, with the signing certificate fingerprint |
| `verify_release.py` | `apksigner verify` plus `aapt2 dump badging` assertions. Exact permission allow-list from `allowed_permissions.txt`, one launcher activity, not debuggable |
| `allowed_permissions.txt` | The permissions a release may request. Adding one is a reviewed change |
| `check_network_imports.py` | Fails when network code appears outside the `update` package |
| `generate_keystore.py` | Creates the release keystore with `keytool` outside the repository |

Enable the pre-commit hook once per clone:

```bash
git config core.hooksPath .githooks
```

## Creating the release keystore

Run this once, on your own machine.

```bash
python scripts/release/generate_keystore.py --out-dir /path/outside/the/repo/dualclone-signing
```

The script prompts for a password, runs `keytool` with the password passed through an environment variable, and writes `release.jks` and `release.jks.base64` to the folder you chose. It prints the public certificate SHA-256 only. It refuses to write inside the repository.

The equivalent manual command, which prompts for the passwords itself:

```bash
keytool -genkeypair -v -keystore release.jks -alias dualclone -keyalg RSA -keysize 4096 -validity 10000
```

Then:

1. Back up the folder in two separate places. A lost key means no user can ever update in place.
2. Add the four GitHub secrets: `KEYSTORE_BASE64` (contents of `release.jks.base64`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Put the printed fingerprint in the README.
4. For local release builds, create the gitignored `keystore.properties` described in `docs/RELEASING.md`.

Never paste the password, the keystore or its base64 text into an issue, a commit or a chat.

## Checking an APK

```bash
python scripts/release/verify_release.py DualClone-v1.0.0.apk --expect-version 1.0.0
```

The script finds `apksigner` and `aapt2` through `ANDROID_HOME` or `ANDROID_SDK_ROOT`, then `PATH`.
