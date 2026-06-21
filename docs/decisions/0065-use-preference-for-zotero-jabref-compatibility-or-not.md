---
nav_order: 65
parent: Decision Records
---

# Do we need a preference for the compatibility mode?

## Context and Problem Statement

JabRef is implementing compatibility with Zotero's reference marks. Apart from JabRef's own reference mark, JabRef should also support Zotero's reference mark.

Do we need a preference for the compatibility mode?

## Decision Drivers

* Users should be disturbed as little as possible.
* Users should be warned about the possible problems.
* Users should be able to use this feature with as few steps as possible.

## Considered Options

* Use a preference for the compatibility mode.
* Do not use a preference.

## Decision Outcome

Chosen option: "Do not use a preference", because JabRef already uses citation-js's mapping logic as the fallback strategy. Adding a preference is unnecessary. It also brings some additional benefits, such as not disturbing users when connecting to a document.

### Consequences

* Good, because JabRef reads and converts Zotero's reference mark automatically, which does not disturb users.
* Good, because user does not need to enable anything to use this feature.
* Good, because it avoids the risk Zotero's reference mark being ignored if user forgets to enable the compatibility mode.
* Bad, because user may not be aware of the problems it may bring (information loss), unless they read the release page or user documentation.

## Pros and Cons of the Options

### Use a preference for the compatibility mode

If a document contains Zotero's reference mark, JabRef shows a popup asking users to choose the compatibility mode. If users choose "Zotero compact mode", Zotero's reference mark will be recognized. Otherwise, JabRef will skip it.

* Good, because it gives user the freedom to enable/disable compatibility mode.
* Bad, because it may make user confuse about which mode to choose.
* Bad, because a popup may disturb users workflow.
* Bad, because JabRef uses citation-js's mapping logic in its converter, using a preference is unnecessary.

### Do not use a preference

Since JabRef uses citation-js's mapping logic (see ADR-64) as fallback strategy, users do not need to manually enable a preference to recognize Zotero's reference mark. JabRef will convert the citation to BibLateX format when it reads Zotero's reference mark. Possible problems will be added in release page and user documentation.

* Good, because JabRef reads and converts Zotero's reference mark automatically, which does not disturb users.
* Good, because user does not need to enable anything to use this feature.
* Good, because it avoids the risk Zotero's reference mark being ignored if user forgets to enable the compatibility mode.
* Bad, because user may not be aware of the problems it may bring, such as information loss, unless they read the release page or user documentation.
