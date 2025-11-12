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

### Use `@ParamterizedTests`

If possible, use `@ParamterizedTests`.
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
Path resourceDir = Paths.get(MSBibExportFormatTestFiles.class.getResource("MsBibExportFormatTest1.bib").toURI()).getParent();
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

## Testing different kinds of components

JabRef is split up in the java library ("jablib"), the CLI ("jabkit"), the HTTP server ("jabsrv"), and the GUI ("jabgui").
When executing tests in the sub project, the tests of the other sub projects are not executed.
When executing tests in the main project, all tests of the sub projects are executed.

The exceptions are the (SQL) database and fetcher tests.
They are marked with `@org.jabref.testutils.category.DatabaseTest`.

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

Then, all DBMS Tests (annotated with `@org.jabref.testutils.category.DatabaseTest`) run properly.

### Fetchers in tests

[JabRef can connect to external services to fetch bibliographic data](https://docs.jabref.org/collect/import-using-online-bibliographic-database).
Since API keys are required and some providers block requests from unknown IP addresses, these tests are not executed by default.
Detailed information is available at [JabRef's fetcher documentation](fetchers.md).

Each fetcher test is marked by `@org.jabref.testutils.category.FetcherTest`.
Some of them are also marked with `@org.jabref.support.DisabledOnCIServer`, to indicate that they are not executed on the CI server.
These test are not executed on the CI, because the rate limits of the API providers are too often reached during the build process.

Fetcher tests can be run locally by executing the Gradle task `fetcherTest`. This can be done by running the following command in the command line:

```shell
./gradlew fetcherTest
```

Alternatively, if one is using IntelliJ, this can also be done by double-clicking the `fetcherTest` task under the `other` group in the Gradle Tool window (`JabRef > Tasks > other > fetcherTest`).

### "No matching tests found"

In case the output is "No matching tests found", the wrong test category is used.

Check "Run/Debug Configurations"

Example

```gradle
:databaseTest --tests ":jablib:org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporterTest.pdfMetadataExtractedFrom2024SPLCBecker"
```

This tells Gradle that `PdfMergeMetadataImporterTest` should be executed as database test.
However, it is marked as `@FetcherTest`.
Thus, change `:databaseTest` to `:fetcherTest` to get the test running.

## Advanced testing and further reading

On top of basic unit testing, there are more ways to test a software:

| Type           | Techniques                                 | Tool (Java)                                                             | Kind of tests                                                  | Used In JabRef                                                |
| -------------- | ------------------------------------------ | ----------------------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------- |
| Functional     | Dynamics, black box, positive and negative | [JUnit-QuickCheck](https://github.com/pholser/junit-quickcheck)         | Random data generation                                         | No, not intended, because other test kinds seem more helpful. |
| Functional     | Dynamics, black box, positive and negative | [GraphWalker](https://graphwalker.github.io)                            | Model-based                                                    | No, because the BibDatabase doesn't need to be tests          |
| Functional     | Dynamics, black box, positive and negative | [TestFX](https://github.com/TestFX/TestFX)                              | GUI Tests                                                      | Yes                                                           |
| Functional     | Dynamics, black box, negative              | [Lincheck](https://github.com/JetBrains/lincheck)                       | Testing concurrent algorithms                                  | No                                                            |
| Functional     | Dynamics, white box, negative              | [PIT](https://pitest.org)                                               | Mutation                                                       | No                                                            |
| Functional     | Dynamics, white box, positive and negative | [Mockito](https://site.mockito.org)                                     | Mocking                                                        | Yes                                                           |
| Non-functional | Dynamics, black box, positive and negative | [JETM](http://jetm.void.fm), [Apache JMeter](https://jmeter.apache.org) | Performance (performance testing vs load testing respectively) | No                                                            |
| Structural     | Static, white box                          | [CheckStyle](https://checkstyle.sourceforge.io)                         | Constient formatting of the source code                        | Yes                                                           |
| Structural     | Dynamics, white box                        | [SpotBugs](https://spotbugs.github.io)                                  | Reocurreing bugs (based on experience of other projects)       | No                                                            |
