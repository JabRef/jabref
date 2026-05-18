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

## Banner shown only at `--help`
`req~jabkit.cli.banner-shown~1`

The banner for the CLI ("JabKit") is only shown if the help is output.
Meaning: If there is no command given (falling back to help) or explicitly `--help` requested.

This increases the accessibility. Source: [Accessibility of Command Line Interfaces](https://dl.acm.org/doi/10.1145/3411764.3445544)

Needs: impl
