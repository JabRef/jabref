plugins {
    id("java")
    id("org.gradlex.java-module-testing")
    // Hint from https://stackoverflow.com/a/46533151/873282
    // id("com.adarshr.test-logger")
}

testing {
    @Suppress("UnstableApiUsage")
    suites.named<JvmTestSuite>("test") {
        useJUnitJupiter()
    }

    @Suppress("UnstableApiUsage")
    suites.withType<JvmTestSuite> {
        // Replace the 'catch-all' junit-jupiter dependency added by Gradle with a dependency to the engine only.
        // This is, because all compile-time dependencies are defined explicitly in the modules, including
        // 'org.junit.jupiter.api'. Unfortunately, there is no simple off switch.
        // https://github.com/gradle/gradle/issues/21299#issuecomment-1316438289
        configurations.getByName(sources.implementationConfigurationName) {
            withDependencies {
                removeIf { it.group == "org.junit.jupiter" && it.name == "junit-jupiter" }
            }
        }
        dependencies { runtimeOnly("org.junit.jupiter:junit-jupiter-engine") }
    }
}

tasks.withType<Test>().configureEach {
    // Note: For now, parallel handling with gradle is enough
    //       How to enable parallel JUnit tests **in addition**: https://docs.junit.org/6.0.1/writing-tests/parallel-execution.html

    // See https://docs.gradle.org/8.1/userguide/performance.html#execute_tests_in_parallel for details.
    maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)

    // Even in sequential tests, to a "force" cleanup
    // See https://docs.gradle.org/current/userguide/performance.html#b_fork_tests_into_multiple_processes for details.
    forkEvery = 100
}

/*
testlogger {
    // See https://github.com/radarsh/gradle-test-logger-plugin#configuration for configuration options

    theme = com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD

    showPassed = false
    showSkipped = false

    showCauses = true
    showStackTraces = true
}
*/

configurations.testCompileOnly {
    extendsFrom(configurations.compileOnly.get())
}

// See https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
    mockitoAgent("org.mockito:mockito-core:5.18.0") { isTransitive = false }
}
