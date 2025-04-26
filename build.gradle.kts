plugins {
    id("checkstyle")
}

subprojects {
    apply(plugin = "checkstyle")

    checkstyle {
        toolVersion = "10.23.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    tasks.withType<Checkstyle> {
        reports {
            xml.required.set(false)
            html.required.set(true)
        }
        exclude("**/generated-sources/**")
    }

    configurations.named("checkstyle") {
        resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
            select("com.google.guava:guava:0")
        }
    }
}
