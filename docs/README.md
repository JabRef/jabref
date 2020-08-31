# Overview on Developing

This page presents all development informatation around JabRef.

## Starting point for newcomers

Go to [Setting up a local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace)

## User documentation

For users documentation, please head to [https://docs.jabref.org](https://docs.jabref.org).

## How tos

* External: [Sync your fork with the JabRef repository](https://help.github.com/articles/syncing-a-fork/)
* External \(🇩🇪\): Branches and pull requests: [https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md](https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md)

## Teaching Exercises

We are very happy that JabRef is part of [Software Engineering](https://en.wikipedia.org/wiki/Software_engineering) trainings. Please head to [Teaching](teaching.md) for more information on using JabRef as teaching object and on previous courses where JabRef was used.

## Miscellaneous Hints

### Command Line

The package `org.jabref.cli` is responsible for handling the command line options.

During development, one can configure IntelliJ to pass command line paramters:

![IntelliJ-run-configuration](.gitbook/assets/intellij-run-configuration-command-line%20%282%29.png)

Passing command line arguments using gradle is currently not possible as all arguments \(such as `-Dfile.encoding=windows-1252`\) are passed to the application.

Without jlink, it is not possible to generate a fat jar any more. During development, the capabilities of the IDE has to be used.

### Groups

Diagram showing aspects of groups: [Groups.uml](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/Groups.uml).

## Decision Records

This log lists the decisions for JabRef.

* [ADR-0000](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0000-use-markdown-architectural-decision-records.md) - Use Markdown Architectural Decision Records
* [ADR-0001](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0001-use-crowdin-for-translations.md) - Use Crowdin for translations
* [ADR-0002](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0002-use-slf4j-for-logging.md) - Use slf4j together with log4j2 for logging
* [ADR-0003](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0003-use-gradle-as-build-tool.md) - Use Gradle as build tool
* [ADR-0004](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0004-use-mariadb-connector.md) - Use MariaDB Connector
* [ADR-0005](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0005-fully-support-utf8-only-for-latex-files.md) - Fully Support UTF-8 Only For LaTeX Files
* [ADR-0006](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0006-only-translated-strings-in-language-file.md) - Only translated strings in language file
* [ADR-0007](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0007-human-readable-changelog.md) - Provide a human-readable changelog
* [ADR-0008](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0008-use-public-final-instead-of-getters.md) - Use public final instead of getters to offer access to immutable variables
* [ADR-0009](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0009-use-plain-junit5-for-testing.md) - Use Plain JUnit5 for advanced test assertions
* [ADR-0010](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/0010-use-h2-as-internal-database.md) - Use H2 as Internal SQL Database

For new ADRs, please use [template.md](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/adr/template.md) as basis. More information on MADR is available at [https://adr.github.io/madr/](https://adr.github.io/madr/). General information about architectural decision records is available at [https://adr.github.io/](https://adr.github.io/).

## FAQ

* Q: I get `java: package org.jabref.logic.journals does not exist`.  

  A: You have to ignore `buildSrc/src/main`  as source directory in IntelliJ as indicated in our [setup guide](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace).

     Also filed as IntelliJ issue [IDEA-240250](https://youtrack.jetbrains.com/issue/IDEA-240250).

