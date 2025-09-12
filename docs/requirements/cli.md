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