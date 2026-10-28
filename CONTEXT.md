# DualClone Project Context

This document explains the platform constraints and design reasoning behind DualClone.

## Background

Android has supported multiple users since 5.0. Samsung One UI uses a lightweight secondary user for Dual Messenger. It is user id 95 on the devices this project was developed on, and it is created the first time you enable Dual Messenger for an app. Cloned apps run in that user with their own data, and One UI marks their launcher icons with a badge.

One UI Settings only offers Dual Messenger for a short list of apps, mostly messengers. The restriction is in the Settings UI. At the package manager level, any app that is installed for the primary user can be installed for the secondary user with `pm install-existing`, which a privileged shell can run.

## Alternatives

* Work profile managers such as Island and Shelter create a managed profile. That works for any app, but it adds work profile UI such as the Work tab, a briefcase badge and a paused state, and it isolates more than some people want.
* Other Shizuku based clone tools exist. DualClone does not benchmark against them or make claims about their behavior.
* Samsung Secure Folder is a separate Knox container and cannot be driven from a shell.

## Design goals

### Fast, local catalog

The installed app list is cached in a local Room database, so the Home screen has data to show while the package manager is queried. Search runs over that table. No startup time or search latency figure is published because none has been measured for a release build.

### Local by default, network for updates only

Everything DualClone does with your apps stays on the device. The network is used for one thing, checking GitHub Releases for a newer version and downloading it when you ask. That code sits in one package, and the build fails if network code appears anywhere else. See [docs/UPDATES.md](docs/UPDATES.md).

### Honest about platform limits

* Knox Secure Folder blocks shell access. DualClone explains that and opens Secure Folder instead of failing with an error.
* A paused work profile cannot receive package changes reliably. DualClone shows the state and warns first.
* Samsung One UI Home can keep a stale icon after a clone is removed. DualClone offers a manual launcher refresh.
* Clones share the APK with the primary user, so there is nothing to update separately. Updating the host app updates every clone.

### Verified changes

A command that exits with success is not treated as proof. DualClone checks the package manager again after every clone and removal and records the result in a local command log.

### One launcher entry

Debug tooling must not add icons to the app drawer. The app registers a single launcher activity, and the release check fails if there are more.

## Updates and support

How releases are made and updated is in [docs/UPDATES.md](docs/UPDATES.md). Architecture is in [ARCHITECTURE.md](ARCHITECTURE.md).
