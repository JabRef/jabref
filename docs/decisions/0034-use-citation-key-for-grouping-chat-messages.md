---
nav_order: 0034
parent: Decision Records
---

# Use Citation Key for Grouping Chat Messages

## Context and Problem Statement

As we store chat messages not inside a BibTeX entry in `.bib` file, the chats file is represented as a map to
BibTeX entry and a list of messages. We need to specify the key of this map. Turns out, it is not that easy.

## Decision Drivers

* The key should exist for every BibTeX entry
* The key should be unique along other BibTeX entries in one library file
* It is assumed that the key does not change at run-time, between launches of JabRef, and should be cross-platform (most important)

## Considered Options

* `BibEntry` Java object
* `BibEntry`'s `id`
* `BibEntry`'s citation key
* `BibEntry`'s `ShareId`

## Decision Outcome

Chosen option: "`BibEntry`'s citation key", because this is the only choice that complies to the third point in Decision Drivers.

### Positive Consequences

* Easy to implement
* Cross-platform

### Negative Consequences

* If the citation key is changed externally, then the chats file becomes out-of-sync
* Additional user interaction in order to make the citation key complain the first and second points of Decision Drivers

## Pros and Cons of the Options

### `BibEntry` Java object

Very bad, because it works only at run-time and is not stable.

### `BibEntry`'s `id`

JabRef stores a unique identifier for each `BibEntry`.
This identifier is created on each load of a library (and not stored permanently).

Very bad, for the same reasons as `BibEntry` Java object.

### `BibEntry`'s citation key

* Good, because it is cross-platform, stable (meaning stays the same across launches of JabRef)
* Bad, because it is not guaranteed that citation key exists on `BibEntry`, and that it is unique across other
BibTeX entries in the library

### `BibEntry`'s `ShareId`

[ADR-0027](0027-synchronization.md) describes the procedure of synchronization of a Bib(La)TeX library with a server.
Thereby, also local and remote entries need to be kept consistent.
The solution chosen there is that the **server** creates a UUID for each entry.

This approach cannot be used here, because there is no server running which we can ask for an UUID of an entry.

## More Information

Refer to [issue #160](https://github.com/JabRef/jabref/issues/160) in JabRef main repository

[ADR-038](0038-use-entryId-for-bibentries.md) takes another option, because it re-generates the index at each start of JabRef.
