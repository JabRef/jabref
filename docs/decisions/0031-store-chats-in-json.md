---
nav_order: 0031
parent: Decision Records
---
# Store chats in JSON

## Context and Problem Statement

The chats with AI should be saved on exit from JabRef and retrieved on launch. We need to decide the format of 
the serialized messages.

## Considered Options

* JSON
* Custom format

## Decision Outcome

Chosen option: "JSON", because
it is the simplest.

## Pros and Cons of the Options

### JSON

* Good, because allows for easy storing and loading of chats
* Good, because cross-platform
* Good, because widely used and accepted, so there are lots of libraries for JSON format
* Good, because it is even possible to reuse the chats file for other purposes
* Bad, because too-verbose (meaning the file size could be much smaller)

### Custom format

* Good, because we have the full control
* Bad, because involves writing our own language and parser
