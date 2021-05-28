package org.jabref.model.openoffice.rangesort;

import com.sun.star.text.XTextRange;

/**
 * This is what {@code visualSort} needs in its input.
 */
public interface RangeSortable<T> {

    /** The XTextRange
     *
     *  For citation marks in footnotes this may be the range of the
     *  footnote mark.
     */
    public XTextRange getRange();

    /**
     * For citation marks in footnotes this may provide order within
     * the footnote.
     */
    public int getIndexInPosition();

    public T getContent();
}
