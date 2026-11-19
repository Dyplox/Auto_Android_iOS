# CLAUDE.md

Mobile test automation framework: **one Appium suite for Android and iOS** of the WebdriverIO Native Demo App, where
only the locators change per platform. Java 27 · Gradle 9 (Kotlin DSL, configuration cache) · JUnit 6 · Appium 4 +
java-client 11 · Allure. `README.md` is the full reference (architecture, environments, configuration); this file is
what you need to work on the repo without breaking it.

## Commands

```bash
./gradlew build                      # what CI's "unit" job runs: compile + Error Prone/NullAway + Spotless + unit tests + JaCoCo
./gradlew spotlessApply              # fix formatting (Spotless fails the build on any diff)
./gradlew test --tests '*DeviceProfileTest'
./gradlew e2eAndroid|e2eIos|e2e -Penv=local -Ptags=smoke   # E2E: needs Appium + emulator/simulator (npm ci, ./scripts/download-apps.sh)
./scripts/update-dependencies.sh     # after ANY dependency/plugin version change (see Dependencies)
```

E2E tests cannot run in this environment unless a device is available: validate framework changes with unit tests
(`src/test/java/com/reto/qa/{config,data,driver,junit}`) and say so when E2E was not run.

## Layout

- `src/main/java/com/reto/qa`: framework (`config`, `driver`, `core`, `navigation`, `pages`, `data`, `app`). Every
  public type and method has Javadoc; NullAway assumes non-null, mark exceptions with `org.jspecify` `@Nullable`.
- `src/test/java/com/reto/qa/tests`: E2E tests (`@MobileTest` ⇒ `@Tag("e2e")`, excluded from `./gradlew test`).
- `src/test/resources/config`: layered `.properties` (`framework` < `<platform>` < `env/<env>` < `env/<env>-<platform>`),
  overridable with `-P<key>=…`, environment variables or `.env`.
- `.github/workflows/mobile-tests.yml`: `unit` on every push/PR; E2E jobs are **manual only** (`workflow_dispatch`,
  device matrix via the `android-api-levels` / `ios-devices` JSON inputs). `release.yml`: GitHub Release on `v*` tags.

## Rules

- **Never push to `main`.** Work on a branch and open a PR. `main` is protected: the `unit` check must pass and the
  branch must be up to date. Merged branches are deleted automatically.
- Commit or push only when asked. Commit messages in English, imperative, explaining the why.
- **GitHub Actions are pinned to full commit SHAs** with the version as a comment (`@<sha> # v7.0.1`). Resolve new ones
  with `git ls-remote --tags https://github.com/<owner>/<repo>.git`. Workflows default to `permissions: contents: read`;
  pass `${{ inputs.* }}` / matrix values to `run:` through `env:`, never interpolated (script injection).
- `npm ci --ignore-scripts` (native addons ship prebuilt binaries).
- `curl` that follows redirects (`-L`) must use `--proto '=https' --proto-redir '=https'`.
- Shell functions: assign positional parameters to `local` named variables first.
- Do not name constants or variables with `password`/`pwd`/`secret`/`token` when they hold a literal (Sonar S2068).
- Releases: bump `version` in `gradle.properties` + move `[Unreleased]` in `CHANGELOG.md` to a `## [x.y.z] - date`
  section in a PR; after merge, `git tag vX.Y.Z origin/main && git push origin vX.Y.Z`. Add user-visible changes to
  `[Unreleased]` as you make them.

## Dependencies (supply chain) — easy to get wrong

- Versions live in `gradle/libs.versions.toml`. Gradle dependencies are **locked** (`gradle.lockfile`,
  `buildscript-gradle.lockfile`, `settings-gradle.lockfile`) **and verified** (`gradle/verification-metadata.xml`,
  SHA-256 of every artifact). Any version change without regenerating both breaks the build in CI.
- Always regenerate with `./scripts/update-dependencies.sh`: it must be **one** Gradle run with `--write-locks` and
  `--write-verification-metadata` together, plus `--refresh-dependencies` (a warm local cache otherwise leaves files
  without checksums, e.g. `.module` metadata). Review that new checksums only belong to the bumped versions.
- Dependabot does **not** update lockfiles or verification metadata: on each Gradle Dependabot PR, check out its
  branch, run the script, commit and push.
- Two PRs that touch versions can each pass CI and still break `main` together (lockfile vs catalog). Keep branches
  up to date with `main` before merging.
- Verify checksum changes from an empty Gradle cache when in doubt:
  `GRADLE_USER_HOME=<empty dir> ./gradlew --no-configuration-cache clean build`.

## SonarQube Cloud

Project `Dyplox_Auto_Android_iOS` (organization `dyplox`), public, analyzed by CI with the Quality Gate enforced. The
SonarQube MCP server needs Docker running; without it, the public Web API works with no token:

```bash
curl -s "https://sonarcloud.io/api/qualitygates/project_status?projectKey=Dyplox_Auto_Android_iOS"            # add &pullRequest=N
curl -s "https://sonarcloud.io/api/issues/search?componentKeys=Dyplox_Auto_Android_iOS&resolved=false&ps=500"
```

Sonar results for `main` only refresh after the `unit` job reaches its analysis step. Branch coverage (~82%) has room
to grow in `IosSimulatorLocator`, `FrameworkConfig` and `UserCredentials`.

## Environment gotchas

- macOS: BSD `sed` (`sed -i ''`); `zsh` does not word-split `$VAR`, use arrays for lists of arguments.
- With `gh … --body-file -`, the heredoc must feed `gh`, not a pipe after it; prefer writing the body to a file.
