plugins {
    id("java")
}

java {
    toolchain {
        // If this is updated, also update
        // - build.gradle -> jacoco -> toolVersion (because JaCoCo does not support newest JDK out of the box. Check versions at https://www.jacoco.org/jacoco/trunk/doc/changes.html)
        // - .devcontainer/devcontainer.json#L34 and
        // - .moderne/moderne.yml
        // - .github/workflows/binaries*.yml
        // - .github/workflows/tests*.yml
        // - .github/workflows/update-gradle-wrapper.yml
        // - docs/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/intellij-12-build.md
        // - .sdkmanrc
        languageVersion = JavaLanguageVersion.of(24)
        // See https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JvmVendorSpec.html for a full list
        // See https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JvmVendorSpec.html for a full list
        // Temurin does not ship jmods, thus we need to use another JDK -- see https://github.com/actions/setup-java/issues/804
        vendor = JvmVendorSpec.AZUL
    }
}
