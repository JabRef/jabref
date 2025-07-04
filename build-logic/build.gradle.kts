import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.autonomousapps:dependency-analysis-gradle-plugin:2.19.0")
    implementation("com.github.andygoossens:gradle-modernizer-plugin:1.11.0")
    implementation("org.gradlex:extra-java-module-info:1.12")
    implementation("org.gradlex:java-module-dependencies:1.9.2")
    implementation("org.gradlex:java-module-packaging:1.1") // required for platform-specific packaging of JavaFX dependencies
    implementation("org.gradlex:java-module-testing:1.7")
    implementation("org.gradlex:jvm-dependency-conflict-resolution:2.4")
    implementation("org.gradle.toolchains:foojay-resolver:1.0.0")
}
