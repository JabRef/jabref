package org.jabref.logic.openoffice.backend;

import java.util.Optional;

import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.DocumentAnnotation;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoCast;
import org.jabref.logic.openoffice.uno.UnoCursor;
import org.jabref.logic.openoffice.uno.UnoDispatch;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// One Writer reference mark, with an explicit policy for safe text replacement.
@NullMarked
public final class WriterReferenceMark {

    private static final String BOUNDARY = "\u200b";
    private static final Logger LOGGER = LoggerFactory.getLogger(WriterReferenceMark.class);

    /// Persistent boundaries remain around rendered text. Temporary boundaries are removed only
    /// when at least two characters remain, so a cursor can always be placed unambiguously inside.
    public enum BoundaryPolicy {
        PERSISTENT,
        TEMPORARY
    }

    private final String rangeName;
    private final BoundaryPolicy boundaryPolicy;

    WriterReferenceMark(String rangeName, BoundaryPolicy boundaryPolicy) {
        this.rangeName = rangeName;
        this.boundaryPolicy = boundaryPolicy;
    }

    /// Insert spaces without extending reference marks on either side of the insertion point.
    /// Two new paragraphs separate the cursor from adjacent marks before the spaces are inserted.
    public static XTextCursor safeInsertSpacesBetweenReferenceMarks(XTextRange position, int numSpaces) {
        XText text = position.getText();
        XTextCursor cursor = text.createTextCursorByRange(position.getStart());
        text.insertString(cursor, "\r\r", false);
        cursor.goLeft((short) 1, false);
        text.insertString(cursor, " ".repeat(numSpaces), false);
        cursor.goRight((short) 1, true);
        cursor.setString("");
        cursor.goLeft((short) numSpaces, false);
        cursor.goLeft((short) 1, true);
        cursor.setString("");
        cursor.goRight((short) numSpaces, true);
        return cursor;
    }

    void createInDocument(XTextDocument doc, Optional<XComponentContext> context, XTextCursor position,
                          boolean insertSpaceBefore, boolean insertSpaceAfter) throws CreationException {
        position.collapseToEnd();
        XTextCursor cursor = safeInsertSpacesBetweenReferenceMarks(position.getEnd(), 2);
        XTextCursor cursorBefore = cursor.getText().createTextCursorByRange(cursor.getStart());
        XTextCursor cursorAfter = cursor.getText().createTextCursorByRange(cursor.getEnd());
        cursor.collapseToStart();
        cursor.goRight((short) 1, false);
        cursor.getText().insertString(cursor, BOUNDARY + BOUNDARY, true);
        XTextRange endRange = cursor.getEnd();
        WriterReferenceMarks.create(new DocumentAnnotation(doc, rangeName, cursor, true));

        if (!insertSpaceBefore) {
            cursorBefore.goRight((short) 1, true);
            cursorBefore.setString("");
        }
        if (!insertSpaceAfter) {
            cursorAfter.goLeft((short) 1, true);
            cursorAfter.setString("");
        }
        position.gotoRange(cursorAfter.getEnd(), false);
        if (!insertSpaceAfter && context.isPresent()) {
            UnoDispatch.resetAttributesAtRangeEnd(doc, context.orElseThrow(), endRange);
        }
    }

    private static Optional<XComponentContext> findComponentContext(XTextDocument doc) {
        Optional<XPropertySet> properties = UnoCast.cast(XMultiServiceFactory.class, doc)
                                                   .flatMap(factory -> UnoCast.cast(XPropertySet.class, factory));
        if (properties.isEmpty()) {
            return Optional.empty();
        }
        try {
            return UnoCast.cast(XComponentContext.class, properties.orElseThrow().getPropertyValue("DefaultContext"));
        } catch (UnknownPropertyException | WrappedTargetException exception) {
            LOGGER.debug("Could not resolve LibreOffice component context from document", exception);
            return Optional.empty();
        }
    }

    public String getRangeName() {
        return rangeName;
    }

    public void removeFromDocument(XTextDocument doc) throws WrappedTargetException, NoDocumentException {
        WriterReferenceMarks.removeIfExists(doc, rangeName);
    }

