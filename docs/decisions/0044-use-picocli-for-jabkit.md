---
nav_order: 44
parent: Decision Records
---

# Use Picocli instead of Apache Commons CLI for JabKit

## Context and Problem Statement

As a part of refactoring and migrations to make JabRef a multi-project build, a decision on which CLI framework to use to handle command-line arguments and options for JabRef's command-line toolkit (JabKit) was needed. The decision was between continuing to use the existing Apache Commons CLI or migrating to Picocli or JCommander, which are more modern CLI frameworks.

We needed to determine which framework would provide better maintainability, usability, and developer experience for the JabKit command-line interface.

## Decision Drivers

* Ease of use and maintainability for developers
* Code readability and organization
* Feature richness and modern API design
* Type safety and reduction of boilerplate code
* Support for nested commands and subcommands
* Documentation quality and active maintenance

## Considered Options

1. Apache Commons CLI
2. Picocli
3. JCommander
4. Airline 2
5. ritopt
6. crest

## Decision Outcome

Chosen option: "Migrate to Picocli", because it offers a more modern, feature-rich, and developer-friendly approach to handling command-line interfaces with significantly less boilerplate code and better organization.

## Pros and Cons of the Options

### Apache Commons CLI

* Good, because it is a well-established library used in many Apache projects
* Good, because it has been stable for many years (around since 2002)
* Good, because it has minimal dependencies
* Bad, because it requires more boilerplate code to define and parse options
* Bad, because it lacks support for nested subcommands
* Bad, because it does not automatically convert arguments to Java types
* Bad, because its API shows its age and is more procedural than declarative
* Bad, because it appears to be near-dormant with few releases (6 releases in 16 years)

### Picocli

* Good, because it has a modern, annotation-based declarative API which reduces boilerplate
* Good, because allows for more modularization
* Good, because it supports automatic argument parsing to types
* Good, because it has built-in support for nested commands and subcommands to any depth
* Good, because it has an easier to understand and better overview
* Good, because it generates more readable, colored help messages
* Good, because it is actively maintained with regular updates
* Good, because it supports ANSI colors in help messages for better readability
* Good, because it offers command completion for bash and zsh shells
* Good, because it has built-in support for argument files (@-files) for very long command lines
* Good, because it is well-documented with extensive user manual and detailed javadocs
* Good, because it has built-in tracing facilities for troubleshooting
* Good, because it is natively supported by GraalVM and Micronaut
* Bad, because its feature set may introduce a steeper learning curve for new contributors
* Bad, because the annotation-driven model can obscure logic flow

### JCommander

* Good, because it is lightweight and simple to use
* Good, because it has minimal dependencies and a small footprint
* Good, because it provides basic annotation-based configuration
* Bad, because it does not support advanced CLI features (e.g., argument files, type conversion)
* Bad, because there are fewer examples and tooling integrations
* Bad, because it has limited documentation

### Airline 2

Available at <https://github.com/rvesse/airline>.

* Bad, because unmaintained in 2025

### ritopt

Availale at <https://ritopt.sourceforge.net/index.shtml>.

* Good, because simple interface
* Good, because used in JabRef a long time ago (before 2.10)
* Bad, because unmaintained in 2025

### crest

"Command-line API styled after JAX-RS"

Available at <https://github.com/tomitribe/crest>

Example:

```java
@Command
public String hello(@Option("name") @Default("${user.name}") String user) throws Exception
    return String.format("Hello, %s%n", user);
}
```

* Good, because good alignment with JAX-RS
* Bad, because not a large user base

## More Information

More CLI parsers are listed and discussed at <https://stackoverflow.com/a/7829772/873282>.
