# JabRef Development Version

[![Build Status](https://travis-ci.org/JabRef/jabref.svg?branch=master)](https://travis-ci.org/JabRef/jabref)
[![Dependency Status](https://www.versioneye.com/user/projects/557f2723386664002000009c/badge.svg?style=flat)](https://www.versioneye.com/user/projects/557f2723386664002000009c)
[![codecov.io](https://codecov.io/github/JabRef/jabref/coverage.svg?branch=master)](https://codecov.io/github/JabRef/jabref?branch=master)
[![Donation](https://img.shields.io/badge/donate-paypal-orange.svg)](https://www.paypal.com/cgi-bin/webscr?item_name=JabRef+Bibliography+Manager&cmd=_donations&lc=US&currency_code=EUR&business=jabrefmail%40gmail.com)
[![Issue Stats](http://www.issuestats.com/github/jabref/jabref/badge/pr)](http://www.issuestats.com/github/jabref/jabref)
[![Issue Stats](http://www.issuestats.com/github/jabref/jabref/badge/issue)](http://www.issuestats.com/github/jabref/jabref)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/327430c894e04086a5bfef618fa44f36)](https://www.codacy.com/app/simonharrer/jabref)

This version is a development version. Features may not work as expected.

Branches of JabRef development are listed at https://github.com/JabRef/jabref/wiki/Branches.

Development builds are available at [builds.jabref.org](http://builds.jabref.org/master/), the [latest release is available via GitHub](https://github.com/JabRef/jabref/releases/latest).

Explanation of donation possibilities and usage of donations is available at our [donations page](https://github.com/JabRef/jabref/wiki/Donations).

We use [install4j], the multi-platform installer builder.

### Background

JabRef is a graphical Java application for editing [BibTeX] and [Biblatex] (`.bib`) databases.
JabRef lets you organize your entries into overlapping logical groups, and with a single click limit your view to a single group or an intersection or union of several groups.
You can customize the entry information shown in the main window, and sort by any of the standard BibTeX fields.
JabRef can autogenerate BibTeX keys for your entries.
JabRef also lets you easily link to PDF or web sources for your reference entries.

JabRef can import from and export to several formats, and you can customize export filters.
JabRef can be run as a command line application to convert from any import format to any export format.

* Homepage: http://www.jabref.org
* Development page: https://github.com/JabRef/jabref
* Open HUB page: https://www.openhub.net/p/jabref

### Bug Reports, Suggestions, Other Feedback

We are thankful for any bug reports or other feedback.
If there are features you want included in JabRef, tell us!

You can use our [GitHub issue tracker](https://github.com/JabRef/jabref/issues) to send in bug reports and suggestions.

To get your code added to JabRef, just fork JabRef and create a pull request.
For details see [CONTRIBUTING](CONTRIBUTING.md).


## Installing and Running

### Requirements

JabRef runs on any system equipped with the Java Virtual Machine (1.8 or newer), which can be downloaded at no cost from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

### Installing and Running, Mac OS X:

Please see our [Mac OS X FAQ](http://jabref.sourceforge.net/faq.php#osx).

### Installing and Running, Windows:

JabRef offers an installer, which also adds a shortcut to JabRef to your start menu.

Please also see our [Windows FAQ](http://jabref.sourceforge.net/faq.php#windows)

### Installing and Running, General:

JabRef can be downloaded as an executable .jar file.
Try to double click the `jar` file or execute the following command:
     `java -jar <path to jar>`


## Documentation

JabRef comes with an [online help](http://help.jabref.org/), accessed by pressing `F1` or clicking on a question mark icon.
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

Requires [install4j].
We've got an [Open Source License](https://www.ej-technologies.com/buy/install4j/openSource).

To get a list of all targets, use `gradlew tasks`.
```
releaseJar - Creates a Jar release.
media - Creates executables and installers.
```

All binaries are created inside the directory `build/releases`.

### Releasing Developer Releases

Run `gradlew -Pdev=true ANY_RELEASE_TASK` to execute any of the previously defined release tasks.
The only difference is that the version contains the keyword *snapshot*, the *date*, the *branch name*, and the *shortened git hash*.

 * Normal: `JabRef--3.0dev.jar`
 * With `-Pdev=true`: `JabRef-3.0dev--snapshot--2015-11-20--master--cc4f5d1.jar`

## License

JabRef is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 2 of the License, or (at your option) any later
version.
See the [LICENSE](LICENSE) for full details.

JabRef also uses libraries distributed by other parties.
See [external libraries](external-libraries.txt) for details.

  [BibTeX]: https://www.ctan.org/pkg/bibtex
  [Biblatex]: https://www.ctan.org/pkg/biblatex
  [install4j]: https://www.ej-technologies.com/products/install4j/overview.html
  [JabRef]: http://www.jabref.org
