# Code Howtos

This page provides some development support in the form of howtos. See also [High Level Documentation](high-level-documentation.md).

## Generic code how tos

We really recommend reading the book [Java by Comparison](http://java.by-comparison.com/).

Please read [https://github.com/cxxr/better-java](https://github.com/cxxr/better-java)

* try not to abbreviate names of variables, classes or methods
* use lowerCamelCase instead of snake\_case
* name enums in singular, e.g. `Weekday` instead of `Weekdays` \(except if they represent flags\)

## Error Handling in JabRef

### Throwing and Catching Exceptions

Principles:

* All Exceptions we throw should be or extend `JabRefException`; This is especially important if the message stored in the Exception should be shown to the user. `JabRefException` has already implemented the `getLocalizedMessage()` method which should be used for such cases \(see details below!\).
* Catch and wrap all API exceptions \(such as `IOExceptions`\) and rethrow them
  * Example:

    ```java
       try {
           // ...
       } catch (IOException ioe) {
           throw new JabRefException("Something went wrong...",
               Localization.lang("Something went wrong...", ioe);
       }
    ```
* Never, ever throw and catch `Exception` or `Throwable`
* Errors should only be logged when they are finally caught \(i.e., logged only once\). See **Logging** for details.
* If the Exception message is intended to be shown to the User in the UI \(see below\) provide also a localizedMessage \(see `JabRefException`\).

_\(Rationale and further reading:_ [https://www.baeldung.com/java-exceptions](https://www.baeldung.com/java-exceptions)_\)_

### Outputting Errors in the UI

Principle: Error messages shown to the User should not contain technical details \(e.g., underlying exceptions, or even stack traces\). Instead, the message should be concise, understandable for non-programmers and localized. The technical reasons \(and stack traces\) for a failure should only be logged.

To show error message two different ways are usually used in JabRef:

* showing an error dialog
* updating the status bar at the bottom of the main window

```text
TODO: Usage of status bar and Swing Dialogs
```

## Using the EventSystem

### What the EventSystem is used for

Many times there is a need to provide an object on many locations simultaneously. This design pattern is quite similar to Java's Observer, but it is much simpler and readable while having the same functional sense.

### Main principle

`EventBus` represents a communication line between multiple components. Objects can be passed through the bus and reach the listening method of another object which is registered on that `EventBus` instance. Hence, the passed object is available as a parameter in the listening method.

### Register to the `EventBus`

Any listening method has to be annotated with `@Subscribe` keyword and must have only one accepting parameter. Furthermore, the object which contains such listening method\(s\) has to be registered using the `register(Object)` method provided by `EventBus`. The listening methods can be overloaded by using different parameter types.

### Posting an object

`post(object)` posts an object through the `EventBus` which has been used to register the listening/subscribing methods.

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

The `event` package contains some specific events which occur in JabRef.

For example: Every time an entry was added to the database a new `EntryAddedEvent` is sent through the `eventBus` which is located in `BibDatabase`.

If you want to catch the event you'll have to register your listener class with the `registerListener(Object listener)` method in `BibDatabase`. `EntryAddedEvent` provides also methods to get the inserted `BibEntry`.

## Logging

JabRef uses the logging facade [SLF4j](https://www.slf4j.org/). All log messages are passed internally to [log4j2](https://logging.apache.org/log4j/2.x/) which handles any filtering, formatting and writing of log messages.

* Obtaining a logger for a class:

  ```java
  private static final Logger LOGGER = LoggerFactory.getLogger(<ClassName>.class);
  ```

* If the logging event is caused by an exception, please add the exception to the log message as:

  ```java
    catch (SomeException e) {
       LOGGER.warn("Warning text.", e);
       ...
    }
  ```

* SLF4J also support parameterized logging, e.g. if you want to print out multiple arguments in a log statement use a pair of curly braces. [Examples](https://www.slf4j.org/faq.html#logging_performance)

## Using Localization correctly

More information about this topic from the translator side is provided at [Translating JabRef Interface](https://docs.jabref.org/faqcontributing/how-to-translate-the-ui).

All labeled UI elements, descriptions and messages shown to the user should be localized, i.e., should be displayed in the chosen language.

JabRef uses ResourceBundles \([see Oracle Tutorial](https://docs.oracle.com/javase/tutorial/i18n/resbundle/concept.html)\) to store `key=value` pairs for each String to be localized.

To show an localized String the following `org.jabref.logic.l10n.Localization` has to be used. The Class currently provides three methods to obtain translated strings:

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

* Use the String you want to localize directly, do not use members or local variables: `Localization.lang("Translate me");` instead of `Localization.lang(someVariable)` \(possibly in the form `someVariable = Localization.lang("Translate me")`
* Use `%x`-variables where appropriate: `Localization.lang("Exported %0 entries.", number)` instead of `Localization.lang("Exported ") + number + Localization.lang(" entries.");`
* Use a full stop/period \("."\) to end full sentences

The tests check whether translation strings appear correctly in the resource bundles.

1. Add new `Localization.lang("KEY")` to Java file. Run the `LocalizationConsistencyTest`under \(src/test/org.jabref.logic.

   \)

2. Tests fail. In the test output a snippet is generated which must be added to the English translation file.
3. Add snippet to English translation file located at `src/main/resources/l10n/JabRef_en.properties`
4. Please do not add translations for other languages directly in the properties. They will be overwritten by [Crowdin](https://crowdin.com/project/jabref)

## Adding a new Language

1. Add the new Language to the Language enum in [https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/l10n/Language.java](https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/l10n/Language.java)
2. Create an empty &lt;locale code&gt;.properties file
3. Configure the new language in [Crowdin](https://crowdin.com/project/jabref)

If the language is a variant of a language `zh_CN` or `pt_BR` it is necessary to add a language mapping for Crowdin to the crowdin.yml file in the root. Of course the properties file also has to be named according to the language code and locale.

## Cleanup and Formatters

We try to build a cleanup mechanism based on formatters. The idea is that we can register these actions in arbitrary places, e.g., onSave, onImport, onExport, cleanup, etc. and apply them to different fields. The formatters themselves are independent of any logic and therefore easy to test.

Example: [NormalizePagesFormatter](https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/formatter/bibtexfields/NormalizePagesFormatter.java)

## Drag and Drop

Drag and Drop makes usage of the Dragboard. For JavaFX the following [tutorial](https://docs.oracle.com/javafx/2/drag_drop/jfxpub-drag_drop.htm) is helpful. Note that the data has to be serializable which is put on the dragboard. For drag and drop of Bib-entries between the maintable and the groups panel, a custom Dragboard is used, `CustomLocalDragboard` which is a generic alternative to the system one.

For accessing or putting data into the Clipboard use the `ClipboardManager`.

## Get the JabRef frame panel

`JabRefFrame` and `BasePanel` are the two main classes. You should never directly call them, instead pass them as parameters to the class.

## Get Absolute Filename or Path for file in File directory

```java
Optional<Path> file = FileHelper.expandFilename(database, fileText, preferences.getFilePreferences());
```

`String path` Can be the files name or a relative path to it. The Preferences should only be directly accessed in the GUI. For the usage in logic pass them as parameter

## Setting a Database Directory for a .bib File

* `@comment{jabref-meta: fileDirectory:<directory>`
* “fileDirectory” is determined by Globals.pref.get\(“userFileDir”\) \(which defaults to “fileDirectory”
* There is also “fileDirectory-&lt;username&gt;”, which is determined by Globals.prefs.get\(“userFileDirIndividual”\)
* Used at DatabasePropertiesDialog

## How to work with Preferences

`model` and `logic` must not know `JabRefPreferences`. See `ProxyPreferences` for encapsulated preferences and [https://github.com/JabRef/jabref/pull/658](https://github.com/JabRef/jabref/pull/658) for a detailed discussion.

See [https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/preferences/TimestampPreferences.java](https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/preferences/TimestampPreferences.java) \(via [https://github.com/JabRef/jabref/pull/3092](https://github.com/JabRef/jabref/pull/3092)\) for the current way how to deal with preferences.

Defaults should go into the model package. See [Comments in this Commit](https://github.com/JabRef/jabref/commit/2f553e6557bddf7753b618b0f4edcaa6e873f719#commitcomment-15779484)

## Test Cases

### General hints on tests

Imagine you want to test the method `format(String value)` in the class `BracesFormatter` which removes double braces in a given string.

* _Placing:_ all tests should be placed in a class named `classTest`, e.g. `BracesFormatterTest`.
* _Naming:_ the name should be descriptive enough to describe the whole test. Use the format `methodUnderTest_ expectedBehavior_context` \(without the dashes\). So for example `formatRemovesDoubleBracesAtBeginning`. Try to avoid naming the tests with a `test` prefix since this information is already contained in the class name. Moreover, starting the name with `test` leads often to inferior test names \(see also the [Stackoverflow discussion about naming](http://stackoverflow.com/questions/155436/unit-test-naming-best-practices)\).
* _Test only one thing per test:_ tests should be short and test only one small part of the method. So instead of

  ```java
  testFormat() {
   assertEqual("test", format("test"));
   assertEqual("{test", format("{test"));
   assertEqual("test", format("test}}"));
  }
  ```

  we would have five tests containing a single `assert` statement and named accordingly \(`formatDoesNotChangeStringWithoutBraces`, `formatDoesNotRemoveSingleBrace`, , etc.\). See [JUnit AntiPattern](https://exubero.com/junit/anti-patterns/#Multiple_Assertions) for background.

* Do _not just test happy paths_, but also wrong/weird input.
* It is recommend to write tests _before_ you actually implement the functionality \(test driven development\).
* _Bug fixing:_ write a test case covering the bug and then fix it, leaving the test as a security that the bug will never reappear.
* Do not catch exceptions in tests, instead use the `assertThrows(Exception.class, ()->doSomethingThrowsEx())` feature of [junit-jupiter](https://junit.org/junit5/docs/current/user-guide/)  to the test method.

### Lists in tests

* Use `assertEquals(Collections.emptyList(), actualList);` instead of `assertEquals(0, actualList.size());` to test whether a list is empty.
* Similarly, use `assertEquals(Arrays.asList("a", "b"), actualList);` to compare lists instead of

  ```java
         assertEquals(2, actualList.size());
         assertEquals("a", actualList.get(0));
         assertEquals("b", actualList.get(1));
  ```

### BibEntries in tests

* Use the `assertEquals` methods in `BibtexEntryAssert` to check that the correct BibEntry is returned.

### Files and folders in tests

* If you need a temporary file in tests, then add the following Annotation before the class:

  ```java
  @ExtendWith(TempDirectory.class)
  class TestClass{

    @BeforeEach
    void setUp(@TempDirectory.TempDir Path temporaryFolder){
    }
  }
  ```

  to the test class. A temporary file is now created by `Files.createFile(path)`. Using this pattern automatically ensures that the test folder is deleted after the tests are run. See the [junit-pioneer doc](https://junit-pioneer.org/docs/temp-directory/) for more details.

### Loading Files from Resources

Sometimes it is necessary to load a specific resource or to access the resource directory

```java
Path resourceDir = Paths.get(MSBibExportFormatTestFiles.class.getResource("MsBibExportFormatTest1.bib").toURI()).getParent();
```

When the directory is needed, it is important to first point to an actual existing file. Otherwise the wrong directory will be returned.

### Preferences in tests

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

### Background on Java testing

In JabRef, we mainly rely to basic JUnit tests to increase code coverage. There are other ways to test:

| Type | Techniques | Tool \(Java\) | Kind of tests | Used In JabRef |
| :--- | :--- | :--- | :--- | :--- |
| Functional | Dynamics, black box, positive and negative | [JUnit-QuickCheck](https://github.com/pholser/junit-quickcheck) | Random data generation | No, not intended, because other test kinds seem more helpful. |
| Functional | Dynamics, black box, positive and negative | [GraphWalker](https://graphwalker.github.io/) | Model-based | No, because the BibDatabase doesn't need to be tests |
| Functional | Dynamics, black box, positive and negative | [TestFX](https://github.com/TestFX/TestFX) | GUI Tests | Yes |
| Functional | Dynamics, white box, negative | [PIT](https://pitest.org/) | Mutation | No |
| Functional | Dynamics, white box, positive and negative | [Mockito](https://site.mockito.org/) | Mocking | Yes |
| Non-functional | Dynamics, black box, positive and negative | [JETM](http://jetm.void.fm/), [Apache JMeter](https://jmeter.apache.org/) | Performance \(performance testing vs load testing respectively\) | No |
| Structural | Static, white box | [CheckStyle](https://checkstyle.sourceforge.io/) | Constient formatting of the source code | Yes |
| Structural | Dynamics, white box | [SpotBugs](https://spotbugs.github.io/) | Reocurreing bugs \(based on experience of other projects\) | No |

## UI

Global variables should be avoided. Try to pass them as dependency.

## "Special Fields"

### keywords sync

Database.addDatabaseChangeListener does not work as the DatabaseChangedEvent does not provide the field information. Therefore, we have to use BibtexEntry.addPropertyChangeListener\(VetoableChangeListener listener\)

## Working with BibTeX data

### Working with authors

You can normalize the authors using `org.jabref.model.entry.AuthorList.fixAuthor_firstNameFirst(String)`. Then the authors always look nice. The only alternative containing all data of the names is `org.jabref.model.entry.AuthorList.fixAuthor_lastNameFirst(String)`. The other `fix...` methods omit data \(like the von parts or the junior information\).

## Benchmarks

* Benchmarks can be executed by running the `jmh` gradle task \(this functionality uses the [JMH Gradle plugin](https://github.com/melix/jmh-gradle-plugin)\)
* Best practices:
  * Read test input from `@State` objects
  * Return result of calculations \(either explicitly or via a `BlackHole` object\)
* [List of examples](https://github.com/melix/jmh-gradle-example/tree/master/src/jmh/java/org/openjdk/jmh/samples)

## Measure performance

Try out the [YourKit JAva Profiler](https://www.yourkit.com/).

## equals

When creating an `equals` method follow:

1. Use the `==` operator to check if the argument is a reference to this object. If so, return `true`.
2. Use the `instanceof` operator to check if the argument has the correct type. If not, return `false`.
3. Cast the argument to the correct type.
4. For each “significant” field in the class, check if that field of the argument matches the corresponding field of this object. If all these tests succeed, return `true` otherwise, return `false`.
5. When you are finished writing your equals method, ask yourself three questions: Is it symmetric? Is it transitive? Is it consistent?

Also, note:

* Always override `hashCode` when you override equals \(`hashCode` also has very strict rules\) \(Item 9 of[Effective Java](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/)\)
* Don’t try to be too clever
* Don’t substitute another type for `Object` in the equals declaration

## Files and Paths

Always try to use the methods from the nio-package. For interoperability, they provide methods to convert between file and path. [https://docs.oracle.com/javase/tutorial/essential/io/path.html](https://docs.oracle.com/javase/tutorial/essential/io/path.html) Mapping between old methods and new methods [https://docs.oracle.com/javase/tutorial/essential/io/legacy.html\#mapping](https://docs.oracle.com/javase/tutorial/essential/io/legacy.html#mapping)

## JavaFX

The following expressions can be used in FXML attributes, according to the [official documentation](https://docs.oracle.com/javase/8/javafx/api/javafx/fxml/doc-files/introduction_to_fxml.html#attributes)

| Type | Expression | Value point to | Remark |
| :--- | :--- | :--- | :--- |
| Location | `@image.png` | path relative to the current FXML file |  |
| Resource | `%textToBeTranslated` | key in ResourceBundle |  |
| Attribute variable | `$idOfControl` or `$variable` | named control or variable in controller \(may be path in the namespace\) | resolved only once at load time |
| Expression binding | `${expression}` | expression, for example `textField.text` | changes to source are propagated |
| Bidirectional expression binding | `#{expression}` | expression | changes are propagated in both directions \(not yet implemented in JavaFX, see [feature request](https://bugs.openjdk.java.net/browse/JDK-8090665)\) |
| Event handler | `#nameOfEventHandler` | name of the event handler method in the controller |  |
| Constant | `<text><Strings fx:constant="MYSTRING"/></text>` | constant \(here `MYSTRING` in the `Strings` class\) |  |

### JavaFX Radio Buttons example

All radio buttons that should be grouped together need to have a ToggleGroup defined in the FXML code Example:

```markup
<VBox>
            <fx:define>
                <ToggleGroup fx:id="citeToggleGroup"/>
            </fx:define>
            <children>
                <RadioButton fx:id="inPar" minWidth="-Infinity" mnemonicParsing="false"
                             text="%Cite selected entries between parenthesis" toggleGroup="$citeToggleGroup"/>
                <RadioButton fx:id="inText" minWidth="-Infinity" mnemonicParsing="false"
                             text="%Cite selected entries with in-text citation" toggleGroup="$citeToggleGroup"/>
                <Label minWidth="-Infinity" text="%Extra information (e.g. page number)"/>
                <TextField fx:id="pageInfo"/>
            </children>
</VBox>
```

### JavaFX Dialogs

All dialogs should be displayed to the user via `DialogService` interface methods. `DialogService` provides methods to display various dialogs \(including custom ones\) to the user. It also ensures the displayed dialog opens on the correct window via `initOwner()` \(for cases where the user has multiple screens\). The following code snippet demonstrates how a custom dialog is displayed to the user:

```java
dialogService.showCustomDialog(new DocumentViewerView());
```

If an instance of `DialogService` is unavailable within current class/scope in which the dialog needs to be displayed, `DialogService` can be instantiated via the code snippet shown as follows:

```java
DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
```

