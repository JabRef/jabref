---
parent: Requirements
---
# CLI

## Unified `--input` option across all commands
`req~jabkit.cli.input-flag~1`

All `jabkit` commands that need a file input must have the `--input` option to specify the input file.
See [ADR 45](../decisions/0045-use-input-flag-always-for-input-files.md) for more details.

Needs: impl

<!-- markdownlint-disable-file MD022 -->

## Banner shown only at `--help`
`req~jabkit.cli.banner-shown~1`

The banner for the CLI ("JabKit") is only shown if the help is output.
Meaning: If there is no command given (falling back to help) or explicitly `--help` requested.

This increases the accessibility. Source: [Accessibility of Command Line Interfaces](https://dl.acm.org/doi/10.1145/3411764.3445544)
