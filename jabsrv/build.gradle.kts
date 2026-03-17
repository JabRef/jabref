import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.jabref.gradle.module")
    id("java-library")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.mockito")
    requires("org.glassfish.jersey.tests.framework.core")
    requires("jul.to.slf4j")
    runtimeOnly("org.glassfish.jersey.tests.framework.provider.grizzly")
    runtimeOnly("org.tinylog.api")
    runtimeOnly("org.tinylog.impl")
    runtimeOnly("org.apache.logging.log4j.to.slf4j")
}

tasks.test {
    testLogging {
        // set options for log level LIFECYCLE
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }
    maxParallelForks = 1
}
