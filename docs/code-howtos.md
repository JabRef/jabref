This page provides some development support in the form of howtos. See also [[High Level Documentation]].

## Table of Contents
  * [Generic code how tos](#generic-code-how-tos)
  * [Error Handling in JabRef](#error-handling-in-jabref)
    * [Throwing and Catching Exceptions](#throwing-and-catching-exceptions)
    * [Outputting Errors in the UI](#outputting-errors-in-the-ui)
  * [Logging](#logging)
  * [Using Localization correctly](#using-localization-correctly)
  * [Cleanup / Formatters](#cleanup-/-formatters)
  * [Drag and Drop](#drag-and-drop)
  * [Get the JabRef frame / panel](#get-the-jabref-frame-/-panel)
  * [Get Absolute/Expanded Filename](#get-absolute/expanded-filename)
  * [Setting a Database Directory for a .bib File](#setting-a-database-directory-for-a-.bib-file)
  * [How to work with Preferences](#how-to-work-with-preferences)
  * [Test Cases](#test-cases)
    * [Lists in tests](#lists-in-tests)
    * [Files and folders](#files-and-folders-in-tests)
    * [Loading Files from Resources](#loading-files-from-resources)
    * [Preferences in tests](#preferences-in-tests)

  * [UI](#ui)
  * [UI for Preferences](#ui-for-preferences)
  * ["Special Fields"](#special-fields)
    * [keywords sync](#keywords-sync)
  * [Working with BibTeX data](#working-with-bibtex-data)
    * [Working with authors](#working-with-authors)

## Generic code how tos

Please read https://github.com/cxxr/better-java

- try not to abbreviate names of variables, classes or methods
- use lowerCamelCase instead of snake_case

## Error Handling in JabRef

### Throwing and Catching Exceptions
Principles: 
- All Exceptions we throw should be or extend `JabRefException`
- Catch and wrap all API exceptions (such as `IOExceptions`) and rethrow them
 - Example:
  ```java
  try {
      // ...
  } catch (IOException ioe) {
      throw new JabRefException("Something went wrong...", 
           Localization.lang("Something went wrong...", ioe);
  }
  ```
- Never, ever throw and catch `Exception` or `Throwable`
- Errors should only be logged when they are finally caught (i.e., logged only once). See **Logging** for details.
- If the Exception message is intended to be shown to the User in the UI (see below) provide also a localizedMessage (see `JabRefException`).

*(Rationale and further reading: https://today.java.net/article/2006/04/04/exception-handling-antipatterns)*

### Outputting Errors in the UI
Principle: Error messages shown to the User should not contain technical details (e.g., underlying exceptions, or even stack traces). Instead, the message should be concise, understandable for non-programmers and localized.

To show error message two different ways are usually used in JabRef:
- showing an error dialog
- updating the status bar at the bottom of the main window

*TODO: Usage of status bar and Swing Dialogs*

## Logging

JabRef uses the logging facade [Apache Commons Logging](http://commons.apache.org/proper/commons-logging/).
All log messages are passed internally to Java's [java.util.logging](http://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html) which handles any filterting, formatting and writing of log messages. 
- Obtaining a Logger for a Class: 

  ```java
  private static final Log LOGGER = LogFactory.getLog(<ClassName>.class);
  ```

- If the logging event is caused by an exception, please add the exception to the log message as: 

  ```java
    catch (SomeException e) {
       LOGGER.warn("Warning text.", e);
       ...
    }
  ```

## Using Localization correctly
*(More information about this topic from the translator side is provided [here in the wiki](https://github.com/JabRef/jabref/wiki/Translating-JabRef))*

All labeled UI elements, descriptions and messages shown to the user should be localized, i.e., should be displayed in the chosen language.

JabRef uses ResourceBundles ([see Oracle Tutorial](https://docs.oracle.com/javase/tutorial/i18n/resbundle/concept.html)) to store `key=value` pairs for each String to be localized. 

To show an localized String the following `net.sf.jabref.logic.l10n.Localization` has to be used. The Class currently provides three methods to optain translated strings:

```java
    public static String lang(String key);
    
    public static String lang(String key, String... params);

    public static String menuTitle(String key, String... params);
```

The actual usage might look like:

```java
    Localization.lang("Get me a translated String");
    Localization.lang("Using %0 or more %1 is also possible", "one", "parameter");
    Localization.menuTitle("Used for Menus only");
```

General hints:

- Use the String you want to localize directly, do not use members or local variables: `Localization.lang("Translate me");` instead of `Localization.lang(someVariable)` (possibly in the form `someVariable = Localization.lang("Translate me")`
- Use `%x`-variables where appropriate: `Localization.lang("Exported %0 entries.", number)` instead of `Localization.lang("Exported ") + number + Localization.lang(" entries.");`
- Use a full stop/period (".") to end full sentences

The tests check whether translation strings appear correctly in the resource bundles.

1. Add new `Localization.lang("KEY")` to Java file.
2. Tests fail. In the test output a snippet is generated which must be added to the English translation file. There is also a snippet generated for the non-English files, but this is irrelevant.
3. Add snippet to English translation file located at `src/main/resources/l10n/JabRef_en.properties`
4. With `gradlew -b localization.gradle compareAndUpdateTranslationsWithEnglishTranslation` the "KEY" is added to the other translation files as well.
5. Tests are green again. 

## Cleanup / Formatters

We try to build a cleanup mechanism based on formatters. The idea is that we can register these actions in arbitrary places, e.g., onSave, onImport, onExport, cleanup, etc. and apply them to different fields. The formatters themself are independent of any logic and therefore easy to test. 

Example: (PageNumbersFormatter)[https://github.com/JabRef/jabref/blob/master/src/main/java/net/sf/jabref/logic/formatter/bibtexfields/PageNumbersFormatter.java]

## Drag and Drop

`net.sf.jabref.external.DroppedFileHandler.handleDroppedfile(String, ExternalFileType, boolean, BibtexEntry) FileListEditor` sets a `TransferHandler` inherited from `FileListEditorTransferHandler`. There, at `importData`, a `DroppedFileHandler` is instantiated and `handleDroppedfile` called. 

## Get the JabRef frame / panel

```java
net.sf.jabref.JabRefFrame jrf = JabRef.jrf;
net.sf.jabref.BasePanel basePanel = JabRef.jrf.basepanel();
```

## Get Absolute/Expanded Filename

```java
File f = Util.expandFilename(flEntry.getLink(), frame.basePanel().metaData().getFileDirectory(GUIGlobals.FILE_FIELD)); 
```

## Setting a Database Directory for a .bib File

  * @comment{jabref-meta: fileDirectory:&lt;directory&gt;} 
  * “fileDirectory” is determined by Globals.pref.get(“userFileDir”) (which defaults to “fileDirectory” 
  * There is also “fileDirectory-&lt;username&gt;”, which is determined by Globals.prefs.get(“userFileDirIndividual”) 
  * Used at DatabasePropertiesDialog 

## How to work with Preferences

`model` and `logic` must not know JabRefPreferences. See `ProxyPreferences` for encapsulated preferences and https://github.com/JabRef/jabref/pull/658 for a detailed discussion.


`Globals.prefs` is a global variable storing a link to the preferences form. 

`Globals.prefs.getTYPE(key)` returns the value of the given configuration key. TYPE has to be replaced by Boolean, Double, Int, ByteArray. If a string is to be put, the method name is only “get”. 

To store the configuration keys in constants, one has two options 

  * as constant in the own class 
  * as constant in `net.sf.jabref.JaRefPreferences.java`

There are JabRef classes existing, where the strings are hard-coded and where constants are not used. That way of configuration should be avoided. 

When adding a new preference, following steps have to be taken: 

  * add a constant for the configuration key 
  * in net.sf.jabref.JaRefPreferences.java put a “defaults.put(&lt;configuration key&gt;, &lt;value&gt;)” statement 

When accessing a preference value, the method Globals.prefs.getTYPE(key) has to be used. 

## Test Cases
Imagine you want to test the method `format(String value)` in the class `BracesFormatter` which removes double braces in a given string.
- *Placing:* all tests should be placed in a class named `classTest`, e.g. `BracesFormatterTest`. 
- *Naming:* the name should be descriptive enough to describe the whole test. Use the format `methodUnderTest_ expectedBehavior_context` (without the dashes). So for example `formatRemovesDoubleBracesAtBeginning`. Try to avoid naming the tests with a `test` prefix since this information is already contained in the class name. Moreover, starting the name with `test` leads often to inferior test names (see also the [Stackoverflow discussion about naming](http://stackoverflow.com/questions/155436/unit-test-naming-best-practices)).
- *Test only one thing per test:* tests should be short and test only one small part of the method. So instead of 
````
testFormat() {
   assertEqual("test", format("test"));
   assertEqual("{test", format("{test"));
   assertEqual("test", format("{{test"));
   assertEqual("test", format("test}}"));
   assertEqual("test", format("{{test}}"));
}
````
we would have five tests containing a single `assert` statement and named accordingly (`formatDoesNotChangeStringWithoutBraces`, `formatDoesNotRemoveSingleBrace`, `formatRemovesDoubleBracesAtBeginning`, etc.). See [JUnit AntiPattern](http://www.exubero.com/junit/antipatterns.html#Multiple_Assertions) for background.
- Do *not just test happy paths*, but also wrong/weird input.
- It is recommend to write tests *before* you actually implement the functionality (test driven development). 
- *Bug fixing:* write a test case covering the bug and then fix it, leaving the test as a security that the bug will never reappear.
- Do not catch exceptions in tests, instead add `@Test(expected=ExpectedException.class)` to the test method.

### Lists in tests
* Use `Assert.assertEquals(Collections.emptyList(), actualList);` instead of `Assert.assertEquals(0, actualList.size());` to test whether a list is empty.
* Similarly, use `Assert.assertEquals(Arrays.asList("a", "b"), actualList);` to compare lists instead of 
```` java
         Assert.assertEquals(2, actualList.size());
         Assert.assertEquals("a", actualList.get(0));
         Assert.assertEquals("b", actualList.get(1));
````
### Files and folders in tests
* If you need a temporary file in tests, then add 
```` java
         @Rule
         public TemporaryFolder testFolder = new TemporaryFolder();
````
to the test class. A temporary file is now created by `File tempFile = testFolder.newFile("file.txt");`. Using this pattern automatically ensures that the test folder is deleted after the tests are run. See the [blog of Gary Gregory](https://garygregory.wordpress.com/2010/01/20/junit-tip-use-rules-to-manage-temporary-files-and-folders/) for more details.

### Loading Files from Resources:
Sometimes it is necessary to load a specific resource as a File:
```` java
AuxCommandLineTest.class.getResource("paper.aux");
````
This returns an java.net.URL object. To avoid problems with whitespaces or any other special characters, the File creation should always be done with the Paths-Class.
```` java
File f = Paths.get(url.toUri()).toFile(); 
//concrete example
File auxFile = Paths.get(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toFile(); 
````
For more information see discussions at 
http://stackoverflow.com/questions/6164448/convert-url-to-normal-windows-filename-java

### Preferences in tests
If `Globals.prefs` are not initialized in a test case, try to add

```java
@BeforeClass
public static void setUp() {
    Globals.prefs = JabRefPreferences.getInstance();
}
```

If you modify preference, use following pattern to ensure that the stored preferences of a developer are not affected:

```java
private JabRefPreferences backup;

@Before
public void setUp() {
    prefs = JabRefPreferences.getInstance();
    backup = prefs;
}

@After
public void tearDown() {
    //clean up preferences to default state
    prefs.overwritePreferences(backup);
}
```

Or even better, try to mock the preferences and insert them via dependency injection.
````java
@Test
public void getTypeReturnsBibLatexArticleInBibLatexMode() {
     // Mock preferences
     JabrefPreferences mockedPrefs = mock(JabrefPreferences.class);        
     // Switch to BibLatex mode
     when(mockedPrefs.getBoolean("BiblatexMode")).thenReturn(true);

     // Now test
     EntryTypes biblatexentrytypes = new EntryTypes(mockedPrefs);
     assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article"));
}
````

## UI

Global variables are OK here. Global variables are NOT OK everywhere else, especially model and logic.

## UI for Preferences

  * `JabRefFrame.preferences()` shows the preferences 
  * class: PrefsDialog3 

## "Special Fields"

### keywords sync

Database.addDatabaseChangeListener does not work as the DatabaseChangedEvent does not provide the field information. Therefore, we have to use BibtexEntry.addPropertyChangeListener(VetoableChangeListener listener) 

## Working with BibTeX data

### Working with authors

You can normalize the authors using `net.sf.jabref.model.entry.AuthorList.fixAuthor_firstNameFirst(String)`. Then the authors always look nice. The only alternative containing all data of the names is `net.sf.jabref.model.entry.AuthorList.fixAuthor_lastNameFirst(String)`. The other `fix...` methods omit data (like the von parts or the junior information).