# Undo/Redo modernization — implementation plan

Status: proposal for review. Nothing implemented. Branch `fix-undoredo`.

## Sequencing at a glance

| | Scope | Behaviour change | Blocked by |
| --- | --- | --- | --- |
| **PR 0** | Fix dropped keyword edit (defect 3) | yes — keyword edits become undoable | nothing |
| **A1** | New change model + manager + bridge, ~12 new files | no | D4, D5, D6, D8 |
| **A2** | Migrate commands to `UndoScope`, one PR each | no | A1 |
| **A3** | Fold recording into the manager, delete `javax.swing.undo` | no | A2 complete |
| **C** | `progressProperty()` null, misc | no | nothing |
| **P1–P12** | Behaviour changes and follow-ups, one PR each | yes | mostly A2/A3 |

PR 0 goes first and is the first commit on this branch. Everything in workstream A is a pure
refactor; everything user-visible lives in "Postponed". Where a decision is open, the answer
is whatever the code does today — see the tie-breaker below.

## Problem statement

JabRef runs two distinct patterns that are often mistaken for duplication:

- `SimpleCommand` (`jabgui/.../gui/actions/SimpleCommand.java`, 103 subclasses) — the
  *invocation* layer. Extends mvvmfx `CommandBase`; supplies `execute()`, the `executable`
  BooleanProperty for menu/button enablement, and `statusMessage`. No `undo()`.
- `AbstractUndoableJabRefEdit` (`jabgui/.../gui/undo/`, 13 subclasses) — the *journal*
  layer. Extends `javax.swing.undo.AbstractUndoableEdit`; records a model delta and
  replays it in both directions. No invocation, no enablement.

**The split is correct and is kept.** Undo units are model-grained (one field write);
commands are intent-grained (one menu click writing 500 entries). Fusing them would couple
JavaFX threading and property binding to mementos. The two layers meet in exactly one
place today (`UndoAction`), and this plan gives them one further, deliberate seam.

The real defects are elsewhere:

1. **Two parallel change vocabularies.** `FieldChange` (jablib model) and
   `UndoableFieldChange` (jabgui) carry the same four fields. `UndoableKeyChange` and
   `UndoableChangeType` both accept a `FieldChange` in their constructor yet each
   re-implement `undo()`/`redo()` from scratch. `UndoableChangeType` round-trips a typed
   value through strings: `EntryTypeFactory.parse(change.getOldValue())`.
2. **Hand-rolled recording ritual at 113 `addEdit` call sites** across ~28 undo-aware
   commands: create `NamedCompoundEdit`, mutate, check `hasEdits()`, call `end()`,
   `undoManager.addEdit(...)`, then `markBaseChanged()`. Four steps, none compiler-enforced.
3. **A live bug from (2).** `ManageKeywordsViewModel.java:108` builds the compound edit and
   then drops it:
   `// TODO: bp.getUndoManager().addEdit(compoundEdit);`
   Keyword management is silently un-undoable.
4. **`markBaseChanged()` maintained by hand at 13 sites**, although `CountingUndoManager`
   already tracks `balanceProperty`/`unchangedPoint`/`hasChanged()`.
5. **Swing in a JavaFX application.** `javax.swing.undo` drags `java.desktop` into jabgui.
   `CountingUndoManager` bolts JavaFX properties on top and must hop to the FX thread after
   every operation purely to refresh `canUndo`/`canRedo`. Presentation names emit Swing HTML
   (`"<html>…"`, `<ul><li>`) that JavaFX menus never render.
6. **Journal stuck in `gui`.** Pure model operations live in `org.jabref.gui.undo`, so
   jabkit and jabsrv get no undo, no dry-run, no change preview.
7. **Inconsistent failure handling.** `UndoableFieldChange` catches `IllegalArgumentException`
   and logs at info; `UndoableInsertEntries` catches `Throwable` and logs at warn on undo,
   nothing on redo. No policy.
