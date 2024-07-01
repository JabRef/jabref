---
nav_order: 0031
parent: Decision Records
---
<!-- markdownlint-disable-next-line MD025 -->
# Store chats alongside database

## Context and Problem Statement

Chats with AI should be stored somewhere. But where and how?

## Considered Options

* Inside `.bib` file
* In local user folder
* Alongside `.bib` file

## Decision Outcome

Chosen option: "Alongside `.bib` file", because
simple to implement and gives the user the ability to share or not to share the chats.

### Positive Consequences

* Simple implementation
* The user can send the chats file alongside the `.bib` file if they want to share the chats. If users do not want
  to share the messages, then they can omit the chats file
* `.bib` files is kept clean

### Negative Consequences

* User may not expect that a new file will be created alongside their `.bib` (or other LaTeX-related) files
* It may be not convenient to share both files (`.bib` file and chats file) in order to share chat history.
* If `.bib` files are edited externally (meaning, not inside the JabRef), then chats file will not be updated correspondingly
* If user moves `.bib` file, they should move the chats file too

## Pros and Cons of the Options

### Inside `.bib` file

* Good, because we already have a machinery for managing the fields and other information of BIB entries
* Good, because chats are stored inside one file, and if the `.bib` file is moved, the chat history is preserved
* Bad, because there may be lots of chats and messages and `.bib` file become too cluttered and too big which slows down the processing of `.bib` file
* Bad, because if user shares a `.bib` file, they will also share chat messages, but chats are not ideal, so user may not
  want to share them

### In local user folder

One can use `%APPDATA%`, where JabRef stores the Lucene index and other information.
See `org.jabref.gui.desktop.os.NativeDesktop#getFulltextIndexBaseDirectory` for use in JabRef and
<https://github.com/harawata/appdirs> for general information.

* Good, because `.bib` file is kept clean
* Good, because chat messages are saved locally
* Neutral, because may be a little harder to implement
* Bad, because chat messages cannot be easily shared
* Bad, because when path of a `.bib` file is changed, the chats are lost

### Alongside `.bib` file

Refer to positive and negative consequences of the decision outcome.
