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
* Preserve native modifier metadata while retaining existing rendering behavior.

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
Per-citation page information is stored under `JabRef_page_info:<uid>:<storageIndex>` so presentation sorting cannot change its owner.

`JStyleRenderer`, `CslRenderer`, and `BstRenderer` produce final formatted markers and requested bibliography paragraphs before mutation.
`CitationMarkerWriter` rewrites within canonical marks rather than renaming them when numbers change, retaining invisible boundaries for short/invisible citations.
`DocumentCitations` is an action-local mutable/live facade; it is not a document subtype or immutable snapshot.
`CitationGroupEditor` prepares merge/separate identities and replacements, then the engine renders that proposed state before applying it in one write session without a second render.
Ordering precedes controller locking and retains the [documented visual-ordering limitations](../code-howtos/openoffice/order-of-appearance.md).

`CitationIds.newId()` generates ten-character CUID2 canonical uids using lowercase letters and digits, with a letter as the first character.
The uid appears directly after the citation type in the mark name.
Stored uids are opaque and can have other lengths; reading, refreshing, and changing styles do not regenerate them.
The connection-local formatted-output baseline is bounded and is not persisted as document properties.

### Conversion on the next CSL citation insertion

The compatibility preference described in [ADR-0068](0068-use-a-preference-for-Zotero-compatibility-mode.md) selects canonical or native output for CSL insertions.
Changing it alone does not mutate a document.
For new native marks, JabRef generates an eight-character CUID2 `citationID` in JSON and a separate `RND` suffix with ten random characters; bibliography mark names also use a ten-character `RND` suffix.
Stored JSON `citationID` values are opaque and can have other lengths; reads do not rewrite them.
Generated decimal item-ID collision checks use bounded decimal-string validation rather than arbitrary-precision parsing of foreign identifiers.
On the next CSL citation insertion, `CitationEngine.requiresFormatConversion` rereads current mark names, including after reconnecting, to detect current canonical/native Zotero citations that do not match the selected mode.
The controller shows the warning titled "Reference mark format" with the message "Converting references to selected format" before insertion changes the document.
Opening, toggling the preference, selecting a style, refreshing, exporting cited entries, and inserting with JStyle/BST do not trigger canonical ↔ native Zotero conversion.
Conversion targeting native Zotero format leaves legacy `JR_cite*` marks unchanged, and `CID_n` is never a conversion input.
The separate one-way migration below can join conversion preparation when targeting canonical format.

`CitationFormatConverter` prepares the combined existing and pending citations, native linking, replacement identities, optional normally synchronized target-mode bibliography output, and style-property deltas.
Required custom-property types, writability, and removability are preflighted.
One render pass supplies final output before application; application does not render again.
Invalid metadata, unresolved sources, unsuitable properties, protected ranges, and rendering failures stop preparation before document writes or undo-history changes.

`ConversionMetadata` persists the citation-only sidecar under `JabRef_citation_conversion:<uid>`.
It preserves canonical uids, storage-order keys, all six stored types, every page-info slot, and supported native Zotero JSON, URI links, opaque identifiers, and harmless unknown metadata through conversion and save/reopen while the association remains valid.
Canonical `EMPTY` and canonical-only partial types use the normal visible citation fallback in Zotero mode; page information remains stored but is not rendered.
Converting back restores the canonical uid, storage-order keys, stored type, and every page-info slot from the sidecar, including invisible rendering for `EMPTY`.
Native insertion supports only normal and narrative citations; native JSON determines the type, not empty or zero-width mark text.
The canonical CSL renderer supports invisible citations independently of native insertion's supported types.
Native modifiers do not block conversion; any item with `suppress-author: true` maps its group to the JabRef in-text citation type, including mixed groups.
The sidecar retains native modifier JSON for restoration on conversion back, but the renderer does not render locators or affixes and does not implement author-only output.
Opaque native JSON may include Zotero display properties; preserving those fields does not make the sidecar a rendering or numbering cache.

The JSON `citationID` or mark suffix associates a native citation with its sidecar.
An externally edited associated mark blocks actual conversion rather than restoring stale canonical state.
That freshness check does not block ordinary native Zotero insertion or refresh, which use the current native citation without overwriting a stale sidecar.
Replacing both identity fields or deleting the sidecar loses the association; the former canonical uid, unsupported type, and page information cannot then be reconstructed from that native citation alone.
Custom properties do not travel with copied citation text.

### One-way legacy JStyle migration on insertion

The next insertion outside effective native Zotero mode detects supported legacy `JR_cite` marks and shows the same "Reference mark format" / "Converting references to selected format" warning before writes.
JStyle and BST always target canonical mode regardless of the saved CSL setting; CSL targets canonical mode when that setting is off.
Connecting, refreshing, exporting, selecting styles, and preference changes alone do not trigger migration.
Native Zotero insertion/conversion does not migrate legacy JStyle marks; unsupported `CID_n` marks remain unmanaged.

Prepare all supported legacy groups alongside existing canonical groups, any native groups being converted to canonical format, and the pending citation.
Map numeric legacy types `1` through `6` respectively to `NORMAL`, `IN_TEXT`, `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY`, preserving storage-order keys.
Assign each migrated group a fresh stable uid and transfer its page property, named by the whole legacy mark, to the last storage-order citation's `JabRef_page_info:<uid>:<storageIndex>` slot.
Replace legacy marks with canonical invisible boundaries after one render of the combined proposed set and any normally synchronized target bibliography; application does not render again.

