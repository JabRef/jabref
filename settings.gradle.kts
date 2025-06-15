pluginManagement {
    includeBuild("build-logic")
}
plugins {
    id("org.jabref.gradle.build")
}

rootProject.name = "JabRef"

javaModules {
    directory(".")
    versions("versions")
    // include("jablib", "jabkit", "jabgui", "jabsrv", "jabsrv-cli", "test-support", "versions")
}
