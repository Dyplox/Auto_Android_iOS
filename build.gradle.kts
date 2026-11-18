import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
}

group = "com.reto.qa"
version = "1.0.0-SNAPSHOT"
description = "Un solo set de pruebas para Android e iOS: lo único que cambia por plataforma son los locators."

// ---------------------------------------------------------------------------------------------
// Dependencias (versiones en gradle/libs.versions.toml)
// ---------------------------------------------------------------------------------------------
val aspectjAgent = configurations.dependencyScope("aspectjAgent")
val aspectjAgentClasspath = configurations.resolvable("aspectjAgentClasspath") {
    extendsFrom(aspectjAgent.get())
}

dependencies {
    // Núcleo del framework (src/main)
    implementation(libs.appium.java.client)
    implementation(libs.selenium.api)
    implementation(libs.slf4j.api)
    implementation(platform(libs.allure.bom))
    implementation(libs.allure.java.commons)

    // Pruebas (src/test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.allure.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.logback.classic)

    // -javaagent para que Allure registre los @Step de los Page Objects
    "aspectjAgent"(libs.aspectj.weaver)
}

// ---------------------------------------------------------------------------------------------
// Compilación: bytecode y API de Java 17 con cualquier JDK 17+ (el mínimo que exige Gradle 9)
// ---------------------------------------------------------------------------------------------
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial", "-parameters"))
}

// ---------------------------------------------------------------------------------------------
// Pruebas
// ---------------------------------------------------------------------------------------------
tasks.withType<Test>().configureEach {
    val agent: FileCollection = aspectjAgentClasspath.get()
    jvmArgumentProviders += CommandLineArgumentProvider { listOf("-javaagent:${agent.singleFile.absolutePath}") }
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

/** ./gradlew test (y check/build): pruebas unitarias del framework, sin Appium ni dispositivos. */
tasks.test {
    description = "Pruebas unitarias del framework (no requieren Appium ni dispositivos)."
    useJUnitPlatform {
        excludeTags("e2e")
    }
    systemProperty("allure.results.directory", layout.buildDirectory.dir("allure-results-unit").get().asFile.absolutePath)
}

val cleanAllureResults = tasks.register<Delete>("cleanAllureResults") {
    description = "Borra los resultados de Allure de la ejecución E2E anterior."
    delete(layout.buildDirectory.dir("allure-results"))
}

/**
 * Propiedades del framework que se pueden sobrescribir desde la línea de comandos y llegan a la JVM de pruebas:
 * ./gradlew e2eIos -Penv=saucelabs -Pios.device.name="iPhone 17 Simulator" -Ptags=smoke
 */
val frameworkPropertyPrefixes = listOf(
    "appium.", "device.", "platform.", "app.", "timeout.", "session.", "navigation.", "capability.", "cloud.",
    "android.", "ios."
)
val tagExpression = providers.gradleProperty("tags")
/** Ambiente de ejecución (local, ci, browserstack, saucelabs). Sin -Penv se usa TEST_ENV, .env o "local". */
val testEnvironment = providers.gradleProperty("env")

/** Una tarea E2E por plataforma: mismo código y mismas clases de prueba, cada una en su propia JVM. */
val e2eTasks = listOf("android", "ios").map { platform ->
    tasks.register<Test>("e2e${platform.replaceFirstChar(Char::titlecase)}") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Pruebas E2E en $platform (requieren Appium y un emulador/simulador)."
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags(tagExpression.map { "e2e & ($it)" }.getOrElse("e2e"))
        }
        systemProperty("platform", platform)
        testEnvironment.orNull?.let { systemProperty("test.env", it) }
        // Un solo dispositivo por plataforma: los tests de una JVM corren en secuencia.
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")
        frameworkPropertyPrefixes.forEach { prefix ->
            systemProperties(providers.gradlePropertiesPrefixedBy(prefix).get())
        }
        dependsOn(cleanAllureResults)
        shouldRunAfter(tasks.test)
        // Una prueba E2E depende de un sistema externo: nunca se da por "al día" ni se reutiliza de la caché.
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
    }
}

tasks.register("e2e") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Pruebas E2E en Android e iOS en paralelo (configuration cache)."
    dependsOn(e2eTasks)
}
