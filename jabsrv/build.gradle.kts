import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("buildlogic.java-common-conventions")

    `java-library`

    id("org.openjfx.javafxplugin") version("0.1.0")
}

dependencies {
    api(project(":jablib"))

    implementation("org.slf4j:slf4j-api:2.0.17")

    // API
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")

    // Implementation of the API
    implementation("org.glassfish.jersey.core:jersey-server:3.1.10")

    // Injection framework
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.10")
    implementation("org.glassfish.hk2:hk2-api:3.1.1")
    implementation("org.glassfish.hk2:hk2-utils:3.1.1")
    implementation("org.glassfish.hk2:hk2-locator:3.1.1")

    // testImplementation("org.glassfish.hk2:hk2-testing:3.0.4")
    // implementation("org.glassfish.hk2:hk2-testing-jersey:3.0.4")
    // testImplementation("org.glassfish.hk2:hk2-junitrunner:3.0.4")

    // HTTP server
    // implementation("org.glassfish.jersey.containers:jersey-container-netty-http:3.1.1")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.10")
    implementation("org.glassfish.grizzly:grizzly-http-server:4.0.2")
    implementation("org.glassfish.grizzly:grizzly-framework:4.0.2")
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:3.1.10")
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.hibernate.validator:hibernate-validator:9.0.0.Final")

    implementation("com.konghq:unirest-modules-gson:4.4.7")

    // Allow objects "magically" to be mapped to JSON using GSON
    // implementation("org.glassfish.jersey.media:jersey-media-json-gson:3.1.1")

    implementation("com.google.guava:guava:33.4.8-jre")

    implementation("org.jabref:afterburner.fx:2.0.0") {
        exclude( group = "org.openjfx")
    }

    implementation("net.harawata:appdirs:1.4.0")

    implementation("de.undercouch:citeproc-java:3.3.0") {
        exclude(group = "org.antlr")
    }

    testImplementation("org.mockito:mockito-core:5.18.0") {
        exclude(group = "net.bytebuddy", module = "byte-buddy")
    }
    testImplementation("net.bytebuddy:byte-buddy:1.17.5")

    testImplementation("org.tinylog:slf4j-tinylog:2.7.0")
    testImplementation("org.tinylog:tinylog-impl:2.7.0")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    testImplementation("org.slf4j:jul-to-slf4j:2.0.17")
    // route all requests to log4j to SLF4J
    testImplementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

}

javafx {
    version = "24"
    // because of afterburner.fx
    modules = listOf("javafx.base", "javafx.controls", "javafx.fxml")
}

tasks.test {
    testLogging {
        // set options for log level LIFECYCLE
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }
    maxParallelForks = 1
}
