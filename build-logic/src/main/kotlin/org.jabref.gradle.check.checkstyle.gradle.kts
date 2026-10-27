plugins {
    id("checkstyle")
}

checkstyle {
    toolVersion = "10.23.0"
    configFile = File(rootDir, "config/checkstyle/checkstyle.xml")
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required = false
        html.required = true
    }
    source = fileTree("src") {
        include("**/*.java")
        // Submodule content is checked in its own repository.
        exclude("main/themes.jabref.org/**")
    }
}
