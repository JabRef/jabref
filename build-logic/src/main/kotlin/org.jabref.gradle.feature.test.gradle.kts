plugins {
    id("java")
    id("org.gradlex.java-module-testing")
    // Hint from https://stackoverflow.com/a/46533151/873282
    id("com.adarshr.test-logger")
}

testing {
    @Suppress("UnstableApiUsage")
    suites.named<JvmTestSuite>("test") {
        useJUnitJupiter()
    }
}

tasks.withType<Test>().configureEach {
    // Enable parallel tests (on desktop).
    // See https://docs.gradle.org/8.1/userguide/performance.html#execute_tests_in_parallel for details.
    if (!providers.environmentVariable("CI").isPresent) {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)
    }
    // See https://docs.gradle.org/current/userguide/performance.html#b_fork_tests_into_multiple_processes for details.
    forkEvery = 100
}

testlogger {
    // See https://github.com/radarsh/gradle-test-logger-plugin#configuration for configuration options

    theme = com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD

    showPassed = false
    showSkipped = false

    showCauses = true
    showStackTraces = true
}

configurations.testCompileOnly {
    extendsFrom(configurations.compileOnly.get())
}
