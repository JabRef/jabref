package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import org.jabref.gui.openoffice.CitationSort;
import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class StorageBaseRefMark implements StorageBase.NamedRange {

    private static final String ZERO_WIDTH_SPACE = "\u200b";

    // for debugging we may want visible bracket
    private static final boolean
    REFERENCE_MARK_USE_INVISIBLE_BRACKETS = true; // !debug;

    public static final String
    REFERENCE_MARK_LEFT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : "<";

    public static final String
    REFERENCE_MARK_RIGHT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : ">";

    private static final Logger LOGGER =
        LoggerFactory.getLogger(StorageBaseRefMark.class);

    private String id; /* reference mark name */

    private StorageBaseRefMark( String id ) {
        this.id = id;
    }

    String getId(){ return id; }

    /**
     *  Insert {@code n} spaces in a way that reference
     *  marks just before or just after the cursor are not affected.
     *
     *  This is based on the observation, that starting two
     *  new paragraphs separates us from a reference mark on either side.
     *
     *  The pattern used is:
     *  {@code safeInsertSpaces(n): para, para, left, space(n), right-delete, left(n), left-delete}
     *
     *  @param position Where to insert (at position.getStart())
     *  @param n  Number of spaces to insert.
     *
     *  @return a new cursor, covering the just-inserted spaces.
     *
     *  This could be generalized to insert arbitrary text safely
     *  between two reference marks. But we do not need that now.
     */
    private static XTextCursor
    safeInsertSpacesBetweenReferenceMarks(XTextRange position, int n) {
        // Start with an empty cursor at position.getStart();
        XText text = position.getText();
        XTextCursor cursor = text.createTextCursorByRange(position.getStart());
        text.insertString(cursor, "\r\r", false); // para, para
        cursor.goLeft((short) 1, false); // left
        text.insertString(cursor, " ".repeat(n), false); // space(n)
        cursor.goRight((short) 1, true);
        cursor.setString(""); // right-delete
        cursor.goLeft((short) n, false); // left(n)
        cursor.goLeft((short) 1, true);
        cursor.setString(""); // left-delete
        cursor.goRight((short) n, true);
        return cursor;
    }

    private static void createReprInDocument(DocumentConnection documentConnection,
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

        final String left = StorageBaseRefMark.REFERENCE_MARK_LEFT_BRACKET;
        final String right = StorageBaseRefMark.REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();
        String bracketedContent = (withoutBrackets
                                   ? ""
                                   : left + right);

        cursor.getText().insertString(
            cursor,
            bracketedContent,
            true);

        documentConnection.insertReferenceMark(refMarkName,
                                               cursor,
                                               true /* absorb */);

        cursorBefore.goRight((short) 1, true);
        cursorBefore.setString("");
        if (!insertSpaceAfter) {
            cursorAfter.goLeft((short) 1, true);
            cursorAfter.setString("");
        }
    }

    private static StorageBaseRefMark create(DocumentConnection documentConnection,
                                            String refMarkName,
                                            XTextCursor position,
                                            boolean insertSpaceAfter,
                                            boolean withoutBrackets)
        throws
        CreationException {
        createReprInDocument(documentConnection,
                             refMarkName,
                             position,
                             insertSpaceAfter,
                             withoutBrackets);
        return new StorageBaseRefMark( refMarkName );
    }

    private static StorageBaseRefMark getFromDocumentOrNull(DocumentConnection documentConnection,
                                                            String refMarkName)
        throws
        NoDocumentException,
        WrappedTargetException {
        XTextRange r = documentConnection.getReferenceMarkRangeOrNull(refMarkName);
        if (r == null) {
            return null;
        } else {
            return new StorageBaseRefMark( refMarkName );
        }
    }

    /*
     * Remove it from the document.
     *
     * See: removeCitationGroups
     */
    @Override
    public void removeFromDocument(DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException {
        documentConnection.removeReferenceMark(this.getName());
    }


    @Override
    public String getName(){ return id; }

    /*
     * ranges controlled by citation groups should not overlap with each other.
     *
     * @return Null if the reference mark is missing.
     *
     * See: getReferenceMarkRangeOrNull
     */
    @Override
    public XTextRange getMarkRangeOrNull(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException {
        String name = this.getName();
        return documentConnection.getReferenceMarkRangeOrNull(name);
    }

    /**
     * Cursor for the reference marks as is, not prepared for filling,
     * but does not need cleanFillCursorForCitationGroup either.
     *
     * @return null if reference mark is missing from the document,
     *         otherwise an XTextCursor for getMarkRangeOrNull
     *
     * See: getRawCursorForCitationGroup
     */
    @Override
    public XTextCursor getRawCursor(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        String name = this.getName();
        XTextCursor full = null;

        XTextContent markAsTextContent =
            documentConnection.getReferenceMarkAsTextContentOrNull(name);

        if (markAsTextContent == null) {
            throw new RuntimeException(
                String.format(
                    "getRawCursor: markAsTextContent(%s) == null",
                    name));
        }

        full = DocumentConnection.getTextCursorOfTextContent(markAsTextContent);
        if (full == null) {
            throw new RuntimeException("getRawCursor: full == null");
        }
        return full;
    }

    /**
     * See: getFillCursorForCitationGroup
     */
    @Override
    public XTextCursor getFillCursor(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        String name = this.getName();

        final boolean debugThisFun = false;
        final String left = StorageBaseRefMark.REFERENCE_MARK_LEFT_BRACKET;
        final String right = StorageBaseRefMark.REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();

        XTextCursor full = null;
        String fullText = null;
        for (int i = 1; i <= 2; i++) {
            XTextContent markAsTextContent =
                documentConnection.getReferenceMarkAsTextContentOrNull(name);

            if (markAsTextContent == null) {
                throw new RuntimeException(
                    String.format("getFillCursor:"
                                  + " markAsTextContent(%s) == null (attempt %d)",
                                  name,
                                  i));
            }

            full = DocumentConnection.getTextCursorOfTextContent(markAsTextContent);
            if (full == null) {
                throw new RuntimeException(
                    String.format("getFillCursor: full == null (attempt %d)", i));
            }

            fullText = full.getString();

            if (debugThisFun) {
                System.out.printf("getFillCursor: fulltext = '%s'%n", fullText);
            }

            if (fullText.length() >= 2) {
                break;
            } else {
                // (fullText.length() < 2)
                if (i == 2) {
                    throw new RuntimeException(
                        String.format("getFillCursor:"
                                      + " (fullText.length() < 2) (attempt %d)",
                                      i));
                }
                // too short, recreate
                if (debugThisFun) {
                    System.out.println("getFillCursor: too short, recreate");
                }
                full.setString("");
                try {
                    documentConnection.removeReferenceMark(name);
                } catch (NoSuchElementException ex) {
                    LOGGER.warn(
                        String.format("getFillCursor got NoSuchElementException"
                                      + " for '%s'",
                                      name));
                }
                createReprInDocument(
                    documentConnection,
                    name,
                    full,
                    false, /* insertSpaceAfter */
                    false  /* withoutBrackets */);
            }
        }

        if (full == null) {
            throw new RuntimeException("getFillCursorFor: full == null (after loop)");
        }
        if (fullText == null) {
            throw new RuntimeException("getFillCursor: fullText == null (after loop)");
        }

        // we have at least two characters inside
        XTextCursor alpha = full.getText().createTextCursorByRange(full);
        alpha.collapseToStart();
        XTextCursor omega = full.getText().createTextCursorByRange(full);
        omega.collapseToEnd();

        XTextCursor beta = full.getText().createTextCursorByRange(full);
        beta.collapseToStart();
        beta.goRight((short) 1, false);
        beta.goRight((short) (fullText.length() - 2), true);
        beta.setString(left + right);
        beta.collapseToEnd();
        beta.goLeft(rightLength, false);
        // drop the initial character
        alpha.goRight((short) 1, true);
        alpha.setString("");
        // drop the last character
        omega.goLeft((short) 1, true);
        omega.setString("");
        return beta;
    }

    /**
     * Remove brackets, but if the result would become empty, leave
     * them; if the result would be a single characer, leave the left bracket.
     *
     * See: cleanFillCursorForCitationGroup
     */
    @Override
    public void cleanFillCursor(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        // alwaysRemoveBrackets : full compatibility with JabRef 5.2:
        // brackets are temporary, only exist between getFillCursor
        // and cleanFillCursor.
        final boolean alwaysRemoveBrackets = true;
        // removeBracketsFromEmpty is intended to force removal if we
        //       are working on an "Empty citation" (INVISIBLE_CIT).
        final boolean removeBracketsFromEmpty = false;

        final String left = REFERENCE_MARK_LEFT_BRACKET;
        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();

        // CitationGroupsV001 cgs = this;
        // String name = cgs.getReferenceMarkName(cgid).orElseThrow(RuntimeException::new);
        String name = this.getName();

        XTextCursor full = this.getRawCursor(documentConnection);
        final String fullText = full.getString();
        final int fullTextLength = fullText.length();

        XTextCursor alpha = full.getText().createTextCursorByRange(full);
        alpha.collapseToStart();

        XTextCursor beta = full.getText().createTextCursorByRange(full);
        beta.collapseToStart();
        beta.goRight(leftLength, false);

        XTextCursor omega = full.getText().createTextCursorByRange(full);
        omega.collapseToEnd();

        if (!fullText.startsWith(left)) {
            throw new RuntimeException(
                String.format("cleanFillCursor:"
                              + " (%s) does not start with REFERENCE_MARK_LEFT_BRACKET",
                              name));
        }

        if (!fullText.endsWith(right)) {
            throw new RuntimeException(
                String.format("cleanFillCursor:"
                              + " (%s) does not end with REFERENCE_MARK_RIGHT_BRACKET",
                              name));
        }

        final int contentLength = (fullTextLength - (leftLength + rightLength));
        if (contentLength < 0) {
            throw new RuntimeException(
                String.format("cleanFillCursor: length(%s) < 0",
                              name));
        }

        boolean removeRight = ((contentLength >= 1)
                               || ((contentLength == 0) && removeBracketsFromEmpty)
                               || alwaysRemoveBrackets);

        boolean removeLeft = ((contentLength >= 2)
                              || ((contentLength == 0) && removeBracketsFromEmpty)
                              || alwaysRemoveBrackets);

        if (removeRight) {
            omega.goLeft(rightLength, true);
            omega.setString("");
        }

        if (removeLeft) {
            alpha.goRight(leftLength, true);
            alpha.setString("");
        }
    }

    private static List<String> getUsedNames(DocumentConnection documentConnection)
        throws
        NoDocumentException {
        return documentConnection.getReferenceMarkNames();
    }

    public static class Manager implements StorageBase.NamedRangeManager {
        @Override
        public StorageBase.NamedRange create( DocumentConnection documentConnection,
                                              String refMarkName,
                                              XTextCursor position,
                                              boolean insertSpaceAfter,
                                              boolean withoutBrackets )
            throws
            CreationException {
            return StorageBaseRefMark.create(documentConnection,
                                             refMarkName,
                                             position,
                                             insertSpaceAfter,
                                             withoutBrackets);
        }

        @Override
        public List<String> getUsedNames(DocumentConnection documentConnection)
            throws
            NoDocumentException {
            return StorageBaseRefMark.getUsedNames(documentConnection);
        }

        @Override
        public StorageBase.NamedRange getFromDocumentOrNull(DocumentConnection documentConnection,
                                                            String refMarkName)
            throws
            NoDocumentException,
            WrappedTargetException {
            return StorageBaseRefMark.getFromDocumentOrNull(documentConnection, refMarkName);
        }
    }
}
