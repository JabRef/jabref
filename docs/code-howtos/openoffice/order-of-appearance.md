---
nav_order: 2
parent: The LibreOffice Panel
grand_parent: Code Howtos
---
# Order of appearance of citation groups

The canonical Writer integration separates three orders:

- **Storage order** is the key order encoded in a group's reference mark. Page-info property indexes always address this order.
- **Local order** is a renderer's presentation order inside one group. JStyle uses `MultiCiteChronological` to choose year/author/title or author/year/title ordering, and numeric marker formatting can sort by assigned numbers.
- **Global order** is the order of citation groups in the document. It contributes to first-appearance numbering and JStyle author-year disambiguation.

Sorting for presentation does not rewrite storage order or move page information to a different citation.
Bibliography sorting is a separate renderer decision: first-appearance order is not necessarily the bibliography order.
See the [overview](overview.md#rendering-numbering-and-style-changes) for the family-specific numbering rules.

## UNO text containers and comparisons

Writer exposes the document body, footnotes, table cells, and other text through `XText` and `XTextRange` interfaces.
Textual range comparison is meaningful only within a compatible text container.
Comparing a body range directly with a range in a footnote or table cell does not establish a global order.

`DocumentOrder` therefore partitions ranges by their original `XText` before sorting them textually.
This shared algorithm is used for canonical JabRef citations with JStyle, CSL, and BST styles.
Zotero mode has its own storage and ordering strategy; do not assume this algorithm defines that mode's behavior.

## Canonical numbering order

`DocumentOrder.inDocumentOrder` prepares and sorts the canonical groups as follows:

1. Read each canonical group's Writer range from `MarkStore`.
2. Partition by the original `XText` and sort within each partition.
3. Assign `indexInPosition` from that textual order.
4. For ranges inside footnotes, substitute the corresponding body footnote-mark range.
5. When an unlocked functional view cursor is available, obtain visual positions and sort top-to-bottom, then left-to-right, then by `indexInPosition`.
6. Otherwise, use textual ordering within the mapped text-container partitions and log a warning.

Mapping a footnote citation to its body anchor counts it where the footnote is referenced, not at the bottom of the page.
Several citations in one footnote then share an anchor, so `indexInPosition` preserves their order within that footnote.
`DocumentOrder.withInsertion` includes an in-memory pending group in this process before its reference mark is created.
This lets insertion render the whole cited set before changing existing document content.

Visual positions require moving Writer's view cursor, which is restored after sorting.
The view cursor cannot perform this work while document controllers are locked.
Canonical insert and refresh therefore order and render before entering `DocumentSession`'s controller-locked write phase.
A missing/nonfunctional view cursor, externally locked controllers, or a visual-sort failure triggers the textual fallback with a warning.

### Visual ordering limitations

**Visual order is not guaranteed reading order for every Writer layout.**
The algorithm uses view coordinates, not a column-aware or page-aware reading-order model.

- In a two-column layout, a citation near the top of the second column can sort before a citation near the bottom of the first column.
- With pages displayed side by side, a citation on the right-hand page can sort before a lower citation on the left-hand page.
- Floating frames and captions do not have a universally correct position in the reading sequence; their anchors and displayed positions can differ.

Sharing the JStyle ordering algorithm with CSL and BST does not fix these limitations.
Avoid describing this as correct multi-column ordering or as a guarantee for arbitrary tables and frames.
Golden-document tests establish the tested layouts, not every possible Writer layout.

### Textual fallback limitations

The fallback orders comparable ranges within each text container and retains the footnote tie-break indexes.
It does not infer a reading sequence between unrelated body, table-cell, or frame containers.
Their partition order is not a guarantee of global reading order.
The warning matters: falling back avoids an invalid cross-container comparison but can change numbering relative to a successful visual pass.

## Group editing order and protected footnotes

Group editing does not use mapped footnote anchors as replacement edit ranges.
Merge scans unmapped textual order within text containers through `DocumentOrder.withinTextContainers`, not visual numbering order.
Merge considers NORMAL groups only, with no intervening nonblank text or paragraph break, and never combines citations from different text containers.
Several citations in one footnote remain distinct ranges for this adjacency check.
Separate also acts on NORMAL groups and preserves each citation's page info in storage order.

Footnote body anchors are protected ranges whenever the footnote contains a managed citation.
Canonical overlap checks include those anchors so inserting or rewriting a body citation cannot erase a footnote that owns citations.
These protection checks are separate from mapping footnotes for numbering.

## Verification

`WriterIntegrationTest` exercises canonical citations in the body, a footnote, and a table, including save/reopen and style-family changes.
`CanonicalStorageWriterTest` checks footnote-anchor ordering/protection and measures insertion/writing plus visual ordering for about 300 citations.
Those checks do not establish a latency budget for the complete renderer pipeline or correct numbering in multi-column layouts.
For execution requirements and saved Writer artifacts, see [Writer integration testing](../testing.md#libreoffice-writer-integration-tests).
