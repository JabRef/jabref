/// Shared Writer citation identity and Zotero protocol support.
/// [CitationIds] generates all new citation identities; [ZoteroReferenceMark] encodes Zotero citation
/// data without representing a live Writer range. [CitedEntryCollector] collects embedded Zotero entries for import without accessing Writer.
/// Document actions and rendering belong to
/// [org.jabref.logic.openoffice.engine], while UNO access belongs to [org.jabref.logic.openoffice.uno].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html#identifier-generation">Writer identifier policy</a>
@NullMarked
package org.jabref.logic.openoffice;

import org.jspecify.annotations.NullMarked;
