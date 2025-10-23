import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.jabref.gradle.module")
    id("java-library")
}

dependencies {
    api(project(":jablib"))

    implementation("org.slf4j:slf4j-api")

    // API
    implementation("jakarta.ws.rs:jakarta.ws.rs-api")

    // Implementation of the API
    implementation("org.glassfish.jersey.core:jersey-server")

    // Injection framework
    implementation("org.glassfish.jersey.inject:jersey-hk2")
    implementation("org.glassfish.hk2:hk2-api")
    implementation("org.glassfish.hk2:hk2-utils")
    implementation("org.glassfish.hk2:hk2-locator")

    // testImplementation("org.glassfish.hk2:hk2-testing")
    // implementation("org.glassfish.hk2:hk2-testing-jersey")
    // testImplementation("org.glassfish.hk2:hk2-junitrunner")

    // HTTP server
    // implementation("org.glassfish.jersey.containers:jersey-container-netty-http")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http")
    implementation("org.glassfish.grizzly:grizzly-http-server")
    implementation("org.glassfish.grizzly:grizzly-framework")
    implementation("org.glassfish.jaxb:jaxb-runtime")
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.hibernate.validator:hibernate-validator")

    implementation("com.konghq:unirest-modules-gson")

    // Allow objects "magically" to be mapped to JSON using GSON
    // implementation("org.glassfish.jersey.media:jersey-media-json-gson")

    implementation("com.google.guava:guava")
    implementation("com.github.ben-manes.caffeine:caffeine") {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }

    implementation("org.jabref:afterburner.fx")
    implementation("org.openjfx:javafx-base")
    implementation("org.openjfx:javafx-controls")
    implementation("org.openjfx:javafx-fxml")

    implementation("net.harawata:appdirs")

    implementation("de.undercouch:citeproc-java")

    testImplementation("org.mockito:mockito-core")
    testImplementation("net.bytebuddy:byte-buddy")

    testImplementation("org.tinylog:slf4j-tinylog")
    testImplementation("org.tinylog:tinylog-impl")
    // route all requests to java.util.logging to SLF4J (which in turn routes to tinylog)
    testImplementation("org.slf4j:jul-to-slf4j")
    // route all requests to log4j to SLF4J
    testImplementation("org.apache.logging.log4j:log4j-to-slf4j")

}

javaModuleTesting.whitebox(testing.suites["test"]) {
    requires.add("jul.to.slf4j")
    requires.add("jersey.test.framework.core")
    requires.add("org.junit.jupiter.api")
    requires.add("org.mockito")
}

tasks.test {
    testLogging {
        // set options for log level LIFECYCLE
        events("FAILED")
        exceptionFormat = TestExceptionFormat.FULL
    }
    maxParallelForks = 1
}
