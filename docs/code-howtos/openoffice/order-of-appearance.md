---
nav_order: 2
parent: The LibreOffice Panel
grand_parent: Code Howtos
---
# Order of appearance of citation groups

The canonical Writer integration separates three orders:

- **Storage order** is the key order encoded in a group's reference mark. Page-info property indexes always address this order.
- **Local order** is a renderer's presentation order inside one group. JStyle uses `MultiCiteChronological` to choose year/author/title or author/year/title ordering, and numeric marker formatting can sort by assigned numbers.
- **Global order** is the order of citation groups in the document. It contributes to first-appearance numbering, JStyle first/repeated author-list rules, and author-year disambiguation.

Sorting for presentation does not rewrite storage order or move page information to a different citation.
Numeric JStyle in-text groups follow local order and pair each source's author list with its own number and page information, without compressing numbers across sources.
BST in-text groups preserve storage order in every notation, even when bibliography sorting assigns numbers or labels in a different order.
Bibliography sorting is a separate renderer decision: first-appearance order is not necessarily the bibliography order.
See the [overview](overview.md#rendering-numbering-and-style-changes) for the family-specific numbering rules.

## Why global order is not just text order

Within continuous text, citation groups can follow their textual order.
Figures, tables, footnotes/endnotes, and floating text frames make the intended reading sequence less obvious:

- Should citations in a footnote follow all page content, or appear where the footnote is referenced? JabRef uses the footnote's reference position.
- Where should a figure caption's citations appear when body text flows on either or both sides of the figure?
- Which column should determine a floating frame's position when it overlaps two columns?

These are reading-order decisions, not just coordinate comparisons.
The implementation below handles footnote references explicitly but does not infer a universal reading sequence for floating content.

## UNO text containers and comparisons

Writer's main text supports the [XText] interface, which allows [XTextContent] objects to be inserted.
Some content, such as frames, contains further text with its own text container.
Citation ranges expose [XTextRange], whose `getText()` identifies their containing `XText`.

### Anchors

[XTextContent.getAnchor()][XTextContent] returns the range to which content is attached.
That attachment is not necessarily the content's displayed position or its intended position in the reading sequence.
For example, a floating frame and its anchor can occupy different positions, and moving the frame can also affect its anchoring.
The API also warns that a table's anchor does not have a text position usable by operations that require one.
Following anchors alone therefore cannot establish a reliable global reading order for arbitrary Writer content.

For a citation inside a footnote, `UnoTextRange.getFootnoteMarkRange` obtains the footnote's anchor through `XFootnote` and `XTextContent.getAnchor()`.
This provides the explicit reference position used for numbering; it is not a general mapping of tables, frames, or captions to logical reading positions.

### Sorting within one text container

The [Text] service optionally supports [XTextRangeCompare], which compares the starts or ends of two ranges **within the same text container**.
Comparing a body range directly with a range in a footnote or table cell does not establish a global order and can raise an `IllegalArgumentException`.

`DocumentOrder` therefore partitions ranges by their original `XText` before sorting them textually through `RangeSort` and `UnoTextRange`.
Those helpers require the container to provide `XTextRangeCompare`; partitioning does not add comparison support to a container that lacks it.
Ordinary canonical insertion, refresh, and group editing use `DocumentOrder` with JStyle, CSL, and BST styles.
Native Zotero ordering and next-insertion conversion have separate ordering implementations in `ZoteroCitationStrategy` and `CitationFormatConverter`; do not assume they share `DocumentOrder`'s fallback behavior.

## Canonical numbering order

`DocumentOrder.inDocumentOrder` prepares and sorts the canonical groups as follows:

1. Read each canonical group's Writer range from `MarkStore`.
2. Partition by the original `XText` and sort within each partition.
3. Assign `indexInPosition` within each original partition from that textual order.
4. For ranges inside footnotes, substitute the corresponding body footnote-mark range.
5. When an unlocked functional view cursor is available, obtain visual positions and sort top-to-bottom, then left-to-right, then by `indexInPosition`.
6. Otherwise, use textual ordering within the mapped text-container partitions and log a warning.

Mapping a footnote citation to its body anchor counts it where the footnote is referenced, not at the bottom of the page.
The substitution changes only the ranges used for sorting; it does not move the citations out of the footnote.
Several citations in one footnote then share an anchor, so `RangeSortable.getIndexInPosition()` preserves their order within that footnote.
Without this tie-breaker, the mapped positions cannot distinguish those citations and their numbering can lose the original footnote order.
`DocumentOrder.withInsertion` adds an in-memory pending group at the insertion cursor's end, then repartitions and sorts the unmapped ranges before assigning indexes and mapping footnotes.
This lets insertion render the whole cited set before changing existing document content.

### View cursor acquisition and screen refresh

`RangeSortVisual` moves an [XTextViewCursor] to each range and reads [XTextViewCursor.getPosition()][XTextViewCursor].
The API describes these coordinates as relative to the top-left position of the document's first page.
JabRef compares the vertical coordinate first, the horizontal coordinate second, and `indexInPosition` third.
This gives ranges from different text containers a common visual comparison, subject to the layout limitations below.

Obtaining a view cursor does not guarantee that it can move to citation ranges:

- With the cursor inside a comment, the text view cursor or original selection may be unavailable.
- With an image or frame selected, a view cursor may be unavailable or nonfunctional for text navigation.
- `RestorableTextViewCursor.get` retains the original text position or selection and checks whether the cursor supports text access.
  For a non-text selection, it can temporarily select the start of the document through `UnoSelection` and [XSelectionSupplier] to obtain a usable text view cursor.
  Navigation can still fail later; `DocumentOrder` catches UNO runtime and Java `IllegalStateException` failures from visual sorting and uses the textual fallback.

After visual sorting, `RestorableTextViewCursor.restore` attempts to restore the original text position or object selection.
Restoration itself can fail; the fallback is not a guarantee of recovery from every cursor or connection failure.
Restoration does not make cursor movement invisible: moving the view cursor with screen refresh enabled can cause user-visible scrolling and flashing.

The view cursor cannot perform this work while document controllers are locked through [XModel.lockControllers()][XModel].
`UnoScreenRefresh.hasControllersLocked` exposes that state; the sorter does not override an existing controller lock.
Canonical insert and refresh therefore order and render before entering `DocumentWriteSession`'s controller-locked write phase.
A missing/nonfunctional view cursor, externally locked controllers, or a visual-sort failure triggers the textual fallback with a warning.

### Visual ordering limitations

**Visual order is not guaranteed reading order for every Writer layout.**
The algorithm uses view coordinates, not a column-aware or page-aware reading-order model.
The first-page-relative coordinates are interpreted in the displayed layout, not as a sequence of page numbers and within-page reading positions.

- In a two-column layout, a citation near the top of the second column can sort before a citation near the bottom of the first column.
- With pages displayed side by side, a citation on the right-hand page can sort before a lower citation on the left-hand page.
- Floating frames and captions do not have a universally correct position in the reading sequence; their anchors and displayed positions can differ.

These limitations apply to JStyle, CSL, and BST.
Avoid describing this as correct multi-column ordering or as a guarantee for arbitrary tables and frames.
Golden-document tests establish the tested layouts, not every possible Writer layout.

### Textual fallback limitations

The fallback orders comparable ranges within each text container and retains the footnote tie-break indexes.
It does not infer a reading sequence between unrelated body, table-cell, or frame containers.
Their partition order is not a guarantee of global reading order.
The warning matters: falling back avoids an invalid cross-container comparison but can change numbering relative to a successful visual pass.

## Group editing order and protected footnotes

Group editing does not use mapped footnote anchors as replacement edit ranges.
`CitationGroupEditor` scans unmapped textual order through `DocumentCitations.getCitationGroupsSortedWithinPartitions(document, false)`, which delegates to `DocumentOrder.withinTextContainers`, not visual numbering order.
Merge considers NORMAL groups only, with no intervening nonblank text or paragraph break, and never combines citations from different text containers.
Several citations in one footnote remain distinct ranges for this adjacency check.
Using mapped visual positions instead would lose the information needed to identify consecutive groups and could miss mergeable pairs or examine the wrong pairs.
Separate also acts on NORMAL groups and preserves each citation's page info in storage order.
The engine separately establishes global order for rendering the proposed merged or separated groups before applying their original, unmapped replacement ranges.

For citations directly in a footnote's `XText`, canonical protection checks include its body anchor.
This is not a recursive search through nested text containers inside a footnote.
The overlap checks reject body citation edits that would cover those anchors and erase their citations.
These protection checks are separate from mapping footnotes for numbering.

## Verification

`WriterIntegrationTest` exercises canonical citations in the body, a footnote, and a table, including save/reopen and style-family changes.
`CanonicalStorageWriterTest.documentOrderMapsFootnoteCitationsAndProtectsTheirBodyAnchor` checks footnote-anchor ordering with the textual fallback and protects the body anchor.
Its `benchmarkThreeHundredCanonicalInsertionsAndVisualOrdering` test measures storage insertion/writing and visual ordering for 300 body citations, checks their order, and verifies an unchanged write pass.
Those checks do not establish a latency budget for the complete renderer pipeline or correct numbering in multi-column layouts.
For execution requirements and saved Writer artifacts, see [Writer integration testing](../testing.md#libreoffice-writer-integration-tests).
For the current packages containing these implementation classes, see the [package layout and ownership guide](code-reorganization.md).

[XText]: https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1text_1_1XText.html
[XTextContent]: https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1text_1_1XTextContent.html
[XTextRange]: https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1text_1_1XTextRange.html
[Text]: https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1text_1_1Text.html
[XTextRangeCompare]: https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1text_1_1XTextRangeCompare.html
[XTextViewCursor]: https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1text_1_1XTextViewCursor.html
[XSelectionSupplier]: https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1view_1_1XSelectionSupplier.html
[XModel]: https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1frame_1_1XModel.html
