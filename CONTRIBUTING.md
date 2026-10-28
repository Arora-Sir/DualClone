# Contributing

Thanks for helping. Bug reports, device test results and pull requests are welcome.

## Before you start

* Search existing issues. For questions use Discussions.
* For a larger change, open an issue first so the direction can be agreed.
* Read [ARCHITECTURE.md](ARCHITECTURE.md). Layers depend inward, and screens never call Shizuku directly.

## Ground rules

* Network use is limited to the updater in the `update` package. A change that adds network code elsewhere, another permission, a network library, analytics or crash reporting will not be merged. `scripts/release/check_network_imports.py` and the release permission allow-list enforce this.
* Keep changes small and focused. Match the style of the surrounding code.
* All user-facing text goes in `strings.xml`. Hardcoded text in Compose code is rejected.
* Every shell argument that comes from outside must pass `PackageValidator` or an allow-list.
* New behavior needs a test. Parser changes need a fixture of real command output.
* Use demo data in screenshots and logs. Remove device serials, addresses and personal app lists.

## Writing style

Docs and comments use plain punctuation. Use straight quotes. Do not use em dashes, en dashes or double hyphens as sentence breaks. Use a period instead of a semicolon between clauses. The pre-commit hook and CI enforce this.

## Development workflow

1. Fork and create a branch.
2. Set up the hook: `git config core.hooksPath .githooks`.
3. Build and test:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
python scripts/release/scan_repo.py
```

4. If you touched clone or remove logic, run the emulator suite (`sh scripts/ci/run_e2e.sh`) or the device suite described in [SETUP.md](SETUP.md), and say which one you ran.
5. Update `CHANGELOG.md` under Unreleased for user-visible changes.
6. Open a pull request using the template.

## Commit messages

Use Conventional Commits, for example `fix: verify removal before updating clone state`.

## License

By contributing you agree that your work is licensed under GPL-3.0, the license of this project.

See also the [Code of Conduct](CODE_OF_CONDUCT.md).
