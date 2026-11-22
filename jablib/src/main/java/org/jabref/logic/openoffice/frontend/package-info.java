/// Writer range access, citation ordering, and overlap protection for canonical citations.
/// [DocumentCitations] is an action-local facade combining a mutable citation model with live Writer range access
/// and [org.jabref.logic.openoffice.engine.storage.MarkStore], not an immutable document snapshot.
/// [DocumentOrder] maps footnotes to body anchors for numbering and uses visual order when an unlocked view cursor is usable,
/// falling back to text-container order otherwise. Group editing uses unmapped text-container order.
/// Visual coordinates do not guarantee reading order in multi-column or side-by-side page layouts.
/// Document actions and renderer coordination belong to [org.jabref.logic.openoffice.engine.CitationEngine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/order-of-appearance.html">Citation order and layout limitations</a>
@NullMarked
package org.jabref.logic.openoffice.frontend;

import org.jspecify.annotations.NullMarked;
