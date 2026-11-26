---
parent: Requirements
---
# CLI

## Remote server must respond to plain-text health check ping
`req~jabref.remote.health-check~1`

The remote listener accepts the versioned plain-text request `JABREF/1 PING` and responds with
`JABREF/1 PONG jabref`. This check identifies a running JabRef instance without requiring clients
to implement the Java serialization protocol. Existing serialized remote-operation requests remain
supported.

Needs: impl

## JabKit commands must accept input file as positional argument
`req~jabkit.cli.input-flag~2`

All `jabkit` commands that need a file input must accept it as a positional `FILE` argument.
For backward compatibility, the `--input` option is also accepted as an alias.
Exactly one of the two forms must be supplied.
See [ADR 57](../decisions/0057-allow-positional-input-file-argument.md) for more details.

Needs: impl

## JabKit input argument must accept HTTP, HTTPS, and FTP URLs
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

## JabKit input argument must accept shared database URLs
`req~jabkit.cli.input-shared-db~1`

The positional `FILE` argument and its `--input` alias additionally accept a PostgreSQL connection
URL (for example `postgresql://user:secret@host:5432/library`) pointing at a JabRef shared library.
The library is exported to a local temporary file before use; the access is read-only, nothing is
written back to the database.
A database that is not a JabRef shared library, and any connection failure, is reported as a regular
CLI error (exit code `SOFTWARE`).
See [ADR 74](../decisions/0074-shared-database-url-as-jabkit-input.md) for more details.

Needs: impl

## JabKit must display banner only when help is requested
`req~jabkit.cli.banner-shown~1`

The banner for the CLI ("JabKit") is only shown if the help is output, meaning when no command is given (falling back to help) or when `--help` is explicitly requested.

Rationale: Suppressing decorative banners during normal command invocations prevents noise in automated scripts and increases accessibility (source: [Accessibility of Command Line Interfaces](https://dl.acm.org/doi/10.1145/3411764.3445544)).

Needs: impl

## JabKit checking commands must emit findings in machine-readable format
`req~jabkit.cli.check-errorformat-output~1`

The `jabkit check` subcommands and `jabkit fix` emit their findings in a line-oriented
`file:line:column:citationKey[:field]: message` format, suitable for editors and CI tooling.

Entry-level findings (for example, on the citation key itself) carry only the citation key.
Field-level findings additionally carry the affected field name.

Needs: impl

## JabKit convert command must write pure converted data to standard output
`req~jabkit.cli.convert-stdout-format~1`

When `jabkit convert` writes to standard output, it uses the exporter selected by
`--output-format`. Progress messages are written to standard error so standard output
contains only the exported data.

Needs: impl

## JabKit BibTeX conversion must preserve library context
`req~jabkit.cli.convert-bibtex-context~1`

This includes the library's string definitions and preamble when writing to a file,
standard output, or the input file itself.
References to those strings continue to resolve to their original values after conversion.

Source: <https://github.com/JabRef/jabref/issues/17409>

Needs: impl, utest

## JabKit check commands must support GitHub Actions output format
`req~jabkit.cli.check-github-actions-output~1`

The `jabkit check` subcommands support an additional `github-actions` output format
that emits each finding as a [GitHub Actions workflow command](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#setting-an-error-message)
of the shape `::error file=<file>,line=<line>,col=<col>,title=<title>::<message>`.

The `file`, `line`, `col`, and `title` property values are URL-encoded so that
Windows-style paths (containing `:`) and titles (containing `:` between citation key and field name)
are parsed correctly by the GitHub Actions runner.

Needs: impl

## JabKit fix must apply the lint rules enabled for a library
`req~jabkit.cli.jabfix~1`

`jabkit fix` reports what the enabled rules find and repairs it where a rule is set to `fix`.
With `--check`, it writes nothing and exits with 1 unless the library is clean.
A usage error exits with 2 and any other error with 3, so that neither is taken for an unclean library.
The library is written in the encoding it states; a character that encoding cannot hold is an error that leaves the library as it was.
Formatting applies exactly the repairs that `--check` reports, and each finding names the rule that reported it.
Rules are enabled in the library's metadata, by a mode or per rule; when no rule is enabled, the command warns.
`--mode` and `--rule RULE=MODE[:NAME=VALUE]` override the library's configuration for one run.
`--save-config` stores them in the library instead, and moves the library's save actions into the same configuration.
An unknown rule, mode, or rule value is a usage error.

Needs: impl

## JabKit must preserve existing citation keys when overwrite avoidance is enabled
`req~jabkit.cli.citation-key-avoid-overwrite~1`

For `citationkeys generate`, when the effective overwrite-avoidance preference is enabled, entries
with an existing citation key retain it, while entries without a key still receive a generated one.
When overwrite avoidance is disabled, existing keys may be regenerated. See
[issue 17458](https://github.com/JabRef/jabref/issues/17458).

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
