plugins {
    id("org.gradlex.java-module-packaging")
}

val javaExt = extensions.getByType<JavaPluginExtension>()
val toolchains = extensions.getByType<JavaToolchainService>()
val isIbm = toolchains.launcherFor(javaExt.toolchain)
    .map { it.metadata.vendor.contains("IBM", ignoreCase = true) }

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
        "--bind-services"
        // "--strip-debug" // We need to keep this removed to get line numbers at stack traces
    )
    jlinkOptions.addAll(
        isIbm.map { ibm ->
            // See https://github.com/eclipse-openj9/openj9/issues/23240 for the reasoning
            if (ibm) emptyList() else listOf("--generate-cds-archive")
        }
    )
    target("ubuntu-24.04") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("app-image", "deb", "rpm")
    }
    target("ubuntu-24.04-arm") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.ARM64
        packageTypes = listOf("app-image", "deb", "rpm")
    }
    target("macos-15-intel") {
        operatingSystem = OperatingSystemFamily.MACOS
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("app-image", "dmg", "pkg")
        singleStepPackaging = true
    }
    target("macos-15") {
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
