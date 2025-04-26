plugins {
    id("buildlogic.java-common-conventions")

    id("checkstyle")

    id("com.github.andygoossens.modernizer") version "1.11.0"
    id("org.openrewrite.rewrite") version "7.5.0"
}

// OpenRewrite should rewrite all sources
// This is the behavior when applied in the root project (https://docs.openrewrite.org/reference/gradle-plugin-configuration#multi-module-gradle-projects)

dependencies {
    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.5.0"))
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
        "**/*.py",
        "**/*.xml",
        "**/*.yml"
    )
    plainTextMask("**/*.md")
    failOnDryRunResults = true
}

subprojects {
    plugins.apply("checkstyle")
    plugins.apply("com.github.andygoossens.modernizer")

    checkstyle {
        toolVersion = "10.23.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    tasks.withType<Checkstyle> {
        reports {
            xml.required.set(false)
            html.required.set(true)
        }
        exclude("**/generated-sources/**")
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
}
