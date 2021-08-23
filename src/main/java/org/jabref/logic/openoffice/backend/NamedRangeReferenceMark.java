package org.jabref.logic.openoffice.backend;

import java.util.Optional;

import org.jabref.model.openoffice.backend.NamedRange;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoCursor;
import org.jabref.model.openoffice.uno.UnoReferenceMark;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// was StorageBaseRefMark

class NamedRangeReferenceMark implements NamedRange {

    private static final String ZERO_WIDTH_SPACE = "\u200b";

    // for debugging we may want visible bracket
    private static final boolean
    REFERENCE_MARK_USE_INVISIBLE_BRACKETS = true; // !debug;

    public static final String
    REFERENCE_MARK_LEFT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : "<";

    public static final String
    REFERENCE_MARK_RIGHT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : ">";

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedRangeReferenceMark.class);

    private String rangeId; /* reference mark name */

    private NamedRangeReferenceMark(String rangeId) {
        this.rangeId = rangeId;
    }

    String getId() {
        return rangeId;
    }

    /**
     *  Insert {@code n} spaces in a way that reference marks just before or just after the cursor
     *  are not affected.
     *
     *  This is based on the observation, that starting two new paragraphs separates us from
     *  reference marks on either side.
     *
     *  The pattern used is:
     *  {@code safeInsertSpaces(n): para, para, left, space(n), right-delete, left(n), left-delete}
     *
     *  @param position Where to insert (at position.getStart())
     *  @param numSpaces  Number of spaces to insert.
     *
     *  @return a new cursor, covering the just-inserted spaces.
     *
     */
    private static XTextCursor safeInsertSpacesBetweenReferenceMarks(XTextRange position, int numSpaces) {
        // Start with an empty cursor at position.getStart();
        XText text = position.getText();
        XTextCursor cursor = text.createTextCursorByRange(position.getStart());
        text.insertString(cursor, "\r\r", false); // para, para
        cursor.goLeft((short) 1, false); // left
        text.insertString(cursor, " ".repeat(numSpaces), false); // space(numSpaces)
        cursor.goRight((short) 1, true);
        cursor.setString(""); // right-delete
        cursor.goLeft((short) numSpaces, false); // left(numSpaces)
        cursor.goLeft((short) 1, true);
        cursor.setString(""); // left-delete
        cursor.goRight((short) numSpaces, true); // select the newly inserted spaces
        return cursor;
    }

    private static void createReprInDocument(XTextDocument doc,
                                             String refMarkName,
                                             XTextCursor position,
                                             boolean insertSpaceAfter,
                                             boolean withoutBrackets)
        throws
        CreationException {

        // The cursor we received: we push it before us.
        position.collapseToEnd();

        XTextCursor cursor = safeInsertSpacesBetweenReferenceMarks(position.getEnd(), 2);

        // cursors before the first and after the last space
        XTextCursor cursorBefore = cursor.getText().createTextCursorByRange(cursor.getStart());
        XTextCursor cursorAfter = cursor.getText().createTextCursorByRange(cursor.getEnd());

        cursor.collapseToStart();
        cursor.goRight((short) 1, false);
        // now we are between two spaces

        final String left = NamedRangeReferenceMark.REFERENCE_MARK_LEFT_BRACKET;
        final String right = NamedRangeReferenceMark.REFERENCE_MARK_RIGHT_BRACKET;
        String bracketedContent = (withoutBrackets
                                   ? ""
                                   : left + right);

        cursor.getText().insertString(cursor, bracketedContent, true);

        UnoReferenceMark.create(doc, refMarkName, cursor, true /* absorb */);

        // eat the first inserted space
        cursorBefore.goRight((short) 1, true);
        cursorBefore.setString("");
        if (!insertSpaceAfter) {
            // eat the second inserted space
            cursorAfter.goLeft((short) 1, true);
            cursorAfter.setString("");
        }
    }

    static NamedRangeReferenceMark create(XTextDocument doc,
                                          String refMarkName,
                                          XTextCursor position,
                                          boolean insertSpaceAfter,
                                          boolean withoutBrackets)
        throws
        CreationException {

        createReprInDocument(doc, refMarkName, position, insertSpaceAfter, withoutBrackets);
        return new NamedRangeReferenceMark(refMarkName);
    }

    /**
     * @return Optional.empty if there is no corresponding range.
     */
    static Optional<NamedRangeReferenceMark> getFromDocument(XTextDocument doc, String refMarkName)
        throws
        NoDocumentException,
        WrappedTargetException {
        return (UnoReferenceMark.getAnchor(doc, refMarkName)
                .map(e -> new NamedRangeReferenceMark(refMarkName)));
    }

    /**
     * Remove it from the document.
     *
     * See: removeCitationGroups
     */
    @Override
    public void removeFromDocument(XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException {
        UnoReferenceMark.removeIfExists(doc, this.getRangeName());
    }

    @Override
    public String getRangeName() {
        return rangeId;
    }

    /**
     *
     * @return Optional.empty if the reference mark is missing.
     *
     * See: UnoReferenceMark.getAnchor
     */
    @Override
    public Optional<XTextRange> getMarkRange(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {
        String name = this.getRangeName();
        return UnoReferenceMark.getAnchor(doc, name);
    }

    /**
     * Cursor for the reference marks as is, not prepared for filling, but does not need
     * cleanFillCursor either.
     *
     * @return Optional.empty() if reference mark is missing from the document,
     *                          otherwise an XTextCursor for getMarkRange
     *
     * See: getRawCursorForCitationGroup
     */
    @Override
    public Optional<XTextCursor> getRawCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {

        String name = this.getRangeName();

        Optional<XTextContent> markAsTextContent = UnoReferenceMark.getAsTextContent(doc, name);

        if (markAsTextContent.isEmpty()) {
            LOGGER.warn("getRawCursor: markAsTextContent({}).isEmpty()", name);
        }

        Optional<XTextCursor> full = UnoCursor.getTextCursorOfTextContentAnchor(markAsTextContent.get());
        if (full.isEmpty()) {
            LOGGER.warn("getRawCursor: full.isEmpty()");
            return Optional.empty();
        }
        return full;
    }

    /**
     * See: getFillCursorForCitationGroup
     */
    @Override
    public XTextCursor getFillCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        String name = this.getRangeName();

        final boolean debugThisFun = false;

        XTextCursor full = null;
        String fullText = null;
        for (int i = 1; i <= 2; i++) {
            Optional<XTextContent> markAsTextContent = UnoReferenceMark.getAsTextContent(doc, name);

            if (markAsTextContent.isEmpty()) {
                String msg = String.format("getFillCursor: markAsTextContent(%s).isEmpty (attempt %d)", name, i);
                throw new IllegalStateException(msg);
            }

            full = UnoCursor.getTextCursorOfTextContentAnchor(markAsTextContent.get()).orElse(null);
            if (full == null) {
                String msg = String.format("getFillCursor: full == null (attempt %d)", i);
                throw new IllegalStateException(msg);
            }

            fullText = full.getString();

            LOGGER.debug("getFillCursor: fulltext = '{}'", fullText);

            if (fullText.length() >= 2) {
                LOGGER.debug("getFillCursor: (attempt: {}) fulltext.length() >= 2, break loop%n", i);
                break;
            } else {
                // (fullText.length() < 2)
                if (i == 2) {
                    String msg = String.format("getFillCursor: (fullText.length() < 2) (attempt %d)", i);
                    throw new IllegalStateException(msg);
                }
                // too short, recreate
                LOGGER.warn("getFillCursor: too short, recreate");

                full.setString("");
                UnoReferenceMark.removeIfExists(doc, name);

                final boolean insertSpaceAfter = false;
                final boolean withoutBrackets = false;
                createReprInDocument(doc, name, full, insertSpaceAfter, withoutBrackets);
            }
        }

        if (full == null) {
            throw new IllegalStateException("getFillCursorFor: full == null (after loop)");
        }
        if (fullText == null) {
            throw new IllegalStateException("getFillCursor: fullText == null (after loop)");
        }

        fullText = full.getString();
        if (fullText.length() < 2) {
            throw new IllegalStateException("getFillCursor: fullText.length() < 2 (after loop)'%n");
        }
        XTextCursor beta = full.getText().createTextCursorByRange(full);
        beta.collapseToStart();
        beta.goRight((short) 1, false);
        beta.goRight((short) (fullText.length() - 2), true);
        LOGGER.debug("getFillCursor: beta(1) covers '{}'", beta.getString());

        final String left = NamedRangeReferenceMark.REFERENCE_MARK_LEFT_BRACKET;
        final String right = NamedRangeReferenceMark.REFERENCE_MARK_RIGHT_BRACKET;
        final short rightLength = (short) right.length();
        if (fullText.startsWith(left) && fullText.endsWith(right)) {
            beta.setString("");
        } else {
            LOGGER.debug("getFillCursor: recreating brackets for '{}'", fullText);

            // we have at least two characters inside
            XTextCursor alpha = full.getText().createTextCursorByRange(full);
            alpha.collapseToStart();

            XTextCursor omega = full.getText().createTextCursorByRange(full);
            omega.collapseToEnd();

            // beta now covers everything except first and last character
            // Replace its content with brackets
            String paddingx = "x";
            String paddingy = "y";
            String paddingz = "z";
            beta.setString(paddingx + left + paddingy + right + paddingz);
            LOGGER.debug("getFillCursor: beta(2) covers '{}'", beta.getString());

            // move beta to before the right bracket
            beta.collapseToEnd();
            beta.goLeft((short) (rightLength + 1), false);
            // remove middle padding
            beta.goLeft((short) 1, true);
            LOGGER.debug("getFillCursor: beta(3) covers '{}'", beta.getString());

            // only drop paddingy later: beta.setString("");

            // drop the initial character and paddingx
            alpha.collapseToStart();
            alpha.goRight((short) (1 + 1), true);
            LOGGER.debug("getFillCursor: alpha(4) covers '{}'", alpha.getString());

            alpha.setString("");
            // drop the last character and paddingz
            omega.collapseToEnd();
            omega.goLeft((short) (1 + 1), true);
            LOGGER.debug("getFillCursor: omega(5) covers '{}'", omega.getString());

            omega.setString("");

            // drop paddingy now
            LOGGER.debug("getFillCursor: beta(6) covers '{}'", beta.getString());

            beta.setString("");
            // should be OK now.
            if (debugThisFun) {
                final short leftLength = (short) left.length();
                alpha.goRight(leftLength, true);
                LOGGER.debug("getFillCursor: alpha(7) covers '{}', should be '{}'", alpha.getString(), left);
                omega.goLeft(rightLength, true);
                LOGGER.debug("getFillCursor: omega(8) covers '{}', should be '{}'", omega.getString(), right);
            }
        }

        // NamedRangeReferenceMark.checkFillCursor(beta);
        return beta;
    }

    /*
     * Throw IllegalStateException if the brackets are not there.
     */
    public static void checkFillCursor(XTextCursor cursor) {
        final String left = REFERENCE_MARK_LEFT_BRACKET;

        XTextCursor alpha = cursor.getText().createTextCursorByRange(cursor);
        alpha.collapseToStart();

        XTextCursor omega = cursor.getText().createTextCursorByRange(cursor);
        omega.collapseToEnd();

        final short leftLength = (short) left.length();
        if (leftLength > 0) {
            alpha.goLeft(leftLength, true);
            if (!left.equals(alpha.getString())) {
                String msg = String.format("checkFillCursor:"
                                           + " ('%s') is not prefixed with REFERENCE_MARK_LEFT_BRACKET, has '%s'",
                                           cursor.getString(), alpha.getString());
                throw new IllegalStateException(msg);
            }
        }

        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short rightLength = (short) right.length();
        if (rightLength > 0) {
            omega.goRight(rightLength, true);
            if (!right.equals(omega.getString())) {
                String msg = String.format("checkFillCursor:"
                                           + " ('%s') is not followed by REFERENCE_MARK_RIGHT_BRACKET, has '%s'",
                                           cursor.getString(), omega.getString());
                throw new IllegalStateException(msg);
            }
        }
    }

    /**
     * Remove brackets, but if the result would become empty, leave them; if the result would be a
     * single characer, leave the left bracket.
     *
     * See: cleanFillCursorForCitationGroup
     */
    @Override
    public void cleanFillCursor(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {

        // alwaysRemoveBrackets : full compatibility with JabRef 5.2: brackets are temporary, only
        // exist between getFillCursor and cleanFillCursor.
        final boolean alwaysRemoveBrackets = false;

        // removeBracketsFromEmpty is intended to force removal if we are working on an "Empty citation" (INVISIBLE_CIT).
        final boolean removeBracketsFromEmpty = false;

        String name = this.getRangeName();

        XTextCursor full = this.getRawCursor(doc).orElseThrow(IllegalStateException::new);
        final String fullText = full.getString();

        final String left = REFERENCE_MARK_LEFT_BRACKET;
        if (!fullText.startsWith(left)) {
            String msg = String.format("cleanFillCursor: (%s) does not start with REFERENCE_MARK_LEFT_BRACKET", name);
            throw new IllegalStateException(msg);
        }

        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        if (!fullText.endsWith(right)) {
            String msg = String.format("cleanFillCursor: (%s) does not end with REFERENCE_MARK_RIGHT_BRACKET", name);
            throw new IllegalStateException(msg);
        }

        final int fullTextLength = fullText.length();
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();
        final int contentLength = (fullTextLength - (leftLength + rightLength));
        if (contentLength < 0) {
            String msg = String.format("cleanFillCursor: length(%s) < 0", name);
            throw new IllegalStateException(msg);
        }

        boolean removeRight = ((contentLength >= 1)
                               || ((contentLength == 0) && removeBracketsFromEmpty)
                               || alwaysRemoveBrackets);

        boolean removeLeft = ((contentLength >= 2)
                              || ((contentLength == 0) && removeBracketsFromEmpty)
                              || alwaysRemoveBrackets);

        if (removeRight) {
            XTextCursor omega = full.getText().createTextCursorByRange(full);
            omega.collapseToEnd();
            omega.goLeft(rightLength, true);
            omega.setString("");
        }

        if (removeLeft) {
            XTextCursor alpha = full.getText().createTextCursorByRange(full);
            alpha.collapseToStart();
            alpha.goRight(leftLength, true);
            alpha.setString("");
        }
    }
}
