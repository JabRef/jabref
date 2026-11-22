/// Shared Writer citation identity and Zotero protocol support.
/// [CitationIds] generates ten-character CUID2 canonical group uids and native Zotero `RND` suffix values;
/// native JSON `citationID` values use eight characters. Canonical uids have no `RND` prefix.
/// Stored canonical uids and JSON `citationID` values are not regenerated or restricted to the generated lengths.
/// [ZoteroReferenceMark] encodes Zotero citation data without representing a live Writer range.
/// [CitedEntryCollector] collects embedded Zotero entries for import without accessing Writer.
/// Document actions and rendering belong to [org.jabref.logic.openoffice.engine]. Concrete reference marks
/// live in [org.jabref.logic.openoffice.backend], UNO helpers in [org.jabref.logic.openoffice.uno], and
/// UNO-backed range ordering in [org.jabref.logic.openoffice.rangesort].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html#identifier-generation">Writer identifier policy</a>
@NullMarked
package org.jabref.logic.openoffice;

import org.jspecify.annotations.NullMarked;
