---
nav_order: 68
parent: Decision Records
status: partially superseded by ADR-0084
---

# Use a Preference to Handle Zotero Reference Marks

[ADR-0084](0084-canonical-writer-citations.md) replaces the legacy JabRef mark encoding described below, not the explicit Zotero compatibility preference or the next-insertion conversion policy.
The Context, Decision Outcome, and Options below preserve the historical rationale; their `CID_n` encoding is no longer supported.
Historical claims that the preference ensures a single mark format are not a current mixed-format repair guarantee.

## Current implementation

Native Zotero mode is effective only for a selected CSL style with the saved compatibility flag enabled.
JStyle and BST always use canonical mode and disable the compatibility control without clearing the saved flag; returning to CSL restores that choice.
The mode is not inferred from document marks.

In the current integration, a format mismatch triggers conversion only on the next CSL citation insertion, after the existing "Reference mark format" / "Converting references to selected format" warning.
Toggling the preference, opening a document, selecting a style, refreshing, or exporting cited entries does not trigger conversion.
This bidirectional conversion covers current canonical JabRef marks ↔ supported native Zotero marks, not legacy `CID_n` marks.
Separately, the next insertion outside effective native Zotero mode migrates supported legacy JStyle `JR_cite` marks one-way to canonical format: always for JStyle/BST, and for CSL when the saved compatibility setting is off.
It uses the same warning and conversion transaction; connecting, refreshing, exporting, and selecting a style do not trigger migration.
Insertion or conversion targeting native Zotero format leaves legacy JStyle marks unchanged.
Detection uses fresh Writer reference-mark names and only the first recognized canonical, supported legacy JStyle, or native Zotero name, skipping unrelated names and `CID_n` marks.
UNO enumeration need not follow physical document order.
Mixed managed formats are unsupported; detection neither searches for every mismatch nor explicitly rejects all mixed documents.
The shared document-action engine preserves the next-insertion trigger, warning, and native modifier compatibility.
Native citation locators, labels, prefixes, suffixes, author-only items, and mixed per-item author suppression do not by themselves block conversion, insertion, or refresh.
Any item with `suppress-author: true` selects the group's existing in-text citation type, including mixed groups.
Canonical/native conversion is lossy and intended for the final citation format, not a lossless round trip.
Canonical → native drops the original UID and page information and maps `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY` to normal visible citations.
Native → canonical creates fresh UIDs and only normal or narrative types; original native JSON, modifiers, unknown fields, item IDs, and native Zotero URI links are not retained or restored on reverse conversion.
Native output rebuilds JSON from the JabRef library, without adding locator, affix, or author-only rendering support.
Ordinary native insertion and refresh preserve existing native JSON while updating links.
Conversion changes citation marks only, not bibliographies.
With synchronization disabled, all bibliography section names, text, and properties stay unchanged; with synchronization enabled, normal updates affect only the target mode's managed bibliography, leaving other-format sections alone.
No bibliography renaming, consolidation, or rejection based on bibliography settings is part of citation conversion.
Only legacy-only canonical migration permits unresolved sources during conversion, using ordinary placeholders and the successful-insertion warning; opening missing libraries and refreshing restores resolved output.
Native-to-canonical conversion and native generation require resolved sources.
Canonical operations and conversions leave `ZOTERO_PREF` XML untouched while updating JabRef's applied style; native insertion, refresh, and conversion to native mode update native preferences.
Immediate Writer Undo/Redo and owned rollback can restore the transaction while undo history is available, subject to recovery limits; there is no restoration guarantee after reopening.
See [ADR-0084's conversion policy](0084-canonical-writer-citations.md) for the data-loss contract, root-undo requirement, rollback limits, and possible loss of pre-existing redo history.

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
