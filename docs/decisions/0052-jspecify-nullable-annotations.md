---
nav_order: 52
parent: Decision Records
---

# Adopt JSpecify nullness annotations for compile-time null safety

## Context and Problem Statement

Our Java codebase contains inconsistent handling of nullability.
We want to detect null-safety issues earlier at compile time and improve API clarity.

## Decision Drivers

* Reduce `NullPointerException`s in production by shifting detection to compile time.
* Provide clearer API contracts.
* Keep runtime overhead low.
* Ensure incremental and low-risk adoption.

## Considered Options

* Do nothing and keep status quo.
* Use `Objects.requireNonNull` and defensive runtime checks.
* Adopt JSpecify annotations.
* Use `assert x == null`.

## Decision Outcome

Chosen option: "Adopt JSpecify annotations" because it comes out best (see below).

## Consequences

* Earlier detection of potential NPEs.
* Clearer API documentation.
* Requires training and CI integration.
* Some friction with unannotated third-party libraries.

## Pros and Cons of the Options

### Do nothing and keep status quo

* Good, because no immediate implementation work.
* Bad, because NPEs remain runtime-only problems.
* Bad, because a mix of different approaches leads to bad code.
* Bad, because it increases technical debt.
* Bad, because assumes that non-annotated symbols allow null.

### Use `Objects::requireNonNull`

* Good, because JDK native.
* Good, because no external dependencies.
* Good, because it makes NPEs more visible on runtime.
* Bad, because it adds runtime overhead.
* Bad, because it does not provide compile-time contracts.
* Bad, because it is not self-documenting for API contract.

### Adopt JSpecify annotations

* Good, because it offers compile-time null safety detection.
* Good, because it works well with common IDEs.
* Good, because of standardized annotations.
* Good, because incremental adoption possible.
* Good, because static analysis supported.
* Good, because compatibility with Kotlin.
* Good, because it is the consensus among major organizations (Google, Microsoft, Jetbrains etc).
* Bad, because it requires annotation effort.
* Bad, because it requires developer training.

### Use `assert x == null`

* Good, because Java language native.
* Good, because no external dependencies.
* Good, because easily readable.
* Good, because it makes NPEs more visible on runtime.
* Bad, because runs by default only in debug mode with option "-ea".
* Bad, because it does not provide compile-time null safety.
* Bad, because it is not self-documenting for API contract.
