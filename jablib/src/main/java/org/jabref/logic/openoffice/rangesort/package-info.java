/// UNO-backed range ordering and overlap detection for Writer documents.
/// [RangeSort] orders ranges within one text container, [RangeSortVisual] uses view-cursor positions,
/// and [RangeOverlapWithin] and [RangeOverlapBetween] detect conflicting ranges.
/// [FunctionalTextViewCursor] preserves the user's view-cursor state while inspecting positions.
/// Citation-level ordering is coordinated by [org.jabref.logic.openoffice.frontend.DocumentOrder];
/// document access helpers such as [org.jabref.logic.openoffice.uno.UnoTextRange] live in the UNO package.
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/order-of-appearance.html">Citation ordering and layout limitations</a>
@NullMarked
package org.jabref.logic.openoffice.rangesort;

import org.jspecify.annotations.NullMarked;
