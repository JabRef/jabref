---
nav_order: 59
parent: Decision Records
status: accepted
date: 2026-04-20
---
# "Store files next to library" is a valid move target alongside the configured directories

## Context and Problem Statement

JabRef's global preferences include an option to store linked files next to the `.bib` library file rather than in a separately configured directory.
When a user invokes the move action from the context menu, should this "next to library" location ([X4](0056-linked-file-move-context-menu-shows-all-configured-directories.md)) appear as a selectable target alongside [X1–X3](0056-linked-file-move-context-menu-shows-all-configured-directories.md)?

See [Directories for Files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files) for user-facing documentation on all directory types.

Issue: [#12287](https://github.com/JabRef/jabref/issues/12287)

## Considered Options

* Exclude X4 — only offer explicitly path-configured directories (X1, X2, X3) as targets
* Include X4 — treat "next to library" as a named, selectable move target

## Decision Outcome

Chosen option: "Include X4", because it exposes the full range of JabRef's storage capabilities and ensures users can always reach any location JabRef itself uses when saving files automatically.
Omitting it would create a silent asymmetry: JabRef might store a file next to the library automatically, but the user could not move a file there manually.

### Consequences

* Good, because all locations JabRef writes to are also accessible as manual move targets — no hidden asymmetry.
* Good, because users explicitly relying on the "next to library" convention can organise files there without workarounds.
* Neutral, because X4 is only available when a library has been saved to disk (an unsaved library has no path); the menu item must be disabled in that case.
* Bad, because the menu can now show up to four items, which is more verbose than a compact single-entry design.
