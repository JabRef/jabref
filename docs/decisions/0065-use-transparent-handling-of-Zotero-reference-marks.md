---
nav_order: 65
parent: Decision Records
---

# Use Transparent Handling of Zotero Reference Marks

## Context and Problem Statement

JabRef is implementing compatibility with [Zotero reference marks](https://github.com/zotero/zotero) in the [LibreOffice integration](https://docs.jabref.org/cite/openofficeintegration). Apart from JabRef's own reference mark, JabRef should also support Zotero's reference mark. Zotero reference marks are supported as an additional input format so that JabRef can read citations created by Zotero. For documents connected to JabRef, JabRef continues to write its own reference mark format.

Do we need a preference for the compatibility mode?

## Decision Drivers

* Users should be disturbed as little as possible
* Users should be warned about possible problems
* Users should be able to use this feature with as few steps as possible

## Considered Options

* Use a preference for the compatibility mode
* Use transparent handling of Zotero Reference Marks

## Decision Outcome

Chosen option: "Use transparent handling of Zotero Reference Marks", because JabRef already uses [citation-js's mapping logic](https://github.com/JabRef/jabref/blob/07e475dcf8e2f00447667ebd1814a09176293948/jablib/src/main/java/org/jabref/logic/openoffice/CSLItemTypeDefinitions.java#L23-L84) as the fallback strategy. Adding a preference is unnecessary. It also brings some additional benefits, such as not disturbing users when connecting to a document.

### Consequences

* Good, because JabRef reads and maps Zotero's reference mark automatically, which does not disturb users.
* Good, because user does not need to enable anything to use this feature.
* Good, because it avoids the risk Zotero's reference mark being ignored if user forgets to enable the compatibility mode.
* Bad, because user may not be aware of the problems it may bring (information loss), unless they read the release page or user documentation.

## Pros and Cons of the Options

### Use a preference for the compatibility mode

If a document contains Zotero's reference mark, JabRef shows a popup asking users to choose the compatibility mode. If users choose "Zotero compact mode", Zotero's reference mark will be recognized. Otherwise, JabRef will skip it.

* Good, because it gives user the freedom to enable/disable compatibility mode.
* Bad, because it may make user confuse about which mode to choose.
* Bad, because a popup may disturb users workflow.
* Bad, because JabRef uses [citation-js's mapping logic](https://github.com/JabRef/jabref/blob/07e475dcf8e2f00447667ebd1814a09176293948/jablib/src/main/java/org/jabref/logic/openoffice/CSLItemTypeDefinitions.java#L23-L84) in its mapper, using a preference is unnecessary.

### Use transparent Handling of Zotero Reference Marks

Since JabRef uses [citation-js's mapping logic](https://github.com/JabRef/jabref/blob/07e475dcf8e2f00447667ebd1814a09176293948/jablib/src/main/java/org/jabref/logic/openoffice/CSLItemTypeDefinitions.java#L23-L84) (see ADR-64) as fallback strategy, users do not need to manually enable a preference to recognize Zotero's reference mark. JabRef will map the citation to BibLaTeX format when it reads Zotero's reference mark. Possible problems will be documented in release page and user documentation. For example, CSL item type `map` is mapped to `Misc`, and the CSL field `Scale` is lost during the conversion. JabRef maps CSL item types and fields into BibLaTeX ones, but the field mapping for each CSL type may be incomplete.

* Good, because JabRef reads and maps Zotero's reference mark automatically, which does not disturb users.
* Good, because user does not need to enable anything to use this feature.
* Good, because it avoids the risk Zotero's reference mark being ignored if user forgets to enable the compatibility mode.
* Bad, because user may not be aware of the problems it may bring, unless they read the release page or user documentation.
