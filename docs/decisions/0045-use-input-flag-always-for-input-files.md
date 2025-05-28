# Use `--input` Flag Consistently for Reading Input Files

## Context and Problem Statement

JabRef provides multiple command-line subcommands, each potentially requiring input files.
Initially, the design employed syntax tailored specifically to each subcommand, such as positional arguments or specialized options, resulting in inconsistent user experiences and increased complexity in parsing and documenting.

To illustrate, the following issue was noted in the code review discussion: [GitHub Discussion](https://github.com/JabRef/jabref/pull/13158#discussion_r2106254233).

This inconsistency causes confusion and requires users to refer to the documentation for each subcommand separately, increasing cognitive load and reducing usability.

## Decision Drivers

* Consistency across subcommands
* Improved usability and user experience
* Easier parsing and documentation

## Considered Options

* Adopt a consistent syntax (`--input`) for reading input files across all subcommands
* Use specialized syntax tailored to each subcommand

## Decision Outcome

Chosen option: "Adopt a consistent syntax (`--input`) for reading input files across all subcommands", because comes out bets (see below).

### Consequences

This decision mandates using the `--input` flag universally across all JabRef subcommands that require file input.

Examples:

```shell
jabkit check --input references.bib
jabkit format --input references.bib
```

### Positive Consequences

* Simplified command-line interface
* Reduced user confusion due to consistent syntax
* Easier parsing logic and reduced code complexity

### Negative Consequences

* Potential migration overhead for existing scripts and automation

## Pros and Cons of the Options

### Adopt a consistent syntax (`--input`) for reading input files across all subcommands

* Good, because uniform and predictable command-line interface
* Good, because simpler documentation
* Good, because easier to maintain parsing logic
* Bad, because more letters to type for each subcommand

### Use specialized syntax tailored to each subcommand

* Good, because "intuitive" command-line options for each subcommand
* Bad, becauuse inconsistency
* Bad, becauuse increased cognitive load
* Bad, becauuse more complex parsing logic

## Links

* [GitHub Discussion prompting this ADR](https://github.com/JabRef/jabref/pull/13158#discussion_r2106254233)
* [Command Line Interface Guidelines](https://clig.dev/)
