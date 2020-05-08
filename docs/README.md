# Develop JabRef

This page presents all development informatation around JabRef. For users documentation see [https://docs.jabref.org](https://docs.jabref.org).

## Teaching Exercises

We are very happy that JabRef is part of [Software Engineering](https://en.wikipedia.org/wiki/Software_engineering) trainings.
Please head to [Teaching](teaching.md) for more information on using JabRef as teaching object and on previous courses where JabRef was used.

## How tos

* External: [Sync your fork with the JabRef repository](https://help.github.com/articles/syncing-a-fork/)
* External \(🇩🇪\): Branches and pull requests: [https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md](https://github.com/unibas-marcelluethi/software-engineering/blob/master/docs/week2/exercises/practical-exercises.md)

## Command Line

The package `org.jabref.cli` is responsible for handling the command line options.

During development, one can configure IntelliJ to pass command line paramters:

![IntelliJ-run-configuration](images/intellij-run-configuration-command-line.png)

Passing command line arguments using gradle is currently not possible as all arguments \(such as `-Dfile.encoding=windows-1252`\) are passed to the application.

Without jlink, it is not possible to generate a fat jar any more. During development, the capabilities of the IDE has to be used.

## Groups

Diagram showing aspects of groups: [Groups.uml](Groups.uml).

## Decision Records

This log lists the decisions for JabRef.

* [ADR-0000](adr/0000-use-markdown-architectural-decision-records.md) - Use Markdown Architectural Decision Records
* [ADR-0001](adr/0001-use-crowdin-for-translations.md) - Use Crowdin for translations
* [ADR-0002](adr/0002-use-slf4j-for-logging.md) - Use slf4j together with log4j2 for logging
* [ADR-0003](adr/0003-use-gradle-as-build-tool.md) - Use Gradle as build tool
* [ADR-0004](adr/0004-use-mariadb-connector.md) - Use MariaDB Connector
* [ADR-0005](adr/0005-fully-support-utf8-only-for-latex-files.md) - Fully Support UTF-8 Only For LaTeX Files
* [ADR-0006](adr/0006-only-translated-strings-in-language-file.md) - Only translated strings in language file
* [ADR-0007](adr/0007-human-readable-changelog.md) - Provide a human-readable changelog
* [ADR-0008](adr/0008-use-public-final-instead-of-getters.md) - Use public final instead of getters to offer access to immutable variables
* [ADR-0009](adr/0009-use-plain-junit5-for-testing.md) - Use Plain JUnit5 for advanced test assertions
* [ADR-0010](0010-use-h2-as-internal-database.md) - Use H2 as Internal SQL Database

For new ADRs, please use [template.md](adr/template.md) as basis. More information on MADR is available at [https://adr.github.io/madr/](https://adr.github.io/madr/). General information about architectural decision records is available at [https://adr.github.io/](https://adr.github.io/).

