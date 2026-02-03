plugins {
    id("com.gradleup.shadow")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    isZip64 = true
}
