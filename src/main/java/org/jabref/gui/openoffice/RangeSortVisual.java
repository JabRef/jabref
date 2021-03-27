package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.star.awt.Point;
import com.sun.star.awt.Selection;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.util.InvalidStateException;

import org.jabref.logic.JabRefException;
// import org.jabref.model.database.BibDatabase;
// import org.jabref.model.entry.BibEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sort XTextRange values visually (top-down,left-to-right).
 *
 * Requires functional XTextViewCursor.
 *
 * Problem: for multicolumn layout and view pages side-by-side mode of
 *          LO, the (top-down,left-to-right) order interpreted
 *          as-on-the-screen: an XTextRange at the top of the second
 *          column or second page is sorted before one at the bottom
 *          of the first column of the first page.
 *
 */
class RangeSortVisual {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(RangeSortVisual.class);

    /* first appearance order, based on visual order */

    /**
     *  Given a location, return its position: coordinates relative to
     *  the top left position of the first page of the document.
     *
     * Note: for text layouts with two or more columns, this gives the
     *       wrong order: top-down/left-to-right does not match
     *       reading order.
     *
     * Note: The "relative to the top left position of the first page"
     *       is meant "as it appears on the screen".
     *
     *       In particular: when viewing pages side-by-side, the top
     *       half of the right page is higher than the lower half of
     *       the left page. Again, top-down/left-to-right does not
     *       match reading order.
     *
     * @param range  Location.
     * @param cursor To get the position, we need az XTextViewCursor.
     *               It will be moved to the range.
     */
    private static Point
    findPositionOfTextRange(XTextRange range, XTextViewCursor cursor) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
    }

    /**
     * A reference mark name paired with its visual position.
     *
     * Comparison is based on (Y,X,indexInPosition): vertical compared
     * first, horizontal second, indexInPosition third.
     *
     * Used for sorting reference marks by their visual positions.
     *
     *
     *
     */
    private static class ComparableMark<T> implements Comparable<ComparableMark<T>> {

        private final Point position;
        private final int indexInPosition;
        private final T content;

        public ComparableMark(Point position, int indexInPosition, T content) {
            this.position = position;
            this.indexInPosition = indexInPosition;
            this.content = content;
        }

        @Override
        public int compareTo(ComparableMark other) {

            if (position.Y != other.position.Y) {
                return position.Y - other.position.Y;
            }
            if (position.X != other.position.X) {
                return position.X - other.position.X;
            }
            return indexInPosition - other.indexInPosition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o instanceof ComparableMark) {
                ComparableMark other = (ComparableMark) o;
                return ((this.position.X == other.position.X)
                        && (this.position.Y == other.position.Y)
                        && (this.indexInPosition == other.indexInPosition)
                        && Objects.equals(this.content, other.content)
                    );
            }
            return false;
        }

        public T getContent() {
            return content;
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, indexInPosition, content);
        }
    }

    /**
     * This is what {@code visualSort} needs in its input.
     */
    public interface VisualSortable<T> {
        public XTextRange getRange();
        public int getIndexInPosition();
        public T getContent();
    }

    /**
     * A simple implementation of {@code VisualSortable}
     */
    public static class VisualSortEntry<T> implements VisualSortable<T> {
        public XTextRange range;
        public int indexInPosition;
        public T content;

        VisualSortEntry(
            XTextRange range,
            int indexInPosition,
            T content
            ) {
            this.range = range;
            this.indexInPosition = indexInPosition;
            this.content = content;
        }

        @Override
        public XTextRange getRange() {
            return range;
        }

        @Override
        public int getIndexInPosition() {
            return indexInPosition;
        }

        @Override
        public T getContent() {
            return content;
        }
    }

    /**
     * Sort its input {@code vses} visually.
     *
     * Requires a functional {@code XTextViewCursor}.
     * In case the user selected a frame, it can get one,
     * but not if the cursor is in a comment. In the latter case
     * it throws a JabRefException asking the user to move the cursor.
     *
     * @param messageOnFailureToObtainAFunctionalXTextViewCursor
     *        A localized message to the user, asking to move the cursor
     *        into the document.
     *
     * @return The input, sorted by the elements XTextRange and
     *          getIndexInPosition.
     */
    public static <T> List<VisualSortable<T>>
    visualSort(List<VisualSortable<T>> vses,
               DocumentConnection documentConnection,
               String messageOnFailureToObtainAFunctionalXTextViewCursor)
        throws
        WrappedTargetException,
        NoDocumentException,
        JabRefException {

        final int inputSize = vses.size();

        // if ( messageOnFailureToObtainAFunctionalXTextViewCursor == null ) {
        //     messageOnFailureToObtainAFunctionalXTextViewCursor =
        //         Localization.lang("Please move the cursor into the document text.")
        //         + "\n"
        //         + Localization.lang("To get the visual positions of your citations"
        //                             + " I need to move the cursor around,"
        //                             + " but could not get it.");
        // }

        if (documentConnection.hasControllersLocked()) {
            LOGGER.warn(
                "visualSort:"
                + " with ControllersLocked, viewCursor.gotoRange"
                + " is probably useless"
                );
        }

        /*
         * A problem with XTextViewCursor: if it is not in text,
         * then we get a crippled version that does not support
         * viewCursor.getStart() or viewCursor.gotoRange(range,false),
         * and will throw an exception instead.
         *
         * Our hope is that either we can move the cursor with a
         * page- or scrolling-based method that does not throw, (see
         * https://docs.libreoffice.org/sw/html/unotxvw_8cxx_source.html#l00896
         * ) or that we can manipulate the cursor via getSelection and
         * select (of XSelectionSupplier).
         *
         * Here we implemented the second, selection-based method.
         * Seems to work when the user selected a frame or image.
         * In these cases restoring the selection works, too.
         *
         * Still, we have a problem when the cursor is in a comment
         * (referred to as "annotation" in OO API): in this case
         * initialSelection is null, and documentConnection.select()
         * does not help to get a function viewCursor. Having no
         * better idea, we ask the user to move the cursor into the
         * document.
         */

        /*
         *  Selection-based
         */
        final boolean debugThisFun = false;

        XServiceInfo initialSelection =  documentConnection.getSelectionAsServiceInfo();

        if (initialSelection != null) {
            if (Arrays.stream(initialSelection.getSupportedServiceNames())
                .anyMatch("com.sun.star.text.TextRanges"::equals)) {
                // we are probably OK with the viewCursor
                if (debugThisFun) {
                    LOGGER.info("visualSort: initialSelection supports TextRanges: no action needed.");
                }
            } else {
                if (debugThisFun) {
                    LOGGER.info("visualSort: initialSelection does not support TextRanges."
                                + " We need to change the viewCursor.");
                }
                XTextRange newSelection = documentConnection.xText.getStart();
                documentConnection.select( newSelection );
            }
        } else {
            if (debugThisFun) {
                LOGGER.info("visualSort: initialSelection is null: no idea what to do.");
            }
            /*
             * XTextRange newSelection = documentConnection.xText.getStart();
             * boolean res = documentConnection.select( newSelection );
             * XServiceInfo sel2 =  documentConnection.getSelectionAsServiceInfo();
             * LOGGER.info(
             * String.format("visualSort: initialSelection is null: result of select: %s, isNull: %s%n",
             *                   res,
             *                   sel2 == null));
             * // ^^^ prints true, true
             */
        }


        XTextViewCursor viewCursor = documentConnection.getViewCursor();
        Objects.requireNonNull(viewCursor);
        try {
            viewCursor.getStart();
        } catch (com.sun.star.uno.RuntimeException ex) {
            throw new JabRefException(
                messageOnFailureToObtainAFunctionalXTextViewCursor,
                ex);
        }

        // find coordinates
        List<Point> positions = new ArrayList<>(vses.size());

        for (VisualSortable<T> v : vses) {
            positions.add(findPositionOfTextRange(v.getRange(),
                                                  viewCursor));
        }

        /*
         * Restore initial state of selection (and thus viewCursor)
         */
        if (initialSelection != null) {
            documentConnection.select(initialSelection);
        }

        if ( positions.size() != inputSize ) {
            throw new RuntimeException("visualSort: positions.size() != inputSize");
        }

        // order by position
        Set<ComparableMark<VisualSortable<T>>> set = new TreeSet<>();
        for (int i = 0; i < vses.size(); i++) {
            set.add(
                new ComparableMark<>(
                    positions.get(i),
                    vses.get(i).getIndexInPosition(),
                    vses.get(i)
                    )
                );
        }

        if ( set.size() != inputSize ) {
            throw new RuntimeException("visualSort: set.size() != inputSize");
        }

        // collect CitationGroupIDs in order
        List<VisualSortable<T>> result = new ArrayList<>(set.size());
        for (ComparableMark<VisualSortable<T>> mark : set) {
            result.add(mark.getContent());
        }

        if ( result.size() != inputSize ) {
            throw new RuntimeException("visualSort: result.size() != inputSize");
        }

        return result;
    }

}
