---
nav_order: 84
parent: Decision Records
date: 2026-11-02
---
# Use canonical Writer citation marks across style families
`adr~canonical-writer-citations~1`

Needs: impl

## Context and Problem Statement

The Writer integration previously stored JStyle citations differently from CSL and BST citations.
Changing style families could leave citations out of a bibliography although their visible text looked similar, as described in <https://github.com/JabRef/jabref/issues/14962>.
Separate adapters duplicated numbering, update policy, and mark management; some encodings stored derived citation numbers in mark names.

How can citation style families share document actions without making stored citation identity depend on the selected renderer?

## Decision Drivers

* Preserve citation identity, type, keys, and page information across style-family changes.
* Keep numbering and formatted output separate from persistent citation identity.
* Validate and render proposed changes before replacing document content.
* Keep native Zotero storage separate while preserving the established next-insertion conversion trigger and warning.
* Limit legacy migration to supported JStyle marks with explicit type, key, and page-info mappings.
* Accept native citation modifiers without promising lossless format conversion.

## Considered Options

* Keep separate backends and translate their formats when changing styles.
* Use canonical JabRef marks and one document-action engine without migrating legacy marks.
* Use canonical JabRef marks with insertion-triggered migration of supported legacy JStyle marks.
* Use canonical JabRef marks with automatic migration of all legacy formats.

## Decision Outcome

Chosen option: "Use canonical JabRef marks with insertion-triggered migration of supported legacy JStyle marks", because citation identity and document-editing policy can remain independent of the renderer while preserving supported legacy citation data.
Legacy migration supports only the JStyle encoding described below, not every legacy citation format.
The framework separates storage, document actions, and rendering; native Zotero citations use a separate CSL-only strategy.

### Canonical identity and rendering

`CitationEngine` coordinates canonical actions over the shared citation model.
`JabRefMarkCodec` encodes storage-order keys, the stored citation type, and an opaque group uid:

```text
JABREF_<key>[, JABREF_<key2> ...] <TYPE> <uid>
```

For example:

```text
JABREF_Smith2000 NORMAL a1b2c3d4e5
JABREF_Smith2000, JABREF_Jones2001 IN_TEXT f6g7h8i9j0
```

The type precedes the uid, and there is no uid-before-type fallback.
The mark contains no derived citation numbers or labels.
All six types (`NORMAL`, `IN_TEXT`, `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY`) are persistent state rather than properties of one renderer.
Per-citation page information is stored under `JabRef_page_info:<uid>:<storageIndex>`; presentation sorting cannot change its owner.

`JStyleRenderer`, `CslRenderer`, and `BstRenderer` implement `CitationRenderer` and return a `RenderResult` containing final formatted markers and requested bibliography paragraphs before mutation.
`UpdatePipeline` validates JStyle bibliography paragraph styles only when bibliography output is requested; a required citation character style is still checked for citation-only output.
BST author-year output without a bibliography skips BST source loading, VM execution, and Pandoc; numeric and style-defined markers still require BST output, but not Pandoc.
See the current implementation in [ADR-0067](0067-use-pandoc-for-bst-conversion.md) for the BST pipeline.
`CitationMarkerWriter` rewrites within canonical marks rather than renaming them when numbers change, retaining invisible boundaries for short/invisible citations.
`DocumentCitations` is an action-local mutable/live facade; it is not a document subtype or immutable snapshot.
`CitationGroupEditor` prepares merge/separate identities and replacements, then the engine renders that proposed state before applying it in one write session without a second render.
Ordering precedes controller locking and retains the [documented visual-ordering limitations](../code-howtos/openoffice/order-of-appearance.md).

`CitationIds.newId()` generates ten-character CUID2 canonical uids using lowercase letters and digits, with a letter as the first character.
The uid appears directly after the citation type in the mark name.
Stored uids are opaque and can have other lengths; reading, refreshing, and changing styles do not regenerate them.
Only native Zotero citation and bibliography names use an `RND` suffix; canonical marks do not.
The connection-local formatted-output baseline is bounded and is not persisted as document properties.
Only explicit canonical refresh performs a global orphan page-info sweep; structural edits and conversion still remove page properties belonging to replaced or removed citations.

