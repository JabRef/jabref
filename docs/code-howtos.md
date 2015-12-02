This page provides some development support in the form of howtos. 

## Tooltips

### Browser plugins
* [Codecov Browser Extension](https://github.com/codecov/browser-extension) - displaying code coverage directly when browsing GitHub
* [ZenHub Browser Extension](https://www.zenhub.io/) - `+1` for GitHub and much more

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

Moreover, there is a Python script (`scripts/syncLang.py`) which is able used to detect all used and unused localization Strings and which is used to synchronize the used resource bundle by adding and removing (no longer) needed Strings.

The following best practices should be applied when dealing with `net.sf.jabref.logic.l10n.Localization` to reduce the amount of Strings to be translated and keep the script working:
- Only one call to `Localization.lang(...)` per line
- Use the String you want to localize directly, do not use members or local variables: `Localization.lang("Translate me");` instead of `Localization.lang(someVariable)` (possibly in the form `someVariable = Localization.lang("Translate me")`
- Use `%x`-variables where appropriate: `Localization.lang("Exported %0 entries.", number)` instead of `Localization.lang("Exported ") + number + Localization.lang(" entries.");`
- Use a full stop/period (".") to end full sentences
- *to be continued*

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

If `Globals.prefs` are not initialized in a test case, try to add

```java
@BeforeClass
public static void setUp() {
    Globals.prefs = JabRefPreferences.getInstance();
}
```

If you modify preference, use following pattern:

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

## UI for Preferences

  * `JabRefFrame.preferences()` shows the preferences 
  * class: PrefsDialog3 

## "Special Fields"

### keywords sync

Database.addDatabaseChangeListener does not work as the DatabaseChangedEvent does not provide the field information. Therefore, we have to use BibtexEntry.addPropertyChangeListener(VetoableChangeListener listener) 

## Working with BibTeX data

### Working with authors

You can normalize the authors using `net.sf.jabref.model.entry.AuthorList.fixAuthor_firstNameFirst(String)`. Then the authors always look nice. The only alternative containing all data of the names is `net.sf.jabref.model.entry.AuthorList.fixAuthor_lastNameFirst(String)`. The other `fix...` methods omit data (like the von parts or the junior information).