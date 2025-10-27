plugins {
    id("org.jabref.gradle.base.repositories")
    id("org.jabref.gradle.feature.compile") // for openrewrite
    id("org.openrewrite.rewrite") version "7.19.0"
    id("org.itsallcode.openfasttrace") version "3.1.0"
    id("org.cyclonedx.bom") version "3.0.1"
}

// OpenRewrite should rewrite all sources
// This is the behavior when applied in the root project (https://docs.openrewrite.org/reference/gradle-plugin-configuration#multi-module-gradle-projects)

dependencies {
    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.16.0"))
    rewrite("org.openrewrite.recipe:rewrite-static-analysis")
    rewrite("org.openrewrite.recipe:rewrite-logging-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")
}

rewrite {
    activeRecipe("org.jabref.config.rewrite.cleanup")
    exclusion(
        "settings.gradle",
        "**/generated/sources/**",
        "**/generated-src/**",
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
            "jabls/src/main/java", "jabls/src/test/java",
            "jabkit/src/main/java", "jabkit/src/test/java",
            "jabgui/src/main/java", "jabgui/src/test/java",
            "jabsrv/src/main/java", "jabsrv/src/test/java"
        )
    )
    // TODO: Short Tag Importer: https://github.com/itsallcode/openfasttrace-gradle#configuring-the-short-tag-importer
}

// TODO: "run" should run the GUI, not all modules
tasks.register("run") {
    group = "application"
    description = "Runs the GUI"
    dependsOn(":jabgui:run")
}

allprojects {
    tasks.cyclonedxDirectBom {
    }
}

tasks.cyclonedxBom {
    // Aggregated SBOM configuration
    projectType = org.cyclonedx.model.Component.Type.APPLICATION
    includeBuildSystem = true
    componentVersion = project.version.toString()
    componentGroup = "org.jabref"
}
