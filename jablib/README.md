# `jablib`

## Development information

### Publish on Maven Central

1. [Get API username and password](https://blog.solidsoft.pl/2015/09/08/deploy-to-maven-central-using-api-key-aka-auth-token/)
2. Set environment variable `SONATYPE_USERNAME` to your token username.
3. Set environment variable `SONATYPE_PASSWORD` to your token password.
4. Adapt `~/.gradle.properties` to contain your GnuPG key information. See <https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials>
5. Publish the artifact to the staging repository: `./gradlew :jablib:publishToSonatype`
6. Close the staging repository: `./gradlew :jablib:findSonatypeStagingRepository :jablib:closeSonatypeStagingRepository`
7. Release the staging repository: `./gradlew :jablib:findSonatypeStagingRepository :jablib:releaseSonatypeStagingRepository`