### Style selection and effective mode

Native Zotero mode is effective only when the selected style is a CSL `CitationStyle` and the saved compatibility flag is enabled.
JStyle and BST always use canonical mode; their selection disables the compatibility control without clearing its saved value.
Returning to CSL restores the saved choice, not a mode inferred from the document's marks.

`StyleSelectDialogView` eagerly initializes previews for every family's non-null selection when the dialog opens, including hidden tabs.
Later selection changes update that family's preview immediately, including CSL selections arriving through asynchronous discovery; tab activation is not a rendering trigger.
A family without a selection creates no preview viewer.
Previewing a row does not apply the style or mutate the Writer document; confirming the selection stores the style preference.

### Conversion on the next CSL citation insertion

The compatibility preference described in [ADR-0068](0068-use-a-preference-for-Zotero-compatibility-mode.md) selects canonical or native output for CSL insertions.
Changing it alone does not mutate a document.
For new native marks, JabRef generates an eight-character CUID2 `citationID` in JSON and a separate `RND` suffix followed by a ten-character CUID2 value; native bibliography section names use the same suffix scheme.
Stored JSON `citationID` values are opaque and can have other lengths; reads do not rewrite them.
Generated decimal item-ID collision checks use bounded decimal-string validation rather than arbitrary-precision parsing of foreign identifiers.
On each insertion, `CitationEngine.requiresFormatConversion` reads fresh reference-mark names from Writer, including after reconnecting.
Detection uses only the first recognized name, skipping unrelated reference marks and unsupported `CID_n` marks.
Recognized formats are canonical `JABREF_<keys> <TYPE> <uid>`, supported legacy `JR_cite` names, and native Zotero citation names.
It does not scan for later mismatching formats; no recognized name means no conversion.
Conversion is supported for homogeneous source documents; mixed-source conversion and mixed-format repair are unsupported.
UNO name enumeration is not guaranteed to be sorted or to follow physical document order; enumeration order is immaterial for homogeneous documents.
For CSL insertion, a canonical/native format mismatch with the selected compatibility mode requires conversion.
The controller shows the warning titled "Reference mark format" with the message "Converting references to selected format" before insertion changes the document.
Opening, toggling the preference, selecting a style, refreshing, exporting cited entries, and inserting with JStyle/BST do not trigger canonical ↔ native Zotero conversion.
Conversion targeting native Zotero format leaves legacy `JR_cite*` marks unchanged, and `CID_n` is never a conversion input.
The separate one-way migration below applies when the first recognized name identifies supported legacy JStyle format and the target is canonical.

Once detection triggers conversion, `CitationFormatConverter` prepares citations, native linking when needed, replacement identities, optional normally synchronized target-mode bibliography output, and style-property deltas.
Flattened entries and native item IDs are prepared only for native output; an empty native citation set skips linking.
Required custom-property types, writability, and removability are preflighted.
One render pass supplies final output before application; application does not render again.
Native-to-canonical conversion and native generation require resolved sources; only legacy-only canonical migration permits unresolved entries.
Invalid metadata, required but unresolved sources, unsuitable properties, protected ranges, and rendering failures stop preparation before document writes or undo-history changes.

Canonical/native conversion is lossy and intended for the final citation format, not a lossless round trip.
Only target-format state is retained; no persistent source-format record supports reverse conversion.
Canonical → native conversion drops the original canonical uid and all per-citation page information.
`EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY` map to normal visible citations; normal and in-text types map to their native equivalents.
Native → canonical conversion generates fresh canonical uids and only normal or narrative types.
Native JSON locators, labels, prefixes, suffixes, author-only flags, unknown fields, item IDs, and native Zotero URI links are not carried into canonical storage or restored on reverse conversion.
Native output rebuilds JSON from the JabRef library.
See the overview's [Limitations](../code-howtos/openoffice/overview.md#limitations) for the loss table.

