plugins {
    id("org.jabref.gradle.module")
    id("org.jabref.gradle.feature.shadowjar")
    id("application")
}

group = "org.jabref.jabkit"
version = providers.gradleProperty("projVersion")
    .orElse(providers.environmentVariable("VERSION"))
    .orElse("100.0.0")
    .get()

testModuleInfo {
    requires("org.jabref.testsupport")
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.mockito")
    requires("com.google.common")
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "4g"
}

application {
    mainClass.set("org.jabref.toolkit.JabKitLauncher")
    mainModule.set("org.jabref.jabkit")

    // Also passed to launcher by java-module-packaging plugin
    applicationDefaultJvmArgs = listOf(
        // JEP 158: Disable all java util logging
        "-Xlog:disable",

        "--enable-native-access=com.sun.jna,javafx.graphics,org.apache.lucene.core",

        // "-XX:+UseZGC", "-XX:+ZUncommit",
        // "-XX:+UseG1GC",
        "-XX:+UseSerialGC",
        // "-XX:+UseStringDeduplication",

        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders"
    )
}

javaModulePackaging {
    applicationName = "jabkit"
    addModules.add("jdk.incubator.vector")

    // general jLinkOptions are set in org.jabref.gradle.base.targets.gradle.kts

    // All targets have to have "app-image" as sole target, since we do not distribute an installer
    targetsWithOs("windows") {
        appImageOptions.addAll("--win-console")
        packageTypes = listOf("app-image")
    }
    targetsWithOs("linux") {
        options.addAll(
            "--icon", "$projectDir/../jabgui/src/main/resources/icons/JabRef-linux-icon-64.png",
        )
        packageTypes = listOf("app-image")
    }
    targetsWithOs("macos") {
        packageTypes = listOf("app-image")
    }
}

val app = the<JavaApplication>()
tasks.register<JavaExec>("runJabKitPortableSmokeTest") {
    group = "test"
    description = "Runs JabKit from test resources dir"
    mainClass = "org.jabref.toolkit.JabKitLauncher"
    mainModule.set("org.jabref.jabkit")
    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs(app.applicationDefaultJvmArgs)
    workingDir = file("src/test/resources")
    args("--debug", "check-consistency", "--input=empty.bib")
}
