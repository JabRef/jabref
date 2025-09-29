plugins {
    id("java")
}

java {
    toolchain {
        // If this is updated, also update
        // - build.gradle -> jacoco -> toolVersion (because JaCoCo does not support newest JDK out of the box. Check versions at https://www.jacoco.org/jacoco/trunk/doc/changes.html)
        // - jitpack.yml
        // - .devcontainer/devcontainer.json#L34 - there, also check if the gradleVersion matches the one of gradle/wrapper/gradle-wrapper.properties
        // - .moderne/moderne.yml
        // - .github/workflows/binaries*.yml
        // - .github/workflows/publish.yml
        // - .github/workflows/tests*.yml
        // - docs/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/intellij-12-build.md
        // - .sdkmanrc
        languageVersion = JavaLanguageVersion.of(25)
        // See https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JvmVendorSpec.html for a full list
        // We also need a JDK without JavaFX, because we patch JavaFX due to modularity issues
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}
