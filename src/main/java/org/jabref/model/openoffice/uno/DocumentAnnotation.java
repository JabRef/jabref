package org.jabref.model.openoffice.uno;

import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

/**
 * Represents a document annotation.
 *
 * @param doc    The document
 * @param name   For the bookmark.
 * @param range  Cursor marking the location or range for the bookmark.
 * @param absorb whether to incorporate the range
 */
public record DocumentAnnotation(XTextDocument doc, String name, XTextRange range, boolean absorb) {
}
