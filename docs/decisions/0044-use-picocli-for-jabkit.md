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

### Consequences

* Good, because command structure is more modular and organized
* Good, because automatic argument parsing to types reduces boilerplate code
* Good, because it's easier to understand with a cleaner, more declarative API
* Good, because it provides better overview of available commands and options
* Good, because it has built-in support for generating help documentation
* Bad, because it requires learning a new API for developers familiar with Commons CLI
* Bad, because we need to migrate existing code

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
* Good, because it supports automatic type conversion for arguments
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

## More Information

After investigating both options, we found that Picocli offers significant advantages over Apache Commons CLI, particularly in terms of code organization, type safety, and developer experience. The annotation-based approach of Picocli allows for a more declarative style that keeps all information about commands and options in one place, resulting in cleaner, more maintainable code.

The main advantages of Picocli that influenced our decision were:

1. **Commands and Subcommands**: Picocli's strong support for nested commands makes it easier to organize complex command-line applications with multiple features.

2. **Modularization**: Better organization of code through a more modular structure, with each command potentially having its own class.

3. **Better Overview**: The annotation-based approach provides a clearer view of all available options directly in the code.

4. **Automatic Argument Parsing**: Automatic conversion of command-line arguments to Java types, which reduces boilerplate code and potential errors.
