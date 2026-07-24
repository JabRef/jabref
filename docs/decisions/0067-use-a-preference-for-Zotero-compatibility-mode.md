---
nav_order: 67
parent: Decision Records
---

# Use a Preference to Handle Zotero Reference Marks

## Context and Problem Statement

JabRef is implementing compatibility with [Zotero reference marks](https://github.com/zotero/zotero-libreoffice-integration/blob/227551c1644c6cb1156226d4db5d1b293fdb271d/build/source/org/zotero/integration/ooo/comp/ReferenceMark.java#L313-L316) in the [LibreOffice integration](https://docs.jabref.org/cite/openofficeintegration). Apart from JabRef's own reference mark, JabRef should also support Zotero's reference mark. Zotero reference marks are supported as an additional input format so that JabRef can read citations created by Zotero. For documents connected to JabRef, JabRef continues to write its own reference mark format.

Do we need a preference for the compatibility mode?

## Decision Drivers

* Users should be disturbed as little as possible
* Users should be warned about possible problems
* Users should be able to use this feature with as few steps as possible

## Considered Options

* Use a preference for the compatibility mode
* Use transparent handling of Zotero Reference Marks

## Decision Outcome

Chosen option: "Use a preference for the compatibility mode", because it keeps the possibilities to customize JabRef's own reference mark by preserving the old reference mark format ("JABREF_Smith2020 CID_1 NORMAL"). Users who do not require Zotero compatibility can continue using the original JabRef reference marks, while users who need compatibility can explicitly enable the alternative mode.

### Consequences

* Good, because preserving JabRef's reference marks brings independence and open the possibilities to customize as we want. On the contrary, only relying on Zotero marks makes JabRef dependent on the Zotero's development.
* Good, because it gives user the freedom to enable/disable compatibility mode.
* Good, because it ensures the document contains only one reference mark format, either "JABREF_Smith2020 CID_1 NORMAL" or "ZOTERO_ITEM CSL_CITATION {json} RND1234abc".
* Bad, because a popup may disturb users workflow.

## Pros and Cons of the Options

### Use a preference for the compatibility mode

Add a preference that allows users to choose between JabRef-only mode and Zotero compatibility mode. Each mode uses its own reference mark format: JabRef-only mode uses JABREF_Smith2020 CID_1 NORMAL, while Zotero compatibility mode uses ZOTERO_ITEM CSL_CITATION {json} RND1234abc. If the selected mode does not match the reference marks already present in a document, JabRef warns the user and automatically converts the reference marks to the chosen format.

* Good, because having custom reference marks brings independence and opens the possibilities to customize as we want.
* Good, because it gives user the freedom to enable/disable compatibility mode.
* Good, because it ensures the document contains only one reference mark format.
* Bad, because a popup may disturb users workflow.

### Use transparent Handling of Zotero Reference Marks

Since JabRef uses [citation-js's mapping logic](https://github.com/JabRef/jabref/blob/07e475dcf8e2f00447667ebd1814a09176293948/jablib/src/main/java/org/jabref/logic/openoffice/CSLItemTypeDefinitions.java#L23-L84) (see [ADR-64](https://github.com/JabRef/jabref/blob/main/docs/decisions/0064-use-citation-js-mapping.md)) as fallback strategy, users do not need to manually enable a preference to recognize Zotero's reference mark. JabRef will map the citation to BibLaTeX format when it reads Zotero's reference mark.

* Good, because JabRef reads and maps Zotero's reference mark automatically, which does not disturb users.
* Good, because user does not need to enable anything to use this feature.
* Bad, because a document may include 2 types of reference mark (old JabRef style reference mark and new Zotero style ones).
* Bad, because all the reference marks start with "ZOTERO_ITEM". As a result, relying on Zotero marks makes JabRef dependent on the Zotero's development.
