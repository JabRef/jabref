---
nav_order: 6
has_children: true
---
# Code Howtos

This page provides some development support in the form of howtos.

## Generic code how tos

We really recommend reading the book [Java by Comparison](http://java.by-comparison.com).

Please read <https://github.com/cxxr/better-java>.

* try not to abbreviate names of variables, classes or methods
* use lowerCamelCase instead of snake\_case
* name enums in singular, e.g. `Weekday` instead of `Weekdays` (except if they represent flags)

## Dependency injection

JabRef uses a [fork](https://github.com/JabRef/afterburner.fx) of the [afterburner.fx framework](https://github.com/AdamBien/afterburner.fx) by [Adam Bien](https://adam-bien.com/).

The main idea is to get instances by using `Injector.instantiateModelOrService(X.class)`, where `X` is the instance one needs.
The method `instantiateModelOrService` checks if there is already an instance of the given class. If yes, it returns it. If not, it creates a new one.
A singleton can be added by `com.airhacks.afterburner.injection.Injector#setModelOrService(X.class, y)`, where X is the class and y the instance you want to inject.

## Using locally published artifacts (mavenLocal)

By default, JabRef does not resolve dependencies from your local Maven repository (`~/.m2`).
To test a locally published artifact (e.g., a SNAPSHOT of a dependency), enable `mavenLocal()` by setting the gradle property `useMavenLocal`:

* Command line: `./gradlew build -PuseMavenLocal=true`
* Persistent (per user, not versioned): add `useMavenLocal=true` to `~/.gradle/gradle.properties` (the [Gradle user home](https://docs.gradle.org/current/userguide/directory_layout.html#dir:gradle_user_home), `$GRADLE_USER_HOME`). Do not add it to the project's `gradle.properties` — that file is versioned.

`mavenLocal()` is also enabled implicitly when overriding the JavaFX version via `-PjavafxVersion=...`, since a custom JavaFX build is typically published to `~/.m2`. You then do not need to set `useMavenLocal` as well.

## Overriding the JavaFX version

The JavaFX version is defined in `versions/build.gradle.kts`.
To test a different (e.g., locally built) JavaFX, override it via the gradle property `javafxVersion`:

* Pin a version: `./gradlew :jabgui:run -PjavafxVersion=27-ea+1`
* Use the latest version available (Gradle dynamic version): `./gradlew :jabgui:run -PjavafxVersion=+`

Setting `javafxVersion` enables `mavenLocal()` implicitly, so a JavaFX build published to `~/.m2` is picked up without setting `useMavenLocal`.

The override applies only to the non-web JavaFX modules; `javafx-web` always stays on the default version defined in `versions/build.gradle.kts`.

## Selecting the JDK vendor and version

The Gradle [toolchain](https://docs.gradle.org/current/userguide/toolchains.html) vendor and Java language version are defined in `build-logic/src/main/kotlin/org/jabref/gradle/Toolchains.kt` (consumed by `org.jabref.gradle.feature.compile`).
The defaults match `.github/workflows/binaries.yml`: Java 25, vendor Amazon Corretto (or BellSoft Liberica when `-PuseLibericaJdkFull` is set).

Override them via gradle properties:

* JDK vendor: `./gradlew :jabgui:run -Pjdk=<name>`. Accepts friendly names (`corretto`, `liberica`, `temurin`, `oracle`, `openj9`, `graalvm`, `microsoft`, `zulu`, `sap`) or a raw [`JvmVendorSpec`](https://docs.gradle.org/current/javadoc/org/gradle/jvm/toolchain/JvmVendorSpec.html) enum name.
* Java language version: `./gradlew :jabgui:run -PjavaVersion=26` (e.g. to test an early-access JDK).

Gradle resolves the toolchain from the JDKs installed locally; if none matches, [foojay auto-provisioning](https://github.com/gradle/foojay-toolchains) downloads one.
To play with [Eclipse OpenJ9](https://eclipse.dev/openj9/) (shipped as IBM Semeru), point Gradle at a locally installed Semeru JDK and select the IBM vendor. For example, with [mise](https://mise.jdx.dev/):

```shell
mise exec java@semeru-openj9-25.0.3.0 -- ./gradlew :jabgui:run -Pjdk=openj9
```

Do not combine `-Pjdk=openj9` (or any non–Liberica-Full vendor) with `-PuseLibericaJdkFull`: those JDKs do not bundle JavaFX, so JabRef must keep resolving JavaFX from Maven.

Conversely, to consume the JavaFX bundled inside a BellSoft Liberica **Full** JDK (instead of Maven), install a Liberica Full JDK 25 and set `-PuseLibericaJdkFull` (which selects vendor BellSoft automatically). With [mise](https://mise.jdx.dev/):

```shell
mise exec java@liberica-javafx-25.0.3+11 -- ./gradlew :jabgui:run -PuseLibericaJdkFull
```

Find the exact id with `mise ls-remote java | grep liberica`. It must be a **Full** build (the `liberica-javafx-*` ids; they bundle JavaFX); a Liberica *Standard* JDK fails later with `module javafx.* not found`, because Gradle's toolchain query cannot tell Full and Standard apart. Avoid having a Liberica Standard 25 installed alongside, or point Gradle at the Full JDK via `org.gradle.java.installations.paths`.

## Cleanup and Formatters

We try to build a cleanup mechanism based on formatters. The idea is that we can register these actions in arbitrary places, e.g., onSave, onImport, onExport, cleanup, etc. and apply them to different fields. The formatters themselves are independent of any logic and therefore easy to test.

Example: [NormalizePagesFormatter](https://github.com/JabRef/jabref/blob/main/jablib/src/main/java/org/jabref/logic/formatter/bibtexfields/NormalizePagesFormatter.java)

## Drag and Drop

Drag and Drop makes usage of the Dragboard. For JavaFX the following [tutorial](https://docs.oracle.com/javafx/2/drag_drop/jfxpub-drag_drop.htm) is helpful. Note that the data has to be serializable which is put on the dragboard. For drag and drop of Bib-entries between the maintable and the groups panel, a custom Dragboard is used, `CustomLocalDragboard` which is a generic alternative to the system one.

For accessing or putting data into the Clipboard use the `ClipboardManager`.

## Get the JabRef frame panel

`JabRefFrame` and `BasePanel` are the two main classes. You should never directly call them, instead pass them as parameters to the class.

## Get Absolute Filename or Path for file in File directory

JabRef stores files relative to one of [multiple possible directories](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files).
The convert the relative path to an absolute one, there is the `find` method in `FileUtil`:

```java
org.jabref.logic.util.io.FileUtil.find(org.jabref.model.database.BibDatabaseContext, java.lang.String, org.jabref.logic.FilePreferences)
```

`String path` Can be the files name or a relative path to it. The Preferences should only be directly accessed in the GUI. For the usage in logic pass them as parameter

## Get a relative filename (or path) for a file

[JabRef offers multiple directories per library to store a file.](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files).
When adding a file to a library, the path should be stored relative to "the best matching" directory of these.
This is implemented in `FileUtil`:

```java
org.jabref.logic.util.io.FileUtil.relativize(java.nio.file.Path, org.jabref.model.database.BibDatabaseContext, org.jabref.logic.FilePreferences)
```

## Setting a directory for a `.bib` file

* `@comment{jabref-meta: fileDirectory:<directory>`
* “fileDirectory” is determined by Globals.pref.get(“userFileDir”) (which defaults to “fileDirectory”
* There is also “fileDirectory-\<username>”, which is determined by Globals.prefs.get(“userFileDirIndividual”)
* Used at DatabasePropertiesDialog

## How to work with Preferences

`model` and `logic` must not know `JabRefPreferences`. See `ProxyPreferences` for encapsulated preferences and [https://github.com/JabRef/jabref/pull/658](https://github.com/JabRef/jabref/pull/658) for a detailed discussion.

See [https://github.com/JabRef/jabref/blob/main/jablib/src/main/java/org/jabref/logic/preferences/TimestampPreferences.java](https://github.com/JabRef/jabref/blob/master/src/main/java/org/jabref/logic/preferences/TimestampPreferences.java) (via [https://github.com/JabRef/jabref/pull/3092](https://github.com/JabRef/jabref/pull/3092)) for the current way how to deal with preferences.

Defaults should go into the model package. See [Comments in this Commit](https://github.com/JabRef/jabref/commit/2f553e6557bddf7753b618b0f4edcaa6e873f719#commitcomment-15779484)

## UI

Global variables should be avoided. Try to pass them as dependency.

## "Special Fields"

### keywords sync

`Database.addDatabaseChangeListener` does not work as the `DatabaseChangedEvent` does not provide the field information.
Therefore, we have to use `BibtexEntry.addPropertyChangeListener(VetoableChangeListener listener)`.

## Working with BibTeX data

### Working with authors

You can normalize the authors using `org.jabref.model.entry.AuthorList.fixAuthor_firstNameFirst(String)`. Then the authors always look nice. The only alternative containing all data of the names is `org.jabref.model.entry.AuthorList.fixAuthor_lastNameFirst(String)`. The other `fix...` methods omit data (like the "von" parts or the junior information).

## Benchmarks

* Benchmarks can be executed by running the `jmh` gradle task (this functionality uses the [JMH Gradle plugin](https://github.com/melix/jmh-gradle-plugin))
* Best practices:
  * Read test input from `@State` objects
  * Return result of calculations (either explicitly or via a `BlackHole` object)
* [List of examples](https://github.com/melix/jmh-gradle-example/tree/master/src/jmh/java/org/openjdk/jmh/samples)

## Measure performance

Try out the [YourKit Java Profiler](https://www.yourkit.com).

## equals

When creating an `equals` method follow:

1. Use the `==` operator to check if the argument is a reference to this object. If so, return `true`.
2. Use the `instanceof` operator to check if the argument has the correct type. If not, return `false`.
3. Cast the argument to the correct type.
4. For each “significant” field in the class, check if that field of the argument matches the corresponding field of this object. If all these tests succeed, return `true` otherwise, return `false`.
5. When you are finished writing your `equals` method, ask yourself three questions: Is it symmetric? Is it transitive? Is it consistent?

Also, note:

* Always override `hashCode` when you override equals (`hashCode` also has very strict rules) (Item 9 of[Effective Java](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/))
* Don’t try to be too clever
* Don’t substitute another type for `Object` in the equals declaration

## Files and Paths

Always try to use the methods from the nio-package. For interoperability, they provide methods to convert between file and path. [https://docs.oracle.com/javase/tutorial/essential/io/path.html](https://docs.oracle.com/javase/tutorial/essential/io/path.html) Mapping between old methods and new methods [https://docs.oracle.com/javase/tutorial/essential/io/legacy.html#mapping](https://docs.oracle.com/javase/tutorial/essential/io/legacy.html#mapping)
