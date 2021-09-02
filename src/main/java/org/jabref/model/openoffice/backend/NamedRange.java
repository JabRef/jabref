package org.jabref.model.openoffice.backend;

import java.util.Optional;

import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

/**
 * NamedRange (with NamedRangeManager) attempts to provide a common interface for working with
 * reference mark based and bookmark based text ranges to be used as locations to fill with citation
 * markers. LibreOffice supports name-based lookup and listing names for both (hence the name).
 *
 * Note: currently only implemented for refence marks (in NamedRangeReferenceMark and
 *       NamedRangeManagerReferenceMark). 
 * 
 */
public interface NamedRange {

    String getRangeName();

    /**
     * @return Optional.empty if the mark is missing from the document.
     */
    Optional<XTextRange> getMarkRange(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException;

    /**
     * Cursor for the reference marks as is, not prepared for filling, but does not need
     * cleanFillCursor either.
     */
    Optional<XTextCursor> getRawCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException;

    /**
     * Get a cursor for filling in text.
     *
     * Must be followed by cleanFillCursor()
     */
    XTextCursor getFillCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException;

    /**
     * Remove brackets, but if the result would become empty, leave them; if the result would be a
     * single character, leave the left bracket.
     *
     */
    void cleanFillCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException;

    /**
     *  Note: create is in NamedRangeManager
     */
    void removeFromDocument(XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException;
}
