# Releasing DualClone

This is the step-by-step guide for signing and publishing a release. It covers the one-time setup, the pipeline, each release, key problems and optional hardening. How users receive updates is in [UPDATES.md](UPDATES.md).

Every command in this guide runs as written. Replace values in angle brackets.

## Overview

* Releases are APKs signed with one long-lived key and published through GitHub Releases.
* Signing happens only in the `Release` workflow, on a pushed `v*` tag in this repository, after a reviewer approves the run.
* The workflow creates a draft release. A maintainer publishes it by hand.
* The in-app updater, Obtainium and the Android installer only update in place when the signing key never changes. The key is the most valuable thing in this project.

## 1. Generate the release keystore

Do this once, on a trusted machine. Never create it inside the repository.

Use the helper, which prompts for the password and never prints it:

```bash
python scripts/release/generate_keystore.py --out-dir <folder outside the repo>
```

Or run `keytool` yourself. It prompts for the passwords and the certificate fields:

```bash
keytool -genkeypair -v -keystore release.jks -alias dualclone -keyalg RSA -keysize 4096 -validity 10000
```

Choices:

* RSA 4096 is the default in this guide. RSA 2048 also works and is accepted everywhere. Pick 4096 unless you have a reason not to.
* `-validity 10000` is about 27 years. Android requires the certificate to be valid beyond October 2033, and a long validity avoids forced rotation.
* Use one strong password for both the keystore and the key. Store it in a password manager.
* The alias is `dualclone`. It becomes the `KEY_ALIAS` secret.

Read the certificate fingerprint. It is public and goes in the README and the release notes:

```bash
keytool -list -v -keystore release.jks -alias dualclone
```

Copy the `SHA256:` line.

### Back it up

Keep at least two copies of `release.jks` in separate places you control, for example an encrypted drive and an encrypted cloud vault. Keep the password in a password manager, not next to the file. Test a restore once by listing the backup copy with `keytool -list`.

### If the key is lost

Android refuses to update an app that was signed with a different key. If the key is lost, nobody can update in place. You must publish a new app signed with a new key. Users have to uninstall and reinstall, which deletes their clones' data. That is why the backup matters. See section 7 for rotation.

## 2. Add the signing secrets

The secrets live in a protected GitHub environment named `release`, not in repository-wide secrets. Environment secrets are only handed to jobs that target the environment, and the environment can demand a human approval first.

### Create the environment

In GitHub, open the repository, then Settings, Environments, New environment. Name it `release`. Then:

1. Turn on Required reviewers and add yourself and any co-maintainer. A sole maintainer must leave "Prevent self-review" off, otherwise nobody can approve.
2. Under Deployment branches and tags, choose Selected branches and tags and add a tag rule `v*`.
3. Save protection rules.

Also add a tag ruleset under Settings, Rules, Rulesets so only maintainers can create or delete `v*` tags.

### Encode the keystore

The workflow receives the keystore as base64 text. The commands below send it straight into the secret so it is never shown or saved.

PowerShell on Windows:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("<path to release.jks>")) | gh secret set KEYSTORE_BASE64 --env release --repo <owner>/DualClone
```

Bash on Linux or Git Bash:

```bash
base64 -w0 <path to release.jks> | gh secret set KEYSTORE_BASE64 --env release --repo <owner>/DualClone
```

On macOS use `base64 -i <path to release.jks>` instead of `base64 -w0`.

### Set the other three secrets

`gh secret set` prompts for the value, so the password does not land in your shell history:

```bash
gh secret set KEYSTORE_PASSWORD --env release --repo <owner>/DualClone
gh secret set KEY_ALIAS --env release --repo <owner>/DualClone
gh secret set KEY_PASSWORD --env release --repo <owner>/DualClone
```

The same works in PowerShell. To use the web page instead, open Settings, Environments, release, Environment secrets, Add environment secret, and create `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.

Then delete any `release.jks.base64` file the helper wrote, and clear the clipboard if you copied anything.

Check the names:

```bash
gh secret list --env release --repo <owner>/DualClone
```

