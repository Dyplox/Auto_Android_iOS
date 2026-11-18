import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.testretry.TestRetryTaskExtension

plugins {
    java
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.test.retry)
    alias(libs.plugins.sonarqube)
}

group = "com.reto.qa"
version = "1.0.0-SNAPSHOT"
description = "One test suite for Android and iOS: the only thing that changes per platform is the locators."

// ---------------------------------------------------------------------------------------------
// Dependencies (versions in gradle/libs.versions.toml)
// ---------------------------------------------------------------------------------------------
val aspectjAgent = configurations.dependencyScope("aspectjAgent")
val aspectjAgentClasspath = configurations.resolvable("aspectjAgentClasspath") {
    extendsFrom(aspectjAgent.get())
}

dependencies {
    // Framework core (src/main)
    implementation(libs.appium.java.client)
    implementation(libs.selenium.api)
    implementation(libs.slf4j.api)
    // @Nullable marks the few values that can be null; everything else is non-null (checked by NullAway)
    implementation(libs.jspecify)
    implementation(platform(libs.allure.bom))
    implementation(libs.allure.java.commons)

    // Tests (src/test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.allure.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.logback.classic)

    // -javaagent so Allure records the @Step methods of the Page Objects
    "aspectjAgent"(libs.aspectj.weaver)

    // Static analysis at compile time
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

// ---------------------------------------------------------------------------------------------
// Java toolchain: compilation, tests and Javadoc run on JDK 27, whatever JDK runs Gradle itself
// (Gradle 9.8+ supports Java 27). If JDK 27 is not installed, the Foojay plugin in settings.gradle.kts
// downloads it.
// ---------------------------------------------------------------------------------------------
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -XDaddTypeAnnotationsToSymbol: lets Error Prone/NullAway read type-use annotations such as @Nullable
    options.compilerArgs.addAll(
        listOf("-Xlint:all,-processing,-serial", "-parameters", "-XDaddTypeAnnotationsToSymbol=true")
    )
}

// ---------------------------------------------------------------------------------------------
// Static analysis: Error Prone (common bug patterns, errors fail the build) + NullAway (null safety).
// Code in com.reto.qa is non-null by default; only values marked @Nullable (JSpecify) may be null.
// ---------------------------------------------------------------------------------------------
tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode = true
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "com.reto.qa")
        // Page Factory injects these fields by reflection after the constructor runs
        option(
            "NullAway:ExcludedFieldAnnotations",
            "io.appium.java_client.pagefactory.AndroidFindBy,io.appium.java_client.pagefactory.iOSXCUITFindBy"
        )
    }
}

// Tests use null on purpose (for example in @CsvSource), so NullAway only checks the framework (src/main).
tasks.compileTestJava {
    options.errorprone.disable("NullAway")
}

// ---------------------------------------------------------------------------------------------
// Formatting: ./gradlew spotlessApply formats the code; spotlessCheck (part of check/build) fails if it is not.
// Eclipse JDT formatter (settings in gradle/eclipse-java-formatter.prefs): unlike google/palantir-java-format it does
// not use javac internals, so it works on JDK 27.
// ---------------------------------------------------------------------------------------------
spotless {
    java {
        target("src/*/java/**/*.java")
        eclipse().configFile("gradle/eclipse-java-formatter.prefs")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ---------------------------------------------------------------------------------------------
// Coverage of the framework unit tests: build/reports/jacoco/test/html/index.html
// ---------------------------------------------------------------------------------------------
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// ---------------------------------------------------------------------------------------------
// SonarQube Cloud: ./gradlew build sonar (needs the SONAR_TOKEN environment variable; CI runs it on every push/PR).
// Project key and organization must match the project on sonarcloud.io.
// ---------------------------------------------------------------------------------------------
sonar {
    properties {
        property("sonar.projectKey", "Dyplox_Auto_Android_iOS")
        property("sonar.organization", "dyplox")
        property("sonar.projectName", "Auto_Android_iOS")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile.absolutePath
        )
        // Code that only runs against a real device: the E2E suite covers it, but E2E coverage is not measured.
        // Excluded from the coverage metric only; it is still analyzed for bugs, smells and security.
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/app/**", "**/core/**", "**/navigation/**", "**/pages/**",
                "**/driver/LocalAppiumServer.java", "**/driver/MobileSession.java",
                "**/driver/MobileSessionFactory.java", "**/driver/AndroidPlatformProvider.java",
                "**/driver/IosPlatformProvider.java"
            ).joinToString(",")
        )
    }
}

