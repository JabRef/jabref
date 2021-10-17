# Overview on Developing

This page presents all development information around JabRef.

## Starting point for newcomers

Go to [Setting up a local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace)

## User documentation

For users documentation, please head to [https://docs.jabref.org](https://docs.jabref.org).

## How tos

* External: [Sync your fork with the JabRef repository](https://help.github.com/articles/syncing-a-fork/)
* External (🇩🇪): Branches and pull requests: [https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md](https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md)

## Teaching Exercises

We are very happy that JabRef is part of [Software Engineering](https://en.wikipedia.org/wiki/Software_engineering) trainings. Please head to [Teaching](teaching.md) for more information on using JabRef as a teaching object and on previous courses where JabRef was used.

## Miscellaneous Hints

### Command Line

The package `org.jabref.cli` is responsible for handling the command line options.

During development, one can configure IntelliJ to pass command line parameters:

![IntelliJ-run-configuration](<.gitbook/assets/intellij-run-configuration-command-line (2) (2) (3).png>)

Passing command line arguments using gradle is currently not possible as all arguments (such as `-Dfile.encoding=windows-1252`) are passed to the application.

Without jlink, it is not possible to generate a fat jar any more. During development, the capabilities of the IDE has to be used.

### Groups

Diagram showing aspects of groups: [Groups.uml](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/Groups.uml).

## Architectural Decision Records

[Architectural decisions for JabRef](adr.md) are recorded.

For new ADRs, please use [template.md](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/template.md) as basis. More information on MADR is available at [https://adr.github.io/madr/](https://adr.github.io/madr/). General information about architectural decision records is available at [https://adr.github.io/](https://adr.github.io). Add them to the [list of architectural decision records](adr.md).

## FAQ

*   Q: I get `java: package org.jabref.logic.journals does not exist`.

    A: You have to ignore `buildSrc/src/main` as source directory in IntelliJ as indicated in our [setup guide](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace).

    Also filed as IntelliJ issue [IDEA-240250](https://youtrack.jetbrains.com/issue/IDEA-240250).
