package org.jabref.model.openoffice.backend;

import java.util.Optional;

import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public interface NamedRange {

    public String nrGetRangeName();

    /**
     * @return Optional.empty if the mark is missing from the document.
     */
    public Optional<XTextRange> nrGetMarkRange(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException;

    /**
     * Cursor for the reference marks as is, not prepared for filling, but does not need
     * nrCleanFillCursor either.
     */
    public Optional<XTextCursor> nrGetRawCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException;

    /**
     * Get a cursor for filling in text.
     *
     * Must be followed by nrCleanFillCursor()
     */
    public XTextCursor nrGetFillCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException;

    /**
     * Remove brackets, but if the result would become empty, leave them; if the result would be a
     * single characer, leave the left bracket.
     *
     */
    public void nrCleanFillCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException;

    /**
     *  Note: create is in NamedRangeManager
     */
    public void nrRemoveFromDocument(XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException;
}
