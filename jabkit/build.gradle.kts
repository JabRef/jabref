plugins {
    id("buildlogic.java-common-conventions")

    application
}

group = "org.jabref.jabkit"
version = project.findProperty("projVersion") ?: "100.0.0"

val luceneVersion = "10.2.1"

val javafxVersion = "24.0.1"

dependencies {
    implementation(project(":jablib"))

    // FIXME: Injector needs to be removed, no JavaFX dependencies, etc.
    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }

    implementation("org.openjfx:javafx-base:$javafxVersion")
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-fxml:$javafxVersion")
    // implementation("org.openjfx:javafx-graphics:$javafxVersion")

    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify:1.0.0")

    implementation("org.slf4j:slf4j-api:2.0.17")
    // implementation("org.tinylog:tinylog-api:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("org.slf4j:slf4j-api:2.0.17")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog in the CLI and GUI)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }

    implementation("commons-cli:commons-cli:1.9.0")

    implementation("org.apache.lucene:lucene-queryparser:${luceneVersion}")

    implementation("io.github.adr:e-adr:2.0.0-SNAPSHOT")

    testImplementation(project(":test-support"))
    testImplementation("org.mockito:mockito-core:5.18.0") {
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    testImplementation("net.bytebuddy:byte-buddy:1.17.5")
}

javaModuleTesting.whitebox(testing.suites["test"]) {
    requires.add("org.junit.jupiter.api")
    requires.add("org.jabref.testsupport")
    requires.add("org.mockito")
}

/*
jacoco {
    toolVersion = "0.8.13"
}
*/

application {
    mainClass.set("org.jabref.JabKit")
    mainModule.set("org.jabref.jabkit")

    // Also passed to launcher (https://badass-jlink-plugin.beryx.org/releases/latest/#launcher)
    applicationDefaultJvmArgs = listOf(
        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",

        // Default garbage collector is sufficient for CLI APP
        // "-XX:+UseZGC", "-XX:+ZUncommit",
        // "-XX:+UseStringDeduplication",

        "--enable-native-access=com.sun.jna,javafx.graphics,org.apache.lucene.core"
    )
}
