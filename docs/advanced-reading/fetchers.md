# Working on fetchers

Fetchers are the implementation of the [search using online services](https://docs.jabref.org/collect/import-using-online-bibliographic-database). Some fetchers require API keys to get them working. To get the fetchers running in a JabRef development setup, the keys need to be placed in the respective enviornment variable. The following table lists the respective fetchers, where to get the key from and the environment variable where the key has to be placed.

| Service | Key Source | Environment Variable | Rate Limit |
| :--- | :--- | :--- | :--- |
| [IEEEXplore](https://docs.jabref.org/collect/import-using-online-bibliographic-database/ieeexplore) | [IEEE Xplore API portal](https://developer.ieee.org/) | `IEEEAPIKey` | 200 calls/day |
| [MathSciNet](http://www.ams.org/mathscinet) | \(none\) | \(none\) | Depending on the current network |
| [SAO/NASA Astrophysics Data System](https://docs.jabref.org/collect/import-using-online-bibliographic-database/ads) | [ADS UI](https://ui.adsabs.harvard.edu/user/settings/token) | `AstrophysicsDataSystemAPIKey` | 5000 calls/day |
| [Springer Nature](https://docs.jabref.org/collect/import-using-online-bibliographic-database/springer) | [Springer Nature API Portal](https://dev.springernature.com/) | `SpringerNatureAPIKey` | 5000 calls/day |
| [Zentralblatt Math](https://www.zbmath.org/) | \(none\) | \(none\) | Depending on the current network |

"Depending on the current network" means that it depends whether your request is routed through a network having paid access. For instance, some universities have subscriptions to MathSciNet.

On Windows, you have to log-off and log-on to let IntelliJ know about the environment variable change. Execute the gradle task "processResources" in the group "others" within IntelliJ to ensure the values have been correctly written. Now, the fetcher tests should run without issues.

## Background on embedding the keys in JabRef

The keys are placed into the `build.properties` file.

```text
springerNatureAPIKey=${springerNatureAPIKey}
```

In `build.gradle`, these variables are filled:

```groovy
"springerNatureAPIKey": System.getenv('SpringerNatureAPIKey')
```

The `BuildInfo` class reads from that file.

```java
new BuildInfo().springerNatureAPIKey
```

When executing `./gradlew run`, gradle executes `processResources` and populates `build/build.properties` accordingly. However, when working directly in the IDE, Eclipse keeps reading `build.properties` from `src/main/resources`. In IntelliJ, the task `JabRef Main` is executing `./gradlew processResources` before running JabRef from the IDE to ensure the `build.properties` is properly populated.

