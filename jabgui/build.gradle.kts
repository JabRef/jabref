import org.gradle.internal.os.OperatingSystem

plugins {
    id("buildlogic.java-common-conventions")

    application

    // Do not activate; causes issues with the modularity plugin (no tests found etc)
    // id("com.redock.classpathtofile") version "0.1.0"
}

group = "org.jabref"
version = project.findProperty("projVersion") ?: "100.0.0"

val luceneVersion = "10.2.1"
val pdfbox = "3.0.5"

val javafxVersion = "24.0.1"

dependencies {
    implementation(project(":jablib"))

    implementation("org.openjfx:javafx-base:$javafxVersion")
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-fxml:$javafxVersion")
    // implementation("org.openjfx:javafx-graphics:24.0.1")
    implementation("org.openjfx:javafx-graphics:$javafxVersion")
    implementation("org.openjfx:javafx-swing:$javafxVersion")
    implementation("org.openjfx:javafx-web:$javafxVersion")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.tinylog:tinylog-api:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-materialdesign2-pack:12.4.0")
    implementation("com.github.sialcasa.mvvmFX:mvvmfx-validation:f195849ca9") //jitpack
    implementation("de.saxsys:mvvmfx:1.8.0")
    implementation("org.fxmisc.flowless:flowless:0.7.4")
    implementation("org.fxmisc.richtext:richtextfx:0.11.5")
    implementation("com.dlsc.gemsfx:gemsfx:3.1.1") {
        exclude(module = "javax.inject") // Split package, use only jakarta.inject
        exclude(module = "commons-lang3")
        exclude(group = "org.apache.commons.validator")
        exclude(group = "org.apache.commons.commons-logging")
        exclude(module = "kotlin-stdlib-jdk8")
        exclude(group = "com.squareup.retrofit2")
        exclude(group = "org.openjfx")
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "tech.units")
    }
    implementation("com.dlsc.pdfviewfx:pdfviewfx:3.1.1") {
        exclude(group = "org.openjfx")
        exclude(module = "commons-lang3")
    }

    // Required by gemsfx
    implementation("tech.units:indriya:2.2.3")
    // Required by gemsfx and langchain4j
    implementation ("com.squareup.retrofit2:retrofit:3.0.0") {
        exclude(group = "com.squareup.okhttp3")
    }

    implementation("org.controlsfx:controlsfx:11.2.2")
    implementation("org.jabref:easybind:2.2.1-SNAPSHOT") {
        exclude(group = "org.openjfx")
    }

    implementation("org.apache.lucene:lucene-core:${luceneVersion}")
    implementation("org.apache.lucene:lucene-queryparser:${luceneVersion}")
    implementation("org.apache.lucene:lucene-queries:${luceneVersion}")
    implementation("org.apache.lucene:lucene-analysis-common:${luceneVersion}")
    implementation("org.apache.lucene:lucene-highlighter:${luceneVersion}")

    implementation("org.jsoup:jsoup:1.20.1")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify:1.0.0")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("dev.langchain4j:langchain4j:1.0.1")

    implementation("io.github.java-diff-utils:java-diff-utils:4.15")

    implementation("org.jooq:jool:0.9.15")

    implementation("commons-io:commons-io:2.19.0")

    implementation ("org.apache.pdfbox:pdfbox:$pdfbox") {
        exclude(group = "commons-logging")
    }

    // implementation("net.java.dev.jna:jna:5.16.0")
    implementation("net.java.dev.jna:jna-platform:5.17.0")

    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.1.202505142326-r")

    implementation("com.konghq:unirest-java-core:4.4.7")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")

    implementation("com.vladsch.flexmark:flexmark-html2md-converter:0.64.8")

    implementation("io.github.adr:e-adr:2.0.0-SNAPSHOT")

    implementation("org.libreoffice:unoloader:24.8.4")
    implementation("org.libreoffice:libreoffice:24.8.4")

    implementation("com.github.javakeyring:java-keyring:1.0.4")

    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    implementation("de.undercouch:citeproc-java:3.3.0") {
        exclude(group = "org.antlr")
    }

    testImplementation(project(":test-support"))

    testImplementation("io.github.classgraph:classgraph:4.8.179")
    testImplementation("org.testfx:testfx-core:4.0.16-alpha")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")

    testImplementation("org.mockito:mockito-core:5.18.0") {
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    testImplementation("net.bytebuddy:byte-buddy:1.17.5")

    // recommended by https://github.com/wiremock/wiremock/issues/2149#issuecomment-1835775954
    testImplementation("org.wiremock:wiremock-standalone:3.12.1")

    testImplementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
}

