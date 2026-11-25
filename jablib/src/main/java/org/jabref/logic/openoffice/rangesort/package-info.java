/// UNO-backed range ordering and overlap detection for Writer documents.
/// [RangeHolder] exposes a text range; [RangeSortable] adds a payload and positional tie-break index.
/// [RangeSort] partitions ranges by text container and orders each partition;
/// [RangeSortVisual] orders ranges by view-cursor coordinates and their tie-break indexes.
///
/// [WithinSetRangeOverlapFinder] reports equal ranges and consecutive overlap or touch findings within a collection;
/// [BetweenSetsRangeOverlapFinder] finds the first conflict between two collections.
/// [RangeOverlap] and [RangeOverlapKind] describe the findings.
/// [RestorableTextViewCursor] provides a usable view cursor and retains its original position or selection for restoration.
///
/// Citation-level ordering is coordinated by [org.jabref.logic.openoffice.frontend.DocumentOrder];
/// document access helpers such as [org.jabref.logic.openoffice.uno.UnoTextRange] live in the UNO package.
///
/// See [Citation ordering and layout limitations](https://devdocs.jabref.org/code-howtos/openoffice/order-of-appearance.html).
@NullMarked
package org.jabref.logic.openoffice.rangesort;

import org.jspecify.annotations.NullMarked;
