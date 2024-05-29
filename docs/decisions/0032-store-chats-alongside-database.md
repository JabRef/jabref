---
nav_order: 0032
parent: Decision Records
---
<!-- markdownlint-disable-next-line MD025 -->
# Store chats alongside database

## Context and Problem Statement

Chats with AI should be stored somewhere. But where and how?

## Considered Options

* Inside BIB database
* In local user folder
* Alongside BIB database

## Decision Outcome

Chosen option: "Alongside BIB database", because
simple to implement and gives the user the ability to share or not to share the chats.

### Positive Consequences

* Simple implementation
* The user can send the chats file alongside the BIB database if they want to share the chats. If users do not want 
to share the messages, then they can omit the chats file
* BIB files is kept clean

### Negative Consequences

* User may not expect that a new file will be created alongside their BIB (or other LaTeX-related) files
* It may be not convenient to share both files (BIB database and chats file) in order to share chat history.
* If BIB files are edited externally (meaning, not inside the JabRef), then chats file will not be updated correspondingly
* If user moves BIB file, they should move the chats file too

## Pros and Cons of the Options

### Inside BIB database

* Good, because we already have a machinery for managing the fields and other information of BIB entries
* Good, because chats are stored inside one file, and if the BIB database is moved, the chat history is preserved
* Bad, because there may be lots of chats and messages and BIB database become too cluttered and big which solves down
the processing of BIB file
* Bad, because if user shares a BIB file, they will also share chat messages, but chats are not ideal, so user may not 
want to share them

### In local user folder

* Good, because BIB database is kept clean
* Good, because chat messages are saved locally
* Neutral, because may be a little harder to implement
* Bad, because chat messages cannot be easily shared
* Bad, because when path of a BIB database is changed, the chats are lost

### Alongside BIB database
Refer to positive and negative consequences of the decision outcome.
