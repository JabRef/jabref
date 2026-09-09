---
nav_order: 73
parent: Decision Records
---

# Node ids identify controls, style classes style them

## Context and Problem Statement

The walkthrough highlights individual controls, so each step needs a handle on one node. It used to match on visible text, node type and even `getClass().getName().contains(...)`. Text-based matching depends on the UI language, and type-based matching picks the first instance it walks into, so steps broke whenever a view was rearranged.

JavaFX offers two handles: the node `id` (`#name`) and style classes (`.name`). Stylesheets used both, and a handful of ids existed purely so `jabref-base.css` could reach a control. An id therefore meant nothing in particular — renaming one for styling reasons could silently break a walkthrough step, and vice versa.

This split was settled inside two bug-fix pull requests without a decision record. This ADR states it and the implications that came with it.

## Decision Drivers

* A walkthrough step must find its control without depending on the UI language or on layout accidents.
* Styling and identification must be changeable independently of each other.
* JabRef ships custom themes and lets users load their own stylesheets.

## Considered Options

* Node ids identify, style classes style
* Keep both uses of ids, resolve conflicts case by case
* Identify by a marker style class per target, leave ids to styling

## Decision Outcome

Chosen option: "Node ids identify, style classes style".

A node `id` names one control so that code can find it; `NodeResolver.fxId(...)` is how a walkthrough step does that. Stylesheets select style classes. Every id a walkthrough resolves is a constant in `WalkthroughNodeIds`, set from there where the node is built, and `WalkthroughNodeIdsTest` fails when a constant no longer names a node or when a stylesheet selects one of them.

Steps whose target is a virtualized cell — a row of the entry table, of the groups tree, of the preferences tab list — keep matching on text. Those cells are created and discarded while scrolling, so there is no node to name.

### Consequences

* Good, because renaming an id cannot change how the application looks, and restyling cannot break a walkthrough.
* Good, because an id is unique per node, whereas a style class is deliberately shared, so lookups are unambiguous.
* Bad, because id selectors outrank class selectors in CSS. Rules that used to win by carrying an id now win only by being longer or later in the file. The conversion was checked rule by rule, but future styling can no longer reach for an id as a trump card.
* Bad, because stylesheets outside this repository break silently. A custom theme selecting `#entryEditor` simply stops applying — no error, no log. Theme authors have to be told, and `themes.jabref.org` needs the same treatment.
* Bad, because JavaFX does not enforce that an id is unique. An id set in a shared factory lands on every node that factory builds, and `lookup("#x")` then returns whichever comes first in the scene. An id used as a handle has to be set at the one node it names, not in a helper.
* Bad, because ids JavaFX derives from an `fx:id` must be Java identifiers, so kebab case cannot be used everywhere and two spellings coexist.
* Neutral, because tests that look up controls by id now share a namespace with the walkthrough: a walkthrough rename is a test change too.
* Neutral, because JavaFX styles its own dialogs by id (the custom-color dialog); the rule binds our nodes, not the platform's.

## Pros and Cons of the Options

### Keep both uses of ids, resolve conflicts case by case

* Good, because nothing has to be converted and CSS specificity stays as it is.
* Bad, because an id has no defined meaning, so every rename needs both a styling and a walkthrough review.

### Identify by a marker style class per target, leave ids to styling

* Good, because it needs no CSS change at all, and JavaFX `lookup` accepts style classes.
* Bad, because style classes are not unique, so a target would have to be found by convention rather than by identity.
* Bad, because a marker class sits in the same namespace as styling classes and can be removed by a restyle — the very coupling this decision removes.