### Local release builds

For a local signed build, create `keystore.properties` in the repository root. The file is gitignored.

```properties
storeFile=/absolute/path/to/release.jks
storePassword=<password>
keyAlias=dualclone
keyPassword=<password>
```

Or set the environment variables `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.

## 3. How the workflow handles the key

`.github/workflows/release.yml` does the following, in this order.

1. Runs only when `github.repository` is the main repository and the ref is a tag. Forks never run it, and pull requests never reach it. `build.yml` runs on pull requests with no secrets and no write permission.
2. Targets the `release` environment, so the job waits for a required reviewer and only then receives the secrets.
3. Checks out the tag without storing the checkout token, and fails when the tagged commit is not on `main`.
4. Passes the secrets to the steps that need them through `env`, never through the command line. GitHub masks secret values in logs automatically. No step prints them.
5. Fails with a clear message, naming the missing secret and never its value, if any of the four secrets is empty.
6. Decodes `KEYSTORE_BASE64` into `release.jks` under `RUNNER_TEMP` with owner-only permissions, and points `KEYSTORE_FILE` at it. Gradle reads `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.
7. Deletes the keystore in a final step that runs even if the build failed. Runners are also discarded after the job.

The release build never falls back to the debug key. With no key configured the build leaves an unsigned APK, and the workflow fails because `app-release.apk` does not exist.

Never add `set -x`, `echo` of a secret, or `cat` of the decoded file to the workflow. Review every change to `release.yml` as security-sensitive. Third-party actions are pinned by commit SHA, and Dependabot proposes updates.

## 4. Pipeline stages

| Stage | Workflow | Trigger | Secrets |
| :--- | :--- | :--- | :--- |
| Build and test | `build.yml` | Pull request and push to `main` | None |
| Emulator e2e | `e2e.yml` | Pull request touching app code, and manual run | None |
| Release | `release.yml` | Push of a `v*` tag | Environment `release` |

### Build and test

`build.yml` validates the Gradle wrapper, runs the repository scan, the version check and the network containment check (no network classes outside the `update` package), then `lintDebug testDebugUnitTest assembleDebug`, and uploads the debug APK. Permissions are `contents: read`.

### Emulator e2e

`e2e.yml` boots an API 34 x86_64 emulator and runs `scripts/ci/run_e2e.sh`. The script creates a managed profile named Work with `pm create-user --profileOf 0 --managed Work`, starts it and runs `connectedDebugAndroidTest`. The tests use a UiAutomation shell gateway in place of Shizuku. Samsung-only behavior stays in the physical-device suite.

### Release

`release.yml` runs these steps once the reviewer approves:

1. Check that the tagged commit is on `main`, then validate the Gradle wrapper.
2. `check_version.py --tag <tag>` fails unless the tag equals `v` plus `versionName` and the changelog has a dated entry for the version.
3. Run the repository scan and the network containment check.
4. Fail if any signing secret is missing.
5. Build with `lintDebug testDebugUnitTest assembleRelease`.
6. Fail unless the signed `app-release.apk` exists, then copy it to `DualClone-vX.Y.Z.apk`.
7. `verify_release.py` runs `apksigner verify`, rejects the Android debug certificate, runs `aapt2 dump badging`, and asserts the package id, the version, exactly one launcher activity and a non-debuggable build. It also compares every requested permission with the exact allow-list in `scripts/release/allowed_permissions.txt`, which allows `INTERNET` and `REQUEST_INSTALL_PACKAGES` for the updater and fails on any other new permission. It writes the signer certificate SHA-256.
8. Write `SHA256SUMS.txt` and cut the release notes from `CHANGELOG.md`. The notes end with the certificate fingerprint. The step fails unless the checksum line has the form `<sha256>  DualClone-vX.Y.Z.apk`, because the in-app updater parses it.
9. Create a draft release with exactly two assets, `DualClone-vX.Y.Z.apk` and `SHA256SUMS.txt`. The updater and the contract in [UPDATES.md](UPDATES.md) depend on those names.

Run the same checks locally before tagging:

