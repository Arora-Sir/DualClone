# Updates

DualClone updates itself from GitHub Releases. Obtainium and manual download are alternatives. The in-app updater is the only network use in the app, and this page is its contract with the release pipeline.

## Channels

### In-app updater

* Settings has a Check for updates action. It runs only when you tap it.
* An optional daily check can be switched on in Settings. It is off by default.
* A check asks the GitHub Releases API for the latest published release. Drafts and pre-releases are not returned. If the release is newer than the installed `versionName`, the app shows the release notes.
* If you accept, the app downloads the APK and `SHA256SUMS.txt` to its cache, verifies the SHA-256 of the APK, and hands the file to the Android installer through a FileProvider. Android asks you to allow installs from DualClone the first time, and checks that the signing certificate matches the installed one.
* A failed check, a missing asset, a checksum mismatch or a network error shows a message and installs nothing.
* The app sends no identifiers and no package list. See [PRIVACY.md](../PRIVACY.md).

### Obtainium

Obtainium polls GitHub Releases with its own network stack and installs new APKs. It compares the release tag and the APK asset. It does not read `SHA256SUMS.txt`. To subscribe, open this link on a device that has Obtainium installed:

```text
obtainium://add/https://github.com/Arora-Sir/DualClone
```

### Manual download

Download `DualClone-vX.Y.Z.apk` from the [Releases page](https://github.com/Arora-Sir/DualClone/releases) and open it. Compare the signing certificate with the README first.

In-place upgrades in every channel only work when each release is signed with the same key.

Google Play is not a channel. DualClone relies on Shizuku and on listing every installed app, and it installs APKs itself, none of which Play policy allows for this kind of tool.

## Release contract used by the updater

The release workflow and the updater depend on these exact facts. Change them together.

| Item | Value |
| :--- | :--- |
| Release source | `https://api.github.com/repos/Arora-Sir/DualClone/releases/latest` |
| Hosts contacted | `api.github.com`, `github.com`, `objects.githubusercontent.com` and `release-assets.githubusercontent.com`, over https only. Asset URLs from the API must be on `api.github.com` or `github.com` under this repository's release paths |
| Tag | `vX.Y.Z`, equal to `v` plus `versionName` |
| APK asset | `DualClone-vX.Y.Z.apk` |
| Checksum asset | `SHA256SUMS.txt` |
| Checksum line | `<64 lowercase hex characters>`, two spaces, `DualClone-vX.Y.Z.apk` (the output of `sha256sum`) |
| Draft releases | Created by the workflow as drafts. Only a published release is seen as the latest |

The workflow fails if the checksum line does not match that format, and `verify_release.py` rejects an APK that is unsigned, debug-signed or has a permission outside the allow-list.

The updater compares the release tag with the installed `versionName` as a semantic version, ignores pre-releases and drafts, and downloads only the asset named above. It limits the APK to 200 MB, resumes a partial download when the server allows it, requires a matching line in `SHA256SUMS.txt`, hashes the file again before the install hand-off, and refuses to install on any mismatch. The checksum line parser accepts one or more spaces and an optional `*` before the file name, but the workflow always writes the two-space `sha256sum` form.

## Permissions for updating

* `INTERNET` for the check and the download.
* `REQUEST_INSTALL_PACKAGES` to hand the verified APK to the installer.

Both are on the release permission allow-list in `scripts/release/allowed_permissions.txt`. Any other new permission fails the release check. Network code is allowed only in the `update` package, and `scripts/release/check_network_imports.py` runs in CI and in the pre-commit hook to enforce it.

## Releasing

The signing setup, the protected `release` environment, the pipeline and the per-release steps are in [RELEASING.md](RELEASING.md).

After publishing, install the previous release, then check for updates inside it and confirm it downloads, verifies and installs the new one. Then confirm Obtainium resolves the new tag.
