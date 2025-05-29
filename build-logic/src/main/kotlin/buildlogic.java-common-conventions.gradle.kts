plugins {
    java

    id("idea")

    // id("jacoco")

    id("project-report")
}

repositories {
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public") }

    // Required for one.jpro.jproutils:tree-showing
    maven { url = uri("https://sandec.jfrog.io/artifactory/repo") }
}

dependencies {
    constraints {
        // Define dependency versions as constraints
        // implementation("org.apache.commons:commons-text:1.12.0")
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.2")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
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

tasks.javadoc {
    ( options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        // version = false
        // author = false

        addMultilineStringsOption("tag").setValue(listOf("apiNote", "implNote"))

        // We cross-link to (non-visible) tests; therefore: no reference check
        addBooleanOption("Xdoclint:all,-reference", true)

        addMultilineStringsOption("-add-exports").value = listOf(
            "javafx.controls/com.sun.javafx.scene.control=org.jabref",
            "org.controlsfx.controls/impl.org.controlsfx.skin=org.jabref"
        )

    }
}
