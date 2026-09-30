---
parent: Requirements
---
# CLI

## Remote server health check
`req~jabref.remote.health-check~1`

The remote listener accepts the versioned plain-text request `JABREF/1 PING` and responds with
`JABREF/1 PONG jabref`. This check identifies a running JabRef instance without requiring clients
to implement the Java serialization protocol. Existing serialized remote-operation requests remain
supported.

Needs: impl

## Input file as positional argument across all commands
`req~jabkit.cli.input-flag~2`

All `jabkit` commands that need a file input must accept it as a positional `FILE` argument.
For backward compatibility, the `--input` option is also accepted as an alias.
Exactly one of the two forms must be supplied.
See [ADR 57](../decisions/0057-allow-positional-input-file-argument.md) for more details.

Needs: impl

## Input file argument accepts an http(s)/ftp URL
`req~jabkit.cli.input-url~2`

The positional `FILE` argument and its `--input` alias additionally accept an `http://`,
`https://`, or `ftp://` URL. This holds for every input a `jabkit` command reads, including
each argument of a command taking several of them.
The URL is downloaded to a local temporary file before use; a download failure is reported
as a regular CLI error (exit code `SOFTWARE`) rather than a "file not found" usage error.
For a command reading several inputs, an unusable input is skipped and the remaining ones are
still processed, with the command exiting non-zero afterwards.
See [ADR 65](../decisions/0065-download-url-input-files.md) for more details.

Needs: impl

## Input file argument accepts a shared database URL
`req~jabkit.cli.input-shared-db~1`

The positional `FILE` argument and its `--input` alias additionally accept a PostgreSQL connection
URL (for example `postgresql://user:secret@host:5432/library`) pointing at a JabRef shared library.
The library is exported to a local temporary file before use; the access is read-only, nothing is
written back to the database.
A database that is not a JabRef shared library, and any connection failure, is reported as a regular
CLI error (exit code `SOFTWARE`).
See [ADR 74](../decisions/0074-shared-database-url-as-jabkit-input.md) for more details.

Needs: impl

## Banner shown only at `--help`
`req~jabkit.cli.banner-shown~1`

The banner for the CLI ("JabKit") is only shown if the help is output.
Meaning: If there is no command given (falling back to help) or explicitly `--help` requested.

This increases the accessibility. Source: [Accessibility of Command Line Interfaces](https://dl.acm.org/doi/10.1145/3411764.3445544)

Needs: impl

## Machine-readable output of the checking commands
`req~jabkit.cli.check-errorformat-output~1`

The `jabkit check` subcommands and `jabkit fix` emit their findings in a line-oriented
`file:line:column:citationKey[:field]: message` format, suitable for editors and CI tooling.

Entry-level findings (for example, on the citation key itself) carry only the citation key.
Field-level findings additionally carry the affected field name.
The place a finding points at is the one the library was read from, so it stays valid for the file the user sees even where the finding has already been repaired in memory.

Needs: impl

## Standard output conversion format
`req~jabkit.cli.convert-stdout-format~1`

When `jabkit convert` writes to standard output, it uses the exporter selected by
`--output-format`. Progress messages are written to standard error so standard output
contains only the exported data.

Needs: impl

## GitHub Actions output of the `check` commands
`req~jabkit.cli.check-github-actions-output~1`

The `jabkit check` subcommands support an additional `github-actions` output format
that emits each finding as a [GitHub Actions workflow command](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#setting-an-error-message)
of the shape `::error file=<file>,line=<line>,col=<col>,title=<title>::<message>`.

The `file`, `line`, `col`, and `title` property values are URL-encoded so that
Windows-style paths (containing `:`) and titles (containing `:` between citation key and field name)
are parsed correctly by the GitHub Actions runner.

Needs: impl

## Linting and formatting a library
`req~jabkit.cli.jabfix~1`

`jabkit fix` checks a library against a set of rules and rewrites it to follow them.
Each finding says where it is and names the rule that reported it by a stable id.
Formatting applies exactly the repairs that `--check` reports, and `--check` exits with 1 if the library would change.
A library that states in its metadata what it wants done to it is taken at its word, down to the fields it names there.
One that states nothing has every rule it has applied, each repairing what it finds.
`--enable` names the rules of the run itself, and then they are the only ones that run; `--disable` switches rules off by id and `--check-only` leaves a rule reporting without repairing.
All three are about the rule wherever it looks, so an option given for one run outranks what the library says about one of its fields, and none of them changes what the library says about itself.
An id that names no rule is a usage error.
`--migrate-save-actions` moves the library's save actions into its stored configuration and drops the old metadata item, leaving what the save applies unchanged.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
