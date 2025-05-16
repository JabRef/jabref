
plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

publishing {
    publications {
        create<MavenPublication>("jablib") {
            from(components["java"])

            repositories {
                maven {
                    name = "sonatype"

                    url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                    credentials {
                        username = System.getenv("SONATYPE_USERNAME")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }
            }
        }
    }
}
