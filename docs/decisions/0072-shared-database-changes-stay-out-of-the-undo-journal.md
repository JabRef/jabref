---
title: Changes pulled from a shared database stay out of the undo journal
nav_order: 72
---

# Changes pulled from a shared database stay out of the undo journal

`adr~shared-changes-are-not-undoable~1`

Needs: impl

## Context and Problem Statement

A library backed by a shared SQL database receives changes made by other people.
`DBMSSynchronizer` applies them to the local model — inserting, removing, and overwriting entries without putting anything on the undo journal.
A local user therefore cannot press Ctrl+Z to take back an edit that arrived from a colleague.

One is typing in the `Author` field, a pull overwrites it, the text vanishes, and one reaches for undo. Should a pulled change become an undo step?
Undo means *reverse the change in the local model*, while the shared database still holds the value the change carried.
The two copies would disagree until the next push, at which point the local revert either overwrites the colleague's edit or is itself overwritten, depending on which side wins the optimistic lock.

## Decision Drivers

* Undo must not be a way to overwrite someone else's work without saying so.
* A user whose text disappears under a pull needs some answer, even if it is not "undo it".
* Whether a local revert may propagate is a question about who owns a change — synchronisation semantics, not journal mechanics.
* The library must still report that it needs saving after a pull, or the change is lost on close.

## Considered Options

* Record pulled changes on the journal, so undo reverses them locally
* Leave pulled changes off the journal, and make the local user's own undo safe in their presence

## Decision Outcome

Chosen option: "leave pulled changes off the journal", because making them undoable answers a synchronisation question by accident.
A local undo would take back something the shared database still holds; deciding what happens next — push the revert, or refuse it — is the shared-database design's call, and no part of it is decided by how the journal is built.
The journal stays a record of the local this user's actions.

Instead the local user will encounter:

* Their own recorded change **refuses** to apply once a pulled change has moved the value on, and says so — "field author holds 'X', not the recorded 'Y'" — rather than overwriting the newer value (`req~logic.undo.stale-change-refused~1`).
  Thus undo after a remote overwrite reports that the library has moved on, and the colleague's edit survives.
* The pulled change **marks the library as needing a save**, because it arrives as `EntriesEventSource.SHARED` and `LibraryTab` treats a change nobody recorded as one only saving can settle (`req~logic.undo.modified-marker-derived~1`).

## Confirmation

`DBMSSynchronizer` carries the tag `[impl->adr~shared-changes-are-not-undoable~1]` at the point where a pulled change is applied to the local model, and its class javadoc states the decision.
The two behaviours the decision leans on are traced separately and covered by tests: refusal by `req~logic.undo.stale-change-refused~1`, and the modified marker by `req~logic.undo.modified-marker-derived~1`.

## Pros and Cons of the Options

### Record pulled changes on the journal

* Good, because Ctrl+Z after a remote overwrite would restore what the user was typing.
* Bad, because the local model and the database disagree from that moment until the next push, and nothing in the journal decides what happens then.
* Bad, because the step would be attributed to a user who did not make the change, so redo and the step's name ("Change field Author") would describe someone else's edit.
* Bad, because it invites undoing a *series* of pulled changes, which the optimistic lock would then have to reconcile change by change.

### Leave pulled changes off the journal (chosen)

* Good, because undo can never silently overwrite a change made by another person.
* Good, because the journal keeps one meaning: the steps this user took, in this session.
* Good, because the journal describes only what the local user did, which is what its steps claim.
* Good, because the dangerous case is handled where it belongs: a stale change refuses and reports, instead of writing over a newer value.
* Good, because the behaviour is now stated in `DBMSSynchronizer` rather than being inferred from the absence of a call.
* Bad, because "undo does not work here" is a rule the user has to learn from behaviour; nothing in the UI says the library is shared in that moment.
* Bad, because a user who watched their text vanish still cannot restore it with one keystroke; they are told what happened, and must retype it.
* Neutral, because the decision is reversible at the cost of a synchronisation design, not of a rewrite.
* Neutral, because nothing is foreclosed: the changes are already values (`BibChange`), so recording them later costs a call, once the synchronisation question is answered.

## More Information

The undo/redo workstream that produced the refusal rule and the derived modified marker is recorded in [ADR-0070](0070-split-the-undo-manager-into-recording-and-gui-layers.md).
Shared-database synchronisation itself is described in `DBMSSynchronizer`'s class javadoc.

<!-- markdownlint-disable-file MD022 -->
