/// Low-level LibreOffice UNO access for Writer documents, cursors, named text content, and properties.
/// [UnoInterfaces#query(Class, Object)] provides optional access to UNO interfaces.
/// [UnoTextDocument] and [UnoCursor] provide document/controller and cursor access.
///
/// [NamedTextContentInserter] creates and inserts named text content;
/// [NamedTextContentPlacement] is its parameter record for the document, name, range, and text-absorption policy.
/// [NamedTextContentLookup] looks up named `XTextContent` in UNO collections.
/// [org.jabref.logic.openoffice.backend.WriterReferenceMarks], [UnoBookmark], and [UnoTextSection]
/// provide access to reference marks, bookmarks, and text sections.
/// [UnoUserDefinedProperty] accesses custom document properties.
///
/// [UnoRedlines], [UnoUndo], and [UnoScreenRefresh] support controlled document writes.
/// [WriterOperationException] reports failures while preparing or applying Writer writes;
/// [WriterDocumentAccessException] reports inaccessible documents or required UNO objects.
/// [OOTextWriter] interprets the pure [org.jabref.model.openoffice.ootext.OOText] representation into Writer text.
/// Higher-level citation actions belong to [org.jabref.logic.openoffice.engine.CitationEngine].
///
/// See [Writer integration architecture](https://devdocs.jabref.org/code-howtos/openoffice/overview.html).
@NullMarked
package org.jabref.logic.openoffice.uno;

import org.jspecify.annotations.NullMarked;
