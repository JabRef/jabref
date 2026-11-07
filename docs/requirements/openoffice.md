---
parent: Requirements
---
# OpenOffice/LibreOffice integration

## Canonical Writer citations must remain reusable across JStyle, CSL, and BST styles
`req~openoffice.cross-family-citations~1`

With Zotero compatibility mode disabled, a document containing canonical JabRef citations can change style families through a refresh.
Citation keys, mark identities, stored citation types, and per-citation page information survive the change, including save/reopen between changes.
A style can approximate an unsupported stored citation type without replacing that type; returning to the original family restores its presentation.

Related issue: <https://github.com/JabRef/jabref/issues/14962>.

Needs: impl, utest

## The canonical Writer integration must leave legacy JabRef citations unmanaged
`req~openoffice.legacy-marks-unmanaged~1`

Legacy JStyle marks named `JR_cite*` and legacy CSL/BST marks containing `CID_n` are excluded from managed citations and export-cited results.
Their existing citation text, mark names, and page-info properties are not converted or rewritten.
There is no automatic migration to canonical marks.
Zotero's own mark format belongs to a separate CSL-only compatibility strategy, not to this legacy JabRef format policy.

Needs: impl, utest

## The next CSL citation insertion must convert managed citations to the selected compatibility format
`req~openoffice.next-citation-format-conversion~1`

When current canonical JabRef or native Zotero citations differ from the selected Zotero compatibility mode, the next CSL citation insertion shows the existing "Reference mark format" / "Converting references to selected format" warning before converting them to that mode's format.
Opening a document, toggling the preference, selecting a style, refreshing, exporting cited entries, and inserting with JStyle or BST do not trigger conversion.
Detection rereads document marks, including after reconnecting, rather than depending on preference-change history.
Legacy `JR_cite*` and `CID_n` marks remain unmanaged and unchanged, including in mixed-format documents.

The combined converted and pending citations and any target-mode bibliography requested by normal synchronization are rendered once before document mutation.
Preparation failures, including invalid metadata, unsupported native semantics, unresolved sources, protected ranges, unsuitable custom-property types or attributes, and rendering failures, leave marks, properties, text, and undo history unchanged.
Conversion requires an available, unlocked Writer undo manager and its own root undo context; nesting inside another undo operation is rejected before document writes.
Successful conversion and insertion form one Undo/Redo step covering identities, formatted text, any normally synchronized target-mode bibliography, page-info properties, citation conversion metadata, and prepared applied-style and Zotero preference properties.
An ordinary application failure is rolled back only after closing and verifying ownership of that context, without undoing earlier user actions.
Beginning the write transaction can clear pre-existing redo history even when document state is subsequently restored by rollback.
Global disconnects, lost ownership, and failed rollbacks produce a distinct recovery error; this is not a universal rollback guarantee.
Ordinary updates retain raw style-property setters outside the conversion-specific undo contract.

A persistent per-document citation sidecar preserves canonical uids, storage-order keys, all six stored citation types, every page-info slot, and supported native Zotero citation JSON, identifiers, URIs, and harmless unknown metadata across conversion and save/reopen.
Previously stored canonical partial-citation types and page information remain stored but are not rendered in Zotero mode.
Native Zotero citations with nonempty locators, labels, prefixes, suffixes, author-only items, or mixed per-item author suppression are rejected before conversion with an actionable error, not converted while silently omitting their semantics.
Uniform author suppression is supported; rejecting a locator-bearing native citation leaves its existing text and JSON unchanged.
An externally edited native mark still associated through its `citationID` or reference-mark suffix causes conversion to fail rather than restore stale canonical state.
Replacing both identity fields or deleting the sidecar loses the association; canonical-only state cannot then be reconstructed from the native citation.

Conversion changes citation marks only; it does not convert, rename, consolidate, or remove bibliography sections, create a bibliography metadata sidecar, or reject citations based on bibliography settings.
With synchronization on insertion disabled, every existing bibliography section's name, text, and properties stay unchanged, and no bibliography is created.
With synchronization enabled and a style that has a bibliography, normal handling updates or creates only the target mode's managed bibliography; other-format sections stay unchanged and may coexist with it.

Needs: impl, utest

## Native Zotero citation updates must reject unsupported modifiers before mutation
`req~openoffice.zotero-modifier-safe-update~1`

Ordinary native Zotero insertion and refresh reject existing citations with nonempty locators, labels, prefixes, suffixes, author-only items, or mixed per-item author suppression before changing any citation, bibliography, document property, or undo history.
The error explains that no citations were changed and directs the user to update the citations in Zotero.
Uniform author suppression remains supported, and export-cited remains available because it does not rewrite citations.
This modifier check does not impose conversion-sidecar freshness requirements on ordinary native updates.

Needs: impl, utest

## Successful citation insertion must warn about unresolved existing citation keys
`req~openoffice.insertion-unresolved-warning~1`

After successful insertion, the controller shows an "Unresolved citations" warning listing every unresolved key returned by the engine and advising the user to open the corresponding libraries and refresh.
This applies with bibliography synchronization enabled or disabled, since insertion always refreshes citation markers.
An empty unresolved-key list produces no warning; an insertion failure retains its error dialog rather than reporting success.

Needs: impl, utest

## Canonical citation merge or separation must be undoable in one complete step
`req~openoffice.single-step-group-undo~1`

With Zotero compatibility mode disabled and an available, unlocked Writer undo manager, each merge or separation uses one undo context for structural replacements, their final formatted output, and page-info property changes.
Undo restores the original citation-group identities and per-citation page information; redo restores the edited groups and their page information.
The proposed post-edit groups and bibliography are rendered before document mutation, so a rendering failure leaves marks, properties, text, and undo history unchanged.
This group-edit undo contract does not cover raw applied-style or Zotero preference property writes in ordinary updates; the conversion-specific contract above includes its prepared property deltas.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
