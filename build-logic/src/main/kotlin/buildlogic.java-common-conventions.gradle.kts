plugins {
    java

    id("idea")

    // id("jacoco")

    id("project-report")

    id("org.gradlex.extra-java-module-info")
    id("org.gradlex.java-module-packaging")
    id("org.gradlex.java-module-testing")
    id("org.gradlex.jvm-dependency-conflict-resolution")
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

/*
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
    // primaryTarget(target("macos-14"))
}
*/

// Tell gradle which jar to use for which platform
// Source: https://github.com/jjohannes/java-module-system/blob/be19f6c088dca511b6d9a7487dacf0b715dbadc1/gradle/plugins/src/main/kotlin/metadata-patch.gradle.kts#L14-L22
jvmDependencyConflicts.patch {
    listOf("base", "controls", "fxml", "graphics", "swing", "web").forEach { jfxModule ->
        module("org.openjfx:javafx-$jfxModule") {
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

val os = org.gradle.internal.os.OperatingSystem.current()
val arch = System.getProperty("os.arch")
val javafxPlatform = when {
    os.isWindows -> "win"
    os.isMacOsX && arch == "aarch64" -> "mac-aarch64"
    os.isMacOsX -> "mac"
    os.isLinux && arch == "aarch64" -> "linux-aarch64"
    os.isLinux -> "linux"
    else -> error("Unsupported OS/arch: ${os.name} / $arch")
}

project.extra["javafxPlatform"] = javafxPlatform

extraJavaModuleInfo {
    failOnMissingModuleInfo = false
    failOnAutomaticModules = false
    // skipLocalJars = true
    deriveAutomaticModuleNamesFromFileNames = true
    /*
    module("org.openjfx:javafx-base", "javafx.base") {
        overrideModuleName()
        preserveExisting()
        /*
        exports("com.sun.javafx.event")
        exports("com.sun.javafx.collections")
        exports("com.sun.javafx.runtime")
        opens("javafx.collections")
        opens("javafx.collections.transformation")
        */
    }

     */
    module("org.openjfx:javafx-controls", "javafx.controls") {
        overrideModuleName()
        preserveExisting()
        exports("com.sun.javafx.scene.control")
        /*
        exports("com.sun.javafx.scene.control.behavior")
        exports("com.sun.javafx.scene.control.inputmat")
        opens("javafx.scene.control", "com.sun.javafx.scene.control", "javafx.scene.control.skin")
        */
    }

    //module("org.openjfx:javafx-graphics", "javafx.graphics") {
        //overrideModuleName()
        //preserveExisting()
        /*
        exports("com.sun.javafx.scene")
        exports("com.sun.javafx.scene.traversal")
        exports("com.sun.javafx.css")
        */
    //}
    /*
    module("org.openjfx:javafx-graphics", "javafx.fxml") {
        overrideModuleName()
        preserveExisting()
    }
    */

    // Based on module-info.class in https://repo1.maven.org/maven2/org/controlsfx/controlsfx/11.2.2/
    module("org.openjfx:javafx-controls", "javafx.controls") {
        overrideModuleName()

        patchRealModule()

        exportAllPackages() // shortcut to just export everything, can be replaced with a dedicated list

        // opens("org.controlsfx.control", "org.controlsfx.fxsampler")
        // opens("org.controlsfx.control.tableview2", "org.controlsfx.fxsampler");
        opens("impl.org.controlsfx.skin");

        // uses("org.controlsfx.glyphfont.GlyphFont");

        // Automatically reconstructed from META-INF
        // provides org.controlsfx.glyphfont.GlyphFont with org.controlsfx.glyphfont.FontAwesome;
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
