import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.jabref.gradle.module")
    id("org.jabref.gradle.feature.shadowjar")
    id("application")
}

group = "org.jabref.languageserver"
version = providers.gradleProperty("projVersion")
    .orElse(providers.environmentVariable("VERSION"))
    .orElse("100.0.0")
    .get()

mainModuleInfo {
    annotationProcessor("info.picocli.codegen")
}

application{
    mainClass = "org.jabref.languageserver.cli.ServerCli"

    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=com.sun.jna,org.apache.lucene.core",

        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",

        "-XX:+UseStringDeduplication"
    )
}

tasks.test {
    testLogging {
        // set options for log level LIFECYCLE
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }
    maxParallelForks = 1
}

tasks.named<JavaExec>("run") {
    // "assert" statements in the code should activated when running using gradle
    enableAssertions = true
    jvmArgs("--enable-native-access=com.sun.jna")
}

javaModulePackaging {
    applicationName = "jabls"
    vendor = "JabRef"

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
