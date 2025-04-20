plugins {
    id("buildlogic.java-common-conventions")

    application

    // This is https://github.com/java9-modularity/gradle-modules-plugin/pull/282
    id("com.github.koppor.gradle-modules-plugin") version "v1.8.15-cmd-1"

    id("com.github.andygoossens.modernizer") version "1.10.0"
    id("org.openrewrite.rewrite") version "7.3.0"

    // nicer test outputs during running and completion
    // Homepage: https://github.com/radarsh/gradle-test-logger-plugin
    id("com.adarshr.test-logger") version "4.0.0"

    id("org.itsallcode.openfasttrace") version "3.0.1"
}

dependencies {
    implementation(project(":jablib"))

    implementation("commons-cli:commons-cli:1.9.0")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.5.0"))
    rewrite("org.openrewrite.recipe:rewrite-static-analysis")
    rewrite("org.openrewrite.recipe:rewrite-logging-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")
}

jacoco {
    toolVersion = "0.8.13"
}
