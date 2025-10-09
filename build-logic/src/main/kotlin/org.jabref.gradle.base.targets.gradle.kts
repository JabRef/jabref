plugins {
    id("org.gradlex.java-module-packaging")
}

// Source: https://github.com/jjohannes/java-module-system/blob/main/gradle/plugins/src/main/kotlin/targets.gradle.kts
// Configure variants for OS. Target name can be any string, but should match the name used in GitHub actions.
javaModulePackaging {
    // Configuration shared by all targets and applications
    vendor = "JabRef"
    jlinkOptions.addAll(
        "--ignore-signing-information",
        "--compress", "zip-6",
        "--no-header-files",
        "--no-man-pages",
        "--bind-services",
    )

    // Required by https://github.com/openjdk/jfx/blob/jfx24/doc-files/release-notes-24.md#applications-using-jlink-to-create-a-custom-java-runtime-image
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jlinkOptions.addAll(listOf("--module-path", "/tmp/javafx-jmods-26"))
    }

    target("ubuntu-22.04") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("app-image", "deb", "rpm")
    }
    target("ubuntu-22.04-arm") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.ARM64
        packageTypes = listOf("app-image", "deb", "rpm")
    }
    target("macos-13") {
        operatingSystem = OperatingSystemFamily.MACOS
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("app-image", "dmg", "pkg")
        singleStepPackaging = true
    }
    target("macos-14") {
        operatingSystem = OperatingSystemFamily.MACOS
        architecture = MachineArchitecture.ARM64
        packageTypes = listOf("app-image", "dmg", "pkg")
        singleStepPackaging = true
    }
    target("windows-latest") {
        operatingSystem = OperatingSystemFamily.WINDOWS
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("app-image", "msi")
    }
}
