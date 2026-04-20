---
nav_order: 58
parent: Decision Records
status: accepted
date: 2026-04-20
---
# Linked file icons indicating storage directory type are tracked in a separate requirements document

## Context and Problem Statement

Issue [#12287](https://github.com/JabRef/jabref/issues/12287) proposes visual icons on linked file entries to indicate which directory type the file resides in — for example a "world + PDF" icon for the library-specific directory and a "person + PDF" icon for the user-specific directory.
Designing these icons requires creating new SVG assets and is independent of the directory-move logic.
Should the icon requirements be part of the move-feature requirements document or tracked separately?

## Considered Options

* Include icon requirements in the move-feature requirements document
* Track icon requirements in a dedicated requirements document

## Decision Outcome

Chosen option: "Track icon requirements in a dedicated requirements document", because icon design (SVG creation, color/shape conventions, accessibility) is a self-contained visual task that does not depend on the move algorithm and benefits from its own focused specification.

### Consequences

* Good, because the move-feature requirements document stays focused on directory selection behavior.
* Good, because icon work can be scheduled and reviewed independently.
* Neutral, because implementers must consult two documents to understand the full feature; cross-references between documents mitigate this.
