plugins {
    id("com.gradleup.shadow")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    mergeServiceFiles()
    isZip64 = true
}
