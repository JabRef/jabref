# `jablib`

## Development information

### Publish on Maven Central

1. [Get API username and password](https://central.sonatype.org/publish/generate-portal-token/)
2. Modify `~/.gradle/gradle.properties` to contain your secrets. See <https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets>.
   Especially have your private key stored in `singing.key`. [Hints are available](https://github.com/gradle/gradle/issues/15718#issuecomment-886246583).
3. Publish a snapshot: `./gradlew :jablib:publishAllPublicationsToMavenCentralRepository`
4. You will find the upload at <https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/jabref/jablib/>

The [Vanniktech Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) is used for it.
