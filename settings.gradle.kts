pluginManagement {
    resolutionStrategy {
        eachPlugin {
            // Hint from https://github.com/jitpack/jitpack.io/issues/1459#issuecomment-1279851731
            // Updated solution at https://github.com/foodiestudio/convention-plugins?tab=readme-ov-file#convention-plugins
            if (requested.id.id.startsWith("com.github.koppor")) {
                // This is https://github.com/java9-modularity/gradle-modules-plugin/pull/282
                useModule("com.github.koppor:gradle-modules-plugin:1.8.15-cmd-1")
            }
        }
    }

    repositories {
        maven {
            url = uri("https://jitpack.io")
        }
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "JabRef"

include("jablib", "jabkit", "jabgui", "jabsrv", "jabsrv-cli", "test-support")
