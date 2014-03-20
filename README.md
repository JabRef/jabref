# JabRef 2.10 development version

This version is a development version. Features may not work as expected.

JabRef is a graphical application for managing bibliographical data.

* Homepage: http://jabref.sourceforge.net/
* Development page: https://github.com/JabRef
* Main git repository: https://github.com/JabRef/jabref
* CI Server: https://travis-ci.org/JabRef/jabref

This repository has been generated out of the old git repository at sourceforge.
The folder  `jabref` of the old repository is now this repository.
Although that changed **all** git commit ids, the advantage is to have a clean separation between plugins, the homepage and the code of JabRef.

### Bug reports, suggestions, other feedback:
We are thankful for any bug reports or other feedback. If there are
features you want included in JabRef, tell us!

The github tracker is the main bug tracker.

However, the "old" trackers at sourceforge still remain intact.

* Bugs: https://sourceforge.net/p/jabref/bugs/
* Feature Requests: https://sourceforge.net/p/jabref/feature-requests/

Do *not* file patches using https://sourceforge.net/p/jabref/patches/.
Just fork JabRef and create a pull request.

### Next Steps

* Change build system from `ant` to `gradle` to get rid of the binaries in the repository.
* Migrate the sourceforge wiki to github
* Fix bugs listed at https://sourceforge.net/p/jabref/bugs/.

## Introduction

JabRef is a graphical Java application for editing bibtex (`.bib`)
databases. JabRef lets you organize your entries into overlapping
logical groups, and with a single click limit your view to a single
group or an intersection or union of several groups. You can customize
the entry information shown in the main window, and sort by any of the
standard Bibtex fields. JabRef can autogenerate bibtex keys for your
entries. JabRef also lets you easily link to PDF or web sources for your
reference entries.

JabRef can import from and export to several formats, and you can
customize export filters. JabRef can be run as a command line application
to convert from any import format to any export format.


## License

JabRef is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, either version 2 of the License, or (at your option) any later
version.  dialog, for details about the GPL. See the enclosed text files
'gpl2.txt' and 'gpl3.txt' for the full details.

JabRef also uses libraries distributed by other parties; see the About
box for details.


## Requirements

JabRef runs on any system equipped with the Java Virtual Machine (1.6 or newer), which can be downloaded at no cost from http://java.sun.com.
If you do not plan to compile JabRef, the Java Runtime Environment may be a better choice than the Java Development Kit.


## Installing and running, Windows:

JabRef is available in Windows Installer (`.msi`) format. To install,
double-click the .msi file. A shortcut to JabRef will be added to your
start menu.

The Windows installation was made by Dale Visser, using the following open-source tools:
JSmooth (.exe wrapper for Java apps), available at http://jsmooth.sf.net/
Wix (tool for compiling MSI files from an XML specification), available at http://wix.sf.net/


## Installing and running, general:

JabRef can be downloaded as an executable .jar file. Run the
program as follows:
If you are using the Java Development Kit:
     java -jar <path to jar>
or, if you are using the Java Runtime Environment:
     jre -new -jar <path to jar> or
     jrew -new -jar <path to jar>

If you run JabRef under Java 1.5, you can add the option "-Dswing.aatext=true" before the
"-jar" option, to activate antialiased text throughout the application.


The jar file containing JabRef can be unpacked with the command:
    jar xf <path to jar>
or  jar xf <path to jar> <list of files to extract>
Unpacking the jar file is not necessary to run the program.


## Documentation
JabRef comes with an online help function, accessed by pressing F1 or
clicking on a question mark icon. The help files are probably not
exhaustive enough to satisfy everyone yet, but they should help sort
out the most important issues about using the program. The help files
can also be viewed outside the program with a standard HTML browser.
If you choose languages other than English, some or all help pages may
appear in your chosen languages.


## Building JabRef from source:

To compile JabRef from source, you need:

* A Java compiler, supporting Java 1.6 and `JAVA_HOME` points to this JDK.

To run it, just execute `gradlew run`.
When you want to develop, it is necessary to generate additional sources using `gradlew generateSource`
and then generate the Eclipse `gradlew eclipse` or IntelliJ IDEA `gradlew idea` project files.

## Release Process

Replace `ANY_ANT_TARGET` with the Ant Target of your choice, and the system will build your binaries.

`gradlew generateSource antTargets.ANY_ANT_TARGET`

To compile, use the command `gradlew generateSource antTargets.jars`.
After the build is finished, you can find the executable jar file
named `JabRef-$VERSION.jar` (where $VERSION is the current version of the
source tree) in the `build/lib` directory. Enjoy!

On Mac OS X you should include the targets osx and osxjar,
making the correct command `gradlew generateSource antTargets.compile antTargets.unjarlib antTargets.osx antTargets.jars antTargets.osxjar`.
After the build is finished, you will find the OS X application
`JabRef.app` in the `build/lib` directory along with the executable
jar.



