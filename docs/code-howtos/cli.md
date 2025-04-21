---
parent: Code Howtos
---
# Command Line Interface

The package `org.jabref.cli` is responsible for handling the command line options.

During development, one can configure IntelliJ to pass command line parameters:

![IntelliJ-run-configuration](../images/intellij-run-configuration-command-line.png)

Passing command line arguments using gradle is currently not possible as all arguments (such as `-Dfile.encoding=windows-1252`) are passed to the application.

Without [jlink](https://docs.oracle.com/en/java/javase/11/tools/jlink.html), it is not possible to generate a fat jar anymore. During development, the capabilities of the IDE has to be used.
