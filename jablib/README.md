# `jablib`

## Development information

### Publish on Maven Central

1. [Get API username and password](https://central.sonatype.org/publish/generate-portal-token/)
2. Modify `~/.gradle/gradle.properties` to contain your secrets. See <https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets>.
3. Publish a snapshot: `./gradlew :jablib:publishAllPublicationsToMavenCentralRepository`

The [Vanniktech Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) is used for it.
