/// Concrete UNO-backed Writer reference marks.
/// [WriterReferenceMark] safely replaces one mark's text; [WriterReferenceMarks] creates, finds,
/// and lists marks without interpreting citation names. Callers explicitly choose persistent
/// boundaries for canonical citations or temporary boundaries for native Zotero citations.
/// [WriterTextContext] reads surrounding text for citation management.
/// Canonical citation identities and page information belong to
/// [org.jabref.logic.openoffice.engine.storage.MarkStore].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html">Writer integration architecture</a>
@NullMarked
package org.jabref.logic.openoffice.backend;

import org.jspecify.annotations.NullMarked;