    /// Returns an empty optional when the mark has disappeared from the document.
    public Optional<XTextRange> getMarkRange(XTextDocument doc) throws NoDocumentException, WrappedTargetException {
        return WriterReferenceMarks.getAnchor(doc, rangeName);
    }

    /// The mark's full cursor, without preparing it for filling or requiring cleanup.
    public Optional<XTextCursor> getRawCursor(XTextDocument doc) throws NoDocumentException, WrappedTargetException {
        return WriterReferenceMarks.getAsTextContent(doc, rangeName)
                                   .flatMap(UnoCursor::getTextCursorOfTextContentAnchor);
    }

    private XTextCursor requireRawCursor(XTextDocument doc) throws NoDocumentException, WrappedTargetException {
        return getRawCursor(doc).orElseThrow(() -> new NoDocumentException("Missing reference mark cursor: " + rangeName));
    }

    /// Obtain a cursor strictly inside the mark. Must be followed by [#cleanFillCursor].
    public XTextCursor getFillCursor(XTextDocument doc) throws NoDocumentException, WrappedTargetException, CreationException {
        XTextCursor full = requireRawCursor(doc);
        String fullText = full.getString();
        if (boundaryPolicy == BoundaryPolicy.PERSISTENT) {
            requireBoundaries(fullText);
        } else if (fullText.length() < 2) {
            // Native Zotero marks can be empty or one character long. Recreate them so both
            // boundary characters are inside the mark, not at an ambiguous insertion edge.
            full.setString("");
            WriterReferenceMarks.removeIfExists(doc, rangeName);
            createInDocument(doc, findComponentContext(doc), full, false, false);
            full = requireRawCursor(doc);
            fullText = full.getString();
            if (fullText.length() < 2) {
                throw new CreationException("Reference mark is too short after recreation: " + rangeName);
            }
        }

        XTextCursor innerEnd = full.getText().createTextCursorByRange(full.getEnd());
        innerEnd.goLeft((short) 1, false);
        XTextCursor inner = full.getText().createTextCursorByRange(full.getStart());
        inner.goRight((short) 1, false);
        inner.gotoRange(innerEnd, true);
        if (fullText.startsWith(BOUNDARY) && fullText.endsWith(BOUNDARY)) {
            inner.setString("");
            return inner;
        }

        XTextCursor start = full.getText().createTextCursorByRange(full.getStart());
        XTextCursor end = full.getText().createTextCursorByRange(full.getEnd());
        // Keep padding on both sides while replacing the original first and last characters:
        // writing directly at either edge can put the replacement outside the reference mark.
        inner.setString("x" + BOUNDARY + "y" + BOUNDARY + "z");
        inner.collapseToEnd();
        inner.goLeft((short) 2, false);
        inner.goLeft((short) 1, true);
        start.goRight((short) 2, true);
        start.setString("");
        end.goLeft((short) 2, true);
        end.setString("");
        inner.setString("");
        return inner;
    }

    private void requireBoundaries(String text) throws CreationException {
        if (text.length() < 2 || !text.startsWith(BOUNDARY) || !text.endsWith(BOUNDARY)) {
            throw new CreationException("Reference mark has lost its boundary characters: " + rangeName);
        }
    }

    /// Remove temporary boundaries only when safe: keep both for empty content, and the left
    /// boundary for one character. Persistent boundaries are never removed.
    public void cleanFillCursor(XTextDocument doc) throws NoDocumentException, WrappedTargetException {
        if (boundaryPolicy == BoundaryPolicy.PERSISTENT) {
            return;
        }
        XTextCursor full = requireRawCursor(doc);
        String fullText = full.getString();
        if (fullText.length() < 2 || !fullText.startsWith(BOUNDARY) || !fullText.endsWith(BOUNDARY)) {
            throw new NoDocumentException("Reference mark has lost its boundary characters: " + rangeName);
        }
        int contentLength = fullText.length() - 2;
        if (contentLength >= 1) {
            XTextCursor end = full.getText().createTextCursorByRange(full.getEnd());
            end.goLeft((short) 1, true);
            end.setString("");
        }
        if (contentLength >= 2) {
            XTextCursor start = full.getText().createTextCursorByRange(full.getStart());
            start.goRight((short) 1, true);
            start.setString("");
        }
    }
}
