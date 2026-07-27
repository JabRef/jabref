---
nav_order: 57
parent: Decision Records
status: "accepted; supersedes ADR-0045"
---
# Allow a Positional Input File Argument for `jabkit` Commands

## Context and Problem Statement

[ADR-0045](0045-use-input-flag-always-for-input-files.md) mandated that every `jabkit` subcommand reading a file must do so via the `--input` option.
The goal was a consistent and predictable command-line interface.

In practice, the most common invocation - "run this command on that file" - became more verbose than users expect from a command-line tool:

```shell
jabkit check integrity --input references.bib
```

Established command-line tools (`cat`, `grep`, `git`, …) take their primary input as a *positional* argument.
The [Command Line Interface Guidelines](https://clig.dev/) - the very source cited by ADR-0045 - recommend using positional arguments for the primary, required input.
Requiring `--input` therefore contradicts a widely held user expectation and adds friction to the single most frequent use case.

How can we keep the consistency that ADR-0045 achieved while removing this friction?

## Decision Drivers

* Convenience for the most common invocation
* Consistency across all subcommands
* No breakage of existing scripts and automation that already use `--input`
* Alignment with established CLI conventions

## Considered Options

* Accept the input file as a positional argument across **all** subcommands, keeping `--input` as an alias
* Accept the positional argument only for the `check` command
* Keep `--input` mandatory everywhere (status quo of ADR-0045)

## Decision Outcome

Chosen option: "Accept the input file as a positional argument across all subcommands, keeping `--input` as an alias", because it gains the convenience of positional arguments without sacrificing the cross-command consistency that ADR-0045 valued, and without breaking existing automation.

A reusable picocli mixin (`InputOption`) provides a positional `FILE` parameter and the `--input` option as a mutually exclusive, required argument group.
Exactly one of the two forms must be supplied; picocli enforces this at parse time.

Examples:

```shell
jabkit check integrity references.bib
jabkit check integrity --input references.bib   # still works
jabkit convert references.bib --output-format html
```

This decision **supersedes [ADR-0045](0045-use-input-flag-always-for-input-files.md)**.

### Consequences

* Good, because the common case becomes shorter and matches what users expect from a CLI.
* Good, because consistency is preserved: the positional argument behaves the same for every subcommand, implemented once in a shared mixin.
* Good, because existing scripts using `--input` keep working - the option remains as an alias.
* Bad, because there are now two ways to pass the input file, which is slightly more to document.

### Confirmation

The shared `InputOption` mixin is used by every input-taking subcommand; an ArchUnit-style review or code review confirms no subcommand declares its own `--input` option.
Tests in `jabkit` exercise both the positional and the `--input` form.

## Pros and Cons of the Options

### Accept the positional argument across all subcommands, keeping `--input` as an alias

* Good, because it is consistent across all subcommands
* Good, because it matches established CLI conventions
* Good, because it is backward compatible
* Neutral, because two input forms must be documented
* Bad, because it requires a small amount of resolution logic (handled once in the mixin)

### Accept the positional argument only for the `check` command

* Good, because it requires the smallest change
* Bad, because it recreates exactly the per-command inconsistency that ADR-0045 was created to remove
* Bad, because users must remember which commands accept a positional argument

### Keep `--input` mandatory everywhere

* Good, because it is the simplest possible rule
* Bad, because it is more verbose than users expect for the most frequent use case
* Bad, because it contradicts the CLI guidelines cited by ADR-0045 itself

## More Information

* [ADR-0045](0045-use-input-flag-always-for-input-files.md), which this decision supersedes
* [Command Line Interface Guidelines](https://clig.dev/)
* [GitHub Discussion prompting ADR-0045](https://github.com/JabRef/jabref/pull/13158#discussion_r2106254233)
