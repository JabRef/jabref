---
nav_order: 57
parent: Decision Records
status: accepted
date: 2026-04-20
---
# Multi-file move uses a single chosen target directory for all selected files

## Context and Problem Statement

The linked files panel allows selecting multiple linked files simultaneously and invoking the move action on all of them at once.
Selected files may currently reside in different configured directories ([X1–X4](0056-linked-file-move-context-menu-shows-all-configured-directories.md)).
How should the move action behave when the selection spans multiple directories?

Issue: [#12287](https://github.com/JabRef/jabref/issues/12287)

## Considered Options

* Per-file target: compute the target directory individually for each file based on an algorithm
* Disable the action when selected files reside in different directories
* Generic rotation: move each file to the "next" directory in the configured order
* Single chosen target: offer all configured directories; all selected files move to the one the user picks

## Decision Outcome

Chosen option: "Single chosen target", because it keeps behavior explicit and predictable — the user selects a destination and every file ends up there, regardless of where it started.

### Consequences

* Good, because the user has full, deliberate control over the destination.
* Good, because no per-file logic is hidden from the user.
* Neutral, because files already in the chosen target directory result in a no-op; the implementation must handle this gracefully (skip without error).
* Bad, because the user cannot move different files to different targets in a single action; separate invocations are needed.

### Confirmation

Code review should verify that the same target directory path is passed to every file's move operation, and that files already residing in the target are skipped without raising an error.

## Pros and Cons of the Options

### Per-file target

* Good, because each file follows the same algorithm a single-file move would.
* Bad, because the outcome is unpredictable when files start in different locations — the user cannot know in advance where each file will land.

### Disable the action when files span different directories

* Good, because it avoids ambiguity entirely.
* Bad, because it punishes the user for a natural multi-select workflow and provides no way to bulk-move files from mixed locations.

### Generic rotation

* Good, because it is consistent with a pure rotation model.
* Bad, because different files move to different targets, making the result hard to predict or explain.

### Single chosen target

* Good, because behavior matches the "move all selected files here" mental model.
* Good, because the UI (all directories listed, pick one) makes the outcome self-evident.
