import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.gradlex:extra-java-module-info:1.12")
    implementation("org.gradlex:java-module-dependencies:1.9")
    implementation("org.gradlex:java-module-packaging:1.0")
    implementation("org.gradlex:java-module-testing:1.7")
    implementation("org.gradlex.jvm-dependency-conflict-detection:org.gradlex.jvm-dependency-conflict-detection.gradle.plugin:2.3")

    configurations
        .matching { it.name.contains("downloadSources") }
        .configureEach {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attribute(
                    OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                    objects.named(OperatingSystemFamily::class.java, DefaultNativePlatform.getCurrentOperatingSystem().name)
                )
                attribute(
                    MachineArchitecture.ARCHITECTURE_ATTRIBUTE,
                    objects.named(MachineArchitecture::class.java, DefaultNativePlatform.getCurrentArchitecture().name)
                )
            }
        }
}
