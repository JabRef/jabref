/// Low-level LibreOffice UNO access for Writer documents, cursors, named ranges, and properties.
/// [UnoTextDocument] and [UnoCursor] provide document/controller and cursor access;
/// [org.jabref.logic.openoffice.backend.WriterReferenceMarks] and [UnoTextSection] manage named text content, and
/// [UnoUserDefinedProperty] accesses custom document properties.
/// [UnoRedlines], [UnoUndo], and [UnoScreenRefresh] support controlled document writes.
/// [DocumentAnnotation] describes named document content; [OOTextWriter] interprets the pure
/// [org.jabref.model.openoffice.ootext.OOText] representation into Writer text.
/// Higher-level citation actions belong to [org.jabref.logic.openoffice.engine.CitationEngine].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/openoffice/overview.html">Writer integration architecture</a>
@NullMarked
package org.jabref.logic.openoffice.uno;

import org.jspecify.annotations.NullMarked;
