This page provides some development support in the form of howtos. 

## Outputting Errors
(to be filled)

In old verisons of JabRef, there was `Utils.showQuickErrorDialog` to output exceptions, but it was not used and therefore removed in 4c11b4b7466 (PR #205).

## Logging

JabRef uses the logging facade [Apache Commons Logging](http://commons.apache.org/proper/commons-logging/).
All log messages are passed internally to Java's [java.util.logging](http://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html) which handles any filterting, formatting and writing of log messages. 

If the logging event is caused by an exception, please add the exception to the log message as: 

    catch (Exception e) {
       LOGGER.warn("Warning text.", e);
       ...
    }

## Drag and Drop

`net.sf.jabref.external.DroppedFileHandler.handleDroppedfile(String, ExternalFileType, boolean, BibtexEntry) FileListEditor` sets a `TransferHandler` inherited from `FileListEditorTransferHandler`. There, at `importData`, a `DroppedFileHandler` is instantiated and `handleDroppedfile` called. 

## Get the JabRef frame / panel

```Java
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

```
@BeforeClass
public static void setUp() {
    Globals.prefs = JabRefPreferences.getInstance();
}
```

If you modify preference, use following pattern:

```
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