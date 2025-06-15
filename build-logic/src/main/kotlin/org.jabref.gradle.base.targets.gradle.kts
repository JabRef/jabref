import org.gradle.internal.os.OperatingSystem

plugins {
    id("org.gradlex.java-module-packaging")
}

// TODO jjohannes: OS detection should be part of packaging plugin
//  https://github.com/gradlex-org/java-module-packaging/issues/51
// the result name must equal the name of one of the targets below in javaModulePackaging
val os = OperatingSystem.current()
val osTarget = when {
    os.isMacOsX -> {
        val osVersion = System.getProperty("os.version")
        val arch = System.getProperty("os.arch")
        if (arch.contains("aarch")) "macos-14" else "macos-13"
    }
    os.isLinux ->  {
        val arch = System.getProperty("os.arch")
        if (arch.contains("aarch")) "ubuntu-22.04-arm" else "ubuntu-22.04"
    }
    os.isWindows -> "windows-2022"
    else -> error("Unsupported OS")
}

// Source: https://github.com/jjohannes/java-module-system/blob/main/gradle/plugins/src/main/kotlin/targets.gradle.kts
// Configure variants for OS. Target name can be any string, but should match the name used in GitHub actions.
javaModulePackaging {
    target("ubuntu-22.04") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.X86_64
        packageTypes = listOf("deb")
    }
    target("ubuntu-22.04-arm") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.ARM64
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
