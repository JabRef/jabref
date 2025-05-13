plugins {
    java

    id("idea")

    // id("jacoco")

    id("project-report")

    id("org.gradlex.extra-java-module-info")
    id("org.gradlex.java-module-testing")
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

extraJavaModuleInfo {
    failOnMissingModuleInfo = false
    failOnAutomaticModules = false
    // skipLocalJars = true
    deriveAutomaticModuleNamesFromFileNames = true
    module("org.openjfx:javafx-base", "org.jabref.merged.module") {
        exports("com.sun.javafx.event")
        patchRealModule()
    }
    module("org.openjfx:javafx-base", "org.jabref") {
        opens("javafx.collections", "javafx.collections.transformation")
        patchRealModule()
    }
    module("org.openjfx:javafx-controls", "org.jabref") {
        exports("com.sun.javafx.scene.control")
        opens("javafx.scene.control", "com.sun.javafx.scene.control", "javafx.scene.control.skin")
        patchRealModule()
    }
    module("org.openjfx:javafx-graphics", "org.controlsfx.controls") {
        exports("com.sun.javafx.scene")
        exports("com.sun.javafx.scene.traversal")
        exports("com.sun.javafx.css")
        patchRealModule()
    }
    module("org.openjfx:javafx-controls", "org.controlsfx.controls") {
        exports("com.sun.javafx.scene.control")
        exports("com.sun.javafx.scene.control.behavior")
        exports("com.sun.javafx.scene.control.inputmap")
        opens("javafx.scene")
        patchRealModule()
    }
    module("org.openjfx:javafx-base", "org.controlsfx.controls") {
        exports("com.sun.javafx.event")
        exports("com.sun.javafx.collections")
        exports("com.sun.javafx.runtime")
        patchRealModule()
    }
    module("org.controlsfx:controls", "org.jabref") {
        opens("impl.org.controlsfx.skin", "org.controlsfx.control.textfield")
        patchRealModule()
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
        // - .github/workflows/deployment*.yml
        // - .github/workflows/tests*.yml
        // - .github/workflows/update-gradle-wrapper.yml
        // - docs/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/intellij-12-build.md
        // - mise.toml
        languageVersion = JavaLanguageVersion.of(24)
        // See https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JvmVendorSpec.html for a full list
        // See https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JvmVendorSpec.html for a full list
        // Temurin does not ship jmods, thus we need to use another JDK -- see https://github.com/actions/setup-java/issues/804
        vendor = JvmVendorSpec.AZUL
    }
}
