package org.jabref.logic.openoffice.uno;

import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

/// Describes the location and text-absorption policy for a named Writer mark, bookmark, or section.
///
/// @param doc    The document
/// @param name   For the ReferenceMark, Bookmark, TextSection. If the name is already in use, LibreOffice may change the name.
/// @param range  Cursor marking the location or range for the thing to be inserted.
/// @param absorb ReferenceMark, Bookmark and TextSection can incorporate a text range. If absorb is true, the text in the range becomes part of the thing. If absorb is false, the thing is inserted at the end of the range.
public record NamedTextContentPlacement(XTextDocument doc, String name, XTextRange range, boolean absorb) {
}
