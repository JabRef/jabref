---
nav_order: 0032
parent: Decision Records
---
# Store Chats Alongside Database

## Context and Problem Statement

Chats with AI should be stored somewhere. But where and how?

## Considered Options

* Inside `.bib` file
* In local user folder
* Alongside `.bib` file

## Decision Drivers

* Should work when shared with OneDrive, Dropbox or similar asynchronous services
* Should work on network drives
* Should be "easy" for users to follow
* Should be the same in a shared and non-shared setting (e.g., if Dropbox is used or not should make a difference)

## Decision Outcome

Chosen option: "In local user folder", because
it's very hard to work with a shared library, if two users will work
simultaneously on one library, then AI chats file will be absolutely arbitrary
and unmergable.

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

Concrete example for backup folder: `C:\Users\${username}\AppData\Local\org.jabref\jabref\backups`.
Example filename: `4a070cf3--Chocolate.bib--2024-03-25--14.20.12.bak`.

* Good, because `.bib` file is kept clean
* Good, because chat messages are saved locally
* Neutral, because may be a little harder to implement
* Bad, because chat messages cannot be easily shared
* Bad, because when path of a `.bib` file is changed, the chats are lost

### Alongside `.bib` file

* Good, because simple implementation
* Good, because, the user can send the chats file alongside the `.bib` file if they want to share the chats. If users do not want
  to share the messages, then they can omit the chats file
* Good, because `.bib` files is kept clean
* Bad, because user may not expect that a new file will be created alongside their `.bib` (or other LaTeX-related) files
* Bad, because, it may be not convenient to share both files (`.bib` file and chats file) in order to share chat history.
* Bad, because if `.bib` files are edited externally (meaning, not inside the JabRef), then chats file will not be updated correspondingly
* Bad, because if user moves `.bib` file, they should move the chats file too
* Bad, because if two persons work in parallel using a OneDrive share, the file is overwritten or a conflict file is generated. ([Dropbox "conflicted copy"](https://help.dropbox.com/en-en/organize/conflicted-copy))
