---
nav_order: 0072
parent: Decision Records
---
# Accept a Shared Database URL as `jabkit` Input

`adr~shared-database-url-as-jabkit-input~1`

Needs: impl

## Context and Problem Statement

A shared SQL library can only be opened from the GUI. Users asked for command line access to it,
for example to export a shared library or to run `jabkit check` on it in CI
(<https://github.com/JabRef/jabref/issues/12948>).
`jabkit` has no shared-database support at all today.

How should a shared library be made available to the `jabkit` commands?

## Decision Drivers

* Every input-taking command should support it, not just `convert`
* Reuse the existing shared-database code (`DBMSProcessor`) rather than a second SQL reader
* Do not pull the `DBMSSynchronizer` (change listeners, offline changes, background writers) into a one-shot CLI process

## Considered Options

* A new `jabkit` subcommand (e.g. `jabkit pull-shared`) writing a `.bib` file
* A dedicated `--shared-url` option on every command
* Another branch in `InputOption#resolveInput`, exporting the library to a temporary `.bib` file

## Decision Outcome

Chosen option: "another branch in `InputOption#resolveInput`", because that method is already the
single choke point where an input argument becomes a local file
(see [ADR 65](0065-download-url-input-files.md)). All input-taking commands gain shared-database
support at once, with no new options and no per-command changes, and the commands keep working on a
plain file.

The export is read-only and connectionless afterwards: `SharedDatabaseExport` (`jablib`) opens a
`DBMSConnection`, reads entries and meta data through `DBMSProcessor`, writes them to a temporary
`.bib` file, and closes the connection. No synchronizer and no notification listener are involved.

### Consequences

* Good, because every `InputOption`-consuming command supports shared libraries for free.
* Good, because failures use the existing `ImportServiceException`/exit-code model of ADR-0063, like a failed download.
* Good, because the export helper lives in `jablib` and is testable against the existing embedded-PostgreSQL test setup.
* Bad, because the access is read-only: nothing is written back to the database. Writing needs the full `DBMSSynchronizer`.
* Bad, because the password has to be part of the URL, so it can end up in the shell history. Only the JDBC URL, which carries no password, is printed on failure.

### Confirmation

`SharedDatabaseExportTest` (`jablib`) fills an embedded PostgreSQL through `DBMSProcessor`, exports it,
and parses the resulting file, asserting that entries and meta data survive the round trip.

## More Information

* [ADR 57](0057-allow-positional-input-file-argument.md) - the shared `InputOption` mixin this decision builds on
* [ADR 65](0065-download-url-input-files.md) - the `resolveInput` branch this decision extends
* `org.jabref.logic.shared.DBMSConnectionUrl` (`jablib`) - parses the PostgreSQL connection URL, also used by the GUI connection dialog

<!-- markdownlint-disable-file MD022 -->
