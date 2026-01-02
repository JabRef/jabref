plugins {
    id("org.jabref.gradle.module")
    id("application")
}

group = "org.jabref.jabkit"
version = project.findProperty("projVersion") ?: "100.0.0"


dependencies {
    implementation(project(":jablib"))

    // FIXME: Injector needs to be removed, no JavaFX dependencies, etc.
    implementation("org.jabref:afterburner.fx")

    implementation("org.openjfx:javafx-base")
    implementation("org.openjfx:javafx-controls")
    implementation("org.openjfx:javafx-fxml")
    // implementation("org.openjfx:javafx-graphics:$javafxVersion")

    implementation("info.picocli:picocli")
    annotationProcessor("info.picocli:picocli-codegen")

    implementation("com.github.ben-manes.caffeine:caffeine")
    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify")

    implementation("org.slf4j:slf4j-api")
    // implementation("org.tinylog:tinylog-api")
    implementation("org.tinylog:slf4j-tinylog")
    implementation("org.tinylog:tinylog-impl")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j")

    implementation("com.google.guava:guava")

    implementation("org.slf4j:slf4j-api")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog in the CLI and GUI)
    implementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j")

    implementation("org.jabref:afterburner.fx")

    implementation("org.apache.lucene:lucene-queryparser")

    implementation("io.github.adr:e-adr")

    implementation("io.github.darvil82:terminal-text-formatter")

    testImplementation(project(":test-support"))
    testImplementation("org.mockito:mockito-core")
    testImplementation("net.bytebuddy:byte-buddy")
}

javaModuleTesting.whitebox(testing.suites["test"]) {
    requires.add("org.jabref.testsupport")
    requires.add("org.junit.jupiter.api")
    requires.add("org.junit.jupiter.params")
    requires.add("org.mockito")
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

        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",

        // Default garbage collector is sufficient for CLI APP
        // "-XX:+UseZGC", "-XX:+ZUncommit",
        // "-XX:+UseStringDeduplication",

        "--enable-native-access=com.sun.jna,javafx.graphics,org.apache.lucene.core"
    )
}

javaModulePackaging {
    applicationName = "jabkit"
    addModules.add("jdk.incubator.vector")
    jlinkOptions.addAll("--generate-cds-archive")

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
