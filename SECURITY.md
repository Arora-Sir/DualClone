# Security Policy

## Supported versions

Only the latest release receives security fixes.

## Reporting a vulnerability

Report privately through [GitHub private vulnerability reporting](https://github.com/Arora-Sir/DualClone/security/advisories/new). Do not open a public issue for a security problem.

Include the app version, the device and Android version, steps to reproduce and the impact you see. You will get an acknowledgement within a week. Fixes ship in a new release and are noted in the changelog once users can update.

## Scope

In scope:

* Command injection through a package name, AppOps key or any other value that reaches a shell command.
* Any way for the app to send data off the device other than the documented update check.
* Weaknesses in how the updater picks, downloads or verifies an APK.
* Bypassing the verification or confirmation steps so a removal happens without the user's intent.
* Release signing or build pipeline weaknesses.

Out of scope:

* Issues that need a rooted or already compromised device.
* Problems in Shizuku, Android or One UI themselves. Report those upstream.

## Design notes

* The only network use is the update check and download, in one package. It talks to `api.github.com` and the GitHub release asset host, and sends no identifiers or package lists.
* A downloaded APK is checked against the SHA-256 in `SHA256SUMS.txt` and then handed to the Android installer, which enforces the signing key.
* Package names and AppOps values are validated against strict patterns before they reach `pm` or `appops`.
* Release APKs are signed with one stable key. Compare the fingerprint in the README before installing.
* Release builds run `scripts/release/verify_release.py` to assert the signature, an exact permission allow-list and a single launcher activity.
