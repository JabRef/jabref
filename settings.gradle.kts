pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "JabRef"

include("jablib", "jabkit", "jabgui", "jabsrv", "jabsrv-cli", "test-support")

// https://github.com/gradlex-org/java-module-dependencies#plugin-dependency
includeBuild(".")
