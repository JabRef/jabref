plugins {
    `kotlin-dsl`
}

repositories {
    // Workaround for Maven Central rate limiting (HTTP 429).
    // gradlePluginPortal() redirects non-portal artifacts (kotlin-stdlib, ...)
    // to repo.maven.apache.org, which enforces consumption limits since 2026.
    // See https://central.sonatype.org/faq/429-error/ and gradle/gradle#37880.
    maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
    gradlePluginPortal()

    mavenCentral()

    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.autonomousapps:dependency-analysis-gradle-plugin:3.19.1")
    implementation("com.github.andygoossens:gradle-modernizer-plugin:1.15.0")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.7.0")
    implementation("org.graalvm.buildtools:native-gradle-plugin:1.1.8")
    implementation("org.gradlex:extra-java-module-info:1.14.2")
    implementation("org.gradlex:java-module-dependencies:1.13.2")
    implementation("org.gradlex:java-module-packaging:1.3")
    implementation("org.gradlex:java-module-testing:1.8.1")
    implementation("org.gradlex:jvm-dependency-conflict-resolution:2.5")
    implementation("org.gradle.toolchains:foojay-resolver:1.0.0")
    implementation("org.itsallcode:openfasttrace-gradle:3.2.0")
    implementation("org.itsallcode.openfasttrace:openfasttrace-api:4.9.0")
    implementation("org.itsallcode.openfasttrace:openfasttrace-core:4.9.0")
    implementation("org.itsallcode.openfasttrace:openfasttrace-exporter-specobject:4.9.0")
    implementation("org.itsallcode.openfasttrace:openfasttrace:4.9.0")
}
