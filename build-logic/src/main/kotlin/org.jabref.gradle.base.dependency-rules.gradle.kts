plugins {
    id("org.gradlex.extra-java-module-info")
    id("org.gradlex.jvm-dependency-conflict-resolution")
}

jvmDependencyConflicts {
    consistentResolution {
        platform(":versions")
    }
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
    module("com.github.tomtung:latex2unicode_2.13") {
        removeDependency("com.lihaoyi:fastparse_2.13")
        addApiDependency("com.lihaoyi:fastparse:2.3.3")
    }
    module("de.rototor.jeuclid:jeuclid-core") {
        removeDependency("org.apache.xmlgraphics:batik-svg-dom")
        removeDependency("org.apache.xmlgraphics:batik-ext")
        removeDependency("org.apache.xmlgraphics:xmlgraphics-commons")
    }
    module("org.wiremock:wiremock") {
        // workaround for https://github.com/wiremock/wiremock/issues/2874
        addApiDependency("com.github.koppor:wiremock-slf4j-spi-shim")
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

    // Workaround for https://github.com/wiremock/wiremock/issues/2149
    module("org.wiremock:wiremock", "wiremock") {
        overrideModuleName()
        patchRealModule()
        exportAllPackages()

        requires("wiremock.slf4j.spi.shim")
        requires("com.fasterxml.jackson.core")
        requires("com.fasterxml.jackson.databind")
        requires("com.fasterxml.jackson.datatype.jsr310")
        requires("com.google.common")
        requires("commons.fileupload")
        requires("org.eclipse.jetty.server")
        requires("org.eclipse.jetty.servlet")
        requires("org.eclipse.jetty.servlets")
        requires("org.eclipse.jetty.webapp")
        requires("org.eclipse.jetty.proxy")
        requires("org.eclipse.jetty.http2.server")
        requires("org.eclipse.jetty.alpn.server")
        requires("org.eclipse.jetty.alpn.java.server")
        requires("org.eclipse.jetty.alpn.java.client")
        requires("org.eclipse.jetty.alpn.client")
        requires("java.xml")
        requires("org.custommonkey.xmlunit")
        requires("org.slf4j")
        requires("org.xmlunit")

        uses("com.github.tomakehurst.wiremock.extension.Extension")

        // workaround for https://github.com/wiremock/wiremock/issues/2874
        mergeJar("com.github.jknack:handlebars")
        mergeJar("com.github.jknack:handlebars-helpers")

        // Required to provide package "wiremock.org.slf4j.helpers"
        mergeJar("com.github.koppor:wiremock-slf4j-shim")
    }
}
