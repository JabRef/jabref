plugins {
    id("buildlogic.java-common-conventions")

    application

    // afterburner.fx
    id("org.openjfx.javafxplugin") version("0.1.0")

    id("org.beryx.jlink") version "3.1.1"
}

group = "org.jabref.jabkit"
version = project.findProperty("projVersion") ?: "100.0.0"

val luceneVersion = "10.2.1"

dependencies {
    implementation(project(":jablib"))

    // FIXME: Injector needs to be removed, no JavaFX dependencies, etc.
    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }

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

    testImplementation(project(":test-support"))
    testImplementation("org.mockito:mockito-core:5.18.0") {
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    testImplementation("net.bytebuddy:byte-buddy:1.17.5")
}

/*
jacoco {
    toolVersion = "0.8.13"
}
*/

javafx {
    version = "24"
    // because of afterburner.fx
    modules = listOf("javafx.base", "javafx.controls", "javafx.fxml")
}

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
        "--bind-services"
    )

    launcher {
        name = "jabkit"
    }

    // TODO: Remove as soon as dependencies are fixed (upstream)
    forceMerge(
        "controlsfx",
        "bcprov",
        "jaxb",
        "istack",
        "stax"
    )

    mergedModule {
        requires(
            "com.google.gson"
        )
        requires(
            "com.fasterxml.jackson.annotation"
        )
        requires(
            "com.fasterxml.jackson.databind"
        )
        requires(
            "com.fasterxml.jackson.core"
        )
        requires(
            "com.fasterxml.jackson.datatype.jdk8"
        )
        requires(
            "jakarta.xml.bind"
        )
        requires(
            "java.compiler"
        )
        requires(
            "java.datatransfer"
        )
        requires(
            "java.desktop"
        )
        requires(
            "java.logging"
        )
        requires(
            "java.management"
        )
        requires(
            "java.naming"
        )
        requires(
            "java.net.http"
        )
        requires(
            "java.rmi"
        )
        requires(
            "java.scripting"
        )
        requires(
            "java.security.jgss"
        )
        requires(
            "java.security.sasl"
        )
        requires(
            "java.sql"
        )
        requires(
            "java.sql.rowset"
        )
        requires(
            "java.transaction.xa"
        )
        requires(
            "java.xml"
        )
        requires(
            "javafx.base"
        )
        requires(
            "javafx.controls"
        )
        requires(
            "javafx.fxml"
        )
        requires(
            "javafx.graphics"
        )
        requires(
            "jdk.security.jgss"
        )
        requires(
            "jdk.unsupported"
        )
        requires(
            "jdk.unsupported.desktop"
        )
        requires(
            "jdk.xml.dom"
        )
        requires(
            "org.apache.commons.lang3"
        )
        requires(
            "org.apache.commons.logging"
        )
        requires(
            "org.apache.commons.text"
        )
        requires(
            "org.apache.commons.codec"
        )
        requires(
            "org.apache.commons.io"
        )
        requires(
            "org.apache.commons.compress"
        )
        requires(
            "org.freedesktop.dbus"
        )
        requires(
            "org.jsoup"
        )
        requires(
            "org.slf4j"
        )
        requires(
            "org.tukaani.xz"
        );
        uses(
            "ai.djl.engine.EngineProvider"
        )
        uses(
            "ai.djl.repository.RepositoryFactory"
        )
        uses(
            "ai.djl.repository.zoo.ZooProvider"
        )
        uses(
            "dev.langchain4j.spi.prompt.PromptTemplateFactory"
        )
        uses(
            "kong.unirest.core.json.JsonEngine"
        )
        uses(
            "org.eclipse.jgit.lib.Signer"
        )
        uses(
            "org.eclipse.jgit.transport.SshSessionFactory"
        )
        uses(
            "org.postgresql.shaded.com.ongres.stringprep.Profile"
        )

        provides(
            "java.sql.Driver"
        ).with(
            "org.postgresql.Driver"
        )
        provides(
            "java.security.Provider"
        ).with(
            "org.bouncycastle.jce.provider.BouncyCastleProvider",
            "org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider"
        )
        provides(
            "kong.unirest.core.json.JsonEngine"
        ).with(
            "kong.unirest.modules.gson.GsonEngine"
        )
        provides(
            "ai.djl.repository.zoo.ZooProvider"
        ).with(
            "ai.djl.engine.rust.zoo.RsZooProvider",
            "ai.djl.huggingface.zoo.HfZooProvider",
            "ai.djl.pytorch.zoo.PtZooProvider",
            "ai.djl.repository.zoo.DefaultZooProvider"
        )
        provides(
            "ai.djl.engine.EngineProvider"
        ).with(
            "ai.djl.engine.rust.RsEngineProvider",
            "ai.djl.pytorch.engine.PtEngineProvider"
        )

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