tasks.named("sonar") {
    dependsOn(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

// ---------------------------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------------------------
tasks.withType<Test>().configureEach {
    val agent: FileCollection = aspectjAgentClasspath.get()
    jvmArgumentProviders += CommandLineArgumentProvider { listOf("-javaagent:${agent.singleFile.absolutePath}") }
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

/** ./gradlew test (and check/build): framework unit tests, with no Appium or devices. */
tasks.test {
    description = "Framework unit tests (no Appium or devices required)."
    useJUnitPlatform {
        excludeTags("e2e")
    }
    systemProperty("allure.results.directory", layout.buildDirectory.dir("allure-results-unit").get().asFile.absolutePath)
    finalizedBy(tasks.jacocoTestReport)
}

val cleanAllureResults = tasks.register<Delete>("cleanAllureResults") {
    description = "Deletes the Allure results of the previous E2E run."
    delete(layout.buildDirectory.dir("allure-results"))
}

/**
 * Framework properties that can be overridden from the command line and are passed to the test JVM:
 * ./gradlew e2eIos -Penv=saucelabs -Pios.device.name="iPhone 17 Simulator" -Ptags=smoke
 */
val frameworkPropertyPrefixes = listOf(
    "appium.", "device.", "platform.", "app.", "timeout.", "session.", "navigation.", "capability.", "cloud.",
    "android.", "ios."
)
val tagExpression = providers.gradleProperty("tags")
/** Execution environment (local, ci, browserstack, saucelabs). Without -Penv, TEST_ENV, .env or "local" is used. */
val testEnvironment = providers.gradleProperty("env")
/** How many times a failed E2E test is re-run (-Pretries=0 turns retries off). */
val e2eRetries = providers.gradleProperty("retries").map(String::toInt).orElse(1)

/** One E2E task per platform: same code and same test classes, each in its own JVM. */
val e2eTasks = listOf("android", "ios").map { platform ->
    tasks.register<Test>("e2e${platform.replaceFirstChar(Char::titlecase)}") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "E2E tests on $platform (require Appium and an emulator/simulator)."
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags(tagExpression.map { "e2e & ($it)" }.getOrElse("e2e"))
        }
        systemProperty("platform", platform)
        testEnvironment.orNull?.let { systemProperty("test.env", it) }
        // One device per platform: the tests in a JVM run sequentially.
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")
        frameworkPropertyPrefixes.forEach { prefix ->
            systemProperties(providers.gradlePropertiesPrefixedBy(prefix).get())
        }
        // Mobile E2E tests can fail for reasons outside the app (WebDriverAgent start-up, emulator lag): a failed
        // test is re-run once. It passes as "flaky" (visible in the Gradle and Allure reports), never silently.
        // If more than 5 tests fail, nothing is re-run: that is a real problem, not flakiness.
        extensions.configure<TestRetryTaskExtension> {
            maxRetries = e2eRetries
            maxFailures = 5
            failOnPassedAfterRetry = false
        }
        // Coverage is measured on the unit tests only
        extensions.configure<JacocoTaskExtension> { isEnabled = false }
        dependsOn(cleanAllureResults)
        shouldRunAfter(tasks.test)
        // An E2E test depends on an external system: it is never "up to date" and never reused from the cache.
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
    }
}

tasks.register("e2e") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "E2E tests on Android and iOS in parallel (configuration cache)."
    dependsOn(e2eTasks)
}
