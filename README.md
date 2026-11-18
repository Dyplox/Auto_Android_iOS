
# Cross-platform mobile automation with Appium (Android + iOS)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Dyplox_Auto_Android_iOS&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Dyplox_Auto_Android_iOS)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Dyplox_Auto_Android_iOS&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Dyplox_Auto_Android_iOS)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Dyplox_Auto_Android_iOS&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=Dyplox_Auto_Android_iOS)

A single test project for **the same app on Android and iOS**, built with **Gradle 9**, **JUnit 6** and **Java 27**.
Page Objects, tests and test data are shared; **the only thing that changes per platform is the locators**,
declared with Page Factory (`@AndroidFindBy` / `@iOSXCUITFindBy`) and resolved at runtime from the session.

```java
@AndroidFindBy(uiAutomator = "new UiSelector().text(\"Please enter a valid email address\")")
@iOSXCUITFindBy(iOSNsPredicate = "label == 'Please enter a valid email address'")
private WebElement invalidEmailError;
```

```java
@MobileTest                                   // Appium session + evidence on failure + @Tag("e2e")
class LoginTest {

    @Test
    void shouldLoginWithValidCredentials(WdioDemoApp app) {      // the app is injected (DIP)
        LoginPage loginPage = app.open(Screen.LOGIN, LoginPage::new);
        loginPage.loginWith(CredentialsMother.valid());
        assertThat(app.alert().waitUntilVisible().title()).contains("Success");
    }
}
```

The same test runs on Android with `./gradlew e2eAndroid`, on iOS with `./gradlew e2eIos` and on both at once with
`./gradlew e2e`, in any environment: local, CI, BrowserStack or Sauce Labs (`-Penv=…`).

---

## App under test