Migration uses the conversion transaction's write path.
Its preflight checks, root undo requirement, ordinary-failure rollback, and recovery limits apply to migration.
One Undo/Redo step covers insertion, legacy mark replacement, stable identities, old and new page properties, prepared conversion properties, and any normally synchronized target bibliography.
Undo restores legacy marks and properties; redo restores the prepared canonical identities and page slots.
There is no conversion back to legacy JStyle storage.

### Bibliography synchronization

Format conversion changes citation marks only, not bibliography sections.
With synchronization on insertion disabled, all bibliography section names, text, and properties remain unchanged and no bibliography is created, regardless of existing sections.
With synchronization enabled and a style that has a bibliography, normal handling updates or creates only the target mode's managed bibliography; other-format sections remain unchanged.
There is no bibliography conversion, renaming, consolidation, cleanup, metadata sidecar, or rejection based on bibliography settings.
Pre-existing canonical and native Zotero bibliography sections may therefore coexist.

### Undo and conversion-failure recovery

Insertion, conversion, refresh, merge, and separate group Writer text and mark edits in undo contexts.
Writer's undo mechanism does not automatically restore custom document properties.
`UndoableDocumentProperties` registers custom `XUndoAction` deltas for page information and citation conversion metadata.
Conversion additionally prepares applied-style and Zotero preference deltas and applies those through the undo mechanism alongside its citation edits.
Replay uses raw property APIs to avoid registering further undo actions.
Ordinary applied-style and document-preference writes use raw property APIs and are not covered by these custom undo actions.

Conversion requires an available, unlocked Writer undo manager and cannot join an existing undo context.
`DocumentWriteSession.writeWithRollback` owns a root context and sentinel, so successful conversion/insertion is one Undo/Redo step covering citation identities, text, page information, conversion metadata, prepared conversion-style properties, and any normally synchronized target bibliography.
On ordinary application failure it closes and verifies ownership of the context before undoing only its own transaction, never an earlier user action.
Automatic failure recovery prevents partial citation replacement after ordinary application failures; grouping edits for manual Undo alone does not provide that recovery.
Starting the transaction may clear pre-existing redo history even if rollback restores document state.
A disconnect, lost ownership, or failed rollback produces a distinct recovery error rather than a universal atomicity guarantee; users may need to restore a saved copy.
Ordinary write sessions do not gain this conversion-specific rollback behavior.

### Compatibility consequences

* Legacy citations remain visible but unmanaged by canonical reading, regenerated bibliographies, and export-cited results until supported JStyle marks are migrated on insertion.
* Legacy CSL/BST `CID_n` and unsupported JStyle marks remain unmanaged; maintaining them requires a compatible older JabRef version or manual replacement.
* Selecting JStyle, CSL, or BST alone does not migrate marks; after migration, the canonical citations support ordinary style-family changes.
* Unsupported stored canonical types and page information survive family changes even when the selected renderer approximates or does not display them.
* Citation sidecar association loss limits recoverability of canonical-only state.

### Consequences

* Good, because one engine owns document actions instead of independent family adapters with divergent state and policies.
* Good, because rendering failures are reported before existing content is replaced.
* Good, because group edits preserve page-info ownership and can be undone with their structural/text changes.
* Good, because citation-format conversion occurs on CSL insertion without converting bibliography sections.
* Good, because native modifiers do not block conversion and their metadata is preserved.
* Good, because owned conversion rollback can recover ordinary application failures without undoing earlier user work.
* Bad, because legacy `CID_n` and unsupported JStyle marks remain incompatible and unmanaged.
* Bad, because metadata association can be invalidated or lost by external edits.
* Bad, because conversion needs root undo ownership, may clear prior redo history, and cannot guarantee recovery from every UNO failure.
* Bad, because whole-document ordering/rendering can still be required even when unchanged output is not rewritten.

### Confirmation

The Writer integration definitions cover golden canonical documents, cross-family save/reopen, all six types/page slots, canonical/native Zotero citation conversion, protected ranges, footnotes/tables, superscripts, tracked changes, and ordinary application-failure rollback.
Native JSON round trips cover URIs, modifiers, and unknown metadata, while modifier fixtures check successful conversion, group-level citation-type mapping, and restoration through save/reopen.
Bibliography assertions follow normal synchronization: all sections unchanged when disabled, target-mode updates only when enabled.
Storage/codec, `ConversionMetadataTest`, `DocumentWriteSessionTest`, and GUI/controller tests complement the Writer coverage with metadata, property, undo-ownership, and warning-order checks.
These descriptions are not evidence that a new test run passed.
The [OpenOffice/LibreOffice requirements](../requirements/openoffice.md) provide implementation/test traceability.

## More Information

* [Current architecture and style-selection overview](../code-howtos/openoffice/overview.md).
* [Real-Writer testing instructions and artifacts](../code-howtos/testing.md), under "LibreOffice Writer integration tests".
* [ADR-0068](0068-use-a-preference-for-Zotero-compatibility-mode.md) explains the explicit preference, next-insertion trigger, and warning; this decision defines the canonical encoding.

<!-- markdownlint-disable-file MD022 -->
