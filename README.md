# Automatización móvil multiplataforma con Appium (Android + iOS)

Un único proyecto de pruebas para **la misma app en Android e iOS**, construido con **Gradle 9** y **JUnit 5**.
Los Page Objects, los tests y los datos son compartidos; **lo único que cambia por plataforma son los locators**,
declarados con Page Factory (`@AndroidFindBy` / `@iOSXCUITFindBy`) y resueltos en tiempo de ejecución según la sesión.

```java
@AndroidFindBy(uiAutomator = "new UiSelector().text(\"Please enter a valid email address\")")
@iOSXCUITFindBy(iOSNsPredicate = "label == 'Please enter a valid email address'")
private WebElement invalidEmailError;
```

```java
@MobileTest                                   // sesión de Appium + evidencia en fallos + @Tag("e2e")
class LoginTest {

    @Test
    void shouldLoginWithValidCredentials(WdioDemoApp app) {      // la app se inyecta (DIP)
        LoginPage loginPage = app.open(Screen.LOGIN, LoginPage::new);
        loginPage.loginWith(CredentialsMother.valid());
        assertThat(app.alert().waitUntilVisible().title()).contains("Success");
    }
}
```

El mismo test corre en Android con `./gradlew e2eAndroid`, en iOS con `./gradlew e2eIos` y en ambos a la vez con
`./gradlew e2e`, en cualquier ambiente: local, CI, BrowserStack o Sauce Labs (`-Penv=…`).

---

## App bajo prueba

