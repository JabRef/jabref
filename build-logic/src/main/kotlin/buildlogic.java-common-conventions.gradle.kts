import org.gradle.internal.os.OperatingSystem

plugins {
    java

    id("idea")

    // id("jacoco")

    id("project-report")

    id("org.gradlex.extra-java-module-info")
    id("org.gradlex.java-module-testing")
    id("org.gradlex.jvm-dependency-conflict-resolution")
    id("org.gradlex.java-module-packaging")
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

val os = OperatingSystem.current()

val osTarget = when {
    os.isMacOsX -> {
        val osVersion = System.getProperty("os.version")
        if (osVersion.startsWith("14")) "macos-14" else "macos-13"
    }
    os.isLinux -> "ubuntu-22.04"
    os.isWindows -> "windows-2022"
    else -> error("Unsupported OS")
}

// Source: https://github.com/jjohannes/java-module-system/blob/main/gradle/plugins/src/main/kotlin/targets.gradle.kts
// Configure variants for OS
javaModulePackaging {
    target("ubuntu-22.04") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("deb")
    }
    target("macos-13") {
        operatingSystem = OperatingSystemFamily.MACOS
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("dmg")
    }
    target("macos-14") {
        operatingSystem = OperatingSystemFamily.MACOS
        architecture = MachineArchitecture.ARM64
        packageTypes = listOf("dmg")
    }
    target("windows-2022") {
        operatingSystem = OperatingSystemFamily.WINDOWS
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("exe")
    }
    primaryTarget(target(osTarget))
}

// Tell gradle which jar to use for which platform
// Source: https://github.com/jjohannes/java-module-system/blob/be19f6c088dca511b6d9a7487dacf0b715dbadc1/gradle/plugins/src/main/kotlin/metadata-patch.gradle.kts#L14-L22
jvmDependencyConflicts.patch {
    listOf("base", "controls", "fxml", "graphics", "swing", "web", "media").forEach { jfxModule ->
        module("org.openjfx:javafx-$jfxModule") {
            addTargetPlatformVariant("", "none", "none") // matches the empty Jars: to get better errors
            addTargetPlatformVariant("linux", OperatingSystemFamily.LINUX, MachineArchitecture.X86_64)
            addTargetPlatformVariant("linux-aarch64", OperatingSystemFamily.LINUX, MachineArchitecture.ARM64)
            addTargetPlatformVariant("mac", OperatingSystemFamily.MACOS, MachineArchitecture.X86_64)
            addTargetPlatformVariant("mac-aarch64", OperatingSystemFamily.MACOS, MachineArchitecture.ARM64)
            addTargetPlatformVariant("win", OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64)
        }
    }
    // Source: https://github.com/jjohannes/java-module-system/blob/be19f6c088dca511b6d9a7487dacf0b715dbadc1/gradle/plugins/src/main/kotlin/metadata-patch.gradle.kts#L9
    module("com.google.guava:guava") {
        removeDependency("com.google.code.findbugs:jsr305")
        removeDependency("org.checkerframework:checker-qual")
        removeDependency("com.google.errorprone:error_prone_annotations")
    }
}

extraJavaModuleInfo {
    failOnMissingModuleInfo = false
    failOnAutomaticModules = false
    // skipLocalJars = true
    deriveAutomaticModuleNamesFromFileNames = true

    module("org.openjfx:javafx-base", "javafx.base") {
        overrideModuleName()
        patchRealModule()
        // jabgui requires at least "javafx.collections"
        exportAllPackages()
    }

    // required for testing of jablib
    module("org.openjfx:javafx-fxml", "javafx.fxml") {
        patchRealModule()
        exportAllPackages()

        requiresTransitive("javafx.base")
        requiresTransitive("javafx.graphics")
        requiresTransitive("java.desktop")
    }

    // required for testing
    module("org.openjfx:javafx-graphics", "javafx.graphics") {
        patchRealModule()
        exportAllPackages()

        requiresTransitive("javafx.base")
        requiresTransitive("java.desktop")
        requiresTransitive("jdk.unsupported")
    }

    module("org.controlsfx:controlsfx", "org.controlsfx.controls") {
        patchRealModule()

        exports("impl.org.controlsfx.skin")
        exports("org.controlsfx.control")
        exports("org.controlsfx.control.action")
        exports("org.controlsfx.control.decoration")
        exports("org.controlsfx.control.table")
        exports("org.controlsfx.control.textfield")
        exports("org.controlsfx.dialog")
        exports("org.controlsfx.validation")
        exports("org.controlsfx.validation.decoration")

        requires("javafx.base")
        requires("javafx.controls")
        requires("javafx.graphics")
    }

    module("org.openjfx:javafx-controls", "javafx.controls") {
        patchRealModule()

        requiresTransitive("javafx.base");
        requiresTransitive("javafx.graphics");

        exports("javafx.scene.chart")
        exports("javafx.scene.control")
        exports("javafx.scene.control.cell")
        exports("javafx.scene.control.skin")

        // PATCH REASON:
        exports("com.sun.javafx.scene.control")
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
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
