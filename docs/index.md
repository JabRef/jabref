---
nav_order: 1
---
# Overview on Developing JabRef

This page presents all development information around JabRef.
In case you are a end user, please head to the [user documentation](https://docs.jabref.org) or to the [general homepage](https://www.jabref.org) of JabRef.

## Starting point for new developers

On the page [Setting up a local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace), we wrote about the initial steps to get your IDE running.
We strongly recommend to continue reading there.
After you successfully cloned and build JabRef, you are invited to continue reading here.

## How tos

* External: [Sync your fork with the JabRef repository](https://help.github.com/articles/syncing-a-fork/)
* External (🇩🇪): Branches and pull requests: [https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md](https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md)

## Teaching Exercises

We are very happy that JabRef is part of [Software Engineering](https://en.wikipedia.org/wiki/Software_engineering) trainings. Please head to [Teaching](teaching.md) for more information on using JabRef as a teaching object and on previous courses where JabRef was used.

## Miscellaneous Hints

### Command Line

The package `org.jabref.cli` is responsible for handling the command line options.

During development, one can configure IntelliJ to pass command line parameters:

![IntelliJ-run-configuration](<images/intellij-run-configuration-command-line.png>)

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
*   Q: I get `Execution failed for task ':buildSrc:jar'. Entry org/jabref/build/JournalAbbreviationConverter$_convert_closure1$_closure2.class is a duplicate but no duplicate handling strategy has been set. Please refer to https://docs.gradle.org/7.4.2/dsl/org.gradle.api.tasks.Copy.html#org.gradle.api.tasks.Copy:duplicatesStrategy for details.` What can I do?\
    A: You have to delete directory `buildSrc/build`.
