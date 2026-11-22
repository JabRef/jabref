---
nav_order: 3
parent: The LibreOffice Panel
grand_parent: Code Howtos
---
# Problems

This page records historical JabRef 5.2/5.3 integration problems and proposed changes, not the current canonical storage contract.
See the [architecture overview](overview.md) and [package guide](code-reorganization.md) for the current implementation.
The proposals and `(change)` annotations below describe historical work, not outstanding tasks or current guarantees.

## Historical: pageInfo should belong to citations, not citation groups

Current canonical page information is stored per citation, indexed by group uid and storage-order slot; presentation sorting does not change that association.
The following proposal predates that contract.

* Creating `[click:Separate]` revealed\
  a `(problem)`: pageInfo strings are conceptually associated with citations, but the implementation associates them to citation groups.\
  The number of available pageInfo slots changes during`[click:Merge]` and `[click:Separate]` while the number of citations remains fixed.
  * The proposed solution was to change the association.
    * Not only reference marks (citation groups) need unique identifiers, but also citations.\
      Possible encoding for reference mark names:\
      `JR_cite{type}_{number1}_{citationKey1},{number2}_{citationKey2}`\
      where `{type}` encodes the citation type (for the group), `{citationKey1}` is made unique by choosing an appropriate number for `{number1}`\
      This would allow `JR_cite_{number1}_{citationKey1}` to be used as a property name for storing the pageInfo.

      Changes required to

      * reference mark search, name generation and parsing
      * name generation and parsing for properties storing pageInfo values
      * in-memory representation
        * JabRef 5.2 does not collect pageInfo values, accesses only when needed.\
          Thus, the code accessing them would have to change.
        * The proposed representation does collect, to allow separation of getting from the document and processing
      * insertion of pageInfo into citation markers: JabRef 5.2 injects a single pageInfo before the closing parenthesis, now we need to handle several values
      * `[click:Manage citations]` should work on citations, not citation groups.

## Historical: backend research

The choice of how do we represent the data and the citation marks in the document has consequences on usability.

Reference marks have some features that make it easy to mess up citations in a document

* They are **not visible** by default, the user is not aware of their boundaries\
  (<kbd>Ctrl</kbd> + <kbd>F8</kbd>, `LO:[View]/[Field shadings]` helps)
* They are **not atomic**:
  * the user can edit the content. This will be lost on `[click:Update]`\
    If an `As character` or `To character` anchor is inserted, the corresponding frame or footnote is deleted.
  * by pressing Enter within, the user can break a reference mark into two parts.\
    The second part is now outside the reference mark: `[click:Update]` will leave it as is, and replace the first part with the full text for the citation mark.
  * If the space separating to citation marks is deleted, the user cannot reliably type between the marks.\
    The text typed usually becomes part of one of the marks. No visual clue as to which one.\
    Note: `[click:Merge]` then `[click:Separate]` adds a single space between. The user can position the cursor before or after it. In either case the cursor is on a boundary: it is not clear if it is in or out of a reference mark.\
    Special case: a reference mark at the start or end of a paragraph: the cursor is usually considered to be within at the corresponding edge.
* (good) They can be moved (<kbd>Ctrl</kbd> + <kbd>X</kbd>, <kbd>Ctrl</kbd> + <kbd>V</kbd>)
* They cannot be copied. <kbd>Ctrl</kbd> + <kbd>C</kbd>, <kbd>Ctrl</kbd> + <kbd>V</kbd> copies the text without the reference mark.
* Reference marks are lost if the document is saved as docx.

These are historical observations, not a current compatibility matrix for every Writer version or export format.
The following implementation links are historical snapshots; current range handling lives in `WriterReferenceMark`, `WriterReferenceMarks`, and `ReferenceMarkTextWriter`.

