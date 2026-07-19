---
parent: Requirements
---
# CLI

## Input file as positional argument across all commands
`req~jabkit.cli.input-flag~2`

All `jabkit` commands that need a file input must accept it as a positional `FILE` argument.
For backward compatibility, the `--input` option is also accepted as an alias.
Exactly one of the two forms must be supplied.
See [ADR 57](../decisions/0057-allow-positional-input-file-argument.md) for more details.

Needs: impl

<!-- markdownlint-disable-file MD022 -->

## Input file argument accepts an http(s)/ftp URL
`req~jabkit.cli.input-url~1`

The positional `FILE` argument and its `--input` alias additionally accept an `http://`,
`https://`, or `ftp://` URL wherever a `jabkit` command reads a single file.
The URL is downloaded to a local temporary file before use; a download failure is reported
as a regular CLI error (exit code `SOFTWARE`) rather than a "file not found" usage error.
See [ADR 65](../decisions/0065-download-url-input-files.md) for more details.

Needs: impl

## Banner shown only at `--help`
`req~jabkit.cli.banner-shown~1`

The banner for the CLI ("JabKit") is only shown if the help is output.
Meaning: If there is no command given (falling back to help) or explicitly `--help` requested.

This increases the accessibility. Source: [Accessibility of Command Line Interfaces](https://dl.acm.org/doi/10.1145/3411764.3445544)

Needs: impl

## Machine-readable output of the `check` commands
`req~jabkit.cli.check-errorformat-output~1`

The `jabkit check` subcommands emit their findings in a line-oriented
`file:line:column:citationKey[:field]: message` format, suitable for editors and CI tooling.

Entry-level findings (for example, on the citation key itself) carry only the citation key.
Field-level findings additionally carry the affected field name.

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

## Shorten a paper's references to fit a page count
`req~jabkit.cli.shorten~1`

The `jabkit shorten FILE.tex` command compiles the referenced LaTeX document with `latexmk`
and applies escalating, information-reducing cleanups to its cited references
(author minification, journal-name abbreviation, DOI normalization), recompiling after each
and stopping as soon as the paper reaches the target page count (`--pages`, default one page fewer).
The referenced `.bib` file(s) are rewritten with the smallest set of cleanups that reaches the target.
A local `latexmk` is preferred; the `texlive/texlive` Docker image is used when no local TeX is present.

Needs: impl
