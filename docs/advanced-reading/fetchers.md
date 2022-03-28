# Working on fetchers

Fetchers are the implementation of the [search using online services](https://docs.jabref.org/collect/import-using-online-bibliographic-database). Some fetchers require API keys to get them working. To get the fetchers running in a JabRef development setup, the keys need to be placed in the respective environment variable. The following table lists the respective fetchers, where to get the key from and the environment variable where the key has to be placed.

| Service                                                                                                                                           | Key Source                                                   | Environment Variable           | Rate Limit                       |
| ------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------ | -------------------------------- |
| [IEEEXplore](https://docs.jabref.org/collect/import-using-online-bibliographic-database#ieeexplore)                                               | [IEEE Xplore API portal](https://developer.ieee.org)         | `IEEEAPIKey`                   | 200 calls/day                    |
| [MathSciNet](http://www.ams.org/mathscinet)                                                                                                       | (none)                                                       | (none)                         | Depending on the current network |
| [SAO/NASA Astrophysics Data System](https://docs.jabref.org/collect/import-using-online-bibliographic-database#sao-nasa-astrophysics-data-system) | [ADS UI](https://ui.adsabs.harvard.edu/user/settings/token)  | `AstrophysicsDataSystemAPIKey` | 5000 calls/day                   |
| [ScienceDirect](https://www.sciencedirect.com)                                                                                                    |                                                              | `ScienceDirectApiKey`          |                                  |
| [Springer Nature](https://docs.jabref.org/collect/import-using-online-bibliographic-database#springer)                                            | [Springer Nature API Portal](https://dev.springernature.com) | `SpringerNatureAPIKey`         | 5000 calls/day                   |
| [Zentralblatt Math](https://www.zbmath.org)                                                                                                       | (none)                                                       | (none)                         | Depending on the current network |

"Depending on the current network" means that it depends on whether your request is routed through a network having paid access. For instance, some universities have subscriptions to MathSciNet.

On Windows, you have to log off and log on to let IntelliJ know about the environment variable change. Execute the gradle task `processResources` in the group "others" within IntelliJ to ensure the values have been correctly written. Now, the fetcher tests should run without issues.

JabRef supports different kinds of fetchers:

* `EntryBasedFetcher`: Completes an existing bibliographic entry with information retrieved by the fetcher
* `FulltextFetcher`: Searches for a PDF for an exiting bibliography entry
* `SearchBasedFetcher`: Searches providers using a given query and returns a set of (new) bibliography entry. The user-facing side is implemented in the UI described at [https://docs.jabref.org/collect/import-using-online-bibliographic-database](https://docs.jabref.org/collect/import-using-online-bibliographic-database).

There are more fetchers supported by JabRef. Investigate the package `org.jabref.logic.importer`. Another possibility is to investigate the inheritance relation of `WebFetcher` (Ctrl+H in IntelliJ).

## Fulltext Fetchers

* all fulltext fetchers run in parallel
* the result with the highest priority wins
* `InterruptedException` | `ExecutionException` | `CancellationException` are ignored

### Trust Levels

* `SOURCE `(highest): definitive URL for a particular paper
* `PUBLISHER`: any publisher library
* `PREPRINT`: any preprint library that might include non final publications of a paper
* `META_SEARCH`: meta search engines
* `UNKNOWN `(lowest): anything else not fitting the above categories

### Current trust levels

All fetchers are contained in the package `org.jabref.logic.importer.fetcher`. Here we list the trust levels of some of them:

* DOI: SOURCE, as the DOI is always forwarded to the correct publisher page for the paper
* ScienceDirect: Publisher
* Springer: Publisher
* ACS: Publisher
* IEEE: Publisher
* Google Scholar: META_SEARCH, because it is a search engine
* Arxiv: PREPRINT, because preprints are published there
* OpenAccessDOI: META_SEARCH

Reasoning:

* A DOI uniquely identifies a paper. Per definition, a DOI leads to the right paper. Everything else is good guessing.
* We assume the DOI resolution surely points to the correct paper and that publisher fetches may have errors: For instance, a title of a paper may lead to different publications of it. One the conference version, the other the journal version. --> the PDF could be chosen randomly

Code was first introduced at [PR#3882](https://github.com/JabRef/jabref/pull/3882).

## Background on embedding the keys in JabRef

The keys are placed into the `build.properties` file.

```
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
