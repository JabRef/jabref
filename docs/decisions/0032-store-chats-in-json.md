---
nav_order: 0032
parent: Decision Records
---
# Store chats in JSON

## Context and Problem Statement

This is a follow-up to [ADR-031](0031-store-chats-alongside-database.md).

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

Chosen option: "JSON", because it is the simplest.
In case memory and disk consumption will be too high, we will consider switching to MVStore (or other memory-efficient formats).

## Pros and Cons of the Options

### JSON

* Good, because allows for easy storing and loading of chats
* Good, because cross-platform
* Good, because widely used and accepted, so there are lots of libraries for JSON format
* Good, because it is even possible to reuse the chats file for other purposes
* Bad, because too verbose (meaning the file size could be much smaller)

### MVStore

* Good, because automatic loading and saving to disk
* Good, because memory-efficient
* Bad, because the order of messages need to be "hand-crafted" (e.g., by mapping from an Integer to the concrete message) 

### Custom format

* Good, because we have the full control
* Bad, because involves writing our own language and parser
