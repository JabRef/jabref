---
nav_order: 80
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
* Make legacy incompatibility explicit instead of guessing a migration.
* Avoid silently losing unsupported native citation semantics.

## Considered Options

* Keep separate backends and translate their formats when changing styles.
* Use canonical JabRef marks and one document-action engine without migrating legacy marks.
* Use canonical JabRef marks with automatic migration of legacy formats.

## Decision Outcome

Chosen option: "Use canonical JabRef marks and one document-action engine without migrating legacy marks", because citation identity and document-editing policy can remain independent of the renderer.
The shared framework separates storage, document actions, and rendering; it does not remove Zotero compatibility or its established conversion trigger.

### Canonical identity and rendering

With Zotero compatibility disabled, `CitationEngine` coordinates actions over the shared citation model.
`MarkCodec` encodes storage-order keys, the stored citation type, and an opaque group uid:

```text
JABREF_<key>[, JABREF_<key2> ...] <TYPE> <uid>
```

For example:

```text
JABREF_Smith2000 NORMAL group-17
JABREF_Smith2000, JABREF_Jones2001 IN_TEXT group-18
```

The type precedes the uid, and there is no uid-before-type fallback.
The mark contains no derived citation numbers or labels.
All six types (`NORMAL`, `IN_TEXT`, `EMPTY`, `NOPAR`, `AUTHOR_ONLY`, and `YEAR_ONLY`) are persistent state rather than properties of one renderer.
Per-citation page information is stored under `JabRef_page_info:<uid>:<storageIndex>` so presentation sorting cannot change its owner.

`JStyleRenderer`, `CslRenderer`, and `BstRenderer` produce final formatted markers and requested bibliography paragraphs before mutation.
`MarkWriter` rewrites within canonical marks rather than renaming them when numbers change, retaining invisible boundaries for short/invisible citations.
`DocumentCitations` is an action-local mutable/live facade; it is not a document subtype or immutable snapshot.
`CitationGroupEditor` prepares merge/separate identities and replacements, then the engine renders that proposed state before applying it in one write session without a second render.
Ordering precedes controller locking and retains the [documented visual-ordering limitations](../code-howtos/openoffice/order-of-appearance.md).

`CitationIds` supplies new CUID2 identities with a default length of 24 lowercase alphanumeric characters.
Zotero wire conventions retain eight characters for new `citationID` values and ten after `RND`; existing foreign identities remain opaque and unchanged.
Generated decimal item-id collision checks use bounded decimal-string validation rather than arbitrary-precision parsing of foreign identifiers.
The connection-local formatted-output baseline is bounded and is not persisted as document properties.

### Conversion on the next CSL citation insertion

The explicit compatibility preference from [ADR-0068](0068-use-a-preference-for-Zotero-compatibility-mode.md) remains.
Changing it alone does not mutate a document.
On the next CSL citation insertion, `CitationEngine.requiresFormatConversion` rereads current mark names, including after reconnecting, to detect current canonical/native Zotero citations that do not match the selected mode.
The controller shows the existing warning titled "Reference mark format" with the message "Converting references to selected format" before insertion changes the document.
Opening, toggling the preference, selecting a style, refreshing, exporting cited entries, and inserting with JStyle/BST do not trigger conversion.
Legacy `JR_cite*` and `CID_n` marks are not conversion inputs.

`CitationFormatConverter` prepares the combined existing and pending citations, native linking, replacement identities, optional normally synchronized target-mode bibliography output, and style-property deltas.
Required custom-property types, writability, and removability are preflighted.
One render pass supplies final output before application; application does not render again.
Invalid metadata, unsupported native citation modifiers, unresolved sources, unsuitable properties, protected ranges, and rendering failures stop preparation before document writes or undo-history changes.

`ConversionMetadata` persists the citation-only sidecar under `JabRef_citation_conversion:<uid>`.
It preserves canonical uids, storage-order keys, all six stored types, every page-info slot, and supported native Zotero JSON, URI links, opaque identifiers, and harmless unknown metadata through conversion and save/reopen while the association remains valid.
Canonical partial types and page information survive but are not rendered in Zotero mode.
Native nonempty locators, labels, prefixes, suffixes, author-only items, and mixed per-item `suppress-author` flags are rejected before conversion with an actionable error; uniform suppression is supported.
Rejected locator-bearing citations keep their existing text and JSON; successful locator conversion/rendering is not claimed.
Opaque native JSON may include Zotero display properties; preserving those fields does not make the sidecar a rendering or numbering cache.

The JSON `citationID` or mark suffix associates a native citation with its sidecar.
An externally edited associated mark blocks actual conversion rather than restoring stale canonical state.
That freshness check does not block ordinary native Zotero insertion or refresh, which use the current native citation without overwriting a stale sidecar.
Replacing both identity fields or deleting the sidecar loses the association; the former canonical uid, unsupported type, and page information cannot then be reconstructed from that native citation alone.
Custom properties do not travel with copied citation text.

