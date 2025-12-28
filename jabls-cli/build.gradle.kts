import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.jabref.gradle.module")
    id("application")
}

group = "org.jabref.languageserver"
version = project.findProperty("projVersion") ?: "100.0.0"

application{
    mainClass.set("org.jabref.languageserver.cli.ServerCli")
    mainModule.set("org.jabref.jabls.cli")

    applicationDefaultJvmArgs = listOf(
        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",

        "-XX:+UseZGC", "-XX:+ZUncommit",
        "-XX:+UseStringDeduplication",

        "--enable-native-access=com.sun.jna,org.apache.lucene.core"
    )
}

dependencies {
    implementation(project(":jablib"))
    implementation(project(":jabls"))

    implementation("org.openjfx:javafx-controls")
    implementation("org.openjfx:javafx-fxml")
    implementation("org.jabref:afterburner.fx")

    implementation("org.slf4j:slf4j-api")
    implementation("org.tinylog:slf4j-tinylog")
    implementation("org.tinylog:tinylog-impl")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j")
    implementation("info.picocli:picocli")
    annotationProcessor("info.picocli:picocli-codegen")
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
