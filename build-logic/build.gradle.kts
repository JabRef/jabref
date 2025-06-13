import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.github.andygoossens:gradle-modernizer-plugin:1.11.0")
    implementation("org.gradlex:extra-java-module-info:1.12")
    implementation("org.gradlex:java-module-packaging:1.0.1") // required for platform-specific packaging of JavaFX dependencies
    implementation("org.gradlex:java-module-testing:1.7")
    implementation("org.gradlex.jvm-dependency-conflict-resolution:org.gradlex.jvm-dependency-conflict-resolution.gradle.plugin:2.4")
}

// TODO jjohannes: is this still needed and where should it go?
configurations
    .named { it.contains("downloadSources") }
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