**[WebdriverIO Native Demo App](https://github.com/webdriverio/native-demo-app) v2.2.0** (MIT): a single
React Native/Expo app published for **Android (.apk)** and **iOS Simulator (.app.zip)**, with login/sign-up with
validation, forms, native alerts, swipe, drag & drop, deep links (`wdio://`) and a webview.

| | Android | iOS |
|---|---|---|
| File | `android.wdio.native.app.v2.2.0.apk` | `ios.simulator.wdio.native.app.v2.2.0.zip` |
| appId | `com.wdiodemoapp` | `org.wdiodemoapp` |
| Runs on | Emulator or real device | **Simulator only** (there is no build for physical iPhones) |


---

## Stack

| Component | Version | Why |
|---|---|---|
| Gradle | 9.9.1 (wrapper, with checksum) | Kotlin DSL, version catalog, configuration cache (runs Android and iOS in parallel); supports Java 27 since 9.8 |
| Java | 27 (toolchain) | Compilation, tests and Javadoc run on JDK 27; Gradle downloads it if it is missing (Foojay) |
| JUnit | 6.1.3 (Jupiter) | Extensions, parameter injection, `@ParameterizedTest`, `@Tag`, `@TempDir`, parallel execution, auto-closed `Store` values |
| Appium java-client | 11.0.0 | Cross-platform Page Factory, typed options (Builder), W3C Actions |
| Selenium API | 4.50.0 | Pinned per java-client 11's official compatibility matrix |
| AssertJ | 3.27.7 | Fluent assertions and clear messages (Self-validating) |
| Allure | 3.1.1 (`allure-jupiter`) / 3.33.0 (CLI) | Report with steps (`@Step`), screenshots and page source on failure |
| AspectJ weaver | 1.9.27 | `-javaagent` for Allure `@Step`; first release that supports Java 27 |
| Error Prone + NullAway | 2.52.0 + 0.14.3 | Bug patterns and null safety at compile time (JSpecify `@Nullable`) |
| Spotless | 8.10.4 (Eclipse JDT formatter) | One code format for everyone; `spotlessCheck` runs in `build`. Works on JDK 27 (palantir/google-java-format do not yet) |
| JaCoCo | 0.8.16 | Coverage of the framework unit tests (first release that supports Java 27) |
| Gradle Test Retry | 1.6.6 | Re-runs a failed E2E test once and reports it as flaky |
| Appium server | 4.0.0 | Latest release (Nov 15, 2026), pinned in `package.json` / `package-lock.json` |
| UiAutomator2 / XCUITest | 9.2.2 / 13.1.3 | First driver lines for Appium 4 |

Library versions live in a single place: [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

---

## Architecture

```
build.gradle.kts                 tasks: test (unit) · e2eAndroid · e2eIos · e2e (both in parallel)
gradle/libs.versions.toml        version catalog
src/main/java/com/reto/qa
├── config/        FrameworkConfig (properties + env + -P) · FrameworkSettings (typed, validated)
│                  DeviceProfile (Builder) · IosSimulatorLocator
├── driver/        PlatformProvider (Abstract Factory) · Android/IosPlatformProvider · MobileSessionFactory
│                  MobileSession · AppiumServer (Local | Remote)
├── core/          Waiter (explicit waits) · Gestures (W3C Actions) · PlatformActions (Strategy)
├── navigation/    Navigator (Strategy): TabBarNavigator | DeepLinkNavigator
├── pages/         BaseUiObject (Page Factory) · BasePage · LoginPage · FormsPage · HomePage
│   └── components TabBar · NativeAlert
├── data/          UserCredentials (Builder) · CredentialsMother (Object Mother)
└── app/           WdioDemoApp (Facade: the tests' entry point)

src/test/java/com/reto/qa
├── junit/         @MobileTest · AppiumSessionExtension · EvidenceExtension
├── tests/         LoginTest · SignUpTest · FormsTest · NavigationTest          ← E2E (Android and iOS)
└── config|data|driver/  framework unit tests (no device)

src/test/resources
├── config/        framework.properties · android.properties · ios.properties
│   └── env/       local · ci (+ci-ios) · browserstack (+android/ios) · saucelabs (+android/ios)
└── junit-platform.properties · allure.properties · logback-test.xml

scripts/           download-apps.sh · upload-app.sh (BrowserStack / Sauce Labs) · update-dependencies.sh
.env.example       credentials template; the real .env is not committed
```

Lifecycle of a test class:

```
@MobileTest ─► AppiumSessionExtension.beforeAll
                 ├─ AppiumServer: one per JVM, in the root Store (JUnit closes it at the end of the run)
                 └─ MobileSessionFactory ─► FrameworkSettings.from(config) (fails fast on bad values)
                                            DeviceProfileFactory (config ► DeviceProfile)
                                            PlatformProviders.forPlatform(ANDROID|IOS)
                                              ├─ capabilities()   UiAutomator2Options | XCUITestOptions
                                              ├─ createDriver()   AndroidDriver | IOSDriver
                                              └─ createActions()  AndroidPlatformActions | IosPlatformActions
                 └─ MobileSession stored in the class's ExtensionContext.Store
             ─► beforeEach: restarts the app ─► the test receives WdioDemoApp as a parameter
             ─► Pages / Components (PageFactory + AppiumFieldDecorator picks @AndroidFindBy or @iOSXCUITFindBy)
             ─► afterAll: closes the session
```

---

## Patterns and principles

### SOLID

| Principle | Where |
|---|---|
| **S** – Single responsibility | `FrameworkConfig` only resolves configuration; `DeviceProfileFactory` only maps; `MobileSessionFactory` only orchestrates; `AppiumSessionExtension` only manages the lifecycle; `EvidenceExtension` only captures evidence; each page models one screen and each component (TabBar, NativeAlert) is reused |
| **O** – Open/closed | A new platform or cloud provider = a new `PlatformProvider` registered in `PlatformProviders`; a new way to navigate = a new `Navigator`; a new cross-cutting behaviour = a new JUnit extension. No existing class changes |
| **L** – Liskov substitution | `AndroidPlatformActions` and `IosPlatformActions` are interchangeable; the generic bound `D extends AppiumDriver & InteractsWithApps & HasOnScreenKeyboard` guarantees it at compile time |
| **I** – Interface segregation | Small interfaces: `Navigator` (1 method), `PlatformActions` (3), `PlatformProvider` (4) |
| **D** – Dependency inversion | Tests receive `WdioDemoApp` as a parameter and pages receive `MobileSession` through the constructor; they depend on abstractions (`PlatformActions`, `Navigator`). There are no singletons, static state, `ThreadLocal`s or test base classes: even the Appium server is an object (`AppiumServer`) owned by the JUnit extension |

### FIRST

| | How it is achieved |
|---|---|
| **Fast** | One session per class (not per test); the app is restarted with `terminateApp/activateApp` instead of a new session; condition-based waits, never `Thread.sleep`; unit tests in parallel; Android and iOS in parallel (`./gradlew e2e`); optional deep-link navigation |
| **Independent** | `beforeEach` restarts the app: every test starts on Home without depending on another one; the session lives in the class's `Store` (isolated per class); each platform runs in its own JVM; `@TempDir` in unit tests |
| **Repeatable** | Deterministic data (`CredentialsMother`); versions pinned in the version catalog, the wrapper (with checksum), `package-lock.json` (Appium and drivers) and the app; the same suite in any environment, only the configuration changes |
| **Self-validating** | AssertJ with descriptive messages; automatic evidence (screenshot + page source) in Allure on failure; a test that only passes on retry is reported as *flaky*, not as passed |
| **Timely** | The framework unit tests, Error Prone, NullAway and Spotless run on every `./gradlew build` and on every PR (CI) |

### Other patterns

- **Builder**: `DeviceProfile.builder(...)`, `UserCredentials.builder()/toBuilder()` and Appium's typed options (`UiAutomator2Options`, `XCUITestOptions`).
- **Page Factory**: `PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ZERO), this)` in `BaseUiObject`; *lazy* elements (looked up on every use, no `StaleElementReference`).
- **Abstract Factory** (`PlatformProvider`), **Strategy** (`PlatformActions`, `Navigator`), **Facade** (`WdioDemoApp`), **Object Mother** (`CredentialsMother`), **JUnit extensions** instead of inheritance (`@MobileTest`).

### Where can platform-specific code live?

Only in two places, by design:

1. **The locators** (Page Factory annotations). Example: native alert → `id` on Android, `iOSNsPredicate` on iOS.
2. **`PlatformActions`**: behaviours each OS handles differently (hiding the keyboard, deep links). Tests and pages
   never ask "am I on iOS?". That is why, for example, the switch is checked by its text
   ("Click to turn the switch ON") and not by the `checked` (Android) / `value` (iOS) attribute.

---

## Requirements on your Mac

- **JDK 17–27** to run Gradle (`java -version`). The project itself compiles and runs tests on **JDK 27**: if it is not
  installed, Gradle downloads it into `~/.gradle/jdks`. No need to install Gradle: the wrapper (`./gradlew`) downloads 9.9.1.
  Check which JDK the build uses with `./gradlew javaToolchains`.
- **Node.js 22.22.2+** or 24.15+ (`node -v`; there is an `.nvmrc`). This is Appium 4's minimum.
- **Android**: Android Studio with the SDK + an emulator (AVD). `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) and `JAVA_HOME` variables.
- **iOS**: Xcode with at least one iPhone simulator (`xcrun simctl list devices available`).

## Getting started

```bash
npm ci                        # Appium 4.0.0 + UiAutomator2 + XCUITest + Allure (pinned versions)
./scripts/download-apps.sh    # downloads the .apk and the simulator .zip to ./apps
npm run doctor:android        # checks the Android setup (SDK, adb, JAVA_HOME…)
npm run doctor:ios            # checks Xcode, simulators…

./gradlew build               # compiles, runs static analysis, formatting check and the unit tests (no device) ✔
```

### Running E2E tests

| Command | What it does |
|---|---|
| `./gradlew e2eAndroid` | Full suite on Android |
| `./gradlew e2eIos` | Full suite on iOS |
| `./gradlew e2e` | Android and iOS **in parallel**, each in its own JVM (enabled by the configuration cache) |
| `./gradlew e2eAndroid -Ptags=smoke` | Only tests tagged `@Tag("smoke")` (accepts expressions: `-Ptags="smoke & !slow"`) |
| `./gradlew e2eIos --tests '*LoginTest'` | A single class |

With Appium already running (`npm run appium` in another terminal), or letting the framework start and stop it:

```bash
./gradlew e2e -Pappium.server.autostart=true      # each JVM starts its own Appium on a free port
./gradlew e2e -Penv=saucelabs                      # or in the cloud: see "Environments"
```

> The first iOS launch builds WebDriverAgent and can take several minutes (the timeouts already allow for it).
>
> **IntelliJ**: the `test` task excludes the E2E tests. To run one from the IDE, use the `e2eAndroid` / `e2eIos`
> task (for example `./gradlew e2eAndroid --tests '*FormsTest'`) or configure the JUnit runner with
> `-Dplatform=android`.

## Environments

An environment is a configuration profile: where Appium is, which device, which app and which provider options.
The code and the tests do not change; pick one with `-Penv=…` (or `TEST_ENV` / `.env`; `local` by default).

| Environment | Appium | Devices | App | Credentials |
|---|---|---|---|---|
| `local` | `npm run appium` on your Mac (or `-Pappium.server.autostart=true`) | Local emulator / simulator | `apps/*.apk`, `apps/*.zip` | — |
| `ci` | Started by each JVM, longer timeouts | Runner emulator / simulator | `apps/*` | — |
| `browserstack` | `hub-cloud.browserstack.com` | Real Android devices (*) | custom_id `WdioDemoApp-Android` | `BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY` |
| `saucelabs` | `ondemand.<region>.saucelabs.com` | Android emulators and iOS simulators | `storage:filename=…` | `SAUCE_USERNAME`, `SAUCE_ACCESS_KEY` (+ `SAUCE_REGION`) |

(*) BrowserStack only offers real iPhones and the demo app only has a simulator build: `e2eIos -Penv=browserstack`
fails immediately and explains why. For iOS in the cloud, use Sauce Labs.

```bash
cp .env.example .env                      # fill in your credentials (.env is not committed)

./gradlew e2eAndroid                      # local (default)
./gradlew e2e -Penv=ci                    # as in the pipeline

./scripts/upload-app.sh saucelabs         # uploads the .apk and .zip to Sauce App Storage
./gradlew e2e -Penv=saucelabs             # Android and iOS in the cloud, in parallel

./scripts/upload-app.sh browserstack      # uploads the .apk with custom_id WdioDemoApp-Android
./gradlew e2eAndroid -Penv=browserstack
```

How it works:

- **Layers**: `env/<environment>-<platform>.properties` > `env/<environment>.properties` > `<platform>.properties` >
  `framework.properties`. An environment only declares what changes.
- **Secrets outside the repository**: values accept `${VARIABLE}` and `${VARIABLE:-default}`, resolved from
  environment variables or `.env` (environment variables win, which is how CI works). They are resolved only when
  used: a local run does not require cloud credentials; if one is missing, the error names it.
- **Provider options**: `cloud.options-capability=bstack:options` (or `sauce:options`), and each
  `cloud.option.<key>` is sent inside that map, as BrowserStack and Sauce Labs require. Each session is named after
  the test class (`cloud.session-name-option`) so you can find it on the dashboard.
- **Unsupported platforms**: `unsupported.reason=…` in the environment file fails fast with that message.
- **New environment** (for example your own grid or LambdaTest): create `config/env/<name>.properties` (and optionally
  `<name>-android/ios.properties`); no code changes are needed. `ShippedEnvironmentsTest` checks on every build that
  the bundled environment × platform combinations produce correct capabilities (add yours there).

### Secrets and `.gitignore`

The `.gitignore` (at the root; in Finder, dotfiles are shown with `Cmd + Shift + .`) excludes `.env` and its variants
(`.env.example` is committed), certificates and keystores, `build/`, `.gradle/`, `node_modules/`, Allure results,
logs, the app binaries (`apps/`, `*.apk`, `*.ipa`), IDE folders and OS files. `gradle-wrapper.jar` is committed.

## Configuration

Files live in `src/test/resources/config/`. Any key can be overridden without editing files; the E2E tasks forward
the framework's `-P` properties to the test JVM (the first one found wins):

| Priority | Source | Example |
|---|---|---|
| 1 | `-P<platform>.<key>` | `./gradlew e2eIos -Pios.device.name="iPhone 17 Pro"` |
| 2 | `-P<key>` | `./gradlew e2e -Ptimeout.explicit.seconds=30` |
| 3 | `<PLATFORM>_<KEY>` (env or `.env`) | `ANDROID_DEVICE_UDID=emulator-5554` |
| 4 | `<KEY>` (env or `.env`) | `APPIUM_SERVER_URL=http://grid:4723` |
| 5 | `config/env/<environment>-<platform>.properties` | `env/saucelabs-ios.properties` |
| 6 | `config/env/<environment>.properties` | `env/browserstack.properties` |
| 7 | `config/<platform>.properties` | |
| 8 | `config/framework.properties` | |

- **Extra capabilities** without code changes: `capability.<name>=<value>` (sent as `appium:<name>`), for example `capability.avd=Pixel_9_API_36` so Appium boots the emulator.
- **iOS without a configured simulator**: if `device.udid` and `platform.version` are empty, the framework picks one with `xcrun simctl` (a booted one first; otherwise `device.name` on the newest runtime).
- **Navigation**: `navigation.strategy=tabbar` (through the UI, default) or `deeplink` (faster).
- **Typed settings**: `FrameworkSettings` turns the Appium server, timeout and navigation keys into typed records
  (`ServerSettings`, `TimeoutSettings`, `NavigationSettings`) and validates them before any session opens: an unknown
  `navigation.strategy`, a zero timeout or a polling interval longer than the timeout fails immediately with the key
  name in the message.

## Flaky tests (retries)

E2E tasks re-run a failed test once (Gradle Test Retry plugin). A test that passes on the second attempt does not
break the build, but it shows up as *flaky* in the Gradle report and as a retried test in Allure, so it is never
hidden. If more than 5 tests fail in a run, nothing is re-run: that is a real problem, not flakiness.

```bash
./gradlew e2eAndroid -Pretries=2    # up to 2 re-runs per failed test
./gradlew e2eIos -Pretries=0        # no retries (for example while debugging a locator)
```

Unit tests are never retried: they must be deterministic.

## Code quality

| Command | What it does |
|---|---|
| `./gradlew spotlessApply` | Formats the code (Eclipse JDT, 4 spaces, 120 columns; settings in `gradle/eclipse-java-formatter.prefs`) |
| `./gradlew spotlessCheck` | Fails if any file is not formatted (part of `check` / `build`) |
| `./gradlew compileJava` | Error Prone + NullAway run inside the compiler: a bug pattern or a possible `NullPointerException` breaks the build |
| `./gradlew test` | Unit tests + JaCoCo report in `build/reports/jacoco/test/html/index.html` |

**Null safety**: code in `com.reto.qa` is non-null by default. A value that can be null must be marked with
`org.jspecify.annotations.Nullable`; NullAway checks every use of it at compile time. Page Factory fields
(`@AndroidFindBy` / `@iOSXCUITFindBy`) are excluded because they are injected by reflection.

## SonarQube Cloud

Error Prone and NullAway stop bugs at compile time; SonarQube Cloud adds what they do not cover: code smells and
maintainability, duplication, security hotspots, history and a **Quality Gate** on every pull request.

- **CI**: the `unit` job runs `./gradlew sonar -Dsonar.qualitygate.wait=true` after `build`. If the Quality Gate fails
  (for example coverage on new code drops or a new bug appears), the job fails and the PR shows it.
- **Coverage**: JaCoCo's XML report from the unit tests. Device-only code (pages, platform actions, navigation, the
  session factory) is excluded from the coverage metric because only the E2E suite exercises it; it is still analyzed.
- **Locally**: `SONAR_TOKEN=<token> ./gradlew build sonar`.

One-time setup on sonarcloud.io:

1. Sign in with GitHub and import the `Dyplox/Auto_Android_iOS` repository (organization `dyplox`, project key
   `Dyplox_Auto_Android_iOS`; if yours differ, update them in `build.gradle.kts` and in the badges above).
2. *Administration › Analysis Method*: turn **Automatic Analysis off** (it conflicts with the CI analysis).
3. *My Account › Security*: generate a token and save it in GitHub as the repository secret `SONAR_TOKEN`
   (*Settings › Secrets and variables › Actions*).
4. Optional: in GitHub *Settings › Branches*, require the `unit` check before merging into `main`.

## Dependency updates

`.github/dependabot.yml` opens weekly pull requests for Gradle (version catalog, plugins, wrapper), npm (Appium and its
drivers) and GitHub Actions. Related libraries are grouped so they move together, for example java-client with
Selenium (they must stay on the official compatibility matrix) and JUnit's modules. The `unit` CI job validates every
pull request before it is merged.

Gradle dependencies are locked (`*.lockfile`) and every artifact is checked against its SHA-256 in
`gradle/verification-metadata.xml`, so a tampered download fails the build. Dependabot does not update those files: on
each Gradle pull request, check out its branch, run `./scripts/update-dependencies.sh`, review the diff (new checksums
should only appear for the versions being bumped) and push the result.

## Report

```bash
npm run report        # builds build/allure-report from build/allure-results and opens it
```

It includes business steps (`@Step` on the pages), severity, epic/feature and, on every failure, a screenshot and the
page source. The PNGs are also saved to `build/screenshots/<platform>/` and Gradle's HTML report to
`build/reports/tests/<task>/`. Unit tests write to a separate folder (`build/allure-results-unit`) so they don't mix.

## Adding a new screen (e.g. Swipe)

1. `Screen`: add `SWIPE("Swipe", "swipe")`.
2. `TabBar`: add the field with `@AndroidFindBy` + `@iOSXCUITFindBy` and the `case SWIPE`.
3. Create `SwipePage extends BasePage` with its locators (both annotations) and business methods with `@Step`.
4. Write the test once in a `@MobileTest` class: `app.open(Screen.SWIPE, SwipePage::new)`.

To inspect locators, use [Appium Inspector](https://github.com/appium/appium-inspector) against each platform.

## CI (GitHub Actions)

`.github/workflows/mobile-tests.yml` (with `gradle/actions/setup-gradle`, which also validates `gradle-wrapper.jar`):

- **unit** (ubuntu): `./gradlew build` on every push/PR (static analysis, formatting, unit tests and coverage; the
  reports are uploaded as artifacts), then the SonarQube Cloud analysis and Quality Gate.
- **E2E jobs are manual only** (*Actions › mobile-tests › Run workflow*, pick a `target`). They need an emulator, a
  simulator or a cloud account, and they use many Actions minutes (on private repositories each macOS minute counts
  10x), so pushes and PRs never run them:
  - `android-emulator` → **android-e2e** (ubuntu + KVM emulator) and `ios-simulator` → **ios-e2e** (macOS +
    simulator): `npm ci`, app download and `./gradlew e2eAndroid|e2eIos -Penv=ci`. They publish Allure results,
    screenshots, the Gradle report and the Appium log as artifacts.
  - `cloud` → **cloud-e2e**: pick the environment (`saucelabs` / `browserstack`) and platform; it uploads the app and
    runs the suite in the cloud. Requires the repository secrets `SAUCE_USERNAME`, `SAUCE_ACCESS_KEY`,
    `BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY` (and the optional `SAUCE_REGION` variable).

## Javadoc

Every public type and method in `src/main` is documented. Generate the HTML with `./gradlew javadoc`
(output in `build/docs/javadoc/`).

## Notes

- **Appium 4** was adopted right after its release, for study purposes. Compared with Appium 3 it only removed old
  JSONWP endpoints that this framework does not use (everything goes through W3C and `mobile:` commands). java-client
  11 officially supports Appium 3 onwards; if you hit an incompatibility, go back to Appium 3 by setting in
  `package.json` `appium` 3.9.1, `appium-uiautomator2-driver` 8.7.3 and `appium-xcuitest-driver` 12.22.8, then run
  `npm install`.
- The locators were taken from the app's v2.2.0 source code (`testID`/`accessibilityLabel`) and from the official
  WebdriverIO boilerplate, which tests it on both platforms.
- There is one session per test class, and a platform's tests run sequentially (a device runs one test at a time).
  For several devices of the same platform, set `device.udid` and one port per device
  (`capability.systemPort` / `capability.wdaLocalPort`).
