---
nav_order: 70
parent: Decision Records
---

# Undo as a value-based change journal, layered from jablib up to JavaFX

## Context and Problem Statement

Undo and redo were built on `javax.swing.undo` in a JavaFX application. Thirteen `Undoable*` classes each extended `AbstractUndoableJabRefEdit` and hand-wrote `undo()` and `redo()` as two independent methods that could drift apart; two of them accepted a `FieldChange` and then re-implemented from scratch what that change already described, one of them round-tripping a typed value through strings. `CountingUndoManager` bolted JavaFX properties onto the Swing manager and hopped to the JavaFX thread after every operation purely to refresh them, so recording a change in a plain unit test failed with "Toolkit not initialized". The whole journal lived in `org.jabref.gui.undo`, so jabkit and jabsrv had no undo, no dry run and no change preview.

Recording was a four-step ritual repeated at 113 call sites: build a compound edit, mutate, check whether anything was collected, push. Nothing enforced the last step, and it was in fact missed — keyword management built its edit and dropped it with a `// TODO` comment, so "Manage keywords" was silently not undoable. The same defect turned up twice more while migrating: file import and "Replace string".

The question this record answers is how undo should be structured instead: what a change *is*, where the journal lives, how commands hand changes to it, and how the JavaFX-facing parts attach without pulling the toolkit back into the model.

## Decision Drivers

* Undo needs strict total order, no losses, and changes visible to the next model read — it is a journal, not a notification stream.
* An inverse that is written by hand is an inverse that can drift from the thing it inverts.
* Handing a collected edit to the journal must not be a step a command can forget.
* The journal should be usable without a started toolkit, so that recording can be unit-tested and reused by jabkit and jabsrv.
* Menu enablement needs JavaFX properties, and updating them needs a hop to the JavaFX thread — which must happen after the journal's lock is released, or a listener waiting for that thread deadlocks the journal.
* Dataflow should stay visible rather than ambient for easier understanding.

## Considered Options

* Keep the `javax.swing.undo` hierarchy and repair it in place
* Changes as values: a sealed interface of records, with the inverse derived, and a plain-Java journal in jablib

## Decision Outcome

Chosen option: "changes as values, with a plain-Java journal in jablib", because it makes the two defects that produced real bugs unexpressible rather than merely discouraged: an inverse cannot drift from its change when it is derived from the same data, and a command cannot forget to hand over what it collected when the journal owns the collecting.

`BibChange` is a sealed interface in `org.jabref.model.undo` implemented by records. Each holds what it needs to perform itself and what it needs to reverse itself, so undoing is `change.inverted().apply()` rather than a second hand-written method. `ChangeSet` groups the changes of one user action and is itself a `BibChange`, so a command delegating to a helper still produces one undo step. Changes carry no user-facing text; only `ChangeSet` has a name, at the granularity a user acts in.

Commands record inside a block — `addEdit(name, edit -> …)` — which builds the set, discards it when empty and pushes it. `applyEdit(change)` performs and records in one operation under a single acquisition of the journal's monitor.

The journal is typed in four parts, following the already present preferences layering:

* `UndoManager` (jablib) — the recording interface: what about 120 classes depend on, and all they can do.
* `JabRefUndoManager` (jablib) — the journal: stacks, undo, redo, saved position. Plain Java, no toolkit.
* `GuiUndoManager` (jabgui) — extends the recording interface with the control surface and the two JavaFX properties. It declares the control methods rather than inheriting them, because an interface cannot inherit from a class.
* `JabRefGuiUndoManager` (jabgui) — extends the journal and implements that interface.

The JavaFX layer *extends* the journal rather than wrapping it, and subscribes to itself, so the thread hop happens where listeners are notified: after the monitor is released.

### Confirmation

`JabRefUndoManagerTest` records, undoes and redoes with no toolkit started, which is the property that separates this design from `CountingUndoManager`. `JabRefGuiUndoManagerTest` starts one and covers the marshalling, including that a queued property update applies the state it finds when it runs. The correctness rules are traced as `req~logic.undo.saved-position-identity~1`, `req~logic.undo.apply-and-record-atomically~1` and `req~logic.undo.journal-per-library~1`. In jabgui, `JabRefUndoManager` is named only where `JabRefGuiUndoManager` extends it, and `JabRefGuiUndoManager` only in `JabRefGuiStateManager`, which creates one journal per open library and drops it when the library closes.

## Pros and Cons of the Options

### Keep the `javax.swing.undo` hierarchy

* Bad, because `undo()` and `redo()` stay two hand-written methods per class, free to disagree.
* Bad, because it keeps `java.desktop` in a JavaFX application and the journal in `org.jabref.gui`.

### Changes as values, journal in jablib (chosen)

* Good, because the inverse is derived, the composite is a member of the same sealed type, and a `switch` over changes is exhaustive without a `default`.
* Good, because failure policy is stated once, on `ChangeSet.apply`, instead of per subclass.
* Good, because the core invariant is executable: `change.inverted().inverted()` equals the change, and records supply the equality that makes such a property testable.
* Good, because the recording block removed the defect it was designed against — found three times before the migration finished.
* Good, because the journal is plain Java: its tests run headless, and jabkit and jabsrv can reuse it.
* Good, because a declared type states what a class does with undo; only five classes ask for more than recording, and no implementation type is threaded through the GUI.
* Bad, because `JabRefUndoManager` is now an extension point: it stays non-final and shares its monitor with the JavaFX subclass.
* Bad, because `GuiUndoManager` restates eight signatures the journal already implements, and the two must be kept in step by hand.
