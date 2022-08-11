---
parent: Decision Records
nav_order: 26
---
# Safe File Writing by Writing to a Temporary File in Another Folder

## Context and Problem Statement

JabRef needs to write to the .bib file. A .bib file of a user should never be damaged. JabRef needs to provide a way to do "safe" writing of a bib file.

## Considered Options

* Create a temporary file in a local folder and copy after successful write
* Create temporary file next to bib file and atomically move it to the original file

## Decision Outcome

Chosen option: "Create a temporary file in a local folder and copy after successful write", because good usage of Dropbox outweighs potential recovery scenarios

## Pros and Cons of the Options

### Create a temporary file in a local folder and copy after successful write

* Good, because Keeps directory of .bib file clean
* Good, because Keeping file access rights is simple as the file content is replaced, not the file itself
* Bad, because Error recovery is hard

### Create temporary file next to bib file and atomically move it to the original file

This implementation is available at [Marty Lamb's AtomicFileOutputStream](https://github.com/martylamb/atomicfileoutputstream/blob/master/src/main/java/com/martiansoftware/io/AtomicFileOutputStream.java)
and [Apache Zookeepr's AtomicFileOutputStream](https://github.com/apache/zookeeper/blob/master/zookeeper-server/src/main/java/org/apache/zookeeper/common/AtomicFileOutputStream.java).

* Good, because Atomic move is an all-or-nothing move
* Bad, because Makes issues with Dropbox, OneDrive, ...
