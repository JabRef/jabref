import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.jabref.gradle.module")
    id("application")

    id("org.beryx.jlink") version "3.1.1"
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

val javafxVersion = "24.0.1"

dependencies {
    implementation(project(":jablib"))
    implementation(project(":jabsrv"))

    implementation("org.openjfx:javafx-controls:${javafxVersion}")
    implementation("org.openjfx:javafx-fxml:${javafxVersion}")
    implementation ("org.openjfx:javafx-graphics:${javafxVersion}")

    implementation("org.slf4j:slf4j-api")
    implementation("org.tinylog:slf4j-tinylog")
    implementation("org.tinylog:tinylog-impl")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j")
    implementation("info.picocli:picocli")
    annotationProcessor("info.picocli:picocli-codegen")

    // required because of "service implementation must be defined in the same module as the provides directive"
    implementation("org.postgresql:postgresql")
    implementation("org.bouncycastle:bcprov-jdk18on")
    implementation("com.konghq:unirest-modules-gson")
    implementation("ai.djl:api")
    implementation("ai.djl.huggingface:tokenizers")
    implementation("ai.djl.pytorch:pytorch-model-zoo")

    // Prevents errors at "createMergedModule"
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // region copied from jabsrv

    // API
    implementation("jakarta.ws.rs:jakarta.ws.rs-api")

    // Implementation of the API
    implementation("org.glassfish.jersey.core:jersey-server")

    // Injection framework
    // implementation("org.glassfish.jersey.inject:jersey-hk2")
    // implementation("org.glassfish.hk2:hk2-api")
    // implementation("org.glassfish.hk2:hk2-utils")
    // Just to avoid the compiler error " org.glassfish.hk2.extension.ServiceLocatorGenerator: module jabsrv.merged.module does not declare `uses`"
    // implementation("org.glassfish.hk2:hk2-locator")

    // testImplementation("org.glassfish.hk2:hk2-testing")
    // implementation("org.glassfish.hk2:hk2-testing-jersey")
    // testImplementation("org.glassfish.hk2:hk2-junitrunner")

    // HTTP server
    // implementation("org.glassfish.jersey.containers:jersey-container-netty-http")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http")
    implementation("org.glassfish.grizzly:grizzly-http-server")
    implementation("org.glassfish.grizzly:grizzly-framework")
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.hibernate.validator:hibernate-validator")

    implementation("com.konghq:unirest-modules-gson")

    // Allow objects "magically" to be mapped to JSON using GSON
    // implementation("org.glassfish.jersey.media:jersey-media-json-gson")

    implementation("com.google.guava:guava")

    implementation("org.jabref:afterburner.fx")
    implementation("net.harawata:appdirs")

    implementation("de.undercouch:citeproc-java")

    // endregion
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
