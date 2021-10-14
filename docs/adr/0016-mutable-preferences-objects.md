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
