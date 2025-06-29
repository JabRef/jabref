plugins {
    id("checkstyle")
}

checkstyle {
    toolVersion = "10.23.0"
    configFile = File(rootDir, "config/checkstyle/checkstyle.xml")
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
    source = fileTree("src") { include("**/*.java") }
}
