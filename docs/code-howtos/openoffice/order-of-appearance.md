---
nav_order: 2
parent: The LibreOffice Panel
grand_parent: Code Howtos
---
# Order of appearance of citation groups

The order of appearance of citations is decided on two levels:

1. their order within each citation group (`localOrder`), and
2. the order of the citation groups that appear as citation markers in the text (`globalOrder`).

This page is about the latter: how to decide the order of appearance (numbering sequence) of a set of citation markers?

## Conceptually

In a continuous text it is easy: take the textual order of citation markers.

In the presence of figures, tables, footnotes/endnotes possibly far from the location they are referred to in the text or wrapped around with text it becomes less obvious what is the correct order.

Examples:

* References in footnotes: are they _after_ the page content, or number them as if they appeared at the footnote mark? (JabRef does the latter)
* A figure with references in its caption. Text may flow on either or both sides.\
  Where should we insert these in the sequence?
* In a two-column layout, a text frame or figure mostly, but not fully in the second column: shall we consider it part of the second column?

## Technically

In LibreOffice, a document has a main text that supports the [XText](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1XText.html) interface.\
This allows several types of [XTextContent](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1XTextContent.html) to be inserted.

* Some of these allow text inside with further insertions.

### Anchors

* Many, but not all XTextContent types support getting a "technical" insertion point or text range through [getAnchor](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1XTextContent.html#ae82a8b42f6b2578549b68b4483a877d3).
* In Libreoffice positioning both a frame and its anchor seems hard: moving the frame tends to also move the anchor.
* Consequence: producing an order of appearance for the citation groups based solely on `getAnchor` calls may be impossible.
  * Allowing or requiring the user to insert "logical anchors" for frames and other "floating" parts might help to alleviate these problems.

### Sorting within a `Text`

The text ranges occupied by the citation markers support the [XTextRange](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1XTextRange.html) interface.

* These provide access to the XText they are contained in.
* The [Text](https://api.libreoffice.org/docs/idl/ref/servicecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1Text.html) service may support (optional) the [XTextRangeCompare](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1XTextRangeCompare.html) interface, that allows two XTextRange values to be compared if both belong to this `Text`

### Visual ordering

* The cursor used by the user is available as an [XTextViewCursor](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1XTextViewCursor.html)
* If we can get it and can set its position in the document to each XTextRange to be sorted, and ask its [getPosition](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1text\_1\_1XTextViewCursor.html#a9b2bafd342ef75b5d504a9313dbb1389) to provide coordinates "relative to the top left position of the first page of the document.", then we can sort by these coordinates in top-to-bottom left-to-right order.
* Note: in some cases, for example when the cursor is in a comment (as in `Libreoffice:[menu:Insert]/[Comment]`), the XTextViewCursor is not available (I know of no way to get it).
* In some other cases, for example when an image is selected, the XTextViewCursor we normally receive is not 'functional': we cannot position it for getting coordinates for the citation marks. The [FunctionalTextViewCursor](https://github.com/antalk2/jabref/blob/improve-reversibility-rebased-03/src/main/java/org/jabref/model/openoffice/rangesort/FunctionalTextViewCursor.java) class can solve this case by accessing and manipulating the cursor through [XSelectionSupplier](https://api.libreoffice.org/docs/idl/ref/interfacecom\_1\_1sun\_1\_1star\_1\_1view\_1\_1XSelectionSupplier.html)

Consequences of getting these visual coordinates and using them to order the citation markers

* allows uniform handling of the markers. Works in footnotes, tables, frames (apparently anywhere)
* requires moving the user visible cursor to each position and with [screen refresh](https://github.com/antalk2/jabref/blob/improve-reversibility-rebased-03/src/main/java/org/jabref/model/openoffice/uno/UnoScreenRefresh.java) enabled.\
  `(problem)` This results in some user-visible flashing and scrolling around in the document view.
* The expression "relative to the top left position of the first page of the document" is understood literally, "as on the screen".\
  `(problem)` Showing pages side by side or using a two-column layout will result in markers in the top half of the second column or page to be sorted before those on the bottom of the first column of the first page.

## JabRef

Jabref uses the following steps for sorting sorting citation markers (providing `globalOrder`):

1. the textranges of citation marks in footnotes are replaced by the textranges of the footnote marks.
2. get the positions (coordinates) of these marks
3. sort in top-to-botton left-to-right order

`(problem)` In JabRef5.2 the positions of citation marks within the same footnote become indistinguishable, thus their order after sorting may differ from their order in the footnote text.\
This caused problems for

1. numbering order\
   `(solved)` by keeping track of the order-in-footnote of citation markers during sorting using [getIndexInPosition](https://github.com/antalk2/jabref/blob/122d5133fa6c7b44245c5ba5600d398775718664/src/main/java/org/jabref/model/openoffice/rangesort/RangeSortable.java#L21))
2. `click:Merge`: It examines _consecutive_ pairs of citation groups if they can be merged. Wrong order may result in not discovering some mergeable pairs or attempting to merge in wrong order.\
   `(solved)` by not using visual order, only XTextRangeCompare-based order within each XText [here](https://github.com/antalk2/jabref/blob/122d5133fa6c7b44245c5ba5600d398775718664/src/main/java/org/jabref/logic/openoffice/action/EditMerge.java#L325))
