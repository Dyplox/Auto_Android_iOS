import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
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
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial", "-parameters"))
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
