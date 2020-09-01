# Mutable preferences objects

## Context and Problem Statement

To create an immutable preferences object every time seems to be a waste of time and computer memory. 

## Considered Options

* Create a new object every time a preferences object should be altered by a with*-method, similar to a Builder.
* Alter the existing object and return it.

## Decision Outcome

Chosen option: 1, because the preferences objects are just wrappers around the basic preferences framework of JDK. They
should be mutable on-the-fly similar to objects with a Builder inside and to be stored immediatly again in the
preferences.
