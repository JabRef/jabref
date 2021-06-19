package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoScreenRefresh;

import com.sun.star.awt.Point;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sort XTextRange values visually (top-down,left-to-right).
 *
 * Requires functional XTextViewCursor.
 *
 * Problem: for multicolumn layout and when viewing pages side-by-side in LO, the
 *          (top-down,left-to-right) order interpreted as-on-the-screen: an XTextRange at the top of
 *          the second column or second page is sorted before an XTextRange at the bottom of the
 *          first column of the first page.
 */
public class RangeSortVisual {

    private static final Logger LOGGER = LoggerFactory.getLogger(RangeSortVisual.class);

    /**
     * Sort the input {@code inputs} visually.
     *
     * Requires a functional {@code XTextViewCursor}.
     *
     * @return The input, sorted by the elements XTextRange and getIndexInPosition.
     */
    public static <T> List<RangeSortable<T>> visualSort(List<RangeSortable<T>> inputs,
                                                        XTextDocument doc,
                                                        FunctionalTextViewCursor fcursor)
        throws
        WrappedTargetException,
        NoDocumentException {

        final int inputSize = inputs.size();

        if (UnoScreenRefresh.hasControllersLocked(doc)) {
            final String msg = "visualSort: with ControllersLocked, viewCursor.gotoRange is probably useless";
            LOGGER.warn(msg);
            throw new IllegalStateException(msg);
        }

        XTextViewCursor viewCursor = fcursor.getViewCursor();

        // find coordinates
        List<Point> positions = new ArrayList<>(inputSize);
        for (RangeSortable<T> v : inputs) {
            positions.add(findPositionOfTextRange(v.getRange(), viewCursor));
        }
        fcursor.restore(doc);

        if (positions.size() != inputSize) {
            throw new IllegalStateException("visualSort: positions.size() != inputSize");
        }

        // order by position
        ArrayList<ComparableMark<RangeSortable<T>>> set = new ArrayList<>(inputSize);
        for (int i = 0; i < inputSize; i++) {
            RangeSortable<T> input = inputs.get(i);
            set.add(new ComparableMark<>(positions.get(i),
                                         input.getIndexInPosition(),
                                         input));
        }
        Collections.sort(set);

        if (set.size() != inputSize) {
            throw new IllegalStateException("visualSort: set.size() != inputSize");
        }

        // collect ordered result
        List<RangeSortable<T>> result = new ArrayList<>(set.size());
        for (ComparableMark<RangeSortable<T>> mark : set) {
            result.add(mark.getContent());
        }

        if (result.size() != inputSize) {
            throw new IllegalStateException("visualSort: result.size() != inputSize");
        }

        return result;
    }

    /**
     *  Given a location, return its position: coordinates relative to the top left position of the
     *  first page of the document.
     *
     * Note: for text layouts with two or more columns, this gives the wrong order:
     *       top-down/left-to-right does not match reading order.
     *
     * Note: The "relative to the top left position of the first page" is meant "as it appears on
     *       the screen".
     *
     *       In particular: when viewing pages side-by-side, the top half of the right page is
     *       higher than the lower half of the left page. Again, top-down/left-to-right does not
     *       match reading order.
     *
     * @param range  Location.
     * @param cursor To get the position, we need az XTextViewCursor.
     *               It will be moved to the range.
     */
    private static Point findPositionOfTextRange(XTextRange range, XTextViewCursor cursor) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
    }

    /**
     * A reference mark name paired with its visual position.
     *
     * Comparison is based on (Y,X,indexInPosition): vertical compared first, horizontal second,
     * indexInPosition third.
     *
     * Used for sorting reference marks by their visual positions.
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
                        && Objects.equals(this.content, other.content));
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

}
