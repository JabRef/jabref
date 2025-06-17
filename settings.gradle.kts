pluginManagement {
    includeBuild("build-logic")

    // get jitpack running
    // see https://github.com/jitpack/jitpack.io/issues/1459
    resolutionStrategy {
        eachPlugin {
            requested.apply {
                // com.github.andygoossens is non-jitpack; therefore stronger id check
                if ("$id".startsWith("com.github.koppor")) {
                    val (_, _, user, name) = "$id".split(".", limit = 4)
                    useModule("com.github.$user:$name:$version")
                }
            }
        }
    }
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "JabRef"

include("jablib", "jabkit", "jabgui", "jabsrv", "jabsrv-cli", "test-support", "versions")

// https://github.com/gradlex-org/java-module-dependencies#plugin-dependency
includeBuild(".")
