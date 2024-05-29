---
nav_order: 0033
parent: Decision Records
---
<!-- markdownlint-disable-next-line MD025 -->
# Use citation key for grouping chat messages

## Context and Problem Statement

Because we store chat messages not inside a BIB entry in BIB database, the chats file is represented as a map to
BIB entry and a list of messages. We need to specify the key of this map. Turns out, it is not that easy.

## Decision Drivers

* The key should exist for every BIB entry
* The key should be unique along other BIB entries in one library file
* The key should not change at run-time, between launches of JabRef, and should be cross-platform (most important)

## Considered Options

* `BibEntry` Java object
* `BibEntry` ID
* Citation key

## Decision Outcome

Chosen option: "Citation key", because
this is the only choice that complains to the third point in Decision Drivers.

### Positive Consequences

* Easy to implement
* Cross-platform

### Negative Consequences

* If the citation key is changed externally, then the chats file becomes out-of-sync
* Additional user interaction in order to make the citation key complain the first and second points of Decision Drivers

## Pros and Cons of the Options

### `BibEntry` Java object
Very bad, because it works only at run-time and is not stable.

### `BibEntry` ID
Very bad, for the same reasons as `BibEntry` Java object.

### Citation key
* Good, because it is cross-platform, stable (meaning stays the same across launches of JabRef)
* Bad, because it is not guaranteed that citation key exists on BIB entry, and that it is unique across other 
BIB entries in the library

## More Information

Refer to [issue #160](https://github.com/JabRef/jabref/issues/160) in JabRef main repository
