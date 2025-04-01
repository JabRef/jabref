---
nav_order: 0033
parent: Decision Records
---

# Store Chats in MVStore

## Context and Problem Statement

This is a follow-up to [ADR-032](0032-store-chats-in-local-user-folder.md).

The chats with AI should be saved on exit from JabRef and retrieved on launch. We need to decide the format of
the serialized messages.

## Decision Drivers

* Easy to implement and maintain
* Memory-efficient (because JabRef is said to consume much memory)

## Considered Options

* JSON
* MVStore
* Custom format

## Decision Outcome

Chosen option: "MVStore", because it is simple and memory-efficient.

## Pros and Cons of the Options

### JSON

* Good, because allows for easy storing and loading of chats
* Good, because cross-platform
* Good, because widely used and accepted, so there are lots of libraries for JSON format
* Good, because it is even possible to reuse the chats file for other purposes
* Good, because has potential for being mergeable by external tooling
* Bad, because too verbose (meaning the file size could be much smaller)

### MVStore

* Good, because automatic loading and saving to disk
* Good, because memory-efficient
* Bad, because does not support mutable values in maps.
* Bad, because the order of messages need to be "hand-crafted" (e.g., by mapping from an Integer to the concrete message), since [MVStore does not support storing list which update](https://github.com/koppor/mvstore-mwe/pull/1).
* Bad, because it stores data as key-values, but not as a custom data type (like tables in RDBMS)

### Custom format

* Good, because we have the full control
* Bad, because involves writing our own language and parser
* Bad, because we need to implement optimizations found in databases on our own (storing some data in RAM, other on disk)
