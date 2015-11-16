# JabRef Development Version (2.11 line)

[![Circle CI](https://circleci.com/gh/JabRef/jabref/tree/dev_2.11.svg?style=shield)](https://circleci.com/gh/JabRef/jabref/tree/dev_2.11)

This version is a development version. Features may not work as expected.

The branch of this README file is `dev_2.11`.
This branch is intended for maintenance releases in the 2.11 line with Java 6 compatibility and plugin support.
The main goal is to include important fixes only.
Main development (bug fixes and new features) is happening in the [master branch](https://github.com/JabRef/jabref/tree/master).
Please check it for latest development information.

JabRef is a graphical Java application for editing bibtex (`.bib`) databases.
JabRef lets you organize your entries into overlapping logical groups, and with a single click limit your view to a single group or an intersection or union of several groups.
You can customize the entry information shown in the main window, and sort by any of the standard Bibtex fields.
JabRef can autogenerate bibtex keys for your entries.
JabRef also lets you easily link to PDF or web sources for your reference entries.

JabRef can import from and export to several formats, and you can customize export filters.
JabRef can be run as a command line application to convert from any import format to any export format.

This is the last version with plugin support.
All plugins will stop working starting with version v2.80 of JabRef.
See https://github.com/JabRef/jabref/issues/152 for status of integration of plugins in the latest version of JabRef.

## Requirements

JabRef runs on any system equipped with the Java Virtual Machine (1.6 or newer), which can be downloaded at no cost from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
If you do not plan to compile JabRef, the Java Runtime Environment may be a better choice than the Java Development Kit.


## Installing and running, Mac OS X:

Please see our [Mac OS X FAQ](http://jabref.sourceforge.net/faq.php#osx).


## Installing and running, Windows:

JabRef offers an installer, which also adds a shortcut to JabRef to your start menu.

Please also see our [Windows FAQ](http://jabref.sourceforge.net/faq.php#windows)


## Installing and running, general:

JabRef can be downloaded as an executable .jar file.
Try to doubleclick the `jar` file or execute the follwing command:
     `java -jar <path to jar>`

If you run JabRef under Java 1.5, you can add the option `-Dswing.aatext=true` before the
`-jar` option, to activate antialiased text throughout the application.


## Documentation

JabRef comes with an online help function, accessed by pressing F1 or
clicking on a question mark icon. The help files are probably not
exhaustive enough to satisfy everyone yet, but they should help sort
out the most important issues about using the program. The help files
can also be viewed outside the program with a standard HTML browser.
If you choose languages other than English, some or all help pages may
appear in your chosen languages.


## Building JabRef v2.11 from source:

To compile JabRef from source, you need a Java compiler supporting Java 1.6 and `JAVA_HOME` pointing to this JDK.

To run it, just execute `gradlew run`.
When you want to develop, it is necessary to generate additional sources using `gradlew generateSource`
and then generate the Eclipse `gradlew eclipse` project files.


## Release process

Requires
 * [launch4j](http://launch4j.sourceforge.net/)
 * [NSIS](http://nsis.sourceforge.net) with the [WinShell plug-in](http://nsis.sourceforge.net/WinShell_plug-in).
 * Eventually a `user.properties` with correct setting of `launch4j.dir` and `nsis.executable`. See [build.xml](build.xml) for defaults.

Replace `ANY_ANT_TARGET` with the Ant Target of your choice (e.g., `macbundle`), and the system will build your binaries.
To get a list of all targets, use `gradlew tasks`.

`gradlew generateSource antTargets.ANY_ANT_TARGET`

To compile, use the command `gradlew generateSource antTargets.jars`.
After the build is finished, you can find the executable jar file
named `JabRef-$VERSION.jar` (where $VERSION is the current version of the
source tree) in the `buildant\lib` directory. Enjoy!
The setup files are created by invoking the command `gradlew generateSource antTargets.release`.


### Releasing on Linux

Run `gradlew antTargets.release.linux`

All binaries (including OSX) and the installer are generated in the directory `buildant/lib`.


### Releasing on Windows

Run `gradlew antTargets.release`

All binaries (including OSX) and the installer are generated in the directory `buildant/lib`.


## License

JabRef is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 2 of the License, or (at your option) any later
version.
See the enclosed text files [gpl2.txt](gpl2.txt) and [gpl3.txt](gpl3.txt) for full details.

JabRef also uses libraries distributed by other parties; see the About box for details.