* I know of no way to insert text into an empty text range denoted by a reference mark
  * JabRef 5.3 recreates the reference mark (using [insertReferenceMark](https://github.com/JabRef/jabref/blob/475b2989ffa8ec61c3327c62ed8f694149f83220/src/main/java/org/jabref/gui/openoffice/OOBibBase.java#L1072)) [at this location](https://github.com/JabRef/jabref/blob/475b2989ffa8ec61c3327c62ed8f694149f83220/src/main/java/org/jabref/gui/openoffice/OOBibBase.java#L706)
  * `(change)` I preferred to (try to) avoid this: [NamedRangeReferenceMark.nrGetFillCursor](https://github.com/antalk2/jabref/blob/122d5133fa6c7b44245c5ba5600d398775718664/src/main/java/org/jabref/logic/openoffice/backend/NamedRangeReferenceMark.java#L225) returns a cursor between two invisible spaces, to provide the caller a location it can safely write some text. [NamedRangeReferenceMark.nrCleanFillCursor](https://github.com/antalk2/jabref/blob/122d5133fa6c7b44245c5ba5600d398775718664/src/main/java/org/jabref/logic/openoffice/backend/NamedRangeReferenceMark.java#L432) removes these invisible spaces unless the content would become empty or a single character. By keeping the content at least two characters, we avoid the ambiguity at the edges: a cursor positioned between two characters inside is always within the reference mark. (At the edges it may or may not be inside.)
* `(change)` `[click:Cite]` at reference mark edges: [safeInsertSpacesBetweenReferenceMarks](https://github.com/antalk2/jabref/blob/122d5133fa6c7b44245c5ba5600d398775718664/src/main/java/org/jabref/logic/openoffice/backend/NamedRangeReferenceMark.java#L67) ensures the we are not inside, by starting two new paragraphs, inserting two spaces between them, then removing the new paragraph marks.
* `(change)` [guiActionInsertEntry](https://github.com/antalk2/jabref/blob/122d5133fa6c7b44245c5ba5600d398775718664/src/main/java/org/jabref/gui/openoffice/OOBibBase2.java#L624) checks if the cursor is in a citation mark or the bibliography.
* `(change)` `[click:Update]` does an [exhaustive check](https://github.com/antalk2/jabref/blob/122d5133fa6c7b44245c5ba5600d398775718664/src/main/java/org/jabref/gui/openoffice/OOBibBase2.java#L927) for overlaps between protected ranges (citation marks and bibliography). This can become slow if there are many citations.

It would be nice if we could have a backend with better properties. We probably need multiple backends for different purposes. This would be made easier if the backend were separated from the rest of the code. This would be the purpose of [logic/openoffice/backend](https://github.com/antalk2/jabref/tree/improve-reversibility-rebased-03/src/main/java/org/jabref/logic/openoffice/backend).

## Undo

### Historical observations and proposal

* JabRef 5.3 does not collect the effects of GUI actions on the document into larger Undo actions.\
  This makes the Undo functionality of LO impractical.
* `(change)` collect the effects of GUI actions into large chunks: now a GUI action can be undone with a single click.
  * Historically, pageInfo was stored at document level and not restored by Undo.

### Current undo and recovery boundaries

The older GUI already used undo contexts for citation insertion including mark conversion, refresh, merge, and separate; the historical notes above do not mean these contexts are new in the canonical integration.
What those contexts did not restore was custom document-property state, as the old Manage citations comment explicitly noted for page information.
The canonical integration now registers custom undo actions through `UndoableDocumentProperties` for per-citation page-info properties.
Merge and Separate combine structural edits, final formatted output, and page-info changes in one undo context.
Next-insertion conversion also includes its prepared citation properties and applied-style properties in one owned root undo context.
Canonical/native conversion on CSL insertion preserves round-trip citation sidecars; supported legacy JStyle migration on JStyle/BST or non-native CSL insertion transfers legacy page properties to canonical slots.
Zotero preference deltas are included only when targeting native mode.
Canonical CSL operations and conversions leave `ZOTERO_PREF` XML untouched; native-mode insertion or refresh updates it.
The conversion-failure rollback described below is new recovery behavior to prevent partial citation corruption after ordinary application failures.
`DocumentWriteSession.writeWithRollback` requires unlocked undo support, rejects nesting, and rolls back ordinary application failures only after closing and verifying ownership of that context.
Starting the write transaction can clear pre-existing redo history even when rollback restores the document; disconnects, lost ownership, and rollback failures require a distinct recovery error rather than an unconditional rollback claim.
Ordinary updates retain raw style-property setters and do not gain this rollback behavior.
Conversion detection reads fresh reference-mark names and decides from the first recognized name returned by Writer, not physical document order or a scan for every format.
Mixed managed formats are unsupported, not actively rejected by a mixed-format detector; after a positive decision, normal group reading and preparation still run.
Native modifiers do not by themselves block conversion, and any native author suppression maps to the group's in-text citation type rather than adding full native rendering semantics.
Legacy-only migration tolerates unresolved entries with placeholders and an insertion warning; opening the missing libraries and refreshing recovers output.
Native-to-canonical conversion and native generation remain strict about unresolved sources.
Only citation marks are converted; normal bibliography synchronization is unchanged, leaving every bibliography section untouched when disabled and updating only the target mode's managed bibliography when enabled.
See [canonical write preparation](overview.md#canonical-insert-and-refresh) and the [conversion policy](../../decisions/0084-canonical-writer-citations.md#conversion-on-the-next-csl-citation-insertion).

## Historical: block screen refresh

* LibreOffice has support in [XModel](https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1frame_1_1XModel.html#a7b7d36374033ee9210ec0ac5c1a90d9f) to "suspend some notifications to the controllers which are used for display updates."
* `(change)` The historical proposal adopted this facility.

Current writes lock controllers through `DocumentWriteSession`, but ordinary canonical ordering and rendering happen before that write phase.
Visual ordering needs an unlocked view cursor; locking controllers is not a way to make that traversal invisible.
See [view cursor acquisition and screen refresh](order-of-appearance.md#view-cursor-acquisition-and-screen-refresh).

## Current pending-deletion safety

Suspending change recording does not accept or remove existing deletions.
Canonical edit validation checks whether managed citation ranges touch pending deleted ranges before writing.
Native operations and conversion use `ZoteroCitationStrategy.validateRanges`, which also checks the insertion range, cited footnote anchors, and bibliography ranges selected for editing, even when recording is already off.
Deletions elsewhere are not a blanket reason to reject conversion: with bibliography synchronization disabled, unrelated pending bibliography deletions are left untouched.
Resolve affected deletions in Writer's `Edit` → `Track Changes` → `Manage` before retrying.