**[WebdriverIO Native Demo App](https://github.com/webdriverio/native-demo-app) v2.2.0** (MIT): una sola app
React Native/Expo publicada para **Android (.apk)** y **iOS Simulator (.app.zip)**, con login/registro con
validaciones, formularios, alertas nativas, swipe, drag & drop, deep links (`wdio://`) y webview.

| | Android | iOS |
|---|---|---|
| Archivo | `android.wdio.native.app.v2.2.0.apk` | `ios.simulator.wdio.native.app.v2.2.0.zip` |
| appId | `com.wdiodemoapp` | `org.wdiodemoapp` |
| Dónde corre | Emulador o dispositivo real | **Solo simulador** (no hay build para iPhone físico) |

> **¿Y Firebase Test Lab?** Test Lab no ejecuta Appium: en Android corre Espresso / UI Automator, Robo y Game Loop,
> y en iOS XCTest, Robo y Game Loop. Para correr *este* proyecto en la nube se usa un proveedor con soporte Appium
> (BrowserStack, Sauce Labs, LambdaTest, AWS Device Farm…): basta con apuntar `appium.server.url` al hub del
> proveedor y `app.path` a la app subida (`storage:…`, `bs://…`, URL); el código no cambia.
>
> La alternativa clásica, *Sauce Labs My Demo App RN*, fue archivada en 2024; por eso se eligió la de WebdriverIO,
> que sigue mantenida.

---

## Stack

| Pieza | Versión | Por qué |
|---|---|---|
| Gradle | 9.9.1 (wrapper, con checksum) | Kotlin DSL, version catalog, configuration cache (paraleliza Android e iOS) |
| Java | 17+ | Mínimo de Gradle 9 y de Allure 3; se compila con `--release 17` |
| JUnit | 5.14.4 (Jupiter) | Extensiones, inyección de parámetros, `@ParameterizedTest`, `@Tag`, `@TempDir`, ejecución paralela |
| Appium java-client | 11.0.0 | Page Factory multiplataforma, opciones tipadas (Builder), W3C Actions |
| Selenium API | 4.50.0 | Fijada según la matriz de compatibilidad oficial de java-client 11 |
| AssertJ | 3.27.7 | Aserciones fluidas y mensajes claros (Self-validating) |
| Allure | 3.1.1 (`allure-jupiter`) / 3.33.0 (CLI) | Reporte con pasos (`@Step`), screenshots y page source en fallos |
| Appium server | 4.0.0 | Última versión (15-nov-2026), fijada en `package.json` / `package-lock.json` |
| UiAutomator2 / XCUITest | 9.2.2 / 13.1.3 | Primeras líneas de drivers para Appium 4 |

Las versiones de las librerías viven en un solo lugar: [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

---

## Arquitectura

```
build.gradle.kts                 tareas: test (unitarias) · e2eAndroid · e2eIos · e2e (ambas en paralelo)
gradle/libs.versions.toml        version catalog
src/main/java/com/reto/qa
├── config/        FrameworkConfig (properties + env + -P), DeviceProfile (Builder), IosSimulatorLocator
├── driver/        PlatformProvider (Abstract Factory) · Android/IosPlatformProvider · MobileSessionFactory
│                  MobileSession · AppiumServerManager
├── core/          Waiter (esperas explícitas) · Gestures (W3C Actions) · PlatformActions (Strategy)
├── navigation/    Navigator (Strategy): TabBarNavigator | DeepLinkNavigator
├── pages/         BaseUiObject (Page Factory) · BasePage · LoginPage · FormsPage · HomePage
│   └── components TabBar · NativeAlert
├── data/          UserCredentials (Builder) · CredentialsMother (Object Mother)
└── app/           WdioDemoApp (Facade: punto de entrada de los tests)

src/test/java/com/reto/qa
├── junit/         @MobileTest · AppiumSessionExtension · EvidenceExtension
├── tests/         LoginTest · SignUpTest · FormsTest · NavigationTest          ← E2E (Android e iOS)
└── config|data|driver/  pruebas unitarias del framework (sin dispositivo)

src/test/resources
├── config/        framework.properties · android.properties · ios.properties
│   └── env/       local · ci (+ci-ios) · browserstack (+android/ios) · saucelabs (+android/ios)
└── junit-platform.properties · allure.properties · logback-test.xml

scripts/           download-apps.sh · upload-app.sh (BrowserStack / Sauce Labs)
.env.example       plantilla de credenciales; el .env real no se versiona
```

Flujo de una clase de prueba:

```
@MobileTest ─► AppiumSessionExtension.beforeAll
                 └─ MobileSessionFactory ─► DeviceProfileFactory (config ► DeviceProfile)
                                            PlatformProviders.forPlatform(ANDROID|IOS)
                                              ├─ capabilities()   UiAutomator2Options | XCUITestOptions
                                              ├─ createDriver()   AndroidDriver | IOSDriver
                                              └─ createActions()  AndroidPlatformActions | IosPlatformActions
                 └─ MobileSession guardada en el ExtensionContext.Store de la clase
             ─► beforeEach: reinicia la app ─► el test recibe WdioDemoApp por parámetro
             ─► Pages / Components (PageFactory + AppiumFieldDecorator elige @AndroidFindBy o @iOSXCUITFindBy)
             ─► afterAll: cierra la sesión
```

---

## Patrones y principios aplicados

### SOLID

| Principio | Dónde |
|---|---|
| **S** – Responsabilidad única | `FrameworkConfig` solo resuelve configuración; `DeviceProfileFactory` solo mapea; `MobileSessionFactory` solo orquesta; `AppiumSessionExtension` solo gestiona el ciclo de vida; `EvidenceExtension` solo captura evidencia; cada página modela una pantalla y cada componente (TabBar, NativeAlert) se reutiliza |
| **O** – Abierto/cerrado | Nueva plataforma o proveedor cloud = nueva implementación de `PlatformProvider` registrada en `PlatformProviders`; nueva forma de navegar = nuevo `Navigator`; nuevo comportamiento transversal = nueva extensión de JUnit. Ninguna clase existente cambia |
| **L** – Sustitución de Liskov | `AndroidPlatformActions` e `IosPlatformActions` son intercambiables; el bound genérico `D extends AppiumDriver & InteractsWithApps & HasOnScreenKeyboard` lo garantiza en compilación |
| **I** – Segregación de interfaces | Interfaces pequeñas: `Navigator` (1 método), `PlatformActions` (3), `PlatformProvider` (4) |
| **D** – Inversión de dependencias | Los tests reciben `WdioDemoApp` por parámetro y las páginas reciben `MobileSession` por constructor; dependen de abstracciones (`PlatformActions`, `Navigator`). No hay singletons, `ThreadLocal` ni clase base de tests |

### FIRST

| | Cómo se cumple |
|---|---|
| **Fast** | Una sesión por clase (no por test); reinicio de app con `terminateApp/activateApp` en vez de sesión nueva; esperas por condición, nunca `Thread.sleep`; unitarias en paralelo; Android e iOS en paralelo (`./gradlew e2e`); navegación opcional por deep link |
| **Independent** | `beforeEach` reinicia la app: cada test arranca en Home sin depender de otro; la sesión vive en el `Store` de la clase (aislada por clase); cada plataforma corre en su propia JVM; `@TempDir` en unitarias |
| **Repeatable** | Datos deterministas (`CredentialsMother`); versiones fijadas en el version catalog, el wrapper (con checksum), `package-lock.json` (Appium y drivers) y la app; misma suite en cualquier ambiente, solo cambia la configuración |
| **Self-validating** | AssertJ con mensajes descriptivos; evidencia automática (screenshot + page source) en Allure al fallar |
| **Timely** | Las unitarias del framework corren en cada `./gradlew build` y en cada PR (CI) |

### Otros patrones

- **Builder**: `DeviceProfile.builder(...)`, `UserCredentials.builder()/toBuilder()` y las opciones tipadas de Appium (`UiAutomator2Options`, `XCUITestOptions`).
- **Page Factory**: `PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ZERO), this)` en `BaseUiObject`; elementos *lazy* (se buscan en cada uso, sin `StaleElementReference`).
- **Abstract Factory** (`PlatformProvider`), **Strategy** (`PlatformActions`, `Navigator`), **Facade** (`WdioDemoApp`), **Object Mother** (`CredentialsMother`), **extensiones de JUnit 5** en lugar de herencia (`@MobileTest`).

### ¿Dónde puede haber código específico de plataforma?

Solo en dos lugares, por diseño:

1. **Los locators** (anotaciones de Page Factory). Ej.: alerta nativa → `id` en Android, `iOSNsPredicate` en iOS.
2. **`PlatformActions`**: comportamientos que el SO resuelve distinto (ocultar teclado, deep link). Los tests y
   las páginas nunca preguntan "¿estoy en iOS?". Por eso, por ejemplo, el switch se valida por su texto
   ("Click to turn the switch ON") y no por el atributo `checked` (Android) / `value` (iOS).

---

## Requisitos en tu Mac

- **JDK 17+** (`java -version`). No hace falta instalar Gradle: el wrapper (`./gradlew`) descarga la 9.9.1.
- **Node.js 22.22.2+** o 24.15+ (`node -v`; hay `.nvmrc`). Es el mínimo de Appium 4.
- **Android**: Android Studio con SDK + un emulador (AVD). Variables `ANDROID_HOME` (o `ANDROID_SDK_ROOT`) y `JAVA_HOME`.
- **iOS**: Xcode con al menos un simulador de iPhone (`xcrun simctl list devices available`).

## Puesta en marcha

```bash
npm ci                        # Appium 4.0.0 + UiAutomator2 + XCUITest + Allure (versiones fijadas)
./scripts/download-apps.sh    # descarga el .apk y el .zip del simulador a ./apps
npm run doctor:android        # verifica el entorno Android (SDK, adb, JAVA_HOME…)
npm run doctor:ios            # verifica Xcode, simuladores…

./gradlew build               # compila y corre las pruebas unitarias del framework (sin dispositivo) ✔
```

### Ejecutar E2E

| Comando | Qué hace |
|---|---|
| `./gradlew e2eAndroid` | Suite completa en Android |
| `./gradlew e2eIos` | Suite completa en iOS |
| `./gradlew e2e` | Android e iOS **en paralelo**, cada uno en su JVM (lo habilita la configuration cache) |
| `./gradlew e2eAndroid -Ptags=smoke` | Solo los tests con `@Tag("smoke")` (acepta expresiones: `-Ptags="smoke & !slow"`) |
| `./gradlew e2eIos --tests '*LoginTest'` | Una clase concreta |

Con un Appium ya levantado (`npm run appium` en otra terminal) o dejando que el framework lo arranque y lo apague:

```bash
./gradlew e2e -Pappium.server.autostart=true      # cada JVM inicia su propio Appium en un puerto libre
./gradlew e2e -Penv=saucelabs                      # o en la nube: ver "Ambientes"
```

> El primer arranque en iOS compila WebDriverAgent y puede tardar varios minutos (los timeouts ya lo contemplan).
>
> **IntelliJ**: la tarea `test` excluye los E2E. Para lanzar uno desde el IDE, ejecútalo con la tarea
> `e2eAndroid` / `e2eIos` (p. ej. `./gradlew e2eAndroid --tests '*FormsTest'`) o configura el runner de JUnit con
> `-Dplatform=android`.

## Ambientes

Un ambiente es un perfil de configuración: dónde está Appium, qué dispositivo, qué app y qué opciones del proveedor.
El código y los tests no cambian; se elige con `-Penv=…` (o `TEST_ENV` / `.env`; por defecto `local`).

| Ambiente | Appium | Dispositivos | App | Credenciales |
|---|---|---|---|---|
| `local` | `npm run appium` en tu Mac (o `-Pappium.server.autostart=true`) | Emulador / simulador local | `apps/*.apk`, `apps/*.zip` | — |
| `ci` | Lo inicia cada JVM, timeouts mayores | Emulador / simulador del runner | `apps/*` | — |
| `browserstack` | `hub-cloud.browserstack.com` | Android reales (*) | custom_id `WdioDemoApp-Android` | `BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY` |
| `saucelabs` | `ondemand.<región>.saucelabs.com` | Emuladores Android y simuladores iOS | `storage:filename=…` | `SAUCE_USERNAME`, `SAUCE_ACCESS_KEY` (+ `SAUCE_REGION`) |

(*) BrowserStack solo ofrece iPhones reales y la app demo solo tiene build de simulador: `e2eIos -Penv=browserstack`
falla de inmediato explicando el motivo. Para iOS en la nube usa Sauce Labs.

```bash
cp .env.example .env                      # completa tus credenciales (el .env no se versiona)

./gradlew e2eAndroid                      # local (por defecto)
./gradlew e2e -Penv=ci                    # como en el pipeline

./scripts/upload-app.sh saucelabs         # sube .apk y .zip a Sauce App Storage
./gradlew e2e -Penv=saucelabs             # Android e iOS en la nube, en paralelo

./scripts/upload-app.sh browserstack      # sube el .apk con custom_id WdioDemoApp-Android
./gradlew e2eAndroid -Penv=browserstack
```

Cómo está armado:

- **Capas**: `env/<ambiente>-<plataforma>.properties` > `env/<ambiente>.properties` > `<plataforma>.properties` >
  `framework.properties`. Un ambiente solo declara lo que cambia.
- **Secretos fuera del repositorio**: los valores aceptan `${VARIABLE}` y `${VARIABLE:-defecto}`, resueltos desde
  variables de entorno o `.env` (las del entorno ganan, así funciona CI). Se resuelven solo al usarse: correr en
  local no exige credenciales de la nube; si falta una, el error dice cuál.
- **Opciones del proveedor**: `cloud.options-capability=bstack:options` (o `sauce:options`) y cada
  `cloud.option.<clave>` se envía dentro de ese mapa, como exigen BrowserStack y Sauce Labs. Cada sesión se nombra
  con la clase de prueba (`cloud.session-name-option`) para encontrarla en el dashboard.
- **Plataformas no soportadas**: `unsupported.reason=…` en el archivo del ambiente hace fallar rápido con ese mensaje.
- **Nuevo ambiente** (p. ej. un grid propio o LambdaTest): crea `config/env/<nombre>.properties` (y opcionalmente
  `<nombre>-android/ios.properties`); no hace falta tocar código. `ShippedEnvironmentsTest` valida en cada build que
  las combinaciones ambiente × plataforma incluidas generan capabilities correctas (agrega ahí la tuya).

### Secretos y `.gitignore`

El `.gitignore` (en la raíz; en Finder los archivos que empiezan con punto se ven con `Cmd + Shift + .`) excluye
`.env` y variantes (`.env.example` sí se versiona), certificados y keystores, `build/`, `.gradle/`, `node_modules/`,
resultados de Allure, logs, los binarios de la app (`apps/`, `*.apk`, `*.ipa`), carpetas de IDE y archivos del sistema.
El `gradle-wrapper.jar` sí se versiona.

## Configuración

Archivos en `src/test/resources/config/`. Cualquier clave se sobrescribe sin tocar archivos; las tareas E2E
reenvían a la JVM de pruebas las propiedades `-P` del framework (gana la primera que exista):

| Prioridad | Fuente | Ejemplo |
|---|---|---|
| 1 | `-P<plataforma>.<clave>` | `./gradlew e2eIos -Pios.device.name="iPhone 17 Pro"` |
| 2 | `-P<clave>` | `./gradlew e2e -Ptimeout.explicit.seconds=30` |
| 3 | `<PLATAFORMA>_<CLAVE>` (env o `.env`) | `ANDROID_DEVICE_UDID=emulator-5554` |
| 4 | `<CLAVE>` (env o `.env`) | `APPIUM_SERVER_URL=http://grid:4723` |
| 5 | `config/env/<ambiente>-<plataforma>.properties` | `env/saucelabs-ios.properties` |
| 6 | `config/env/<ambiente>.properties` | `env/browserstack.properties` |
| 7 | `config/<plataforma>.properties` | |
| 8 | `config/framework.properties` | |

- **Capabilities extra** sin tocar código: `capability.<nombre>=<valor>` (se envía como `appium:<nombre>`), p. ej. `capability.avd=Pixel_9_API_36` para que Appium encienda el emulador.
- **iOS sin configurar simulador**: si `device.udid` y `platform.version` están vacíos, el framework elige uno con `xcrun simctl` (primero uno encendido; si no, `device.name` en el runtime más nuevo).
- **Navegación**: `navigation.strategy=tabbar` (por UI, por defecto) o `deeplink` (más rápida).

## Reporte

```bash
npm run report        # genera build/allure-report desde build/allure-results y lo abre
```

Incluye pasos de negocio (`@Step` en las páginas), severidad, épica/feature y, en cada fallo, screenshot y page
source. Los PNG también quedan en `build/screenshots/<plataforma>/` y el reporte HTML de Gradle en
`build/reports/tests/<tarea>/`. Las unitarias escriben aparte (`build/allure-results-unit`) para no mezclarse.

## Agregar una pantalla nueva (ej. Swipe)

1. `Screen`: agrega `SWIPE("Swipe", "swipe")`.
2. `TabBar`: agrega el campo con `@AndroidFindBy` + `@iOSXCUITFindBy` y el `case SWIPE`.
3. Crea `SwipePage extends BasePage` con sus locators (ambas anotaciones) y métodos de negocio con `@Step`.
4. Escribe el test una sola vez en una clase `@MobileTest`: `app.open(Screen.SWIPE, SwipePage::new)`.

Para inspeccionar locators usa [Appium Inspector](https://github.com/appium/appium-inspector) contra cada plataforma.

## CI (GitHub Actions)

`.github/workflows/mobile-tests.yml` (con `gradle/actions/setup-gradle`, que además valida el `gradle-wrapper.jar`):

- **unit** (ubuntu): `./gradlew build` en cada push/PR.
- **android-e2e** (ubuntu + emulador con KVM) y **ios-e2e** (macOS + simulador): `npm ci`, descarga de apps y
  `./gradlew e2eAndroid|e2eIos -Penv=ci`. Publican resultados de Allure, screenshots, el reporte de Gradle y el log
  de Appium como artefactos.
- **cloud-e2e** (manual: *Actions › mobile-tests › Run workflow*): elige ambiente (`saucelabs` / `browserstack`) y
  plataforma; sube la app y ejecuta la suite en la nube. Requiere los secrets del repositorio
  `SAUCE_USERNAME`, `SAUCE_ACCESS_KEY`, `BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY` (y la variable opcional
  `SAUCE_REGION`).

## Notas

- **Appium 4** se adoptó recién publicado, con fines de estudio. Respecto de Appium 3 solo eliminó endpoints
  antiguos de JSONWP que este framework no usa (todo va por W3C y comandos `mobile:`). java-client 11 declara
  soporte oficial desde Appium 3; si encuentras una incompatibilidad, vuelve a Appium 3 cambiando en `package.json`
  `appium` 3.9.1, `appium-uiautomator2-driver` 8.7.3 y `appium-xcuitest-driver` 12.22.8, y ejecuta `npm install`.

- Los locators se tomaron del código fuente de la app v2.2.0 (`testID`/`accessibilityLabel`) y del boilerplate
  oficial de WebdriverIO, que la prueba en ambas plataformas.
- La sesión es por clase de prueba y los tests de una plataforma corren en secuencia (un dispositivo ejecuta un test
  a la vez). Para varios dispositivos de la misma plataforma, define `device.udid` y un puerto por dispositivo
  (`capability.systemPort` / `capability.wdaLocalPort`).
