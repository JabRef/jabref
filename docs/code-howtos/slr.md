---
parent: Code Howtos
---

# Systematic Literature Reviews

A [systematic literature review](https://en.wikipedia.org/wiki/Systematic_review) is a structured way of finding and synthesizing all published research on a defined question. The method matters most in fields where the literature is growing faster than any one researcher can read — software engineering, medicine, policy research — and where reproducibility of the search itself is a quality criterion.

JabRef provides infrastructure for the search and study-management steps of an SLR. The longer-term goal is laid out in [Voigt, Kopp, and Wild's ZEUS 2021 paper](https://ceur-ws.org/Vol-2839/paper13.pdf), which surveys existing SLR tools, ranks the eight most-requested features (R1–R8 from Al-Zubidy and Carver's earlier interview study), and identifies JabRef as the open-source reference manager best positioned to close the remaining gaps. JabRef already supports R2 (deduplication), R3 (filtering), R4 (merging new results), R6 (a study repository), R7 (standardized export formats), and R8 (full-text PDF acquisition). The two requirements driving recent and current work are:

- **R1** — running a standardized query across multiple databases. Each catalog's API speaks a different query language, and JabRef's internal query transformer is lossy for power users who need publisher-native syntax. This gap is now closed by per-catalog raw query routing — see [Raw query routing](#raw-query-routing-catalog-specific) below.
- **R5** — synonym recommendation for search strings. Not currently scoped.

This page documents the v2 study format introduced in [#12642](https://github.com/JabRef/jabref/issues/12642), the architecture as it stands, and the raw query routing added on top of it. Active work is tracked on the [SLR Kanban board](https://github.com/users/koppor/projects/2).

## What an SLR looks like in JabRef

From a user's perspective, an SLR in JabRef has four pieces:

- A **study definition** — `study.yml` — declaring the queries, target catalogs, research questions, and authors.
- A **study repository** — a git-tracked directory that holds `study.yml`, `study-lock.yml`, and the crawl results, with per-catalog `.bib` files committed on each search.
- A **crawl** — JabRef runs every query against every enabled catalog, deduplicates the results, and commits them.
- A **lock file** — `study-lock.yml` — records the exact, resolved query sent to each active catalog, so re-running a crawl on an unchanged study is reproducible. See [The lock file](#the-lock-file-study-lockyml) below.

The user creates and edits the study through the "Manage SLR" dialog. The dialog reads and writes `study.yml`. The crawl itself is driven by the `Crawler` class in `jablib`.

## Architecture

SLR support spans both JabRef modules:

- **`jablib`** holds the data model and crawler logic. It has no UI dependencies, so it can be reused by JabKit (the headless CLI) and any future server-side tooling.
- **`jabgui`** holds the dialog that lets users create and edit studies.

### Reading and writing `study.yml`

Both module boundaries cross at `study.yml`. Two independent code paths touch the file:

```text
[Manage SLR dialog]                       [Crawler]
        |                                     |
ManageStudyDefinitionViewModel        StudyRepository
        |                                     |
        +--------- StudyYamlParser ----------+
                          |
                  study.yml on disk
```

The write path runs when a user saves the study from the dialog (`saveStudy()`) and again when a new study is first initialized (`StartNewStudyAction.crawlPreparation()`). The read path runs when `Crawler` constructs a `StudyRepository` over an existing study directory.

Because both paths go through `StudyYamlParser`, the v1→v2 migration only needs to be implemented in one place. See [The v2 schema](#the-v2-schema) below.

### The model layer

The shared data shuttle between UI and logic is `Study`, in `jablib/.../model/study/`:

- `Study` — top-level object representing one `study.yml`. Holds `version`, `authors`, `title`, `researchQuestions`, `queries`, `catalogs`, and `maxResultsPerCatalog`.
- `StudyCatalog` — one enabled or disabled catalog with an optional inclusion `reason`. Maps 1:1 to a `SearchBasedFetcher` by name (e.g. "IEEEXplore", "ACM Portal").
- `StudyQuery` — one search query string, optionally with `catalogSpecific` overrides (per-catalog native query syntax — see [Raw query routing](#raw-query-routing-catalog-specific) below).

The JavaFX view layer wraps these in observable equivalents:

- `StudyCatalogItem` (in `jabgui/.../slr/`) is the observable view of a `StudyCatalog` with JavaFX properties for the table bindings.

### The dialog

`ManageStudyDefinitionView` + `ManageStudyDefinitionViewModel` (in `jabgui/.../slr/`) drive the "Manage SLR" dialog. Two entry points:

- **New study**: dialog opens empty. On save, the ViewModel calls `StudyYamlParser.writeStudyYamlFile()` and creates a git commit.
- **Edit study**: dialog opens prefilled from an existing `Study` object. The ViewModel reconstructs `ObservableList`s for authors, questions, queries, and catalogs from the study, then writes back on save.

The dialog presents four tabs (authors, research questions, queries, catalogs). The catalog list is built by enumerating every available `SearchBasedFetcher` and joining against the saved `study.getCatalogs()` to preserve enabled state and reason text.

The dialog also has a "Share on SearchRxiv" action (`ManageStudyDefinitionView#shareOnSearchRxiv()` / `...ViewModel#shareOnSearchRxiv()`, backed by `SearchRxivExporter`), enabled once queries exist and at least one catalog is active. It predates the v2 schema and raw query work described here — shipped separately in [#15373](https://github.com/JabRef/jabref/pull/15373) — and is out of scope for this page.

### The crawl

When a study is crawled, the flow runs entirely in `jablib`:

```text
ExistingStudySearchAction.crawl()
        |
        v
new Crawler(studyDirectory, ...)
        |
        +-- StudyRepository (reads study.yml, sets up git, manages per-catalog .bib files)
        +-- StudyCatalogToFetcherConverter (StudyCatalog name -> SearchBasedFetcher instance)
        +-- StudyFetcher (runs queries x fetchers in parallel)
        |
        v
crawler.performCrawl() -> List<QueryResult> -> studyRepository.persist()
```

The `StudyFetcher` sends every query to every enabled fetcher (`searchQueries × activeFetchers`). Each fetcher returns a list of `BibEntry` objects. For paged fetchers, results are capped by a per-catalog result limit and trimmed to size; non-paged fetchers return whatever their single call yields, uncapped (see [Raw query routing](#raw-query-routing-catalog-specific) below for how the paged/non-paged split works). The effective limit resolves in order: a catalog's own `max-results` in `study.yml`, else the study's top-level `max-results-per-catalog`, else `StudyRepository.DEFAULT_RESULT_LIMIT` (100) — catalog names are matched case-insensitively throughout. Results are wrapped in `FetchResult` (one per fetcher per query), grouped by query into `QueryResult`, and persisted by `StudyRepository` as per-catalog `.bib` files in the study directory. Each crawl produces a git commit on a search branch.

### File and class reference

| What you're looking for | Where it lives |
| --- | --- |
| Top-level study model | `jablib/.../model/study/Study.java` |
| Catalog model | `jablib/.../model/study/StudyCatalog.java` |
| Query model | `jablib/.../model/study/StudyQuery.java` |
| YAML parser and migrator entry point | `jablib/.../logic/crawler/StudyYamlParser.java` |
| v1→v2 migration logic | `jablib/.../logic/crawler/StudyYamlV1Migrator.java` |
| Study repository (git, file I/O) | `jablib/.../logic/crawler/StudyRepository.java` |
| Crawler orchestration | `jablib/.../logic/crawler/Crawler.java` |
| Per-query/per-fetcher execution and raw query routing | `jablib/.../logic/crawler/StudyFetcher.java` |
| Catalog-name → fetcher resolution | `jablib/.../logic/crawler/StudyCatalogToFetcherConverter.java` |
| Dialog view | `jabgui/.../gui/slr/ManageStudyDefinitionView.java` |
| Dialog view-model | `jabgui/.../gui/slr/ManageStudyDefinitionViewModel.java` |
| Catalog item (observable view of `StudyCatalog`) | `jabgui/.../gui/slr/StudyCatalogItem.java` |
| Dialog FXML | `jabgui/.../resources/.../slr/ManageStudyDefinition.fxml` |

## The v2 schema

The current `study.yml` format is version `"2.0.0"`, introduced in [#12642](https://github.com/JabRef/jabref/issues/12642). A v2 file looks like:

```yaml
version: "2.0.0"
authors:
  - Researcher Name
title: Example Study
research-questions:
  - What does X look like in Y?
queries:
  - query: "machine learning" AND "code review"
    catalog-specific:
      IEEEXplore: "(\"Document Title\":machine learning) AND (\"Document Title\":code review)"
      ACM Portal: "[Title: machine learning] AND [Title: code review]"
catalogs:
  - name: IEEEXplore
    enabled: true
    reason: Primary source for software engineering venues
  - name: ACM Portal
    enabled: true
    reason: ""
```

A canonical fixture with every field populated lives at `jablib/src/test/resources/org/jabref/logic/crawler/study-v2-full.yml`.

### What changed from v1

| Field | v1 | v2 |
| --- | --- | --- |
| Schema version | (absent) | `version: "2.0.0"` at top |
| Catalog list key | `databases:` | `catalogs:` |
| Per-catalog inclusion reason | (no field) | `reason:` on each catalog entry |
| Per-catalog query overrides | (no field) | `catalog-specific:` map on each query |
| Per-catalog result cap | (no field) | `max-results:` on each catalog entry |
| Global result cap | (no field) | `max-results-per-catalog:` at top level |
| Last search date | `last-search-date:` at top level | dropped — no v2 equivalent |

The rename from `databases` to `catalogs` aligns the YAML key with the UI label (the dialog has always said "Catalogs") and matches the project's domain terminology.

The v1 `last-search-date` field is dropped during migration entirely, with nothing carrying its information forward into v2. `StudyYamlParserTest#readsJabRef57StudySuccessfully` (using a 5.7-era fixture) covers this specifically.

The new `reason` field captures why a catalog was included or excluded — useful for transparency when a study is published or reviewed.

The new `catalog-specific` map carries native-syntax query strings per catalog, which bypass JabRef's internal query transformer. See [Raw query routing](#raw-query-routing-catalog-specific).

### Migration

JabRef migrates v1 files to v2 transparently on read. The flow is in `StudyYamlParser.parseStudyYamlFile()`:

1. Read the raw YAML into a generic `Map<String, Object>` for inspection.
2. If the map has no `version` key, treat it as v1 and pass it through `StudyYamlV1Migrator.migrate(...)`, which transforms the YAML string into v2 shape (renames `databases` to `catalogs`, adds `version`, injects empty `reason` fields). The migrator operates on the YAML at the string/tree level, not on parsed Java objects.
3. If the version is present but doesn't match `Study.CURRENT_SCHEMA_VERSION`, log a warning and try to read it anyway. Jackson's `@JsonIgnoreProperties(ignoreUnknown = true)` on `Study` means forward-compatible additions (new fields in a hypothetical v3) won't break v2 readers — they'll just be silently dropped.
4. Pass the (possibly migrated) YAML string back to Jackson for deserialization into a `Study` object.

The migration is one-way — JabRef does not write v1 files. Once a v1 study is opened and saved, it's persisted as v2.

### Round-trip stability

`StudyYamlV1Migrator` is paired with `StudyYamlV1MigratorTest`, which uses golden-file fixtures for byte-for-byte migration verification:

- `study-v1-full.yml` + `study-v1-full-expected.yml` — verifies that a v1 file migrates to the expected v2 output, byte-for-byte (after CRLF normalization).
- `study-v1-minimal.yml` + `study-v1-minimal-expected.yml` — same, for the smallest valid v1 file.

`StudyYamlParser` is paired with `StudyYamlParserTest`, which covers:

- `study-v2-full.yml` — verifies that a native v2 file parses correctly without migration.
- Two object-equality round-trip tests — one for `catalog-specific` overrides, one for result limits — that write a parsed `Study` back to YAML, re-parse it, and assert equality. This catches semantic regressions that a string-comparison test would miss.

### Versioning policy

The `version` field uses a SEMVER string (`"2.0.0"`). The current version is exposed as `Study.CURRENT_SCHEMA_VERSION`. When the schema changes incompatibly, the major number increments and a new `StudyYamlVNMigrator` is added, chained from `StudyYamlParser`. Backward-compatible additions don't change the major version — they rely on Jackson's `ignoreUnknown` to handle older readers.

## Raw query routing (`catalog-specific`)

### Why it exists

JabRef's standard search path parses every query into a `BaseQueryNode` AST and runs it through a per-fetcher `QueryTransformer` to produce catalog-specific syntax. This works for simple queries, but SLR researchers often need the full power of a catalog's native syntax — field codes, proximity operators, and filters that the transformer cannot express (ZEUS R1). The `catalog-specific` field in `study.yml` lets a researcher supply a native query string per catalog, which is sent to the API verbatim:

```yaml
queries:
  - query: Quantum
    catalog-specific:
      IEEEXplore: "(Document Title:Quantum)"
      ACM Portal: "[Title: Quantum]"
```

Catalogs without an entry fall back to the standard transformed query. Catalog names are matched case-insensitively against `SearchBasedFetcher#getName()`; if two `catalog-specific` keys in `study.yml` differ only by case, the first matching entry wins. Blank or empty override values are treated as absent.

### The dispatch point

`StudyFetcher#performSearchOnQueryForFetcher` only decides which *shape* of search to run — paged (`fetcher instanceof PagedSearchBasedFetcher`) or non-paged — and delegates to `performPagedSearch` or `performNonPagedSearch` accordingly. Each of those two methods independently calls `getCatalogOverride(searchQuery, fetcher)` to look up a `catalogSpecific` entry for the active fetcher:

- **Override present, paged fetcher** → `performPagedSearch` loops `performRawSearchQueryPaged(rawQuery, page)` across the same page range the standard path would use, then applies the same per-catalog result limit and trim described above.
- **Override present, non-paged fetcher** → `performNonPagedSearch` calls `performRawSearchQuery(rawQuery)` directly. As noted above, non-paged fetchers aren't subject to any result limit, on either the raw or standard path.
- **No override** → the standard `performSearchPaged` / `performSearch` path runs unchanged.

If a fetcher has not been migrated (i.e. still inherits the default `UnsupportedOperationException` implementation), the exception is wrapped into a `FetcherException` and the catalog is omitted from the crawl results entirely — rather than silently falling back to the transformed query, which would mislead a researcher into thinking their native query ran. This currently surfaces in the log as `"{} API request failed"`, the same message used for a genuine network failure — there's no way yet to tell an unmigrated-fetcher rejection apart from an actual API outage by log message alone.

### Implementing raw query support on a fetcher

Most fetchers extend `SearchBasedParserFetcher` or `PagedSearchBasedParserFetcher` and only need to override the URL hook:

```java
@Override
public URL getURLForRawQuery(String rawQuery, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException {
    return buildSearchURL(rawQuery, pageNumber);
}
```

The base class handles the blank check, download, parsing, and post-cleanup — identical to the standard path. Extract shared URL construction into a private `buildSearchURL` helper used by both `getURLForQuery` and `getURLForRawQuery`.

Fetchers that don't fit this mold need a direct implementation of `performRawSearchQuery` / `performRawSearchQueryPaged`:

- **ArXiv** wraps an inner API client and applies async DOI enrichment on top; it implements `performRawSearchQueryPaged` on both layers.
- **Unpaywall** is intentionally not migrated — it is a DOI resolver with no text search API; its `performSearch` is a no-op stub.

Why this hook exists on the fetcher and not on `StudyFetcher` directly is explained in [ADR-0014](../decisions/0014-separate-URL-creation-to-enable-proper-logging.md) — URL/payload construction is kept inside the fetcher for proper logging, which is also why a fetcher's transformed query is a local variable that nothing outside it can currently read (relevant background for the lock file's current scope, below).

### Migration status

| Catalog | Status |
| --- | --- |
| IEEEXplore | Migrated |
| ACM Portal | Migrated |
| Scopus | Migrated |
| Springer | Migrated |
| arXiv | Migrated |
| SemanticScholar, ISIDORE, LOBID, ScholarArchive | Migrated |
| ADS | Migrated ([#16248](https://github.com/JabRef/jabref/pull/16248)) |
| Medline/PubMed | Migrated ([#16283](https://github.com/JabRef/jabref/pull/16283)) |
| CiteSeerX | Not supported (maintainer decision) |
| Unpaywall | Not supported (no text search API) |

*CiteSeerX was removed entirely rather than migrated — the fetcher, its tests, and its UI entries were deleted in [#16316](https://github.com/JabRef/jabref/pull/16316), following a dev-call decision ([#16299](https://github.com/JabRef/jabref/issues/16299)) that the underlying service was defunct (its own search results pointed to a Wayback Machine archive).*

Catalogs not listed here still throw `UnsupportedOperationException` on the raw path and can be migrated on demand using the `getURLForRawQuery` hook.

## The lock file (`study-lock.yml`)

Each crawl writes `study-lock.yml` next to `study.yml`, recording the *resolved* query sent to every active catalog: the `catalog-specific` override if one exists for that catalog, otherwise the plain study query. It does **not** record the transformed query the standard path actually sends to the API — that value is a local variable inside the fetcher and isn't currently surfaced anywhere outside it (see [ADR-0014](../decisions/0014-separate-URL-creation-to-enable-proper-logging.md) above).

The lock file is built entirely from `study.yml` — it never reads from crawl results — so re-crawling an unchanged study definition always produces byte-identical lock content. This makes the search history reproducible and auditable: given a `study.yml` and its `study-lock.yml`, you can see exactly which native or standard query was resolved for each catalog without re-running the crawl.

A catalog that's enabled in `study.yml` but has no matching `SearchBasedFetcher` (name mismatch, or a catalog JabRef doesn't support) is silently omitted from the lock file, with no warning logged — worth knowing if a lock file comes out shorter than expected.

Shipped in [#16298](https://github.com/JabRef/jabref/pull/16298), closing [#12640](https://github.com/JabRef/jabref/issues/12640).

## Roadmap

- **Surfacing the transformed query** — exposing the standard path's transformed query (not just raw overrides) so the lock file can record it too, per the note above. Needs a new fetcher-facing accessor; under design discussion.
- **Remaining fetcher migrations** — catalogs not present in study fixtures (DBLP, CrossRef, zbMATH, DOAJ, DOAB) can be migrated on demand.
- **`performSearch(BaseQueryNode)` as a default wrapper** — a longer-term refactor where the standard path becomes a default that transforms and delegates to the raw path, unifying the two code paths.

## Related documents

- [`docs/requirements/slr.md`](../requirements/slr.md) — the formal requirements this feature traces to. Changes here that touch requirement-level behavior need matching `[impl->req~slr...]` tags, or [OpenFastTrace](https://github.com/itsallcode/openfasttrace) fails CI — this is what broke CI on [#16298](https://github.com/JabRef/jabref/pull/16298).
- [ADR-0015](../decisions/0015-support-an-abstract-query-syntax-for-query-conversion.md) — the abstract query syntax the standard (non-raw) search path parses into.
- [ADR-0020](../decisions/0020-use-Jackson-to-parse-study-yml.md) — Jackson as the parser for `study.yml`, used throughout `StudyYamlParser`.
- [ADR-0021](../decisions/0021-keep-study-as-a-dto.md) — `Study` kept as a DTO, informing [The model layer](#the-model-layer) above.
- [Fetchers](fetchers.md) — the general fetcher implementation guide; overlaps with [Implementing raw query support on a fetcher](#implementing-raw-query-support-on-a-fetcher) above.

One terminology note: the code still uses "library"/"E-Library" internally in places (`getActiveLibraryEntries`, the `StudyFetcher` javadoc) left over from before the `catalogs`/`catalog` rename in the v2 schema, while `study.yml` and the UI consistently say "catalog." Worth keeping in mind if the two terms seem to refer to the same thing in different parts of the code — they do.

## Contributing

SLR-related work is tracked under the [SLR label](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3Aslr) and on the [Kanban board](https://github.com/users/koppor/projects/2). A good starting order for reading the code: this page, then `StudyYamlParser` and `ManageStudyDefinitionViewModel`, then `StudyRepository` and `StudyFetcher`.
