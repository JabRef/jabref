---
nav_order: 46
parent: Decision Records
---
# Use JBang for index file generation

## Context and Problem Statement

Due to speed reasons, JabRef generates index files during build time.
For instance, `.mv` files for journal list abbreviations and a `.json` file for an index on properties of CSL files.

These indexes need to be generated.
Thereby, existing, up-to-date code, from JabRef should be used

## Decision Drivers

* High maintainability
* Low effort for implementing

## Considered Options

* JBang
* Gradle-based project

## Decision Outcome

Chosen option: "JBang", because

* All "scripts" have a total length of 300 lines of code
* JBang is a well-enough supported tool
* Using gradle would lead to introduce another "jablib" project: "jablib-journals-csl", which depends on the jablib. This is much effort to implement and get project dependencies right.
