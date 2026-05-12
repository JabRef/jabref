---
nav_order: 56
parent: Decision Records
status: accepted
date: 2026-04-20
---
# Context menu for moving linked files should show all configured directories

## Context and Problem Statement

JabRef supports up to four file directory types for linked files:

* **X1 — Main file directory**: set in global preferences; accessible to the current user only
* **X2 — Library-specific file directory**: set per library; accessible to all users of that library
* **X3 — User-specific file directory**: set per user per library; only accessible to the current user
* **X4 — Next to library**: the global preference "store files next to library" is active; files are stored alongside the `.bib` file itself

See [Directories for Files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files) for a user-facing explanation of these directory types.

When a user right-clicks a linked file, a context menu entry allows moving the file.
How should that menu communicate the available move targets?

Issue: [#12287](https://github.com/JabRef/jabref/issues/12287)

## Decision Drivers

* Users should know exactly where a file will land before confirming the action.
* All configured directories should be reachable without multiple interactions.
* JabRef's directory model — with its four distinct scopes — is a core feature worth exposing.

## Considered Options

* Show one dynamic label computed from an algorithm ("Move file to [best target]")
* Show a single generic label that rotates through directories ("Move file to next configured directory")
* Show all configured directories as separate menu items; disable the item for the directory the file already resides in

## Decision Outcome

Chosen option: "Show all configured directories as separate menu items", because it gives users explicit, predictable control over the target without requiring them to understand a rotation algorithm, and because it surfaces the full breadth of JabRef's directory configuration.

### Consequences

* Good, because the user directly picks a target — no hidden rotation order to learn.
* Good, because disabled items passively inform the user which directories are configured and which is current.
* Good, because adding or removing a directory configuration is immediately reflected in the menu.
* Bad, because users with all three directories configured see up to three items, which is more verbose than a single entry.

### Confirmation

Code review should verify that the context menu dynamically builds one item per configured directory and that the item corresponding to the file's current directory is disabled (not hidden).

## Pros and Cons of the Options

### One dynamic label ("Move file to [best target]")

* Good, because the menu stays compact with exactly one item.
* Bad, because the user cannot choose; the algorithm decides the target for them.
* Bad, because the algorithm (issue #12287 comments) has acknowledged inconsistencies between the two-directory and three-directory cases that are hard to explain to users.

### Single generic rotating label ("Move file to next configured directory")

Implemented by [PR #15055](https://github.com/JabRef/jabref/pull/15055).

* Good, because the menu stays compact.
* Bad, because "next" is opaque — the user must know the rotation order (X3 → X2 → X1 → X3) to predict where the file lands.
* Bad, because reaching a non-adjacent target requires multiple interactions.

### All configured directories as separate items, current one disabled

* Good, because behavior is explicit and discoverable.
* Good, because any target is reachable in one action.
* Neutral, because the menu grows proportionally with the number of configured directories (maximum four items).