Native insertion supports only normal and narrative citations; native JSON determines the type, not empty or zero-width mark text.
The canonical CSL renderer supports invisible citations independently of native insertion's supported types.
Native modifiers do not block conversion; any item with `suppress-author: true` maps its group to the JabRef in-text citation type, including mixed groups.
The renderer does not render locators or affixes and does not implement author-only output.
Ordinary native insertion and refresh preserve existing native JSON while updating URI links; these same-format operations do not have conversion's data-loss contract.
Custom properties do not travel with copied citation text.

### One-way legacy JStyle migration on insertion

The next insertion outside effective native Zotero mode migrates a homogeneous legacy JStyle document when fresh detection selects a supported `JR_cite` name as the first recognized name returned by Writer.
It shows the same "Reference mark format" / "Converting references to selected format" warning before writes.
JStyle and BST always target canonical mode regardless of the saved CSL setting; CSL targets canonical mode when that setting is off.
Connecting, refreshing, exporting, selecting styles, and preference changes alone do not trigger migration.
Native Zotero insertion/conversion does not migrate legacy JStyle marks; unsupported `CID_n` marks remain unmanaged.

After detection selects migration, the existing preparation path handles citation groups and the pending citation.
Mixed-source conversion is unsupported.
Legacy-only canonical migration uses ordinary placeholders for unresolved entries and reports the successful-insertion warning; opening missing libraries and refreshing restores resolved output.
`LegacyJStyleMark` recognizes `JR_cite<digits>_<type>_<comma-separated keys>` with an optional empty digit sequence and type `1` through `6`; decoding validates the keys with the canonical codec.
Map numeric legacy types `1` through `6` respectively to `NORMAL`, `IN_TEXT`, `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY`, preserving storage-order keys.
Assign each migrated group a fresh stable uid and transfer its page property, named by the whole legacy mark, to the last storage-order citation's `JabRef_page_info:<uid>:<storageIndex>` slot.
Replace legacy marks with canonical invisible boundaries after one render of the combined proposed set and any normally synchronized target bibliography; application does not render again.

Migration uses the conversion transaction's write path.
Its preflight checks, root undo requirement, ordinary-failure rollback, and recovery limits apply to migration.
One Undo/Redo step covers insertion, legacy mark replacement, stable identities, old and new page properties, prepared conversion properties, and any normally synchronized target bibliography.
Undo restores legacy marks and properties; redo restores the prepared canonical identities and page slots.
Conversion back to legacy JStyle storage is unsupported.

### Bibliography synchronization

Format conversion changes citation marks only, not bibliography sections.
With synchronization on insertion disabled, all bibliography section names, text, and properties remain unchanged and no bibliography is created, regardless of existing sections.
With synchronization enabled and a style that has a bibliography, normal handling updates or creates only the target mode's managed bibliography; other-format sections remain unchanged.
Citation conversion does not convert, rename, consolidate, or clean up bibliographies, or reject citations based on bibliography settings.
Pre-existing canonical and native Zotero bibliography sections may therefore coexist.

### Undo and conversion-failure recovery

Insertion, conversion, refresh, merge, and separate group Writer text and mark edits in undo contexts.
Writer's undo mechanism does not automatically restore custom document properties.
`UndoableDocumentProperties` registers custom `XUndoAction` deltas for page information.
Conversion additionally prepares applied-style deltas and, only when targeting native mode, Zotero preference deltas, applying them through the undo mechanism alongside citation edits.
Canonical CSL operations and conversions leave `ZOTERO_PREF` XML untouched while still updating JabRef's applied style; native-mode insertion or refresh updates native preferences.
Replay uses raw property APIs to avoid registering further undo actions.
Ordinary applied-style and document-preference writes use raw property APIs and are not covered by these custom undo actions.

