import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("buildlogic.java-common-conventions")

    application

    id("org.panteleyev.jpackageplugin") version "1.6.1"
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

task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into("${layout.buildDirectory.get()}/jmods")
}

task("copyJar", Copy::class) {
    from(tasks.jar).into("${layout.buildDirectory.get()}/jmods")
}

val version: String = project.findProperty("projVersion") as? String ?: "100.0.0"

tasks.jpackage {
    dependsOn("build", "copyDependencies", "copyJar")

    destination = "${layout.buildDirectory.get()}/distribution"

    appName = "JabSrv"
    appVersion = version
    vendor = "JabRef e.V."
    // copyright = "Copyright (c) 2020 Vendor"
    module = "org.jabref.jabsrv/org.jabref.http.server.cli.ServerCLI"
    modulePaths = listOf("${layout.buildDirectory.get()}/jmods")
    javaOptions = listOf("-Dfile.encoding=UTF-8")
    jLinkOptions = listOf(
        "--strip-native-commands",
        "--compress",
        "zip-6",
        "--no-header-files",
        "--no-man-pages",
        "--bind-services"
    )
    javaOptions = listOf(
        "--add-reads jabsrv.merged.module=jakarta.inject",
        "--enable-native-access=org.jabref.jabsrv"
    )
}
