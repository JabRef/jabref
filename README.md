# JabRef Development Version

[![Build Status](https://api.travis-ci.org/JabRef/jabref.png?branch=master)](https://travis-ci.org/JabRef/jabref)
[![Dependency Status](https://www.versioneye.com/user/projects/557f2723386664002000009c/badge.svg?style=flat)](https://www.versioneye.com/user/projects/557f2723386664002000009c)
[![Coverage Status](https://coveralls.io/repos/JabRef/jabref/badge.svg)](https://coveralls.io/r/JabRef/jabref)
[![License](https://img.shields.io/badge/license-GPLv2-blue.svg)](http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
[![Join the chat at https://gitter.im/JabRef/jabref](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/JabRef/jabref?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Donation](https://img.shields.io/badge/donate-paypal-orange.svg)](https://www.paypal.com/cgi-bin/webscr?item_name=JabRef+Bibliography+Manager&cmd=_donations&lc=US&currency_code=EUR&business=jabrefmail%40gmail.com)
[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=koppor&url=https%3A%2F%2Fgithub.com%2FJabRef%2Fjabref&title=JabRef&language=Java&tags=github&category=software)
[![Download JabRef](https://img.shields.io/sourceforge/dw/jabref.svg)](http://sourceforge.net/projects/jabref/files/jabref/)

This version is a development version. Features may not work as expected.

The branch of this README file is `master`.
The intention of this branch is to move JabRef forward to modern technologies such as Java8 and JavaFX.
The development version will be called `v2.80` and is meant as preparation to the `v3.0` release.

The last version with Java6 support is `v2.11` being developed at the [dev_2.11 branch](https://github.com/JabRef/jabref/tree/dev_2.11).

### Breaking Changes in Comparison to v2.11

* No plugin support
* Transition to Java8

### Background

JabRef is a graphical Java application for editing bibtex (`.bib`) databases.
JabRef lets you organize your entries into overlapping logical groups, and with a single click limit your view to a single group or an intersection or union of several groups.
You can customize the entry information shown in the main window, and sort by any of the standard Bibtex fields.
JabRef can autogenerate bibtex keys for your entries.
JabRef also lets you easily link to PDF or web sources for your reference entries.

JabRef can import from and export to several formats, and you can customize export filters.
JabRef can be run as a command line application to convert from any import format to any export format.

* Homepage: http://jabref.sourceforge.net/
* Development mailing list: https://lists.sourceforge.net/lists/listinfo/jabref-devel
* Development page: https://github.com/JabRef
* Main git repository: https://github.com/JabRef/jabref
* CI Servers: https://travis-ci.org/JabRef/jabref and https://circleci.com/gh/JabRef/jabref
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

JabRef comes with an online help function, accessed by pressing `F1` or
clicking on a question mark icon. The help files are probably not
exhaustive enough to satisfy everyone yet, but they should help sort
out the most important issues about using the program. The help files
can also be viewed outside the program with a standard HTML browser.
If you choose languages other than English, some or all help pages may
appear in your chosen languages.


## Building JabRef From Source

If you want a step-by-step tutorial, please check [this guideline](https://github.com/JabRef/jabref/wiki/Guidelines-for-setting-up-a-local-workspace)

To compile JabRef from source, you need a Java compiler supporting Java 1.8 and `JAVA_HOME` pointing to this JDK.

To run it, just execute `gradlew run`.
When you want to develop, it is necessary to generate additional sources using `gradlew generateSource`
and then generate the Eclipse `gradlew eclipse`.
For IntelliJ IDEA, just import the project via a Gradle Import by pointing at the `build.gradle`.


## Release Process

Requires
 * [launch4j](http://launch4j.sourceforge.net/) available in PATH
 * [NSIS](http://nsis.sourceforge.net) with the [WinShell plug-in](http://nsis.sourceforge.net/WinShell_plug-in).

To get a list of all targets, use `gradlew tasks`.
```
release - Creates a release for all target platforms.
releaseJar - Creates a Jar release.
releaseMac - Creates an OSX release.
releaseWindows - Creates a Windows executable and installer.
```

To set the path to your local NSIS executable pass it via a Gradle property:

`gradlew -PnsisExec=PATH ANY_RELEASE_TASK`
Typically, this is `"C:\Program Files (x86)\NSIS\makensis.exe"` resulting in the command line `gradlew -PnsisExec="C:\Program Files (x86)\NSIS\makensis.exe" release`.

All binaries are created inside the directory `build/releases`.

### Releasing Developer Releases

Run `gradlew -Pdev=true ANY_RELEASE_TASK` to execute any of the previously defined release tasks.
The only difference is that the version contains the keyword *snapshot*, the *date*, and the *shortend git hash*.

Normal: `JabRef--2.80dev.jar`
With `-Pdev=true`: `JabRef--2.80dev--snapshot--2015-07-30--48a23d1.jar`

## License

JabRef is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 2 of the License, or (at your option) any later
version.
See the [LICENSE](LICENSE) for full details.

JabRef also uses libraries distributed by other parties.
See [external libraries](external-libraries.txt) for details.