```bash
./gradlew lintDebug testDebugUnitTest assembleRelease
python scripts/release/verify_release.py app/build/outputs/apk/release/app-release.apk --expect-version <X.Y.Z>
```

## 5. Cutting a release

1. Bump `versionCode` by one and set `versionName` in `app/build.gradle.kts`. Use Semantic Versioning.
2. In `CHANGELOG.md`, rename `## [Unreleased]` to `## [X.Y.Z] - YYYY-MM-DD` and add a new empty `## [Unreleased]` section above it. The release notes are cut from this section, so write it for users.
3. Run the local checks:

```bash
python scripts/release/scan_repo.py
python scripts/release/check_version.py
```

4. Merge to `main`.
5. Tag the merge commit and push the tag:

```bash
git tag -s v<X.Y.Z> -m "DualClone v<X.Y.Z>"
git push origin v<X.Y.Z>
```

Drop `-s` if you do not sign tags.

6. Open the Actions tab, find the `Release` run and approve the deployment to `release`.
7. When the run is green, open the draft release. Check the APK name, `SHA256SUMS.txt`, the notes and the fingerprint against the README. Download the APK and run `verify_release.py` on it once more.
8. Install it over the previous release on a test device and confirm clone and remove still work.
9. Publish the draft. Publishing is what makes Obtainium see the release.

If the workflow fails, fix the cause, delete the tag locally and on the remote, and tag again. A draft release left behind can be deleted from the Releases page.

## 6. Update verification

The in-app updater reads the latest published release, so it sees nothing until you publish the draft. After publishing, install the previous release on a test device, tap Check for updates, and confirm it shows the notes, downloads, verifies the checksum and installs as an update.

Obtainium works from the same release.

Subscribe with the link in the README, or open this on a device that has Obtainium:

```text
obtainium://add/https://github.com/<owner>/DualClone
```

Obtainium reads the GitHub release, picks the APK asset and installs it. It does not read `SHA256SUMS.txt`. Users who want a checksum compare it by hand.

After each publish:

1. Check that the in-app updater and Obtainium show the new version and installs it as an update, not a reinstall.
2. Check the installed signature:

```bash
apksigner verify --print-certs DualClone-v<X.Y.Z>.apk
```

3. Compare the SHA-256 digest with the README and the release notes. A mismatch means the key changed. Do not publish, and see section 7.

## 7. Key rotation and compromise

### Suspected compromise

Treat any of these as a compromise: the keystore or password appeared in a log, a chat or a commit, a maintainer's machine or account was taken over, or an APK with an unknown signature claims to be DualClone.

1. Disable the `Release` workflow and remove the four environment secrets.
2. Rotate the GitHub tokens and passwords of every maintainer account involved.
3. Review recent runs, tags and releases. Delete anything you did not create.
4. Publish a security advisory that states which releases are affected and which fingerprint is genuine.

### Rotating the key

Android supports signing certificate rotation through APK Signature Scheme v3 and `apksigner rotate`, and it lets an old key vouch for a new one. This works only while you still hold the old key. Follow the Android documentation for `apksigner rotate` and `--lineage`, and test the upgrade on devices before publishing.

If the old key is lost or fully compromised, rotation cannot be proven. Generate a new keystore, update the secrets, publish the app under the new signature with a clear notice that users must uninstall and reinstall, and update the fingerprint in the README. Users should export a backup file from the app first.

## 8. Optional later additions

* Build provenance. Add the `actions/attest-build-provenance` action to `release.yml` with `id-token: write` and `attestations: write`, attest `dist/DualClone-vX.Y.Z.apk`, and tell users to verify with `gh attestation verify`. Pin the action by SHA.
* Reproducibility. Build the tag twice on clean runners and compare the unsigned APK contents. Fix timestamps and dependency versions until they match.
* Dependency review. Enable Dependabot alerts and the dependency review action on pull requests.
* Branch protection. Require the `Build` workflow and one review before merging to `main`.
* Play App Signing does not apply. DualClone is not distributed through Google Play, so there is no upload key and the key described here is the only signing key.
