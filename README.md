# JabRef Development Version

[![Build Status](https://travis-ci.org/JabRef/jabref.svg?branch=master)](https://travis-ci.org/JabRef/jabref)
[![codecov.io](https://codecov.io/github/JabRef/jabref/coverage.svg?branch=master)](https://codecov.io/github/JabRef/jabref?branch=master)
[![Donation](https://img.shields.io/badge/donate%20to-jabref-orange.svg)](https://donations.jabref.org)
[![Help Contribute to Open Source](https://www.codetriage.com/jabref/jabref/badges/users.svg)](https://www.codetriage.com/jabref/jabref)
[![Join the chat at https://gitter.im/JabRef/jabref](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/JabRef/jabref?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This version is a development version. Features may not work as expected.

Development builds are available at [builds.jabref.org](https://builds.jabref.org/master/) whereas the [latest release is available via GitHub](https://github.com/JabRef/jabref/releases/latest).

Explanation of donation possibilities and usage of donations is available at our [donations page](https://donations.jabref.org).

We use [install4j], the multi-platform installer builder.

### Background

JabRef is a graphical Java application for editing [BibTeX] and [Biblatex] `.bib` databases.
JabRef lets you organize your entries into overlapping logical groups, and with a single click limit your view to a single group or an intersection or union of several groups.
You can customize the entry information shown in the main window, and sort by any of the standard BibTeX fields.
JabRef can autogenerate BibTeX keys for your entries.
JabRef also lets you easily link to PDF or web sources for your reference entries.

JabRef can import from and export to several formats, and you can customize export filters.
JabRef can be run as a command line application to convert from any import format to any export format.

* Homepage: <https://www.jabref.org>
* Development page: <https://github.com/JabRef/jabref>
* Open HUB page: <https://www.openhub.net/p/jabref>

### Bug Reports, Suggestions, Other Feedback

We are thankful for any bug reports or other feedback.

If there are features you want included in JabRef, [tell us in our forum](http://discourse.jabref.org/c/features)!

If you have questions regarding the usage, or you want to give general feedback the forum is also the right place for this: [discourse.jabref.org](http://discourse.jabref.org/c/features)

You can use our [GitHub issue tracker](https://github.com/JabRef/jabref/issues) to send in bug reports and suggestions.

To get your code added to JabRef, just fork JabRef and create a pull request.
For details see [CONTRIBUTING](CONTRIBUTING.md).


## Installing and Running

### Requirements

JabRef runs on any system equipped with the Java Virtual Machine (1.8 or newer), which can be downloaded at no cost from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
From JabRef 4.0 onwards, [JavaFX] support has to be available.

### Installing and Running, Linux:

Please see our [Installation Guide](http://help.jabref.org/en/Installation).

### Installing and Running, Mac OS X:

Please see our [Mac OS X FAQ](https://help.jabref.org/en/FAQosx).

### Installing and Running, Windows:

JabRef offers an installer, which also adds a shortcut to JabRef to your start menu.

Please also see our [Windows FAQ](https://help.jabref.org/en/FAQwindows)

### Installing and Running, General:

JabRef can be downloaded as an executable .jar file.
Try to double click the `jar` file or execute the following command:
     `java -jar <path to jar>`


## Documentation

JabRef comes with an [online help](https://help.jabref.org/), accessed by pressing `F1` or clicking on a question mark icon.
The help is are probably not exhaustive enough to satisfy everyone yet, but it should help sort out the most important issues about using the program. 
If you choose languages other than English, some or all help pages may appear in your chosen languages.


## Building JabRef From Source

If you want a step-by-step tutorial, please check [this guideline](https://github.com/JabRef/jabref/wiki/Guidelines-for-setting-up-a-local-workspace)

To compile JabRef from source, you need a Java compiler supporting Java 1.8 and `JAVA_HOME` pointing to this JDK.
You have to set `GRADLE_OPTS` to `-Dfile.encoding=UTF-8` as [gradle uses the JVM's platform encoding](https://discuss.gradle.org/t/is-there-a-way-to-tell-gradle-to-read-gradle-build-scripts-using-a-specified-encoding/7535).

To run it, just execute `gradlew run`.
When you want to develop, it is necessary to generate additional sources using `gradlew generateSource`
and then generate the Eclipse `gradlew eclipse`.
For IntelliJ IDEA, just import the project via a Gradle Import by pointing at the `build.gradle`.


## Testing

`gradlew test` executes the normal unit tests.
If you want to test the UI, execute `gradlew integrationTest`.
Sources for the integration test are kept in `src/integrationTest`.


## Release Process

Building a release requires [install4j]. We've got an [Open Source License](https://www.ej-technologies.com/buy/install4j/openSource).
Releasing is done using [CircleCI](https://circleci.com/gh/JabRef/jabref). A full release howto is available [in our wiki](https://github.com/JabRef/jabref/wiki/Releasing-a-new-Version).


## License

Since version 3.6, JabRef is licensed under the [MIT license](https://tldrlegal.com/license/mit-license).
See the [LICENSE.md](LICENSE.md) for the full MIT license.

JabRef also uses libraries, fonts, and icons distributed by other parties.
See [external libraries](external-libraries.txt) for details.

  [BibTeX]: https://www.ctan.org/pkg/bibtex
  [Biblatex]: https://www.ctan.org/pkg/biblatex
  [install4j]: https://www.ej-technologies.com/products/install4j/overview.html
  [JabRef]: https://www.jabref.org
  [JavaFX]: https://en.wikipedia.org/wiki/JavaFX
