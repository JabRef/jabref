pluginManagement {
    repositories {
        // Same Maven Central rate-limiting workaround as in build.gradle.kts: the
        // kotlin-dsl plugin's own classpath (kotlin-stdlib, annotations, ...) resolves
        // through these repositories, not the project ones, so the mirror must be
        // declared here too or those artifacts fall back to the plugin portal alone.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }

        gradlePluginPortal()
    }
}

rootProject.name = "build-logic"
