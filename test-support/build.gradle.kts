plugins {
    id("buildlogic.java-common-conventions")

    id("org.openjfx.javafxplugin") version("0.1.0")
}

dependencies {
    implementation(project(":jablib"))

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.tinylog:tinylog-api:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("org.junit.jupiter:junit-jupiter-api:5.12.2")

    implementation("org.mockito:mockito-core:5.18.0") {
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    implementation("net.bytebuddy:byte-buddy:1.17.5")

    implementation("org.jspecify:jspecify:1.0.0")
}

javafx {
    version = "24"
    // because of afterburner.fx
    modules = listOf("javafx.base", "javafx.controls", "javafx.fxml")
}
