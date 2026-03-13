plugins {
    id("org.jabref.gradle.module")
    id("java-library")
}

dependencies {
    api(platform(project(":versions")))
    api("io.zonky.test:embedded-postgres")
}
