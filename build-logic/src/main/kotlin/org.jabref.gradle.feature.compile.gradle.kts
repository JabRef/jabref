import org.jabref.gradle.useLibericaJdkFull

plugins {
    id("java")
}

// Opt-in (-PuseLibericaJdkFull, see org.jabref.gradle.JavaFxBundling): build against a JDK that bundles
// JavaFX (BellSoft Liberica "Full"). When set, JabRef does NOT pull JavaFX from Maven; 'requires javafx.*'
// resolves against the JDK's own system modules instead. See also:
// - versions/build.gradle.kts
// - org.jabref.gradle.base.dependency-rules.gradle.kts
// - jabgui/build.gradle.kts (runtime --add-opens/--add-exports)

java {
    toolchain {
        // If this is updated, also update
        // - build.gradle -> jacoco -> toolVersion (because JaCoCo does not support newest JDK out of the box. Check versions at https://www.jacoco.org/jacoco/trunk/doc/changes.html)
        // - jitpack.yml
        // - .sdkmanrc
        // - .devcontainer/devcontainer.json#L34 - there, also check if the gradleVersion matches the one of gradle/wrapper/gradle-wrapper.properties
        // - .github/actions/setup-gradle/action.yml
        // - .github/workflows/test-code.yml
        // - .jbang/Jab*.java
        // - .moderne/moderne.yml
        // - build-support/src/main/java/*.java
        // - docs/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/intellij-12-build.md
        // - jablib-examples/jbang/*.java
        // - jablib-examples/maven3/*/pom.xml
        // - Dockerfile.* (first line)
        languageVersion = JavaLanguageVersion.of(25)
        // See https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JvmVendorSpec.html for a full list
        // Temurin does not ship jmods, thus we need to use another JDK -- see https://github.com/actions/setup-java/issues/804
        // We default to a JDK without JavaFX (Amazon Corretto), because we patch JavaFX (Maven) due to modularity issues.
        // With -PuseLibericaJdkFull=true we instead use BellSoft Liberica, which ships JavaFX, and consume that bundled JavaFX.
        // NOTE: foojay auto-provisioning resolves BellSoft to Liberica *Standard* (no JavaFX). To actually get the
        //       bundled JavaFX, install Liberica *Full* JDK 25 locally so the toolchain detects it (and avoid having a
        //       Standard 25 of the same vendor installed, as the toolchain query cannot distinguish the two).
        // If this is changed, binaries.yml needs to be adapted (e.g., sed'ing to another JDK)
        vendor = if (useLibericaJdkFull) JvmVendorSpec.BELLSOFT else JvmVendorSpec.AMAZON
    }
}

// -PuseLibericaJdkFull only selects vendor BELLSOFT, but Gradle's toolchain query cannot distinguish Liberica
// "Full" (bundles JavaFX) from Liberica "Standard" (does not). If Standard gets selected, compilation fails later
// with a cryptic "module javafx.* not found", because the Maven JavaFX was already dropped (see
// org.jabref.gradle.base.dependency-rules). Resolve the toolchain and fail fast with an actionable message if its
// jmods/ does not contain JavaFX.
if (useLibericaJdkFull) {
    val launcher = javaToolchains.launcherFor(java.toolchain)
    tasks.withType<JavaCompile>().configureEach {
        doFirst {
            val installationPath = launcher.get().metadata.installationPath
            val javafxJmod = installationPath.file("jmods/javafx.controls.jmod").asFile
            if (!javafxJmod.exists()) {
                throw GradleException(
                    """
                    -PuseLibericaJdkFull is set, but the resolved JDK toolchain does not bundle JavaFX:
                      ${installationPath.asFile}
                    (missing $javafxJmod)

                    A BellSoft Liberica *Standard* JDK was most likely selected instead of Liberica *Full*.
                    Install Liberica *Full* JDK 25 and ensure no Liberica *Standard* 25 of the same vendor/version
                    is installed (the toolchain query cannot tell them apart), or point Gradle at the Full JDK via
                    the org.gradle.java.installations.paths property.
                    """.trimIndent()
                )
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    // --release is incompatible with the --add-exports for JDK-bundled JavaFX system modules used on the
    // -PuseLibericaJdkFull path (see jabgui/build.gradle.kts). The toolchain is pinned to the same Java
    // version, so on that path we let the toolchain set the target release instead of passing --release.
    if (!useLibericaJdkFull) {
        options.release = 25
    }
    // See https://docs.gradle.org/current/userguide/performance.html#a_run_the_compiler_as_a_separate_process
    options.isFork = true
}
