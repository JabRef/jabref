plugins {
    id("buildlogic.java-common-conventions")

    application

    // afterburner.fx
    id("org.openjfx.javafxplugin") version("0.1.0")
}

val luceneVersion = "10.2.0"

dependencies {
    implementation(project(":jablib"))

    // FIXME: Injector needs to be removed, no JavaFX dependencies, etc.
    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }

    implementation("commons-cli:commons-cli:1.9.0")

    // Because of GraalVM quirks, we need to ship that. See https://github.com/jspecify/jspecify/issues/389#issuecomment-1661130973 for details
    implementation("org.jspecify:jspecify:1.0.0")

    implementation("org.slf4j:slf4j-api:2.0.17")
    // implementation("org.tinylog:tinylog-api:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("org.slf4j:slf4j-api:2.0.17")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog in the CLI and GUI)
    implementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }

    implementation("commons-cli:commons-cli:1.9.0")

    implementation("org.apache.lucene:lucene-queryparser:${luceneVersion}")

    testImplementation(project(":test-support"))
    testImplementation("org.mockito:mockito-core:5.17.0")
}

/*
jacoco {
    toolVersion = "0.8.13"
}
*/

javafx {
    version = "24"
    // because of afterburner.fx
    modules = listOf("javafx.base", "javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.jabref.cli.JabKit")
    mainModule.set("org.jabref.jabkit")
}
