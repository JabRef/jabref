package org.jabref.model.openoffice;

import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

/**
 * Represents a document annotation.
 *
 * @param doc    The document
 * @param name   For the ReferenceMark, Bookmark, TextSection. If the name is already in use, LibreOffice may change the name.
 * @param range  Cursor marking the location or range for the thing to be inserted.
 * @param absorb ReferenceMark, Bookmark and TextSection can incorporate a text range. If absorb is true, the text in the range becomes part of the thing. If absorb is false, the thing is inserted at the end of the range.
 */
public record DocumentAnnotation(XTextDocument doc, String name, XTextRange range, boolean absorb) {
}
