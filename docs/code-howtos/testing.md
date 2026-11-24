---
parent: Code Howtos
---
# Testing JabRef

In JabRef, we mainly rely on basic [JUnit](https://junit.org/junit5/docs/current/user-guide/) unit tests to increase code coverage.

## General hints on tests

Imagine you want to test the method `format(String value)` in the class `BracesFormatter` which removes double braces in a given string.

* _Placing:_ all tests should be placed in a class named `classTest`, e.g. `BracesFormatterTest`.
* _Naming:_ the name should be descriptive enough to describe the whole test. Use the format `methodUnderTest_ expectedBehavior_context` (without the dashes). So for example `formatRemovesDoubleBracesAtBeginning`. Try to avoid naming the tests with a `test` prefix since this information is already contained in the class name. Moreover, starting the name with `test` leads often to inferior test names (see also the [Stackoverflow discussion about naming](http://stackoverflow.com/questions/155436/unit-test-naming-best-practices)).
* _Test only one thing per test:_ tests should be short and test only one small part of the method. So instead of

    ```java
    void format() {
        assertEqual("test", format("test"));
        assertEqual("{test", format("{test"));
        assertEqual("test", format("test}}"));
    }
    ```

    we would have five tests containing a single `assert` statement and named accordingly (`formatDoesNotChangeStringWithoutBraces`, `formatDoesNotRemoveSingleBrace`, , etc.). See [JUnit AntiPattern](https://exubero.com/junit/anti-patterns/#Multiple_Assertions) for background.
* Do _not just test happy paths_, but also wrong/weird input.
* It is recommended to write tests _before_ you actually implement the functionality (test driven development).
* _Bug fixing:_ write a test case covering the bug and then fix it, leaving the test as a security that the bug will never reappear.
* Do not catch exceptions in tests, instead use the `assertThrows(Exception.class, () -> doSomethingThrowsEx())` feature of [junit-jupiter](https://junit.org/junit5/docs/current/user-guide/) to the test method.

### Use `@ParameterizedTest`

If possible, use `@ParameterizedTest`.
Read more at <https://mikemybytes.com/2021/10/19/parameterize-like-a-pro-with-junit-5-csvsource/>.

Example for a nicely formatted `@CsvSource`

```java
@ParameterizedTest
@CsvSource(textBlock = """
    # underscore removed
    junit_jupiter, JunitJupiter
    # camel case kept
    fooBar,        FooBar
    CsvSource,     CsvSource
""")
void convertsToUpperCamelCase(String input, String expected) {
    String converted = caseConverter.toUpperCamelCase(input);
    Assertions.assertEquals(expected, converted);
}
```

## Profiling test memory

See [Profiling Gradle test memory with JFR](profiling-gradle-tests.md) for test-worker recordings, allocation analysis, live heap capture, and VisualVM retention analysis.

## Legacy library migration profiling

Generate a deterministic 100,000-entry migration fixture from the repository root:

```sh
python3 scripts/bib-file-generator_legacy.py
```

The script creates `generated-large-legacy-library.bib` and refuses to overwrite an existing file.
Use `--entries`, `--separator`, and `--output` for smaller fixtures or libraries with a semicolon keyword separator.
The fixture contains large and overlapping explicit groups, nested hierarchy modes, duplicate and missing citation keys, markings, and special field values stored in keywords.
It also includes keyword, regular-expression, search, automatic, and legacy smart groups.

Measure parsing separately from `PerformLoadDatabaseMigrations` with a JVM flight recording.
Migration runs before GUI attachment: only conversions with remaining legacy content run, using temporarily detached entries and a single citation-key index.
Entries are reinserted in a batch to restore observation and database indices, and the library is marked modified so the converted representation can be saved.
A localized notification announces migration when legacy content is found.
Save and reopen the fixture to verify that no conversion runs again; the content checks still run to detect legacy data in mixed-format libraries.

## Coverage

IntelliJ has build in test coverage reports. Choose "Run with coverage".

For a full coverage report as HTML, execute the gradle task `jacocoTestReport` (available in the "verification" folder in IntelliJ).
Then, you will find <build/reports/jacoco/test/html/index.html> which shows the coverage of the tests.

## Lists in tests

Instead of

```java
assertTrue(actualList.isEmpty());
```

use

```java
assertEquals(List.of(), actualList);
```

Similarly, to compare lists, instead of following code:

```java
assertEquals(2, actualList.size());
assertEquals("a", actualList.get(0));
assertEquals("b", actualList.get(1));
```

use the following code:

```java
assertEquals(List.of("a", "b"), actualList);
```

## BibEntries in tests

* Use the `assertEquals` methods in `BibtexEntryAssert` to check that the correct BibEntry is returned.

## Files and folders in tests

If you need a temporary file in tests, use the `@TempDir` annotation:

```java
class TestClass{

  @Test
  void deletionWorks(@TempDir Path tempDir) {
  }
}
```

to the test class. A temporary file is now created by `Files.createFile(path)`. Using this pattern automatically ensures that the test folder is deleted after the tests are run. See <https://www.geeksforgeeks.org/junit-5-tempdir/>  for more details.

## Loading Files from Resources

Sometimes it is necessary to load a specific resource or to access the resource directory

```java
Path resourceDir = Paths.get(MSBibImportExportRoundTripFilesTest.class.getResource("MsBibImportExportRoundTripTest1.bib").toURI()).getParent();
```

When the directory is needed, it is important to first point to an actual existing file. Otherwise the wrong directory will be returned.

## Preferences in tests

If you modify preference, use following pattern to ensure that the stored preferences of a developer are not affected:

Or even better, try to mock the preferences and insert them via dependency injection.

```java
@Test
public void getTypeReturnsBibLatexArticleInBibLatexMode() {
     // Mock preferences
     PreferencesService mockedPrefs = mock(PreferencesService.class);
     GeneralPreferences mockedGeneralPrefs = mock(GeneralPReferences.class);
     // Switch to BibLatex mode
     when(mockedPrefs.getGeneralPrefs()).thenReturn(mockedGeneralPrefs);
     when(mockedGeneralPrefs.getDefaultBibDatabaseMode())
        .thenReturn(BibDatabaseMode.BIBLATEX);

     // Now test
     EntryTypes biblatexentrytypes = new EntryTypes(mockedPrefs);
     assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article"));
}
```

To test that a preferences migration works successfully, use the mockito method `verify`. See `PreferencesMigrationsTest` for an example.

## Localization in tests

In case you or your called code uses `Localization.lang`, annotate your test with following annotation:

```java
@ResourceLock("Localization.lang")
```

## Testing different kinds of components

JabRef is split up in the java library ("jablib"), the CLI ("jabkit"), the HTTP server ("jabsrv"), and the GUI ("jabgui").
When executing tests in the sub project, the tests of the other sub projects are not executed.
When executing tests in the main project, all tests of the sub projects are executed.

The exceptions are the (SQL) database and fetcher tests.
They are marked with `@org.jabref.support.DatabaseTest`.

### Database tests

JabRef can [use an external PostgreSQL database to store bibliographic data](https://docs.jabref.org/collaborative-work/sqldatabase).
The tests require such an external database while running.
Therefore, these tests are annotated with `@DatabaseTest` and are not executed by default.

### PostgreSQL

To quickly host a local PostgreSQL database, execute following statement:

```shell
docker run -d -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=postgres -p 5432:5432 --name db postgres:10 postgres -c log_statement=all
```

Set the environment variable `DBMS` to `postgres` (or leave it unset)

Then, all DBMS Tests (annotated with `@org.jabref.support.DatabaseTest`) run properly.

### Fetchers in tests

[JabRef can connect to external services to fetch bibliographic data](https://docs.jabref.org/collect/import-using-online-bibliographic-database).
Since API keys are required and some providers block requests from unknown IP addresses, these tests are not executed by default.
Detailed information is available at [JabRef's fetcher documentation](fetchers.md).

Each fetcher test is marked by `@org.jabref.support.ExternalServicesTest`.
Some of them are also marked with `@org.jabref.support.DisabledOnCIServer`, to indicate that they are not executed on the CI server.
These test are not executed on the CI, because the rate limits of the API providers are too often reached during the build process.

Fetcher tests can be run locally by executing the Gradle task `externalServicesTest`. This can be done by running the following command in the command line:

```shell
./gradlew externalServicesTest
```

Alternatively, if one is using IntelliJ, this can also be done by double-clicking the `externalServicesTest` task under the `other` group in the Gradle Tool window (`JabRef > Tasks > other > externalServicesTest`).

### RTF formatter tests

`RTFCharsTest` covers LaTeX highlighting and character conversion for RTF exports.
The highlighting tests include commands ending at the field boundary, both without a brace group and with a missing closing brace.
These inputs must preserve the available text without reading past the end of the field.

### Converter integration tests

The BIBFRAME converter integration tests are marked with `@Tag("converter")`. They run the Library of Congress MARC-to-BIBFRAME and BIBFRAME-to-MARC converters, so `git`, `make`, and `xsltproc` must be available on `PATH`.

If you do not already have the converter repositories, clone them next to the JabRef checkout. Run these commands from the JabRef checkout root:

```shell
git clone https://github.com/lcnetdev/marc2bibframe2.git ../marc2bibframe2
git -C ../marc2bibframe2 checkout ed9abb038214474e8fc8ba4035d01c42fe0246de
git clone https://github.com/lcnetdev/bibframe2marc.git ../bibframe2marc
git -C ../bibframe2marc checkout 36a96c813437ac714e8c9479b2f7be56dc78671d
```

Run the task from the JabRef checkout root:

```shell
./gradlew :jablib:converterTest
```

To run only one test method, use Gradle's standard class and method filter:

```shell
./gradlew :jablib:test --tests "org.jabref.logic.exporter.BibframeConverterIntegrationTest.bookSurvivesConverterChain"
```

Gradle passes sibling checkout paths to all `jablib` test tasks as `MARC2BIBFRAME2_DIR` and `BIBFRAME2MARC_DIR`. The test verifies each checkout is at the pinned commit above. If a sibling checkout is missing, JUnit skips the tests that need it. If the repositories are elsewhere, set those environment variables to their paths before running Gradle.

### LibreOffice Writer integration tests

Run the real-Writer safety gate with:

```sh
./gradlew :jablib:libreOfficeTest
```

This dedicated task runs the `LibreOfficeTest`-tagged tests and requires LibreOffice Writer and Pandoc; missing executables fail the gate rather than skipping Writer tests.
`HeadlessLibreOffice` reads `LIBREOFFICE_EXECUTABLE`, falling back to `soffice`; tests needing Pandoc set their preferences from `PANDOC_EXECUTABLE`, falling back to `pandoc`.
Set these variables to full executable paths when they are not on `PATH`.
For example, in PowerShell:

```powershell
$env:LIBREOFFICE_EXECUTABLE = 'C:\Program Files\LibreOffice\program\soffice.exe'
$env:PANDOC_EXECUTABLE = 'C:\Program Files\Pandoc\pandoc.exe'
.\gradlew.bat :jablib:libreOfficeTest
```

To reproduce the workflow's separate OpenOffice unit-test step, also run:

```sh
./gradlew :jablib:test --tests 'org.jabref.logic.openoffice.*' --rerun --no-build-cache --console=plain
```

Put Pandoc on the process-local `PATH` for this step: `BstPipelineTest` invokes `pandoc` by name and does not read `PANDOC_EXECUTABLE`.
Its Pandoc-dependent tests can skip locally; CI rejects those skips, so a green unit task alone is not proof that the full gate ran.

#### Harness and fixtures

The tests start an isolated headless Writer with a temporary user profile and a loopback-only UNO listener, not an existing office session or the developer's preferences.
Human-readable golden `.fodt` documents and native Zotero JSON fixtures live under `jablib/src/test/resources/org/jabref/logic/openoffice/integration`.
There are two package-local `HeadlessLibreOffice` helpers: one in `org.jabref.logic.openoffice.integration` and one in `org.jabref.logic.openoffice.engine.storage`.
Both own the process and documents, disable macros, and preserve documents and a process log for inspection.
The suite exercises canonical JStyle, CSL, and BST citations, canonical cross-family style changes with save/reopen, all six stored types, stable identities, per-citation page information, numbering, bibliography ordering, invisible citations, footnotes, tables, repeated superscript refresh, and restored change recording.
`WriterIntegrationTest.numericNarrativeCitationsRetainTheirTypesAcrossStyleChangesAndReopen` inserts numeric normal and in-text citations with JStyle or BST, then checks marker text and stable identities through CSL/JStyle/BST refresh and save/reopen.
Pure renderer tests cover numeric JStyle's configured local order, separators, page information, and first/repeated author rules, plus BST in-text output across `NUMERIC`, `AUTHOR_YEAR`, and `STYLE_DEFINED` in storage order.
`CslRendererTest` checks sorted numeric author/number pairing, interleaved unresolved keys, repeated sources, authors outside superscript markers, and author/editor/key fallbacks.
It also checks bounded author–year in-text conversion, including no-date and comma-free forms, year suffixes, and unchanged markup or unfamiliar output.
The canonical mark grammar is `JABREF_<keys> <TYPE> <uid>`, for example `JABREF_Smith2000 NORMAL group-17`; the uid-before-type order is not a fallback format.
Legacy `JR_cite*` and `CID_n` fixtures are persistence oracles for operations that do not trigger migration; canonical reading and export must not interpret legacy marks as canonical citations.

#### Insertion-only migration and conversion

Detection checks must use fresh reference-mark names on each insertion, including after reconnecting, and decide solely from the first recognized name returned by Writer.
Recognized formats are canonical `JABREF_<keys> <TYPE> <uid>`, supported legacy `JR_cite` names, and native Zotero citation names; skip unrelated marks and unsupported `CID_n` marks.
Do not scan for later mismatching formats; no recognized name means no conversion.
Conversion acceptance covers homogeneous source documents; mixed managed formats and mixed-format repair are unsupported, not actively rejected by a mixed-format detector.
UNO name enumeration is not guaranteed sorted or in physical document order; enumeration order is immaterial for homogeneous documents.
For homogeneous supported legacy JStyle input selected by that detection, migration occurs on the next JStyle/BST insertion or CSL insertion with the saved Zotero setting off; the controller shows the format-conversion warning before mutation.
`LegacyJStyleMigrationWriterTest` exercises JStyle and non-native CSL migration; BST migration detection has unit coverage in `CitationFormatConverterTest`, but the Writer migration suite has no BST case.
Check all six numeric types, storage-order keys, fresh stable identities, transfer of the whole-mark-named page property to the last storage-order citation's canonical slot, and canonical invisible boundaries.
A positive name-only detection decision does not replace actual group reads: preparation still reads the applicable canonical, legacy, and native groups, validates them, and renders the proposed set once before writes.
Rendering failure must leave the document unchanged, and ordinary application failure must use owned conversion rollback.
Legacy-only canonical migration must accept unresolved entries with ordinary placeholders and the successful-insertion warning; opening missing libraries and refreshing restores resolved output.
Undo must restore legacy marks and page properties; redo must restore the prepared canonical identities and page slots.
Connecting, refreshing, exporting, style selection, and native Zotero insertion/conversion must leave legacy JStyle marks unchanged; `CID_n` must remain unmanaged in every mode.
Rendering failures must leave preparation targets unchanged, including missing Pandoc when BST output requires it.
BST `AUTHOR_YEAR` without requested bibliography output skips source loading, VM execution, and Pandoc for both normal and in-text groups; other notations retain program-derived labels and ordering.
JStyle bibliography paragraph validation applies only when bibliography output is requested.
Only successful explicit canonical refresh sweeps orphan canonical page-info properties globally; other actions still remove properties for citations they replace or remove.
GUI style-selection tests verify that each non-null selected style eagerly loads its preview even on an inactive tab, including when CSL discovery completes after the dialog opens; loading is not deferred until tab activation.

`CitationFormatConversionWriterTest` covers homogeneous canonical ↔ native Zotero conversion on the next CSL citation insertion, with the format-conversion warning, not on preference changes, refresh, or export.
JStyle/BST insertion must not convert native Zotero input; native Zotero mode must not migrate legacy JStyle input.

Conversion changes citation marks only: with synchronization disabled, all bibliography section names, text, and properties remain unchanged; with synchronization enabled, normal handling updates or creates only the target mode's managed bibliography and leaves other-format sections alone.
There is no bibliography conversion, renaming, consolidation, or rejection based on bibliography settings.
Canonical/native conversion acceptance must check lossy target-format output, not source-format restoration after save/reopen or reverse conversion:

* Canonical → native drops the original uid and all per-citation page information; `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY` become normal visible citations.
* Native → canonical creates fresh uids and only normal or narrative types. Original native JSON locators, labels, prefixes, suffixes, author-only flags, unknown fields, item IDs, and native Zotero URI links are not carried into canonical storage.
* Reverse conversion does not restore discarded information; native output rebuilds JSON from the JabRef library.
* Native modifiers do not by themselves block conversion; any `suppress-author: true` item maps the whole group to narrative, including mixed groups.
* Ordinary native insertion and refresh preserve existing native JSON while updating links; test that same-format contract separately.

See the overview's [Limitations](openoffice/overview.md#limitations).
Unsuitable custom properties, protected-range intersections, and affected pending deleted ranges are rejected before actual conversion.
Pending deletions touching edited citations, their footnote anchors, the insertion range, or a bibliography selected for synchronization remain unsafe even with recording off.
Unrelated pending bibliography deletions must survive citation-only conversion with synchronization off; suspending recording does not resolve them.
Native-to-canonical conversion and native generation still reject unresolved sources.
Canonical output skips native-only flattening and item-ID preparation; an empty native citation set skips source linking.
Conversion retains only target-format state, with no persistent source-format record.

#### Undo, recovery, and property boundaries

Storage, `DocumentWriteSessionTest`, and GUI/controller tests complement the real-Writer suite with codec/property checks, warning order, and owned conversion rollback.
Insertion (including conversion), refresh, merge, and separate use undo contexts; custom-property undo deltas and automatic conversion-failure rollback are separate mechanisms.
Conversion rollback requires an available, unlocked undo manager and a root context of its own, closes and verifies that context before undoing an ordinary application failure, and never undoes an earlier user action.
Immediate Writer Undo/Redo must restore the prepared transaction while its undo history is available; this is distinct from reverse format conversion, and there is no restoration guarantee after reopening.
Starting the transaction can clear pre-existing redo history even if rollback restores document state; disconnects, lost ownership, or failed rollback have a distinct recovery error, not a universal recovery guarantee.
Canonical CSL operations and conversions targeting canonical format must leave `ZOTERO_PREF` XML untouched while still updating JabRef's applied style.
Native-mode insertion/refresh updates native preferences; conversion targeting native mode includes prepared preference deltas in Undo/Redo.
Ordinary applied-style and native preference writes use properties directly and are not made undoable by those conversion-specific deltas.

#### Reports and CI minimums

Saved documents and office process logs are under `jablib/build/writer-integration/` in `engine-acceptance`, `zotero-acceptance`, `canonical-storage`, `entity-contract`, `format-conversion`, and `legacy-jstyle-migration`.
Standard JUnit XML results are in `jablib/build/test-results/libreOfficeTest/` and the HTML report is in `jablib/build/reports/tests/libreOfficeTest/`; the separate unit step uses the corresponding `test/` directories.
The task runs serially, always executes instead of restoring cached results, and has a 10-minute task timeout; Writer startup is bounded to one minute and each tagged test invocation to 180 seconds.
Ordinary `:jablib:test` excludes the `LibreOfficeTest` tag.

`.github/workflows/tests-libreoffice.yml` requires named tagged reports with minimum invocations of 28 (`WriterIntegrationTest`), 25 (`ZoteroWriterTest`), 20 (`CanonicalStorageWriterTest`), 6 (`OOTextWriterTest`), and 48 (`CitationFormatConversionWriterTest`).
`LegacyJStyleMigrationWriterTest` also runs in the tagged task, but has no separate required-report/minimum entry in the workflow.
The workflow additionally checks named unit reports; see the [fixture README](https://github.com/JabRef/jabref/blob/main/jablib/src/test/resources/org/jabref/logic/openoffice/integration/README.md#storage-regression-gate) for their minimums and package locations.
Every discovered tagged and unit report must have zero skips, failures, and errors.
These are CI discovery minimums, not current suite sizes; do not replace them with counts observed in a local run.
CI uploads documents, logs, and both sets of reports even on failure; making **Required Writer integration** mandatory in branch protection is a separate repository setting.
See the [current overview](openoffice/overview.md), [ordering limitations](openoffice/order-of-appearance.md#visual-ordering-limitations), and the [fixture README](https://github.com/JabRef/jabref/blob/main/jablib/src/test/resources/org/jabref/logic/openoffice/integration/README.md) for the contract and tested-layout limitations.

### "No matching tests found"

In case the output is "No matching tests found", the wrong test category is used.

Check "Run/Debug Configurations"

Example

```gradle
:databaseTest --tests ":jablib:org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporterTest.pdfMetadataExtractedFrom2024SPLCBecker"
```

This tells Gradle that `PdfMergeMetadataImporterTest` should be executed as database test.
However, it is marked as `@ExternalServicesTest`.
Thus, change `:databaseTest` to `:externalServicesTest` to get the test running.

## Advanced testing and further reading

On top of basic unit testing, there are more ways to test a software:

| Type           | Techniques                                 | Tool (Java)                                                             | Kind of tests                                                  | Used In JabRef                                                |
|----------------|--------------------------------------------|-------------------------------------------------------------------------|----------------------------------------------------------------|---------------------------------------------------------------|
| Functional     | Dynamics, black box, positive and negative | [JUnit-QuickCheck](https://github.com/pholser/junit-quickcheck)         | Random data generation                                         | No, not intended, because other test kinds seem more helpful. |
| Functional     | Dynamics, black box, positive and negative | [GraphWalker](https://graphwalker.github.io)                            | Model-based                                                    | No, because the BibDatabase doesn't need to be tests          |
| Functional     | Dynamics, black box, positive and negative | [JavaFX](https://openjfx.io/)                                           | GUI tests                                                      | Yes                                                           |
| Functional     | Dynamics, black box, negative              | [Lincheck](https://github.com/JetBrains/lincheck)                       | Testing concurrent algorithms                                  | No                                                            |
| Functional     | Dynamics, white box, negative              | [PIT](https://pitest.org)                                               | Mutation                                                       | No                                                            |
| Functional     | Dynamics, white box, positive and negative | [Mockito](https://site.mockito.org)                                     | Mocking                                                        | Yes                                                           |
| Non-functional | Dynamics, black box, positive and negative | [JETM](http://jetm.void.fm), [Apache JMeter](https://jmeter.apache.org) | Performance (performance testing vs load testing respectively) | No                                                            |
| Structural     | Static, white box                          | [CheckStyle](https://checkstyle.sourceforge.io)                         | Constient formatting of the source code                        | Yes                                                           |
| Structural     | Dynamics, white box                        | [SpotBugs](https://spotbugs.github.io)                                  | Reocurreing bugs (based on experience of other projects)       | No                                                            |
