import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.jabref.gradle.module")
    id("java-library")
}

testModuleInfo {
    requires("org.junit.jupiter.api")
    requires("org.mockito")

    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    runtimeOnly("jul.to.slf4j")
}

tasks.test {
    testLogging {
        // set options for log level LIFECYCLE
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }
    maxParallelForks = 1
}
