---
nav_order: 44
parent: Decision Records
---

# Use Picocli instead of Apache Commons CLI for JabKit

## Context and Problem Statement

As a part of refactoring and migrations to make JabRef a multi-project build, a decision on which CLI framework to use to handle command-line arguments and options for JabRef's command-line toolkit (JabKit) was needed. The decision was between continuing to use the existing Apache Commons CLI or migrating to Picocli, a more modern CLI framework.

We needed to determine which framework would provide better maintainability, usability, and developer experience for the JabKit command-line interface.

## Decision Drivers

* Ease of use and maintainability for developers
* Code readability and organization
* Feature richness and modern API design
* Type safety and reduction of boilerplate code
* Support for nested commands and subcommands
* Documentation quality and active maintenance

## Considered Options

1. Continue using Apache Commons CLI
2. Migrate to Picocli

## Decision Outcome

Chosen option: "Migrate to Picocli", because it offers a more modern, feature-rich, and developer-friendly approach to handling command-line interfaces with significantly less boilerplate code and better organization.

## Pros and Cons of the Options

### Apache Commons CLI

* Good, because it's a well-established library used in many Apache projects
* Good, because it has been stable for many years (around since 2002)
* Good, because it has minimal dependencies
* Good, because team members might already be familiar with it
* Bad, because it requires more boilerplate code to define and parse options
* Bad, because it lacks support for nested subcommands
* Bad, because it doesn't automatically convert arguments to Java types
* Bad, because its API shows its age and is more procedural than declarative
* Bad, because it appears to be near-dormant with few releases (6 releases in 16 years)

### Picocli

* Good, because it has a modern, annotation-based declarative API
* Good, because it supports automatic argument parsing to types
* Good, because it has built-in support for nested commands and subcommands to any depth
* Good, because it generates more readable, colored help messages
* Good, because it greatly reduces boilerplate code through annotations
* Good, because it's actively maintained with regular updates
* Good, because it supports ANSI colors in help messages for better readability
* Good, because it offers command completion for bash and zsh shells
* Good, because it has built-in support for argument files (@-files) for very long command lines
* Good, because it's well-documented with extensive user manual and detailed javadocs
* Good, because it has built-in tracing facilities for troubleshooting
* Bad, because it's a newer library and might be less familiar to some developers
* Bad, because it introduces an additional dependency to the project