8. **Contract violation.** `SimpleCommand.progressProperty()` returns `null`.
9. **Context menu and keyboard drive different undo stacks in the entry editor.**
   `FieldEditorFX.java:30-47` already installs a `KeyEvent.ANY` filter that intercepts
   Ctrl+Z/Ctrl+Y, calls the global `UndoAction`, and consumes the event — the comment there
   states the intent explicitly ("use the 'global' UndoManager instead of JavaFX
   TextInputControl's native undo/redo handling", issue #11420). So the *keyboard* path is
   already decided and correct.

   The *context menu* was not migrated: `EditorContextAction.java:38-39,81` still binds to
   `textInputControl.undoableProperty()` and calls `textInputControl.undo()`. Symptom: menu
   Undo mutates the control's text, which re-fires the binding at `FieldEditorFX.java:70` and
   pushes a **new forward edit** onto the global stack. Undo-by-menu therefore appears in
   history as a redoable change.

   Narrower than it first appears, and entirely separable from the rest of this plan.
10. **Typing produces one undo entry per keystroke.** `FieldEditorFX.java:70` subscribes to
    `textProperty`; `AbstractEditorViewModel.java:74` pushes an `UndoableFieldChange` on every
    change. Typing "Einstein" creates eight stack entries.
11. **The entire presentation-name mechanism is dead code.** All 13 `getPresentationName()`
    implementations, `getUndoPresentationName()`/`getRedoPresentationName()`, the `<html>`
    wrappers and the `StringUtil.boldHTML` calls have **no consumers**: the only readers are
    `AbstractUndoableJabRefEdit:11` and `NamedCompoundEdit:42`, both of which feed
    `getUndoPresentationName()`, which nothing calls. `UndoAction` notifies with a bare
    `Localization.lang("Undo")`. The associated l10n strings ("change field %0 of entry %1
    from %2 to %3", "insert entry %0", "change key from %0 to %1", "change type of entry %0
    from %1 to %2", …) appear nowhere else in the codebase — they have been translated in
    Crowdin for text no user has seen.

## Governing constraint — the refactor changes no observable behaviour

**PR 0 excepted, every PR in workstreams A and C is behaviour-preserving.** Same undo
granularity, same stack contents, same menu labels, same failure modes — only the code
expressing them changes.

This is what keeps a long migration reviewable. A reviewer gets one acceptance criterion:
*does this change what the user sees? then it does not belong in this PR.* No need to reason
about undo semantics while also reasoning about a type-hierarchy rewrite.

Consequence: every question about *desired* behaviour leaves the critical path by
construction. They are collected under "Postponed" below and are not blockers. Only invariants
a reviewer can check by reading — D4, D5, D6, D8 — remain blocking.

The most visible price: after A2 migrates `AbstractEditorViewModel`, typing still produces one
`ChangeSet` per keystroke (defect 10). That is intentional. Coalescing is a separate PR whose
entire diff is the merge rule.

### Tie-breaker: in doubt, keep current behaviour

Where a decision is genuinely open, the answer is **whatever the code does today** — even when
a different answer looks better in isolation. Minimising blast radius outranks arriving at the
ideal semantics in this pass.

Each such deferral is recorded under "Postponed" or inline in the decision, so choosing the
conservative option now is not the same as pretending the question does not exist. This rule
is what resolves D4–D8 below; without it, three of the four would have argued for a change.

## Design decisions (and rejected alternatives)

**Changes are values; inverses are derived.** A sealed interface of records replaces 13
edit classes. `undo` stops being a method and becomes `apply(inverted())`.

**Rejected: reactive streams (Mutiny/RxJava) as the change transport.** Mutiny is
`Uni`/`Multi` async result composition for Quarkus/Vert.x, not an event bus. Undo requires
strict total order, zero loss, and causal synchrony with model reads; reactive streams give
per-publisher order only, treat dropping as a legitimate backpressure strategy, and are
async by construction. Not in the dependency tree, and jabsrv is Jersey/HK2, not Quarkus.

**Rejected: feeding the journal from Guava `EventBus`.** Google has deprecated it; replacing
the bus is a separate chapter and out of scope. This plan therefore introduces no dependency
on it in either direction — the two efforts do not block each other.

**Rejected: ambient recording via `ScopedValue`.** Available (Java 25, JEP 506) and
superficially elegant, but `ScopedValue` bindings do not cross an executor hand-off.
`GenerateCitationKeyAction` mutates via `UiTaskExecutor.runInJavaFXThread(...)` from inside a
`BackgroundTask`; such commands would silently record nothing — reintroducing defect (3) by
design. Explicit recorder passing also keeps dataflow visible, which matters given JabRef's
use as teaching material.

**Rejected: `javafx.event` for the journal.** `javafx.event` is in fact reachable from jablib
(`module-info.java:166`, `requires transitive javafx.base`), so this is a semantic rejection,
not a modular one. `Event` is a mutable class hierarchy with `EventType` parent chains — no
`equals`, no exhaustive `switch`, no property testing. Worse, any handler may `consume()` a
change, and a journal a listener can silently swallow is not a journal.

**Accepted in principle, postponed in practice: `javafx.event` for gesture routing.** Bubbling
*is* Chain of Responsibility, and `consume()` is exactly how a responder claims a gesture.
JabRef already hand-rolls a degenerate version of this at `FieldEditorFX.java:35` (a
`KeyEvent.ANY` filter that consumes Ctrl+Z and calls the global action). Generalising it into
an `UndoRequestEvent` would be cleaner, but the existing filter works, so this is an
improvement rather than a fix. Postponed — see "Postponed" below.

**Postponed, not rejected: converting `FieldChange` itself into a record.** The conversion is
worth doing — value semantics, free `equals`/`hashCode`/`toString` (the class currently
hand-writes all three, including a 25-line `equals`), and it would let `FieldEdit` wrap or
even replace it rather than duplicate its shape. It is deferred purely on blast radius:
records generate `entry()`, not `getEntry()`, and `FieldChange` is returned by logic APIs
throughout jablib, so the rename touches hundreds of otherwise-unrelated call sites and would
bury the undo change under mechanical noise.

Do it as **its own later PR**, after workstream A2 has settled. It is then a pure mechanical
rename with no design content, reviewable as such, and `FieldEdit` collapses to a thin
adapter — or disappears entirely if `FieldChange` grows `inverted()`/`applyTo()` directly.
Until then `FieldEdit` is constructed *from* `FieldChange`. Tracked here so it is not lost.

## Target design

### Change model — `jablib`, new package `org.jabref.model.change`

```java
public sealed interface BibChange {
    /// The inverse change. `c.inverted().inverted()` must equal `c`.
    BibChange inverted();

    void applyTo(BibDatabaseContext context);
}
```

**No `describe()`.** Per-change text has no consumers today (defect 11), so porting 13
`getPresentationName()` implementations would be porting dead code. Individual changes stay
pure data — two operations, no strings, and therefore **no `Localization` dependency in the
change records at all**.

Description lives where it is actually needed: as a plain record component on `ChangeSet`,
which is the granularity a user thinks in ("Manage keywords") and the only granularity the
stack ever exposes. One string per user gesture instead of one per change — a 500-entry batch
carries one name, not 500.

If an undo-history UI is ever built and wants per-change text, it belongs in jabgui as a
`BibChangeDescriber` switching over the sealed type — exhaustive without a `default`, and it
keeps l10n in the GUI layer rather than in model records.

```java
public record FieldEdit(BibEntry entry, Field field,
                        @Nullable String before, @Nullable String after) implements BibChange {

    public FieldEdit(FieldChange change) {
        this(change.getEntry(), change.getField(), change.getOldValue(), change.getNewValue());
    }

    @Override
    public FieldEdit inverted() {
        return new FieldEdit(entry, field, after, before);
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        if (after == null) {
            entry.clearField(field);
        } else {
            entry.setField(field, after);
        }
    }
}
```

Grouping reuses the same interface — Composite, no second mechanism:

```java
public record ChangeSet(String name, List<BibChange> changes) implements BibChange {

    @Override
    public ChangeSet inverted() {
        return new ChangeSet(name, changes.reversed().stream().map(BibChange::inverted).toList());
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        changes.forEach(change -> change.applyTo(context));
    }
}
```

`changes.reversed()` (`SequencedCollection`, Java 21+) — reverse the order *and* invert each
element. That single line is the entire correctness argument for compound undo, and it
subsumes `NamedCompoundEdit` including its `hasEdits`/`end()` lifecycle: an empty
`ChangeSet.changes()` is the `hasEdits` check, and there is no "ended" state to forget.

Permitted members: `FieldEdit`, `EntryTypeEdit`, `EntriesInserted`, `EntriesRemoved`,
`StringEdit`, `PreambleEdit`, `GroupEdit`, `ChangeSet`. `EntriesInserted.inverted()` returns
`EntriesRemoved`, so the pairing is checked by the type system, and `sealed` makes a `switch`
over changes exhaustive without a `default`.

Failure policy is defined once, on `ChangeSet.applyTo`, instead of per subclass (defect 7).

### Manager — `jabgui`, `org.jabref.gui.undo.UndoRedoManager`

```java
public final class UndoRedoManager {
    private final Deque<BibChange> undoStack = new ArrayDeque<>();
    private final Deque<BibChange> redoStack = new ArrayDeque<>();
    private final BibDatabaseContext context;

    private final ReadOnlyBooleanWrapper canUndo = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper canRedo = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(false);
}
```

`javafx.beans` properties work headless, so this is unit-testable with no Swing and no toolkit
initialization — unlike `CountingUndoManager`, which hops threads on every operation just to
refresh its own properties. `dirty` is derived from stack depth against a saved marker, which
absorbs the 13 hand-maintained `markBaseChanged()` calls (defect 4).

### Recording seam — `UndoScope`

```java
undoScope.record(Localization.lang("Manage keywords"), recorder -> {
    for (BibEntry entry : entries) {
        recorder.record(entry.putKeywords(keywords, separator));   // Optional<FieldChange>
    }
});
```

`record(...)` owns `ChangeSet` construction, discards it when empty, pushes it, and updates
dirty state. The four-step ritual (defect 2) and the dropped-edit bug (defect 3) become
unexpressible.

`Recorder` overloads accept `Optional<FieldChange>` and `List<FieldChange>` **because that is
already what the model returns** — `setField`, `putKeywords`, `setCitationKey` all hand back
`Optional<FieldChange>` today. Migration is mechanical and requires no model changes.

### Testability

Records supply `equals`, so the core invariants become real property tests:

```java
@Property
void invertingTwiceIsIdentity(@ForAll BibChange change) {
    assertEquals(change, change.inverted().inverted());
}

@Property
void applyThenUndoRestoresState(@ForAll BibChange change) {
    String before = snapshot(context);
    change.applyTo(context);
    change.inverted().applyTo(context);
    assertEquals(before, snapshot(context));
}
```

Neither test is writable against today's `UndoableFieldChange`.

## Scope of undoability

### The command is not the undo unit — its `ChangeSet` is

The command object is not retained, and redo does **not** re-run `execute()`. Redo re-applies
the recorded values.

This is deliberate and is a real advantage over a Command-pattern `unexecute()`. Redoing
"generate citation key" replays the keys that were actually produced, not whatever the current
key pattern would produce now. Re-execution would be nondeterministic against changed
preferences, changed fetcher results, or a changed library; value replay is not.

### Three classes of command effect

| Effect | Journaled | Undoable |
| --- | --- | --- |
| Model mutations (fields, entries, strings, preamble, groups) | yes | yes |
| External side effects (filesystem, network, clipboard) | no | **no** |
| Pure UI (open dialog, focus, selection) | no | correctly not |

The middle row is a **pre-existing gap**, not one this plan introduces. Commands that touch
disk include `DeleteFileAction`, `DownloadLinkedFileAction`, `LinkedFileViewModel`, and the
move/rename cleanups. Undo restores the `file` field; it does not restore the file. Undoing a
rename therefore leaves the field pointing at a path that no longer exists.

Two options, and the project should state which one is the contract:

1. **Declare the boundary (recommended).** Undo covers the bibliography, not the filesystem.
   Commands with external effects warn the user or are excluded from the journal. Simple,
   defensible, and matches what most editors do.
2. **Compensating changes.** Add a `FileMoved(from, to)` member to the sealed hierarchy whose
   `inverted()` moves the file back. Honest, but it must handle the file having changed or
   vanished in the meantime, and it makes undo *fallible* in a way pure model changes are not.

Choose (1) now. The sealed interface makes adding (2) later a compile-checked exercise rather
than a redesign.

### Batch changes are one undo step

A 500-entry batch is a single stack entry, not 500:

```java
undoScope.record(Localization.lang("Set publisher"), recorder -> {
    for (BibEntry entry : entries) {
        recorder.record(entry.setField(StandardField.PUBLISHER, value));
    }
});
```

One scope → one `ChangeSet` holding 500 `FieldEdit`s → one push. Ctrl+Z reverts all 500 in
reverse order. Same guarantee `NamedCompoundEdit` gives today, minus its failure modes: no
`end()` to forget, no `hasEdits()` to check, and no way to build the set and then drop it
(defect 3).

Three consequences to settle:

**Nesting.** `ChangeSet` is itself a `BibChange`, so a scope inside a scope nests naturally.
Recommendation: nested scopes **flatten** into the parent, and only the outermost `record()`
pushes — otherwise a command delegating to a helper command produces two stack entries for one
user gesture. One flag on `UndoScope`.

**Notification storm.** Undoing 500 edits fires 500 model mutations and 500 FX listener
notifications. Already true today. The improvement is structural: `ChangeSet.applyTo` is a
*single* choke point, so batching or listener suppression can later be added in one method.
That is impossible today, where 13 classes each own their `undo()`.

**Atomicity.** If edit 300 of 500 throws mid-undo, the library is left half-undone. Also
already true today — but today the policy differs per class (`catch IllegalArgumentException`
at info level versus `catch Throwable` at warn). `ChangeSet.applyTo` is the one place a single
policy gets defined. This overlaps defect (7) and is listed under open decisions.

## PR 0 — fix the dropped keyword edit (first commit on this branch)

Independent of everything below. Fixes defect (3) against the code as it stands today, using
today's classes.

`ManageKeywordsViewModel.java:108` builds a `NamedCompoundEdit` and then discards it:

```java
NamedCompoundEdit compoundEdit = updateKeywords(entries, keywordsToAdd, keywordsToRemove);
// TODO: bp.getUndoManager().addEdit(compoundEdit);
```

Push it — guarded by `hasEdits()` like every other call site — and mark the library changed.
Roughly three lines plus a regression test asserting the stack grows by one and that undo
restores the prior keywords.

This is the **only** deliberate behaviour change in the whole plan: keyword management becomes
undoable. It is a bug fix, not a refactor, so it must not be folded into A2. Ships first so
the fix is not held hostage to the migration, and so A2 inherits a codebase where every
`addEdit` site is already correct.

Note: the view model needs access to the undo manager and the library tab; check whether they
are reachable from its current constructor or whether wiring must be added. If wiring is
non-trivial, that is still PR 0's problem, not A2's.

## Workstream A — change model and journal

### Phase A1 — new code only, zero existing files modified

- Add `org.jabref.model.change` in jablib: `BibChange` and the eight permitted records.
- Add `UndoRedoManager`, `UndoScope`, `Recorder` in jabgui.
- Add the property tests above.
- Add one bridge class so old and new coexist on the existing stack:

```java
public final class ChangeSetEdit extends AbstractUndoableJabRefEdit {
    private final ChangeSet changeSet;
    private final BibDatabaseContext context;

    @Override public void undo() { super.undo(); changeSet.inverted().applyTo(context); }
    @Override public void redo() { super.redo(); changeSet.applyTo(context); }
    @Override public String getPresentationName() { return changeSet.name(); }
}
```

New-style changes now reach `CountingUndoManager`; old and new interleave correctly on one
stack. Roughly 12 new files, **no existing file touched**, so the phase is reviewable in
isolation and carries no regression risk.

### Phase A2 — migrate commands, one per PR

Every step here is behaviour-preserving (see "Governing constraint"). Order, simplest first:

1. `UndoableKeyChange` → `FieldEdit` with `InternalField.KEY_FIELD`; delete the class.
2. `UndoableChangeType` → `EntryTypeEdit`; removes the `EntryTypeFactory.parse` string
   round-trip.
3. `UndoableFieldChange` call sites (the bulk). Includes `AbstractEditorViewModel:74` — which
   **keeps pushing one change per keystroke**, identical to today. Coalescing is postponed.
4. `UndoableInsertEntries` / `UndoableRemoveEntries`.
5. `UndoableInsertString` / `UndoableRemoveString` / `UndoableStringChange` /
   `UndoablePreambleChange`.
6. The group edits — done, but not as expected. `UndoableAddOrRemoveGroup` and
   `UndoableMoveGroup` turned out to have **no live callers** and were deleted rather than
   modelled; see **P10** for what that means. `UndoableChangeEntriesOfGroup` needed nothing:
   it builds a compound of field changes and was carried along by step 3.
   `UndoableModifySubtree` remains — see below.

Each `Undoable*` class is deleted when its last caller migrates.

**Carried into A3:** `UndoableModifySubtree` is the last `AbstractUndoableJabRefEdit` user,
with one live call site (`GroupChange:35`). It resists the value model because it is a
*stateful* memento rather than a value: `m_modifiedSubtree` is populated during `undo()` and
read by `redo()`, so a redo that has not been preceded by an undo would clear the subtree.
Converting it means capturing both the before- and after-state at construction time, which
restructures `GroupChange.applyChange` — the first change in this migration with real
behavioural risk, in collab code that is awkward to test. It is deliberately deferred to A3,
where the Swing removal forces the question anyway.

`FieldRowViewModel` also extends `AbstractUndoableEdit` directly, via an inner
`MergeFieldsUndo` class, and needs its own look at A3.

### The seam today

Three layers, with Swing confined to the middle one:

- **Values** — `org.jabref.model.change` in jablib. Nine records behind a sealed `BibChange`.
  No Swing, no JavaFX, no `Localization`. `apply()` plus `inverted()`; undo is
  `inverted().apply()`.
- **Adapters** — `org.jabref.gui.undo`, and this *is* the seam. Exactly two classes extend
  `AbstractUndoableJabRefEdit` (which extends `javax.swing.undo.AbstractUndoableEdit`):
  `BibChangeEdit`, wrapping one change, and `NamedCompoundEdit`, holding a `List<BibChange>`
  that it folds into a `ChangeSet` on undo/redo. Nothing else extends it.
- **Swing** — `CountingUndoManager extends javax.swing.undo.UndoManager`, plus `UndoAction`
  and `RedoAction`.

`UndoScope` sits above the seam: `push` wraps a change in `BibChangeEdit` and hands it to the
manager.

**Why the change model stays in jablib.** Moving it into `org.jabref.gui.undo` would save one
import line in ~40 files and nothing else — the cost of this work is constructor threading,
not imports. It would also give up the placement that lets jabkit and jabsrv reuse the model
(defect 6, P8) and would put pure model values back inside a GUI package. The seam is in the
right place; it was the *handle* that was threaded badly.

### Phase A3 — remove Swing

**Superseded approach.** A3 originally assumed commands would be migrated to `UndoScope`
first, and an attempt at that was reverted. `UndoScope` is a *second* handle threaded
alongside `UndoManager`, which is already passed down roughly six constructor layers and
appears in ~160 references. Adding a parallel type doubles the plumbing: a mechanical sweep
converted 95 files and still left 47 errors in three distinct shapes — converted callers
handing `UndoScope` to classes that legitimately still need the manager (`UndoAction`,
`RedoAction`, anything calling `canUndo`/`hasChanged`), unconverted callers handing a manager
to converted classes, and rewrite-rule leftovers. Nothing of it was kept.

**Current approach: fold the recording API into the manager.** `UndoRedoManager` gains
`push(BibChange)` and `record(name, recorder -> ...)` as methods, and `UndoScope` disappears
into it. There is then one handle, threaded exactly where `UndoManager` already goes, and the
type swap that removes Swing is the same edit that introduces the recording API — one
mechanical rename rather than two.

Steps, in order:

1. Add `push`/`record` to a new `UndoRedoManager` (JavaFX properties, `Deque<BibChange>`, no
   Swing), keeping `CountingUndoManager` in place.
2. Replace the declared type at its ~160 references, `javax.swing.undo.UndoManager` →
   `UndoRedoManager`. Mechanical, one type, compiler-verified.
3. Rebind `UndoAction` and `RedoAction` to the new properties. `CountingUndoManager` already
   maintains `undoableProperty`/`redoableProperty`, so this is a swap, not new behaviour.
4. Delete `AbstractUndoableJabRefEdit`, `BibChangeEdit`, `NamedCompoundEdit` and `UndoScope`.
   The first three are the whole seam; the fourth is absorbed by step 1.
5. Drop `java.desktop` from jabgui's module graph.

Only after that does adopting `record(...)` inside individual commands become worthwhile — see
**P11**, which is where the "cannot forget to push" benefit actually lands.

The presentation-name mechanism (defect 11) is already gone: every `getPresentationName()`
implementation was deleted with its `Undoable*` class during A2, and the 16 l10n keys they
used were retired as each step made them unreferenced. `LocalizationConsistencyTest` catches
these one step at a time, so no separate l10n sweep is needed — but Crowdin still has to see
the removals.

Workstream A ends at A3. Extending the change model to jabkit and jabsrv was formerly phase
A4 and is now **P8** under "Postponed" — a capability addition rather than a refactor. Nothing
in A1–A3 depends on it, and A1 already places the change model in jablib, so the door stays
open at no extra cost.

## Postponed — behaviour changes, each its own later PR

None of these block PR 0 or workstream A. They are listed so they are not lost, and so a
reviewer can see they were considered and deliberately deferred. All are behaviour changes,
which is precisely why they are excluded (see "Governing constraint").

Rough order of value:

### P1 — Context menu Undo/Redo routed to the global stack

Fixes defect (9). `EditorContextAction.java:38-39,81` binds to `textInputControl`'s private
stack while Ctrl+Z already uses the global one, so menu Undo pushes a new forward edit onto
history. One file, small diff, but user-visible — hence separate.

### P2 — Keystroke coalescing

Fixes defect (10). Recommendation: coalesce by *entry + field + focus session*, implemented as
a `ChangeSet` merge rule on push. Deterministic and clock-free, unlike a time-window approach.
Only meaningful after A2 step 3 has migrated `AbstractEditorViewModel`, since the merge rule
lives on `ChangeSet`.

### P3 — Bounded undo stack depth

Cap the stack at N `ChangeSet`s (not N individual changes, so one 500-entry batch costs one
slot). Bounds the memory retained by `FieldEdit`'s hard `BibEntry` references. A constant, not
a user preference.

### P4 — Warn when a command's file-system effects are not undoable

Follows from the D7 decision that undo covers the bibliography, not the filesystem.
`DeleteFileAction`, `DownloadLinkedFileAction`, `LinkedFileViewModel` and the move/rename
cleanups should say so rather than leave the user to discover a dangling `file` path.

### P5 — Surface partially applied `ChangeSet` failures

Per D9: keep best-effort application, but notify instead of only logging.

### P6 — Generalise gesture routing into `UndoRequestEvent`

Replace the hand-rolled `KeyEvent.ANY` filter at `FieldEditorFX.java:35` with a proper
bubbling event, so the "innermost responder that can handle it wins" policy is stated once in
the mechanism designed for it. An improvement, not a fix — the current filter works. Only
worth doing if P1 lands first, otherwise there is nothing for the chain to arbitrate.

### P7 — Convert `FieldChange` to a record

See "Postponed, not rejected" above. Mechanical, no design content, after A2.

### P8 — Change model beyond the GUI

Former phase A4: jabkit dry-run and change preview, jabsrv undoable REST commands. Needs its
own requirements and API review.

### P9 — Should shared-database sync changes be undoable?

Raised by D6 and deliberately left unanswered. Today, changes arriving via `DBMSSynchronizer`
mutate the local model without touching the undo stack, so a remote overwrite of a field the
user was editing cannot be undone locally. Defensible (the change is not the local user's) and
also arguably wrong (the user watched their text vanish and reached for Ctrl+Z).

Making it undoable is not simply "journal it": undoing a synced change locally would put the
local copy out of step with the shared database until the next push, which is a
synchronisation design question, not an undo one. Out of scope; recorded so the current
behaviour is understood as a decision rather than an oversight.

### P10 — Group operations are not undoable

Discovered while migrating A2 step 6, not caused by it. Adding, removing, moving or
restructuring groups leaves no undo entry, so Ctrl+Z after a group operation either does
nothing or undoes whatever the user did before it.

**Evidence.** The undo registrations are present in the source but commented out:

- `GroupTreeViewModel.java:203` — add group
- `GroupTreeViewModel.java:532` — remove subgroups
- `GroupTreeViewModel.java:559` — remove group, keep subgroups
- `GroupTreeViewModel.java:597` — remove group and subgroups
- `GroupTreeViewModel.java:632` — remove group without children
- `GroupTreeViewModel.java:421`, `:680`, `:708` — entry-assignment changes
- `GroupNodeViewModel.java:186`, `:461` — assignment on drop, group move
- `ImportHandler.java:523` — group assignment on import

Entry *assignment* to a group is the exception: `GroupTreeNodeViewModel:159-166` still
registers `UndoableChangeEntriesOfGroup`, which is a compound of field changes and works.
It is the tree structure that has no undo.

**What was deleted, and why it does not help to keep it.** `UndoableAddOrRemoveGroup` and
`UndoableMoveGroup` were removed in this workstream because nothing constructed them. They
are recoverable from git history, but reviving them as they stand would not work:

- `UndoableAddOrRemoveGroup` records the edited node as its index path *from the tree root*
  (`getIndexedPathFromRoot`) yet begins traversal at the handle passed to its constructor. Its
  only ever caller, the likewise-dead `GroupTreeNodeViewModel#addNewGroup`, passed the
  *parent* node, not the root. Correct only when the parent happens to be the root.
- Its three modes were selected by `int` constants (`ADD_NODE`,
  `REMOVE_NODE_KEEP_CHILDREN`, `REMOVE_NODE_AND_CHILDREN`), and the commented-out call at
  `GroupTreeViewModel:632` names a fourth, `REMOVE_NODE_WITHOUT_CHILDREN`, that never existed.
  The commented-out code does not compile against the class it references, so it is not a
  reliable guide to intent.

**What implementing this needs.** Group structure is the one part of the model that is not a
before/after pair, so it needs records the current sealed hierarchy does not have:

- `GroupSubtreeInserted(root, path, subtree)` / `GroupSubtreeRemoved(root, path, subtree)` —
  an inverse pair, covering add and remove-with-children.
- Removing a node while keeping its children is *not* the inverse of adding one: undo must
  re-parent the orphaned children under a restored node. It needs its own pair, and the child
  count at removal time.
- `GroupMoved(root, fromParentPath, fromIndex, toParentPath, toIndex)` — a clean inverse pair,
  the shape `UndoableMoveGroup` already had.

Address nodes by index path from the root and resolve on apply, as the deleted classes
intended: node references do not survive a subtree copy. Note that `GroupTreeNode.copySubtree`
means undo restores *equal* nodes, not the same objects, so anything holding a node reference
across an undo sees a stale one. That is pre-existing and is worth checking before building on
it.

**Related.** `UndoableModifySubtree` (A2 step 6, carried into A3) is the surviving piece of
this area and covers whole-subtree replacement from external-file changes. Whoever implements
P10 should look at it first, since a `GroupSubtreeReplaced` record would likely subsume it.

### P11 — Adopt `record(...)` inside commands

The point of a recording block is that a command cannot collect changes and then forget to
push them, which is the bug class PR 0 fixed by hand. That benefit only arrives when command
bodies actually use it:

```java
undoManager.record(Localization.lang("Manage keywords"), recorder -> {
    for (BibEntry entry : entries) {
        recorder.record(entry.putKeywords(keywords, separator));
    }
});
```

Blocked on A3, and deliberately so: after A3 the manager already carries `record`, so adopting
it threads nothing new. Attempting it before A3 means threading a second handle through the
whole GUI construction tree, which is what failed.

Roughly 28 commands build a compound by hand today. Each is a small, individually reviewable
rewrite, and each should keep its current grouping and granularity — the point is to make the
push unforgettable, not to change what lands on the stack.

### P12 — The undo handle is threaded through the whole GUI tree

Not an undo problem, but this work exposed it. `UndoManager` reaches leaf view models through
roughly six layers of pass-through constructor parameters and appears in ~160 references;
`FieldEditors` alone forwards it to every editor view model. Any future service that leaf
components need will pay the same tax.

A `LibraryTab`-scoped context object, or letting afterburner `@Inject` reach the leaves (the
undo handle is already registered with the injector), would collapse most of it. That is a
change to how jabgui wires dependencies and is far outside undo — recorded because this work
is what made the cost measurable.

## Workstream C — independent micro-fixes

Each is a small standalone PR, unblocked by A and the postponed items:

- `SimpleCommand.progressProperty()` returns `null` (defect 8). Return a constant
  `ReadOnlyDoubleProperty`, or reconsider whether `CommandBase` is the right base for the
  no-progress case.
- Audit `Undoable*` exception handling for a single policy in the interim, if A2 runs long.

The `FieldChange`-to-record conversion previously listed here is now **P7** under
"Postponed" — mechanical, but a large enough diff to warrant its own PR after A2.

## Open decisions

### Blocking — must be settled before phase A1

All four are internal invariants a reviewer can check by reading, and all four are resolved by
the tie-breaker: **match what the code does today.** Each records the alternative so the
question can be reopened deliberately later.

**D4 — Entry identity across insert/remove round trips.**
`EntriesInserted.inverted() == EntriesRemoved` is sound only while entry *identity* survives.

| Option | Pro | Con |
| --- | --- | --- |
| **A. Hold object references** | Identity preserved for free | Retains removed entries; a future ID-based model breaks it silently |
| B. Hold entry IDs, resolve on apply | Survives an ID migration | Resolution can fail; needs an error path |

**Decided: A** — this is what `UndoableInsertEntries` and `UndoableRemoveEntries` already do
(they hold `List<BibEntry>`). State the assumption in the javadoc of both records so a future
move to entry IDs trips over it rather than silently breaking undo. *Postponed:* B, only if
and when the model moves to IDs.

**D5 — `BibEntry.equals` is content-based**, so a record-generated `FieldEdit.equals` would
compare entries by content rather than identity.

| Option | Pro | Con |
| --- | --- | --- |
| **A. Compare the entry field by identity** | Matches how edits are used today | Must hand-write `equals`, losing part of the "free" record benefit |
| B. Accept content equality | Nothing to write | Property tests can pass while conflating two same-content entries |

**Decided: A**, narrowly — only the entry component uses `==`; everything else stays generated.
Today's `Undoable*` classes declare no `equals` at all, so they inherit identity semantics, and
their `undo()`/`redo()` operate on the held reference. A is therefore the option that preserves
current semantics; B would introduce a new notion of equality that nothing in the codebase
currently has. No observable behaviour either way — this only affects the property tests.

**D6 — Which changes are journaled?** The earlier framing ("shared-database changes must never
enter the stack") was too broad. The code already draws a finer and deliberate line:

| Source | Today | Keep? |
| --- | --- | --- |
| External-file changes the user **accepts** in the change-resolution dialog (`gui/collab/**`, e.g. `EntryChange.java:39-47`) | journaled, undoable | **yes** — the user chose to apply them |
| Shared SQL database sync (`DBMSSynchronizer.java:184-225`, `EntriesEventSource.SHARED`) | not journaled | **yes** — not user-initiated |

**Decided: preserve both.** The distinction is *user-initiated versus pushed-at-you*, not
local-versus-remote. Explicit `record()` scopes give this for free: collab handlers open a
scope, `DBMSSynchronizer` does not. Add a test asserting a synthetic `SHARED` mutation leaves
the stack untouched, and another asserting an accepted collab change adds exactly one entry.
*Postponed:* see P9 — whether a remote overwrite arriving via DBMS sync *should* be undoable is
a genuine product question, deliberately not answered here.

**D8 — Nested scopes: flatten or nest?** Twice reframed. Nesting already happens
(`EntryChange.applyChange` nests a raw `CompoundEdit` inside the passed `NamedCompoundEdit`),
but the argument for *preserving* nesting was that flattening would lose the inner names shown
to users — and defect 11 establishes that **no names are shown to anyone**. That constraint is
gone.

| Option | Pro | Con |
| --- | --- | --- |
| **A. Nest structurally, flatten at push** | Matches today's object graph; costs nothing, since `ChangeSet` is a member of the sealed type | Two concepts where one might do |
| B. Flatten fully on `record()` | Simplest model; a `ChangeSet` is always one level deep | Discards structure nothing currently reads — but a future history UI might |

**Decided: A**, on the tie-breaker rather than on merit. Both options are observably identical
(one stack entry per user gesture either way), so neither is a behaviour change and the
question is pure taste. A is chosen because nesting is free — `ChangeSet` containing a
`ChangeSet` requires no code at all — and because it keeps structure that a future undo-history
UI could render, whereas B destroys it irreversibly at record time.

*Note:* if A2 finishes and nothing has ever consumed the nesting, flattening becomes a trivial
follow-up simplification. The reverse is not true. That asymmetry, not a strong preference, is
the argument.

### Deferred — decided when the corresponding postponed PR is written

Each of these keeps **current behaviour** for now, per the tie-breaker. The analysis below is
recorded so it is not repeated when the corresponding postponed PR is written; the leading
recommendation is a starting position for that discussion, not a decision taken here.

- **Undo granularity for typing** (→ P2). Options: coalesce by field + focus session
  (recommended, deterministic, clock-free), coalesce by time window (feels native, but
  timer state and flaky tests), or keep per keystroke (current, floods the stack).
- **Which stack owns the field-editor gesture** (→ P1). Largely already answered in code: the
  keyboard path routes to the global stack. Only the context menu is inconsistent.
- **Undo stack depth** (→ P3). Bounded caps memory but silently loses deep history;
  unbounded never loses history but retains everything.
- **External side-effect boundary** (→ P4). Declare undo covers the bibliography only
  (recommended — simple, matches most editors, leaves a dangling `file` path as a visible
  wart), or add compensating `FileMoved` changes (truly reversible, but makes undo fallible).
- **Partial-`ChangeSet` failure policy** (→ P5). Best-effort and log (matches today, can leave
  the library half-undone), abort and roll forward (never half-applied, but roll-forward can
  itself fail), or best-effort plus a user notification (recommended).

## Risks

- **A2 is long.** ~28 undo-aware commands and 113 `addEdit` sites. It is incremental and each
  step is independently shippable, but it will not finish in one pass. The A1 bridge means an
  abandoned migration leaves the codebase working, merely mixed.
- **Group edits (A2 step 6) are the hardest.** `UndoableModifySubtree` and
  `UndoableChangeEntriesOfGroup` mutate tree structure; the record representation for those is
  not yet designed and may need a different shape.
- **A3 touches translated strings.** Coordinate with Crowdin.
- **"Behaviour-preserving" needs proving, not asserting.** A2 has no characterization tests to
  lean on. Before migrating a command, capture its current undo behaviour in a test, then
  migrate — otherwise the governing constraint is an intention rather than a check.
- **Accelerator-versus-focus-owner ordering** (relevant to P1/P6) differs across JavaFX
  versions and platforms. Verify current behaviour on the real target before building on any
  assumption about it.

## Teaching notes

JabRef is used to demonstrate software engineering, so the following are the intended lessons
and should survive review:

- Functional core, imperative shell: changes are values in jablib; the stack and FX bindings
  are the shell in jabgui.
- Behavior variation via a sealed interface and records, not an inheritance hierarchy.
- Composite (`ChangeSet`) expressed as a member of the same sealed type, not a parallel class.
- Invertibility as a *derived* property with an executable invariant, rather than two
  hand-written methods that can drift apart.
- Command (invocation) and Memento (journal) deliberately kept distinct, with one named seam.
- Chain of Responsibility via JavaFX event bubbling — using a framework mechanism where it
  fits, and refusing it where it does not.
