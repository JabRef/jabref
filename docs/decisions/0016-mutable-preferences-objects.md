---
parent: Decision Records
nav_order: 16
---
# Mutable preferences objects

## Context and Problem Statement

To create an immutable preferences object every time seems to be a waste of time and computer memory.

## Considered Options

* Alter the existing object and return it (by a `with*` -method, similar to a builder, but changing the object at hand).
* Create a new object every time a preferences object should be altered.

## Decision Outcome

Chosen option: "Alter the exiting object", because the preferences objects are just wrappers around the basic preferences framework of JDK. They
should be mutable on-the-fly similar to objects with a Builder inside and to be stored immediately again in the
preferences.

### Consequences

* Import logic will be more hard as exising preferences objects have to be altered; and it is very hard to know which preference objects exactly are needed to be modified.
* Cached variables need to be observables, too. AKA The cache needs to be observable.
* There is NO "real" factory pattern for the preferences objects, as they are mutable --> they are passed via the constructor and long-lived
