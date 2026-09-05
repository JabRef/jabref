import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.language.base.plugins.LifecycleBasePlugin

plugins {
    id("org.jabref.gradle.module")
    id("java-library")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.junit.jupiter.params")
    requires("org.mockito")
    requires("org.glassfish.jersey.tests.framework.core")
    requires("jul.to.slf4j")
    requires("org.jabref.testsupport")
    requires("com.tngtech.archunit")
    requires("com.tngtech.archunit.junit5.api")
    runtimeOnly("com.tngtech.archunit.junit5.engine")
    runtimeOnly("org.glassfish.jersey.tests.framework.provider.grizzly")
    runtimeOnly("org.tinylog.api")
    runtimeOnly("org.tinylog.impl")
    runtimeOnly("org.apache.logging.log4j.to.slf4j")
    // Jersey's external test container, used by nativeSmokeTest. Harmless for the regular
    // test task: with both providers present JerseyTest picks its default (Grizzly).
    runtimeOnly("org.glassfish.jersey.tests.framework.provider.external")
}

tasks.test {
    testLogging {
        // set options for log level LIFECYCLE
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }
    maxParallelForks = 1
}

val testSourceSet = sourceSets.test.get()

tasks.register<Test>("nativeSmokeTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs server tests against a running native jabsrv binary (external Jersey container)."
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    maxParallelForks = 1

    useJUnitPlatform()

    filter {
        includeTestsMatching("org.jabref.http.server.*")
    }

    testLogging {
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }

    val smokePort = providers.gradleProperty("jabsrv.native.smoke")
    inputs.property("jabsrv.native.smoke", smokePort.orElse(""))
    doFirst {
        val port = smokePort.orNull?.toIntOrNull()
            ?: throw GradleException("nativeSmokeTest requires -Pjabsrv.native.smoke=<port> (the port the running native jabsrv binary serves on).")
        systemProperty(
            "jersey.config.test.container.factory",
            "org.glassfish.jersey.test.external.ExternalTestContainerFactory"
        )
        systemProperty("jersey.config.test.container.port", port.toString())
    }

    // Skip the tests that cannot pass against a GUI-less standalone binary.
    val excludeFile = layout.projectDirectory.file("src/test/nativeimage/smoke-excluded-tests.txt")
    providers.fileContents(excludeFile).asText.orNull
        ?.lines()
        ?.map { it.substringBefore('#').trim() }
        ?.filter { it.isNotEmpty() }
        ?.forEach { filter.excludeTestsMatching(it) }
}
