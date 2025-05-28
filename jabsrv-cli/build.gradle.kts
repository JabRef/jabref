import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("buildlogic.java-common-conventions")

    application

    id("org.openjfx.javafxplugin") version("0.1.0")

    id("org.beryx.jlink") version "3.1.1"

    id("org.kordamp.gradle.jdeps") version "0.20.0"
}

application{
    mainClass.set("org.jabref.http.server.cli.ServerCli")
    mainModule.set("org.jabref.jabsrv.cli")

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
    implementation(project(":jabsrv"))

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")
    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    // required because of "service implementation must be defined in the same module as the provides directive"
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("com.konghq:unirest-modules-gson:4.4.7")
    implementation(platform("ai.djl:bom:0.33.0"))
    implementation("ai.djl:api")
    implementation("ai.djl.huggingface:tokenizers")
    implementation("ai.djl.pytorch:pytorch-model-zoo")

    // Prevents errors at "createMergedModule"
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")

    // region copied from jabsrv

    // API
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")

    // Implementation of the API
    implementation("org.glassfish.jersey.core:jersey-server:3.1.10")

    // Injection framework
    // implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.10")
    // implementation("org.glassfish.hk2:hk2-api:3.1.1")
    // implementation("org.glassfish.hk2:hk2-utils:3.1.1")
    // Just to avoid the compiler error " org.glassfish.hk2.extension.ServiceLocatorGenerator: module jabsrv.merged.module does not declare `uses`"
    // implementation("org.glassfish.hk2:hk2-locator:3.1.1")

    // testImplementation("org.glassfish.hk2:hk2-testing:3.0.4")
    // implementation("org.glassfish.hk2:hk2-testing-jersey:3.0.4")
    // testImplementation("org.glassfish.hk2:hk2-junitrunner:3.0.4")

    // HTTP server
    // implementation("org.glassfish.jersey.containers:jersey-container-netty-http:3.1.1")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.10")
    implementation("org.glassfish.grizzly:grizzly-http-server:4.0.2")
    implementation("org.glassfish.grizzly:grizzly-framework:4.0.2")
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:3.1.10")
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.hibernate.validator:hibernate-validator:9.0.0.Final")

    implementation("com.konghq:unirest-modules-gson:4.4.7")

    // Allow objects "magically" to be mapped to JSON using GSON
    // implementation("org.glassfish.jersey.media:jersey-media-json-gson:3.1.1")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }

    implementation("net.harawata:appdirs:1.4.0")

    implementation("de.undercouch:citeproc-java:3.3.0") {
        exclude(group = "org.antlr")
    }

    // endregion
}

javafx {
    version = "24"
    // because of afterburner.fx
    modules = listOf("javafx.base", "javafx.controls", "javafx.fxml")
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
    doFirst {
        application.applicationDefaultJvmArgs =
            listOf(
                "--enable-native-access=com.sun.jna"
            )
    }
}

// This is more or less a clone of jabgui/build.gradle.kts -> jlink
jlink {
    // https://github.com/beryx/badass-jlink-plugin/issues/61#issuecomment-504640018
    addExtraDependencies(
        "javafx"
    )

    mergedModuleName = "jabsrv.merged.module"

    // We keep debug statements - otherwise "--strip-debug" would be included
    addOptions(
        "--compress",
        "zip-6",
        "--no-header-files",
        "--no-man-pages",
        "--bind-services"
    )

    launcher {
        name = "jabsrv"
    }

    // TODO: Remove as soon as dependencies are fixed (upstream)
    forceMerge(
        "bcprov",
        "jaxb",
        "istack",
        "stax"
    )

    mergedModule {
        uses("org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl")

        uses("org.glassfish.jersey.internal.inject.InjectionManager")
        uses("dev.langchain4j.spi.prompt.PromptTemplateFactory")

        excludeRequires("org.glassfish.hk2.locator")
        excludeRequires("org.apache.logging.log4j")
        excludeRequires("kotlin.stdlib")

    }
    jpackage {
        outputDir = "distribution"

        imageOptions.addAll(listOf(
            "--java-options", "--add-reads jabsrv.merged.module=jakarta.inject",
            "--java-options", "--enable-native-access=jabsrv.merged.module"))

        // See https://docs.oracle.com/en/java/javase/24/docs/specs/man/jpackage.html#platform-dependent-options-for-creating-the-application-package for available options
        if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
            imageOptions.addAll(
                listOf(
                    "--win-console"
                )
            )
            skipInstaller = true
        } else if (org.gradle.internal.os.OperatingSystem.current().isLinux) {
            imageOptions.addAll(
                listOf(
                    "--icon", "$projectDir/../jabgui/src/main/resources/icons/JabRef-linux-icon-64.png",
                    "--app-version", "$version"
                )
            )
            skipInstaller = true
        } else if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
            skipInstaller = true
        }
    }
}
