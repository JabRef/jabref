pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "JabRef"

include("jablib", "jabkit", "jabgui", "jabsrv", "jabsrv-cli", "test-support")

// https://github.com/gradlex-org/java-module-dependencies#plugin-dependency
includeBuild(".")
