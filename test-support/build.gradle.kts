plugins {
    id("org.jabref.gradle.module")
    id("java-library")
}

dependencies {
    implementation(project(":jablib"))

    implementation("org.openjfx:javafx-base")
    implementation("org.openjfx:javafx-controls")
    implementation("org.openjfx:javafx-fxml")

    implementation("org.slf4j:slf4j-api")
    implementation("org.tinylog:tinylog-api")
    implementation("org.tinylog:slf4j-tinylog")
    implementation("org.tinylog:tinylog-impl")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j")

    implementation("org.junit.jupiter:junit-jupiter-api")

    implementation("org.mockito:mockito-core")
    implementation("net.bytebuddy:byte-buddy")

    implementation("org.jspecify:jspecify")

    implementation("com.tngtech.archunit:archunit")
    implementation("com.tngtech.archunit:archunit-junit5-api")
}