application {
    mainClass.set("org.jabref.Launcher")
    mainModule.set("org.jabref")

    applicationDefaultJvmArgs = listOf(
        // On a change here, also adapt
        //   1. "run > moduleOptions"
        //   2. "binaries.yml" (macOS part)

        // Note that the arguments are cleared for the "run" task to avoid messages like "WARNING: Unknown module: org.jabref.merged.module specified to --add-exports"

        // Enable JEP 450: Compact Object Headers
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders",

        "-XX:+UseZGC", "-XX:+ZUncommit",
        "-XX:+UseStringDeduplication",

        // Fix for https://github.com/JabRef/jabref/issues/11188
        "--add-exports=javafx.base/com.sun.javafx.event=org.jabref.merged.module",
        "--add-exports=javafx.controls/com.sun.javafx.scene.control=org.jabref.merged.module",

        // Fix for https://github.com/JabRef/jabref/issues/11198
        "--add-opens=javafx.graphics/javafx.scene=org.jabref.merged.module",
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref.merged.module",
        "--add-opens=javafx.controls/com.sun.javafx.scene.control=org.jabref.merged.module",
        // fix for https://github.com/JabRef/jabref/issues/11426
        "--add-opens=javafx.controls/javafx.scene.control.skin=org.jabref.merged.module",

        // Fix for https://github.com/JabRef/jabref/issues/11225 on linux
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref",
        "--add-exports=javafx.base/com.sun.javafx.event=org.jabref",
        "--add-exports=javafx.controls/com.sun.javafx.scene.control=org.jabref",
        "--add-opens=javafx.graphics/javafx.scene=org.jabref",
        "--add-opens=javafx.controls/javafx.scene.control=org.jabref",
        "--add-opens=javafx.controls/com.sun.javafx.scene.control=org.jabref",

        "--add-opens=javafx.base/javafx.collections=org.jabref",
        "--add-opens=javafx.base/javafx.collections.transformation=org.jabref",

        "--enable-native-access=org.jabref.merged.module,ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core"
    )
}

/*
jacoco {
    toolVersion = "0.8.13"
}
*/

tasks.named<JavaExec>("run") {
    // "assert" statements in the code should activated when running using gradle
    enableAssertions = true

    doFirst {
        // Clear the default JVM arguments to avoid warnings
        // application.applicationDefaultJvmArgs = emptyList()
        application.applicationDefaultJvmArgs =
            listOf(
                "--enable-native-access=ai.djl.tokenizers,ai.djl.pytorch_engine,com.sun.jna,javafx.graphics,javafx.media,javafx.web,org.apache.lucene.core"
            )
    }
}

javaModuleTesting.whitebox(testing.suites["test"]) {
    requires.add("org.junit.jupiter.api")
    requires.add("org.junit.jupiter.params")
    requires.add("org.mockito")
    requires.add("org.jabref.testsupport")
}

tasks.test {
    jvmArgs = listOf(
        "--add-opens", "javafx.graphics/com.sun.javafx.application=org.testfx",
        "--add-reads", "org.mockito=java.prefs",
        "--add-reads", "org.mockito=javafx.scene",
    )
}
