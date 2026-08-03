---
nav_order: 69
parent: Decision Records
---

# Collect preferences search targets by builder registration, scanning only escape hatches

## Context and Problem Statement

The search box of the preferences dialog filters the tab list to the tabs matching the query and highlights the matching controls inside the selected tab. To do that, `PreferencesSearchHandler` needs, per tab, a set of *(searchable text, node to highlight)* pairs.

There are two ways to obtain those pairs. The dialog can walk each tab's scene graph and read the text off whatever it finds, or each tab can declare its searchable texts as it builds itself. Since the preference tabs are assembled by `PreferencesFormBuilder`, the builder is in a position to declare them: it knows the caption it just placed and the control that caption belongs to.

Declaring them has one structural gap, and the builder has two escape hatches through which it opens. `custom(Node)` takes a node the caller assembled, and `attach(Node)` takes a node the caller appends right after a control it placed. In both cases the builder does not know what text that node contains and can declare nothing for it. Text inside a hand-built row, a reused FXML panel, or a caption appended between two fields is then absent from the search — with no compile error, no failing test, and a dialog that looks correct. How should the dialog determine what its search can find?

## Decision Drivers

* A gap in the search is invisible. The failure mode is a user who cannot find a setting.
* `custom(...)` and `attach(...)` are permanent parts of the builder API, so any mechanism relying on callers remembering to opt in decays as tabs are added.
* Every text should have exactly one spelling. A search registration repeating a `Localization.lang(...)` key already used in the layout drifts once one of the two is edited.
* The mechanism must yield the node to highlight, not only the text.
* Match quality: matching every `TextField` content and every `ComboBox` item produces hits that surprise users (typing "English" matches the language combo; a path fragment matches whichever directory field happens to hold it).
* The preference tabs are builder-based; the scene graph reached through the escape hatches is mostly hand-built rows, reusable FXML panels, and single captions attached to a field.

## Considered Options

* Registration only — the builder registers what it places, the escape hatches contribute nothing
* Scene-graph scan only — the handler walks each tab's content, the builder registers nothing
* Registration plus an explicit opt-in per escape-hatch call
* Registration plus an automatic scan of the subtree passed to an escape hatch

## Decision Outcome

Chosen option: "Registration plus an automatic scan of the subtree passed to an escape hatch", because it is the only option that leaves no gap behind a `custom(...)` or `attach(...)` call without requiring anything of the caller, while keeping the precise text-to-node captioning and the narrowed match set that registration provides.

Concretely:

* Every builder method placing captioned content (`label`, `check`, `button`, `link`, `radio`, `field`, `caption`, `section`) registers the text together with the node it captions. `PreferencesTab#getSearchableElements()` exposes the result; `PreferencesSearchHandler` consumes it and does no traversal of its own.
* `custom(Node)` walks the node it is handed and registers the text of every `Labeled` it finds, stopping the descent at a `Control` so that a control's own skin is not scanned. This uses public JavaFX API (`Parent#getChildrenUnmodifiable`, `Labeled#getText`); no reflection is involved.
* `attach(Node)` is scanned the same way and for the same reason: an attachment the caller brought is as opaque to the builder as a custom node, only one level further down. The attachments the builder makes itself — a help button, a browse button, an inline value field — carry no text of their own, so the scan finds nothing to register for them.
* `ElementBase#searchable(String...)` is the explicit fallback for text held by no `Labeled`, and for a node populated only after it was handed to an escape hatch.
* `ComboBox` items and `TextField` contents are not matched. This is a deliberate narrowing.
* `buttonRow(...)` registers nothing. Its buttons carry list actions — "Add", "Modify", "Remove" and the like — whose specific variants ("Add new file type", "Add protected terms file") are already covered by the tab's own keywords, while the generic ones would highlight in half the dialog.

### Consequences

* Good, because a tab is searchable by construction — via registration for builder-placed content, via the scan for whatever it hands to an escape hatch.
* Good, because no visible text is spelled twice: the scan reads back the string the layout already set.
* Good, because traversal is confined to the escape hatches, so the ordinary path stays declarative and the handler stays free of scene-graph knowledge.
* Bad, because the scan requires the node to be fully populated when it is handed over; a node filled later is silently missed and has to use `searchable(String...)`.
* Bad, because two mechanisms coexist, and a reader has to know which applies where.
* Neutral, because table column headers stay unsearchable: `TableColumn` is not a scene-graph child, so no traversal reaches it.

### Confirmation

A test over all registered preference tabs: instantiate each tab, walk `getContent()` for `Labeled` nodes with non-blank text, and assert every such text is covered by that tab's `getSearchableElements()`, with an explicit, named opt-out list for decoration that is deliberately not searchable. This turns an unregistered escape-hatch node into a failing test rather than a silent hole, and is what makes the decision self-enforcing rather than a convention.

## Pros and Cons of the Options

### Registration only

The builder registers what it places; a caller handing a node to `custom(...)` or `attach(...)` has no way to make it searchable.

* Good, because the handler has a single, declarative source and no scene-graph knowledge.
* Good, because the text-to-node mapping is exact: the builder knows which control a caption belongs to.
* Bad, because everything behind an escape hatch is unfindable, with no API to change that.
* Bad, because each further escape-hatch call widens the gap, invisibly.

### Scene-graph scan only

`PreferencesSearchHandler` recursively walks each tab's content and matches `Labeled#getText`, the `toString` of every `ComboBox` item, and the content of every `TextField`.

* Good, because it cannot have holes: whatever is on screen and carries text is findable, regardless of how the tab was built.
* Good, because it needs no builder API and covers tabs that are not builder-based.
* Bad, because the scan cannot tell a caption from its control, so highlighting is coarser than registration achieves.
* Bad, because item and content matching produce hits users do not expect.
* Bad, because it places layout traversal in the handler, which the builder makes unnecessary for the ordinary case.
* Neutral, because it needs no reflection — the objection is about responsibility placement and match quality, not about the mechanism being unsafe.

### Registration plus explicit opt-in per escape-hatch call

`ElementBase#searchable(String...)`, or a `searchableLabels()` the caller invokes on the element handle.

* Good, because the call site states what the tab claims to be searchable.
* Good, because it also covers text held by no `Labeled`.
* Bad, because the default for a new `custom(...)` or `attach(...)` remains "invisible to search", so the structural problem is unsolved.
* Bad, because `searchable(String...)` repeats the `Localization.lang(...)` calls of the layout code, and the two spellings drift.

### Registration plus automatic scan of the escape-hatch subtree

See the decision outcome. `searchable(String...)` is retained from the previous option as the explicit fallback.

* Good, because the default for a new `custom(...)` or `attach(...)` is "searchable".
* Good, because no string is spelled twice.
* Good, because the ordinary builder path and the handler are unaffected.
* Bad, because it depends on the subtree being populated at call time.
* Bad, because two mechanisms coexist.
