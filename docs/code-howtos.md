This page provides some development support in the form of howtos. See also [[High Level Documentation]].

## Table of Contents
  * [Generic code how tos](#generic-code-how-tos)
  * [Error Handling in JabRef](#error-handling-in-jabref)
    * [Throwing and Catching Exceptions](#throwing-and-catching-exceptions)
    * [Outputting Errors in the UI](#outputting-errors-in-the-ui)
  * [Using the EventSystem](#using-the-eventsystem)
  * [Logging](#logging)
  * [Using Localization correctly](#using-localization-correctly)
  * [Cleanup and Formatters](#cleanup-and-formatters)
  * [Drag and Drop](#drag-and-drop)
  * [Get the JabRef frame panel](#get-the-jabref-frame-panel)
  * [Get Absolute Filename or Path](#get-absolute-filename-or-path)
  * [Setting a Database Directory for a .bib File](#setting-a-database-directory-for-a-.bib-file)
  * [How to work with Preferences](#how-to-work-with-preferences)
  * [Test Cases](#test-cases)
    * [Lists in tests](#lists-in-tests)
    * [BibEntries in tests](#bibentries-in-tests)
    * [Files and folders](#files-and-folders-in-tests)
    * [Loading Files from Resources](#loading-files-from-resources)
    * [Preferences in tests](#preferences-in-tests)
  * [UI](#ui)
  * [UI for Preferences](#ui-for-preferences)
  * [Designing GUI Confirmation dialogs](#designing-gui-confirmation-dialogs)
  * ["Special Fields"](#special-fields)
    * [keywords sync](#keywords-sync)
  * [Working with BibTeX data](#working-with-bibtex-data)
    * [Working with authors](#working-with-authors)
  * [Benchmarks](#benchmarks)
  * [equals](#equals)
  * [Files & Paths](#files-and-paths)
  * [JavaFX](#javafx)


## Generic code how tos

Please read https://github.com/cxxr/better-java

- try not to abbreviate names of variables, classes or methods
- use lowerCamelCase instead of snake_case
- name enums in singular, e.g. `Weekday` instead of `Weekdays` (except if they represent flags)

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

## Using the EventSystem

### What the EventSystem is used for?
Many times there is a need to provide an object on many locations simultaneously.
This design pattern is quite similar to Java's Observer, but it is much simplier and readable while having the same functional sense.


### Main principle
`EventBus` represents a communication line between multiple components.
Objects can be passed through the bus and reach the listening method of another object which is registered on that `EventBus` instance.
Hence the passed object is available as a parameter in the listening method.

### Register to the `EventBus`
Any listening method has to be annotated with `@Subscribe` keyword and must have only one accepting parameter. Furthermore the object which contains such listening method(s) has to be registered using the `register(Object)` method provided by `EventBus`. The listening methods can be overloaded by using differnt parameter types.


### Posting an object
`post(object)` posts an object trough the `EventBus` which has been used to register the listening/subscribing methods.

### Short example
```java
/* Listener.java */

import com.google.common.eventbus.Subscribe;

public class Listener {
   
   private int value = 0;
   
   @Subscribe
   public void listen(int value) {
      this.value = value;
   }

   public int getValue() {
      return this.value;
   }
}
```

```java
/* Main.java */

import com.google.common.eventbus.EventBus;

public class Main {
   private static EventBus eventBus = new EventBus();
   
   public static void main(String[] args) {
      Main main = new Main();
      Listener listener = new Listener();
      eventBus.register(listener);
      eventBus.post(1); // 1 represents the passed event

      // Output should be 1
      System.out.println(listener.getValue());
   }
}
```

### Event handling in JabRef

The `event` package contains some specific events which occure in JabRef.

For example: Every time an entry was added to the database a new `EntryAddedEvent` is sent trough the `eventBus` which is located  in `BibDatabase`.

If you want to catch the event you'll have to register your listener class with the `registerListener(Object listener)` method in `BibDatabase`. `EntryAddedEvent` provides also methods to get the inserted `BibEntry`.


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
4. With `gradlew generateMissingTranslationKeys` the "KEY" is added to the other translation files as well.
5. Tests are green again. 

## Cleanup and Formatters

We try to build a cleanup mechanism based on formatters. The idea is that we can register these actions in arbitrary places, e.g., onSave, onImport, onExport, cleanup, etc. and apply them to different fields. The formatters themself are independent of any logic and therefore easy to test. 

Example: (PageNumbersFormatter)[https://github.com/JabRef/jabref/blob/master/src/main/java/net/sf/jabref/logic/formatter/bibtexfields/PageNumbersFormatter.java]

## Drag and Drop

`net.sf.jabref.external.DroppedFileHandler.handleDroppedfile(String, ExternalFileType, boolean, BibtexEntry) FileListEditor` sets a `TransferHandler` inherited from `FileListEditorTransferHandler`. There, at `importData`, a `DroppedFileHandler` is instantiated and `handleDroppedfile` called. 

## Get the JabRef frame panel

```java
net.sf.jabref.JabRefFrame jrf = JabRef.jrf;
net.sf.jabref.BasePanel basePanel = JabRef.jrf.basepanel();
```

## Get Absolute Filename or Path

```java
File f = FileUtil.expandFilename(basePanel.getDatabaseContext(), path, JabRefPreferences.getInstance().getFileDirectoryPreferences()).get(); 
```
`String path` Can be the files name or a relative path to it.


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

Defaults should go into the model package. See [here](https://github.com/JabRef/jabref/commit/2f553e6557bddf7753b618b0f4edcaa6e873f719#commitcomment-15779484) for the comment.

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

### BibEntries in tests
* Use the `assertEquals` methods in `BibtexEntryAssert` to check that the correct BibEntry is returned.

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

### Find out issues with the EDT

If someone wants to find out more, add the following first in `main` or `start` in `JabRefMain`:
``` Java
RepaintManager.setCurrentManager(new CheckingRepaintManager());
```
then it is just to work with JabRef until an exception happens.

Source: <https://github.com/JabRef/jabref/pull/1725>

## UI for Preferences

  * `JabRefFrame.preferences()` shows the preferences 
  * class: PrefsDialog3 

## Designing GUI Confirmation dialogs

1. Avoid asking questions 
2. Be as concise as possible
3. Identify the item at risk
4. Name your buttons for the actions

More information:
http://ux.stackexchange.com/a/768

## "Special Fields"

### keywords sync

Database.addDatabaseChangeListener does not work as the DatabaseChangedEvent does not provide the field information. Therefore, we have to use BibtexEntry.addPropertyChangeListener(VetoableChangeListener listener) 

## Working with BibTeX data

### Working with authors

You can normalize the authors using `net.sf.jabref.model.entry.AuthorList.fixAuthor_firstNameFirst(String)`. Then the authors always look nice. The only alternative containing all data of the names is `net.sf.jabref.model.entry.AuthorList.fixAuthor_lastNameFirst(String)`. The other `fix...` methods omit data (like the von parts or the junior information).

## Benchmarks
- Benchmarks can be executed by running the `jmh` gradle task (this functionality uses the [JMH Gradle plugin]( https://github.com/melix/jmh-gradle-plugin))
- Best practices:
  - Read test input from `@State` objects
  - Return result of calculations (either explicitly or via a `BlackHole` object)
- [List of examples](https://github.com/melix/jmh-gradle-example/tree/master/src/jmh/java/org/openjdk/jmh/samples)

## equals
When creating an `equals`method follow:
 1.   Use the `== `operator to check if the argument is a reference to this object. If so, return `true`.
 2.   Use the `instanceof` operator to check if the argument has the correct type. If not, return `false`.
 3.   Cast the argument to the correct type.
 4.   For each “significant” field in the class, check if that field of the argument matches the corresponding field of this object. If all these tests succeed, return `true` otherwise, return `false`.
 5.   When you are finished writing your equals method, ask yourself three questions: Is it symmetric? Is it transitive? Is it consistent?

Also, note:
 *   Always override `hashCode` when you override equals (`hashCode` also has very strict rules)
 *   Don’t try to be too clever
 *   Don’t substitute another type for `Object` in the equals declaration

##Files and Paths
Always try to use the methods from the nio-package. For interoperability, they provide methods to convert between file and path.
https://docs.oracle.com/javase/tutorial/essential/io/path.html
Mapping between old methods and new methods
https://docs.oracle.com/javase/tutorial/essential/io/legacy.html#mapping

##JavaFX

The following expressions can be used in FXML attributes, according to the [official documentation](https://docs.oracle.com/javase/8/javafx/api/javafx/fxml/doc-files/introduction_to_fxml.html#attributes)

Type | Expression | Value point to | Remark
--- | --- | --- | ---
Location | `@image.png` | path relative to the current FXML file | 
Resource | `%textToBeTranslated` | key in ResourceBundle | 
Attribute variable | `$idOfControl` or `$variable` | named control or variable in controller (may be path in the namespace) | resolved only once at load time
Expression binding | `${expression}` | expression, for example `textField.text` | changes to source are propagated
Bidirectional expression binding | `#{expression}` | expression | changes are propagated in both directions (not yet implemented in JavaFX, see [feature request](https://bugs.openjdk.java.net/browse/JDK-8090665))
Event handler | `#nameOfEventHandler` | name of the event handler method in the controller | 
Constant | `<text><Strings fx:constant="MYSTRING"/></text>` | constant (here `MYSTRING` in the `Strings` class) | 