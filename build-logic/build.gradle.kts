plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.autonomousapps:dependency-analysis-gradle-plugin:3.5.1")
    implementation("com.github.andygoossens:gradle-modernizer-plugin:1.12.0")
    implementation("org.gradlex:extra-java-module-info:1.13.1")
    implementation("org.gradlex:java-module-dependencies:1.11")
    implementation("org.gradlex:java-module-packaging:1.2")
    implementation("org.gradlex:java-module-testing:1.8")
    implementation("org.gradlex:jvm-dependency-conflict-resolution:2.5")
    implementation("org.gradle.toolchains:foojay-resolver:1.0.0")
}
