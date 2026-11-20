/// Writer range access, citation ordering, and overlap protection for canonical citations.
/// [DocumentCitations] combines a fresh citation model with [org.jabref.logic.openoffice.engine.storage.MarkStore].
/// [DocumentOrder] distinguishes visual numbering order from unmapped text-container order used for merging.
/// Document actions and renderer coordination belong to [org.jabref.logic.openoffice.engine.CitationEngine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/order-of-appearance.html">Citation order and layout limitations</a>
@NullMarked
package org.jabref.logic.openoffice.frontend;

import org.jspecify.annotations.NullMarked;