### Bibliography synchronization remains separate

Format conversion changes citation marks only, preserving the old conversion boundary rather than adding bibliography migration.
With synchronization on insertion disabled, all bibliography section names, text, and properties remain unchanged and no bibliography is created, regardless of existing sections.
With synchronization enabled and a style that has a bibliography, normal handling updates or creates only the target mode's managed bibliography; other-format sections remain unchanged.
There is no bibliography conversion, renaming, consolidation, cleanup, metadata sidecar, or rejection based on bibliography settings.
Pre-existing canonical and native Zotero bibliography sections may therefore coexist.

### Undo and conversion-failure recovery

The older GUI already had undo contexts for insertion including conversion, refresh, merge, and separate.
Those contexts grouped Writer edits but did not restore custom document properties; the old Manage citations comment explicitly documented that limitation for page information.
`UndoableDocumentProperties` adds custom `XUndoAction` deltas for page information and citation conversion metadata.
Conversion additionally prepares applied-style and Zotero preference deltas and applies those through the undo mechanism alongside its citation edits.
Replay uses raw property APIs to avoid registering further undo actions.
Ordinary applied-style and Zotero preference writes remain raw and are not made undoable by this mechanism.

Conversion requires an available, unlocked Writer undo manager and cannot join an existing undo context.
`DocumentSession.writeWithRollback` owns a root context and sentinel, so successful conversion/insertion is one Undo/Redo step covering citation identities, text, page information, conversion metadata, prepared conversion-style properties, and any normally synchronized target bibliography.
On ordinary application failure it closes and verifies ownership of the context before undoing only its own transaction, never an earlier user action.
This automatic failure recovery is new behavior intended to prevent partially replaced citations, not the first introduction of undo grouping.
Starting the transaction may clear pre-existing redo history even if rollback restores document state.
A disconnect, lost ownership, or failed rollback produces a distinct recovery error rather than a universal atomicity guarantee; users may need to restore a saved copy.
Ordinary write sessions do not gain this conversion-specific rollback behavior.

### Compatibility consequences

* Legacy JStyle `JR_cite*` and CSL/BST `CID_n` citations remain visible but unmanaged by canonical reading, regenerated bibliographies, and export-cited results.
* There is no automatic legacy migration; maintaining those citations requires a compatible older JabRef version or manual replacement.
* Switching JStyle, CSL, and BST is supported for current canonical citations, not as a migration mechanism.
* Unsupported stored canonical types and page information survive family changes even when the selected renderer approximates or does not display them.
* Citation sidecar association loss limits recoverability of canonical-only state.

### Consequences

* Good, because one engine owns document actions instead of independent family adapters with divergent state and policies.
* Good, because rendering failures are reported before existing content is replaced.
* Good, because group edits preserve page-info ownership and can be undone with their structural/text changes.
* Good, because the established next-CSL-insertion trigger and citation-only conversion boundary remain.
* Good, because unsupported native citation modifiers fail before content can be silently lost.
* Good, because owned conversion rollback can recover ordinary application failures without undoing earlier user work.
* Bad, because legacy JabRef mark formats are incompatible and are not migrated.
* Bad, because metadata association can be invalidated or lost by external edits.
* Bad, because conversion needs root undo ownership, may clear prior redo history, and cannot guarantee recovery from every UNO failure.
* Bad, because whole-document ordering/rendering can still be required even when unchanged output is not rewritten.

### Confirmation

The Writer integration definitions cover golden canonical documents, cross-family save/reopen, all six types/page slots, current canonical/native Zotero citation conversion, protected ranges, footnotes/tables, superscripts, tracked changes, and ordinary application-failure rollback.
Native JSON round trips cover supported URI and unknown metadata, while the native locator fixture checks pre-write rejection with content unchanged.
Bibliography assertions follow normal synchronization: all sections unchanged when disabled, target-mode updates only when enabled.
Storage/codec, `ConversionMetadataTest`, `DocumentSessionTest`, and GUI/controller tests complement the Writer coverage with metadata, property, undo-ownership, and warning-order checks.
These descriptions are not evidence that a new test run passed.
The [OpenOffice/LibreOffice requirements](../requirements/openoffice.md) provide implementation/test traceability.

## More Information

* [Current architecture and style-selection overview](../code-howtos/openoffice/overview.md).
* [Real-Writer testing instructions and artifacts](../code-howtos/testing.md), under "LibreOffice Writer integration tests".
* [ADR-0068](0068-use-a-preference-for-Zotero-compatibility-mode.md) remains the rationale for the explicit preference, next-insertion trigger, and warning; this decision replaces its legacy encoding, not that trigger.

<!-- markdownlint-disable-file MD022 -->
