plugins {
    id("buildlogic.java-common-conventions")

    id("checkstyle")

    id("com.github.andygoossens.modernizer") version "1.11.0"
    id("org.openrewrite.rewrite") version "7.6.1"

    id("org.itsallcode.openfasttrace") version "3.0.1"

    id("com.adarshr.test-logger") version "4.0.0"
}

// OpenRewrite should rewrite all sources
// This is the behavior when applied in the root project (https://docs.openrewrite.org/reference/gradle-plugin-configuration#multi-module-gradle-projects)

dependencies {
    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.8.0"))
    rewrite("org.openrewrite.recipe:rewrite-static-analysis")
    rewrite("org.openrewrite.recipe:rewrite-logging-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")
}

rewrite {
    activeRecipe("org.jabref.config.rewrite.cleanup")
    exclusion(
        "settings.gradle",
        "**/generated-sources/**",
        "**/src/main/resources/**",
        "**/src/test/resources/**",
        "**/module-info.java",
        "**/*.kts",
        "**/*.py",
        "**/*.xml",
        "**/*.yml"
    )
    plainTextMask("**/*.md")
    failOnDryRunResults = true
}

requirementTracing {
    inputDirectories.setFrom(files("docs",
            "jablib/src/main/java", "jablib/src/test/java",
            "jabkit/src/main/java", "jabkit/src/test/java",
            "jabgui/src/main/java", "jabgui/src/test/java",
            "jabsrv/src/main/java", "jabsrv/src/test/java"
        )
    )
    // TODO: Short Tag Importer: https://github.com/itsallcode/openfasttrace-gradle#configuring-the-short-tag-importer
}


subprojects {
    plugins.apply("checkstyle")

    plugins.apply("com.github.andygoossens.modernizer")

    // Hint from https://stackoverflow.com/a/46533151/873282
    plugins.apply("com.adarshr.test-logger")

    checkstyle {
        toolVersion = "10.23.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(false)
            html.required.set(true)
        }
        source = fileTree("src") { include("**/*.java") }
    }

    configurations.named("checkstyle") {
        resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
            select("com.google.guava:guava:0")
        }
    }

    modernizer {
        failOnViolations = true
        includeTestClasses = true
        exclusions = setOf(
            "java/util/Optional.get:()Ljava/lang/Object;"
        )
    }

    testlogger {
        // See https://github.com/radarsh/gradle-test-logger-plugin#configuration for configuration options

        theme = com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD

        showPassed = false
        showSkipped = false

        showCauses = false
        showStackTraces = false
    }

    tasks.withType<Test>().configureEach {
        reports.html.outputLocation.set(file("${reporting.baseDirectory}/${name}"))

        // Enable parallel tests (on desktop).
        // See https://docs.gradle.org/8.1/userguide/performance.html#execute_tests_in_parallel for details.
        if (!providers.environmentVariable("CI").isPresent) {
            maxParallelForks = maxOf(Runtime.getRuntime().availableProcessors() - 1, 1)
        }
    }
}

// TODO: "run" should run the GUI, not all modules
tasks.register("run") {
    group = "application"
    description = "Runs the GUI"
    dependsOn(":jabgui:run")
}
