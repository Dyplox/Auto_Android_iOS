# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html):

- **MAJOR**: incompatible changes for whoever uses the framework (public API of `src/main`, configuration keys,
  Gradle tasks or properties).
- **MINOR**: new screens, tests, environments or features that keep compatibility.
- **PATCH**: fixes, dependency updates and internal changes.

Dependency updates from Dependabot are not listed one by one; see the release's commit history.

## [Unreleased]

## [1.0.0] - 2026-11-19

First release.

### Added

- Cross-platform framework with Appium 4 and java-client 11: one suite for Android and iOS where only the locators
  change (Page Objects with `@AndroidFindBy` / `@iOSXCUITFindBy`, platform strategies and factories).
- E2E tests for login, sign-up, forms and navigation of the WebdriverIO Native Demo App, with `smoke` tags.
- Environments `local`, `ci`, `browserstack` and `saucelabs`, layered `.properties` configuration and overrides from
  `-P`, environment variables or `.env`.
- Automatic iOS simulator selection, condition-based waits (no `Thread.sleep`), W3C gestures and one Appium session
  per test class.
- Allure reports with steps, severity and screenshot plus page source on every failure; one retry for flaky E2E tests.
- Multi-device runs in CI: Android API levels and iOS simulators as a matrix chosen when the workflow is started, and
  one Allure report that merges every device (the device is a test parameter).
- Quality gates: Error Prone with NullAway, Spotless, JaCoCo and SonarQube Cloud with a Quality Gate in CI.
- Supply chain: GitHub Actions pinned to commit SHAs, `npm ci --ignore-scripts`, Gradle dependency locking and
  SHA-256 verification of every artifact (`scripts/update-dependencies.sh`).
- `SECURITY.md`, MIT license and the release workflow for `v*` tags.

[Unreleased]: https://github.com/Dyplox/Auto_Android_iOS/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Dyplox/Auto_Android_iOS/releases/tag/v1.0.0
