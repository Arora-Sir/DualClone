# Privacy

DualClone connects to the internet to check for and download updates from GitHub Releases. All package operations, profile management, and catalog queries execute locally on the device.

## What the network is used for

* **Check for updates.** When you tap Check for updates, the app queries the GitHub Releases API (`api.github.com`) for the latest release. An optional daily check runs once a day if enabled in Settings (off by default).
* **Download the update.** When you accept an update, the app downloads the signed APK and `SHA256SUMS.txt` from GitHub release assets, and verifies the APK SHA-256 digest before launching the Android installer. Android checks the signing key before installing.

These requests connect directly to GitHub. GitHub receives standard web server request headers, including your IP address.

## Data handling

* Zero analytics, crash reporting, advertising, or telemetry libraries.
* No collection of device identifiers, hardware serials, or user accounts.
* No transmission of installed app catalogs, clone lists, command logs, or preferences.
* Anonymous requests with no cookies or authentication required.

## Local data storage

A local database holds:

* A catalog of installed apps (package name, label, install and update times).
* The clone state of each app in each profile, and any AppOps presets you chose to remember.
* A command log of the `pm` and `appops` commands DualClone ran through Shizuku, with package name, profile id, result and time. It is capped at 500 entries. You can view it and clear it in the app.
* Theme choices and the update settings, kept in preferences.

Android backup is disabled (`allowBackup="false"`), so none of this is copied to cloud backups. A backup file exists only when you export one, and you choose where it is saved. You can protect it with a password.

## Permissions and why they exist

| Permission | Why |
| :--- | :--- |
| `INTERNET` | The update check and download described above. No other feature uses it |
| `REQUEST_INSTALL_PACKAGES` | Lets the app hand the downloaded, verified APK to the Android installer. You still have to allow installs from DualClone and confirm the install |
| `QUERY_ALL_PACKAGES` | The core function is to list every installed app so you can choose which to clone. A narrower package visibility query cannot express "all apps". The list is stored only on the device |
| Shizuku API permissions (`moe.shizuku.manager.permission.API_V23`, `moe.shizuku.privileged.api.permission.API_V2` and the matching fork variants) | Let the app ask Shizuku to run `pm install-existing`, `pm uninstall` and `appops` commands |
| `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` | Merged in by the WorkManager library for periodic background work such as the local reconcile and the optional daily update check. The app posts no notifications for them |

Every release is checked against an exact allow-list of these permissions. A new permission fails the release build until it is reviewed.

## How this is enforced

All network code lives in one package of the source (`update`). A build check fails if network classes are used anywhere else in the app.

## Links

Links to GitHub, support pages and social profiles open in your default browser. The browser, not DualClone, makes those requests.

## Contact

Raise privacy questions in the project's issue tracker.
