---
parent: Code Howtos
nav_order: 12
---
# Testing JabRef

## Background on Java testing

In JabRef, we mainly rely on basic JUnit tests to increase code coverage. There are other ways to test:

| Type           | Techniques                                 | Tool (Java)                                                             | Kind of tests                                                  | Used In JabRef                                                |
| -------------- | ------------------------------------------ | ----------------------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------- |
| Functional     | Dynamics, black box, positive and negative | [JUnit-QuickCheck](https://github.com/pholser/junit-quickcheck)         | Random data generation                                         | No, not intended, because other test kinds seem more helpful. |
| Functional     | Dynamics, black box, positive and negative | [GraphWalker](https://graphwalker.github.io)                            | Model-based                                                    | No, because the BibDatabase doesn't need to be tests          |
| Functional     | Dynamics, black box, positive and negative | [TestFX](https://github.com/TestFX/TestFX)                              | GUI Tests                                                      | Yes                                                           |
| Functional     | Dynamics, white box, negative              | [PIT](https://pitest.org)                                               | Mutation                                                       | No                                                            |
| Functional     | Dynamics, white box, positive and negative | [Mockito](https://site.mockito.org)                                     | Mocking                                                        | Yes                                                           |
| Non-functional | Dynamics, black box, positive and negative | [JETM](http://jetm.void.fm), [Apache JMeter](https://jmeter.apache.org) | Performance (performance testing vs load testing respectively) | No                                                            |
| Structural     | Static, white box                          | [CheckStyle](https://checkstyle.sourceforge.io)                         | Constient formatting of the source code                        | Yes                                                           |
| Structural     | Dynamics, white box                        | [SpotBugs](https://spotbugs.github.io)                                  | Reocurreing bugs (based on experience of other projects)       | No                                                            |

## General hints on tests

Imagine you want to test the method `format(String value)` in the class `BracesFormatter` which removes double braces in a given string.

* _Placing:_ all tests should be placed in a class named `classTest`, e.g. `BracesFormatterTest`.
* _Naming:_ the name should be descriptive enough to describe the whole test. Use the format `methodUnderTest_ expectedBehavior_context` (without the dashes). So for example `formatRemovesDoubleBracesAtBeginning`. Try to avoid naming the tests with a `test` prefix since this information is already contained in the class name. Moreover, starting the name with `test` leads often to inferior test names (see also the [Stackoverflow discussion about naming](http://stackoverflow.com/questions/155436/unit-test-naming-best-practices)).
*   _Test only one thing per test:_ tests should be short and test only one small part of the method. So instead of

    ```java
    testFormat() {
     assertEqual("test", format("test"));
     assertEqual("{test", format("{test"));
     assertEqual("test", format("test}}"));
    }
    ```

    we would have five tests containing a single `assert` statement and named accordingly (`formatDoesNotChangeStringWithoutBraces`, `formatDoesNotRemoveSingleBrace`, , etc.). See [JUnit AntiPattern](https://exubero.com/junit/anti-patterns/#Multiple\_Assertions) for background.
* Do _not just test happy paths_, but also wrong/weird input.
* It is recommend to write tests _before_ you actually implement the functionality (test driven development).
* _Bug fixing:_ write a test case covering the bug and then fix it, leaving the test as a security that the bug will never reappear.
* Do not catch exceptions in tests, instead use the `assertThrows(Exception.class, ()->doSomethingThrowsEx())` feature of [junit-jupiter](https://junit.org/junit5/docs/current/user-guide/) to the test method.

## Lists in tests

* Use `assertEquals(Collections.emptyList(), actualList);` instead of `assertEquals(0, actualList.size());` to test whether a list is empty.
*   Similarly, use `assertEquals(Arrays.asList("a", "b"), actualList);` to compare lists instead of

    ```java
           assertEquals(2, actualList.size());
           assertEquals("a", actualList.get(0));
           assertEquals("b", actualList.get(1));
    ```

## BibEntries in tests

* Use the `assertEquals` methods in `BibtexEntryAssert` to check that the correct BibEntry is returned.

## Files and folders in tests

*   If you need a temporary file in tests, then add the following Annotation before the class:

    ```java
    @ExtendWith(TempDirectory.class)
    class TestClass{

      @BeforeEach
      void setUp(@TempDirectory.TempDir Path temporaryFolder){
      }
    }
    ```

    to the test class. A temporary file is now created by `Files.createFile(path)`. Using this pattern automatically ensures that the test folder is deleted after the tests are run. See the [junit-pioneer doc](https://junit-pioneer.org/docs/temp-directory/) for more details.

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

## Database tests

### PostgreSQL

To quickly host a local PostgreSQL database, execute following statement:

```
docker run -d -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=postgres -p 5432:5432 --name db postgres:10 postgres -c log_statement=all
```

Set the environment variable `DBMS` to `postgres` (or leave it unset)

Then, all DBMS Tests (annotated with `@org.jabref.testutils.category.DatabaseTest`) run properly.

### MySQL

A MySQL DBMS can be started using following command:

```
docker run -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=jabref -p 3800:3307 mysql:8.0 --port=3307
```

Set the environment variable `DBMS` to `mysql`.