Conversion requires an available, unlocked Writer undo manager and cannot join an existing undo context.
`DocumentWriteSession.writeWithRollback` owns a root context and sentinel, so successful conversion/insertion is one Undo/Redo step covering citation identities, text, page-info changes, prepared conversion-style properties, and any normally synchronized target bibliography.
On ordinary application failure it closes and verifies ownership of the context before undoing only its own transaction, never an earlier user action.
Automatic failure recovery prevents partial citation replacement after ordinary application failures; grouping edits for manual Undo alone does not provide that recovery.
Starting the transaction may clear pre-existing redo history even if rollback restores document state.
A disconnect, lost ownership, or failed rollback produces a distinct recovery error rather than a universal atomicity guarantee; users may need to restore a saved copy.
Immediate Writer Undo/Redo and owned rollback restore the transaction while undo history is available, subject to these recovery limits.
This does not make reverse format conversion lossless or guarantee restoration after reopening.
Ordinary write sessions do not gain this conversion-specific rollback behavior.

### Compatibility consequences

* Legacy citations remain visible but unmanaged by canonical reading, regenerated bibliographies, and export-cited results until supported JStyle marks are migrated on insertion.
* Legacy CSL/BST `CID_n` and unsupported JStyle marks remain unmanaged; maintaining them requires a compatible older JabRef version or manual replacement.
* Selecting JStyle, CSL, or BST alone does not migrate marks; after migration, the canonical citations support ordinary style-family changes.
* Unsupported stored canonical types and page information survive family changes even when the selected renderer approximates or does not display them.
* Canonical/native conversion discards source-format data; reverse conversion does not restore it.

### Consequences

* Good, because one engine owns document actions instead of independent family adapters with divergent state and policies.
* Good, because rendering failures are reported before existing content is replaced.
* Good, because group edits preserve page-info ownership and can be undone with their structural/text changes.
* Good, because citation-format conversion occurs on CSL insertion without converting bibliography sections.
* Good, because native modifiers do not block conversion.
* Good, because owned conversion rollback can recover ordinary application failures without undoing earlier user work.
* Bad, because legacy `CID_n` and unsupported JStyle marks remain incompatible and unmanaged.
* Bad, because canonical/native conversion loses original identities and format-specific citation data.
* Bad, because conversion needs root undo ownership, may clear prior redo history, and cannot guarantee recovery from every UNO failure.
* Bad, because whole-document ordering/rendering can still be required even when unchanged output is not rewritten.

### Confirmation

The Writer integration definitions cover golden canonical documents, cross-family save/reopen, all six types/page slots, canonical/native Zotero citation conversion, protected ranges, footnotes/tables, superscripts, tracked changes, and ordinary application-failure rollback.
Conversion acceptance must check target-format data, documented losses, modifier acceptance, group-level citation-type mapping, and immediate transaction Undo/Redo separately from same-format save/reopen preservation.
Bibliography assertions follow normal synchronization: all sections unchanged when disabled, target-mode updates only when enabled.
Storage/codec, `DocumentWriteSessionTest`, and GUI/controller tests complement the Writer coverage with property, undo-ownership, and warning-order checks.
`BstRendererTest` and `UpdatePipelineTest` cover citation-only work and validation; `OpenOfficeStyleModeTest` and `StyleSelectDialogViewTest` cover the saved CSL mode and eager previews, including hidden selections and delayed discovery.
The [OpenOffice/LibreOffice requirements](../requirements/openoffice.md) provide implementation/test traceability.

## More Information

* [Current architecture and style-selection overview](../code-howtos/openoffice/overview.md).
* [Real-Writer testing instructions and artifacts](../code-howtos/testing.md), under "LibreOffice Writer integration tests".
* [ADR-0068](0068-use-a-preference-for-Zotero-compatibility-mode.md) explains the explicit preference, next-insertion trigger, and warning; this decision defines the canonical encoding.

<!-- markdownlint-disable-file MD022 -->
