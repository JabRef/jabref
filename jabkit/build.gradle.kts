plugins {
    id("org.jabref.gradle.module")
    id("application")

    id("org.beryx.jlink") version "3.1.1"
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

    testImplementation(project(":test-support"))
    testImplementation("org.mockito:mockito-core")
    testImplementation("net.bytebuddy:byte-buddy")
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

// This is more or less a clone of jabgui/build.gradle.kts -> jlink
jlink {
    // https://github.com/beryx/badass-jlink-plugin/issues/61#issuecomment-504640018
    addExtraDependencies(
        "javafx"
    )

    mergedModuleName = "jabkit.merged.module"

    // We keep debug statements - otherwise "--strip-debug" would be included
    addOptions(
        "--compress",
        "zip-6",
        "--no-header-files",
        "--no-man-pages",
        "--bind-services",
        "--add-modules", "jdk.incubator.vector"
    )

    launcher {
        name = "jabkit"
    }

    // TODO: Remove as soon as dependencies are fixed (upstream)
    forceMerge(
        "bcprov",
        "stax"
    )

    mergedModule {
        requires("com.google.gson")
        requires("com.fasterxml.jackson.annotation")
        requires("com.fasterxml.jackson.databind")
        requires("com.fasterxml.jackson.core")
        requires("com.fasterxml.jackson.datatype.jdk8")
        requires("java.compiler")
        requires("java.datatransfer")
        requires("java.desktop")
        requires("java.logging")
        requires("java.management")
        requires("java.naming")
        requires("java.net.http")
        requires("java.rmi")
        requires("java.scripting")
        requires("java.security.jgss")
        requires("java.security.sasl")
        requires("java.sql")
        requires("java.sql.rowset")
        requires("java.transaction.xa")
        requires("java.xml")
        requires("javafx.base")
        requires("javafx.controls")
        requires("javafx.fxml")
        requires("javafx.graphics")
        requires("jdk.security.jgss")
        requires("jdk.unsupported")
        requires("jdk.unsupported.desktop")
        requires("jdk.xml.dom")
        requires("org.apache.commons.lang3")
        requires("org.apache.commons.logging")
        requires("org.apache.commons.text")
        requires("org.apache.commons.codec")
        requires("org.apache.commons.io")
        requires("org.apache.commons.compress")
        requires("org.freedesktop.dbus")
        requires("org.jsoup")
        requires("org.slf4j")
        requires("org.tukaani.xz");
        uses("ai.djl.engine.EngineProvider")
        uses("ai.djl.repository.RepositoryFactory")
        uses("ai.djl.repository.zoo.ZooProvider")
        uses("dev.langchain4j.spi.prompt.PromptTemplateFactory")
        uses("kong.unirest.core.json.JsonEngine")
        uses("org.eclipse.jgit.lib.Signer")
        uses("org.eclipse.jgit.transport.SshSessionFactory")
        uses("org.postgresql.shaded.com.ongres.stringprep.Profile")
        provides("java.sql.Driver").with(
            "org.postgresql.Driver")
        provides("java.security.Provider").with(
            "org.bouncycastle.jce.provider.BouncyCastleProvider",
            "org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider")
        provides("kong.unirest.core.json.JsonEngine").with(
            "kong.unirest.modules.gson.GsonEngine")
        provides("ai.djl.repository.zoo.ZooProvider").with(
            "ai.djl.engine.rust.zoo.RsZooProvider",
            "ai.djl.huggingface.zoo.HfZooProvider",
            "ai.djl.pytorch.zoo.PtZooProvider",
            "ai.djl.repository.zoo.DefaultZooProvider")
        provides("ai.djl.engine.EngineProvider").with(
            "ai.djl.engine.rust.RsEngineProvider",
            "ai.djl.pytorch.engine.PtEngineProvider")

    }
    jpackage {
        outputDir = "distribution"
        skipInstaller = true

        imageOptions.addAll(listOf(
            "--java-options", "--enable-native-access=jabkit.merged.module"))

        // See https://docs.oracle.com/en/java/javase/24/docs/specs/man/jpackage.html#platform-dependent-options-for-creating-the-application-package for available options
        if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
            imageOptions.addAll(
                listOf(
                    "--win-console"
                )
            )
        } else if (org.gradle.internal.os.OperatingSystem.current().isLinux) {
            imageOptions.addAll(
                listOf(
                    "--icon", "$projectDir/../jabgui/src/main/resources/icons/JabRef-linux-icon-64.png",
                    "--app-version", "$version"
                )
            )
        }
    }
}
