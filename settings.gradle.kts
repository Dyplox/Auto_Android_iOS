plugins {
    // Downloads the JDK 27 toolchain automatically (from Foojay / Adoptium) if it is not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mobile-cross-platform-automation"

dependencyResolutionManagement {
    // Repositories are declared only here: a single, reproducible place.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
