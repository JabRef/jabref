# Use `public final` instead of getters to offer access to immutable variables

## Context and Problem Statement

When making immutable data accessible in a java class, should it be using getters or by non-modifiable fields?

## Considered Options

* Offer public static field
* Offer getters

## Decision Outcome

Chosen option: "Offer public static field", because getters used to be a convention which was even more manifested due to libraries depending on the existence on getters/setters. In the case of immutable variables, adding public getters is just useless since one is not hiding anything.

### Positive Consequences

* Shorter code

### Negative Consequences

* newcomers could get confused, because getters/setters are still taught
