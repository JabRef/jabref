---
nav_order: 0061
parent: Decision Records
status: proposed
date: 2026-05-31
---

# Separate JabKit Commands and Services

## Context and Problem Statement

JabKit commands previously duplicated import and export logic across commands and used static helper methods from the JabKit class for multiple responsibilities (This led to subtly diverging implementations). We should group these functionalities in separate Classes for each responsibility as single integration points.

## Decision Drivers

* Keep import/export behavior consistent
* Reduce/avoid duplication across commands
* Improve testability

## Considered Options

* Use specialized Utility classes for importing/exporting/fetching with static methods
* Introduce shared JabKit services to separate Commands and logic
* Keep import/export logic and fetcher creation inside each command

## Decision Outcome

Chosen option: "Introduce shared JabKit services to separate Commands and logic", mostly to ease testing and centralize behavior.

### Consequences

* Good, because import/export behavior is kept consistent and reusable.
* Good, because changes to adapt to new creation or processing are made in one place.
* Good, because the service layer can be tested without picocli, like typical java classes.
* Bad, because command execution adds a service layer indirection.

### Confirmation

The Command classes act as thin CLI layer and mostly delegate the technical work to the services/facades. Their main responsibility is the workflow and the orchestration of the services. (e.g. the decision when to call them, how to handle the results)
