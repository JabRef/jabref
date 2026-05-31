---
nav_order: 0062
parent: Decision Records
status: proposed
date: 2026-05-31
---

# Use picocli ExceptionHandler for JabKit Error Handling

## Context and Problem Statement

JabKit commands and helper functions use a lot of `try-catch` blocks to handle and translate exceptions into user-facing error messages and exit codes. This makes the code harder to read and adds the mental load of keeping track of which exceptions are (not) handled already.

## Decision Drivers

* Simplify code for maintainability and readability
* Avoid `try-catch` blocks just to translate exceptions
* Avoid Optional/Either types that create `if-else` clutter
* Centralize error handling and exit codes

## Considered Options

* Let the commands be responsible for communicating errors and manage exit codes.
* Introduce a custom picocli IExecutionExceptionHandler and handle exceptions there.

## Decision Outcome

Chosen option: "Introduce a custom picocli IExecutionExceptionHandler and handle exceptions there", because it simplifies the code to regular return types and specialized CliExceptions that also contain the exit code.

### Consequences

* Good, because error handling is centralized and consistent.
* Good, because code is simpler and easier to read.
* Bad, because services/helpers must rethrow JabRef Exceptions as CliExceptions (or subclasses).
* Bad, because it requires knowledge about picocli's exception handling.
* Neutral, because unexpected exceptions still flow through Picocli’s default handler.

### Confirmation

Commands rarely catch exceptions or check for valid return values. Exceptions are thrown as `CliException` and subclasses (or `JabRefException`) from the service and helper classes but not handled in the command.

## Pros and Cons of the Options

### Let the commands be responsible for communicating errors and manage exit codes

* Good, because it does not require framework knowledge and is plain java.
* Bad, because it's hard to track which cases are handled and which are not (or somewhere else).
* Bad, because it duplicates a lot of error handling and risks divergent behavior.
