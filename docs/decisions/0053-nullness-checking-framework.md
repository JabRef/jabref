---
nav_order: 53
parent: Decision Records
---

# Null Checking Framework Selection

## Context and Problem Statement

Null pointer exceptions are a pervasive source of bugs in Java. To alleviate this, we want a compile-time null‐checking / nullness analysis framework. Two major contenders are Checker Framework and Error Prone (with NullAway). We also want our solution to align with modern nullness specification efforts such as JSpecify.

## Decision Drivers

* Static prevention of null dereference errors.
* Developer experience: low friction, good IDE, and build integration.
* Scalability to our codebase.
* Interoperability with JSpecify and future ecosystem alignment.
* Incremental adoption: we should not be forced to annotate everything at once.

## Considered Options

* Error Prone + NullAway
* Checker Framework
* No nullness analysis (status quo)

## Decision Outcome

Chosen option: "Error Prone + NullAway" because it comes out best (see below).

## Consequences

* We will annotate public APIs (interfaces, service endpoints, widely used modules) using JSpecify `@Nullable` / `@NonNull` (or `@NullMarked` scoping) progressively.
* We enable NullAway in CI builds; thus violations break the build.
* NullAway may not check some corner cases (especially around generics); therefore, manual reviews need to keep that in mind.
* Developers must be trained on JSpecify and on how NullAway behaves.
* Over time, we gradually annotate more of the codebase; initially non-annotated code will be treated as “unspecified nullness” until annotated.

## Pros and Cons of the Options

### Checker Framework

Good, because it supports advanced nullness contracts, refinement, and pluggability.
Bad, because compile times increase significantly.
Bad, because it lacks support for JSpecify’s `@NullMarked` / `@NullUnmarked` scopes.

## Error Prone + NullAway

Good, because it is lightweight, fast, and has a low overhead.
Good, because support for JSpecify is built-in.
Good, because it fully supports JSpecify annotations.
Good, because it is [endorsed by JUnit 6](https://github.com/junit-team/junit-framework/wiki/Upgrading-to-JUnit-6.0#null-safety).
Bad, because it lacks support for advanced nullness contracts, refinement, and pluggability, and misses some edge cases.

## No Nullness Framework

Good, because zero overhead or tooling complexity.
Bad, because we suffer from null pointer exceptions at runtime.
