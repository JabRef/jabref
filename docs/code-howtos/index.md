---
nav_order: 6
has_children: true
---
# Code Howtos

This page provides some development support in the form of howtos.
See also [High Level Documentation](../getting-into-thecode/high-level-documentation.md).

## Generic code how tos

We really recommend reading the book [Java by Comparison](http://java.by-comparison.com).

Please read [https://github.com/cxxr/better-java](https://github.com/cxxr/better-java)

* try not to abbreviate names of variables, classes or methods
* use lowerCamelCase instead of snake\_case
* name enums in singular, e.g. `Weekday` instead of `Weekdays` (except if they represent flags)

## Cleanup and Formatters

We try to build a cleanup mechanism based on formatters. The idea is that we can register these actions in arbitrary places, e.g., onSave, onImport, onExport, cleanup, etc. and apply them to different fields. The formatters themselves are independent of any logic and therefore easy to test.

Example: [NormalizePagesFormatter](https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/formatter/bibtexfields/NormalizePagesFormatter.java)

## Drag and Drop

Drag and Drop makes usage of the Dragboard. For JavaFX the following [tutorial](https://docs.oracle.com/javafx/2/drag\_drop/jfxpub-drag\_drop.htm) is helpful. Note that the data has to be serializable which is put on the dragboard. For drag and drop of Bib-entries between the maintable and the groups panel, a custom Dragboard is used, `CustomLocalDragboard` which is a generic alternative to the system one.

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
* “fileDirectory” is determined by Globals.pref.get(“userFileDir”) (which defaults to “fileDirectory”
* There is also “fileDirectory-\<username>”, which is determined by Globals.prefs.get(“userFileDirIndividual”)
* Used at DatabasePropertiesDialog

## How to work with Preferences

`model` and `logic` must not know `JabRefPreferences`. See `ProxyPreferences` for encapsulated preferences and [https://github.com/JabRef/jabref/pull/658](https://github.com/JabRef/jabref/pull/658) for a detailed discussion.

See [https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/preferences/TimestampPreferences.java](https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/preferences/TimestampPreferences.java) (via [https://github.com/JabRef/jabref/pull/3092](https://github.com/JabRef/jabref/pull/3092)) for the current way how to deal with preferences.

Defaults should go into the model package. See [Comments in this Commit](https://github.com/JabRef/jabref/commit/2f553e6557bddf7753b618b0f4edcaa6e873f719#commitcomment-15779484)

## UI

Global variables should be avoided. Try to pass them as dependency.

## "Special Fields"

### keywords sync

Database.addDatabaseChangeListener does not work as the DatabaseChangedEvent does not provide the field information. Therefore, we have to use BibtexEntry.addPropertyChangeListener(VetoableChangeListener listener)

## Working with BibTeX data

### Working with authors

You can normalize the authors using `org.jabref.model.entry.AuthorList.fixAuthor_firstNameFirst(String)`. Then the authors always look nice. The only alternative containing all data of the names is `org.jabref.model.entry.AuthorList.fixAuthor_lastNameFirst(String)`. The other `fix...` methods omit data (like the von parts or the junior information).

## Benchmarks

* Benchmarks can be executed by running the `jmh` gradle task (this functionality uses the [JMH Gradle plugin](https://github.com/melix/jmh-gradle-plugin))
* Best practices:
    * Read test input from `@State` objects
    * Return result of calculations (either explicitly or via a `BlackHole` object)
* [List of examples](https://github.com/melix/jmh-gradle-example/tree/master/src/jmh/java/org/openjdk/jmh/samples)

## Measure performance

Try out the [YourKit JAva Profiler](https://www.yourkit.com).

## equals

When creating an `equals` method follow:

1. Use the `==` operator to check if the argument is a reference to this object. If so, return `true`.
2. Use the `instanceof` operator to check if the argument has the correct type. If not, return `false`.
3. Cast the argument to the correct type.
4. For each “significant” field in the class, check if that field of the argument matches the corresponding field of this object. If all these tests succeed, return `true` otherwise, return `false`.
5. When you are finished writing your equals method, ask yourself three questions: Is it symmetric? Is it transitive? Is it consistent?

Also, note:

* Always override `hashCode` when you override equals (`hashCode` also has very strict rules) (Item 9 of[Effective Java](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/))
* Don’t try to be too clever
* Don’t substitute another type for `Object` in the equals declaration

## Files and Paths

Always try to use the methods from the nio-package. For interoperability, they provide methods to convert between file and path. [https://docs.oracle.com/javase/tutorial/essential/io/path.html](https://docs.oracle.com/javase/tutorial/essential/io/path.html) Mapping between old methods and new methods [https://docs.oracle.com/javase/tutorial/essential/io/legacy.html#mapping](https://docs.oracle.com/javase/tutorial/essential/io/legacy.html#mapping)

