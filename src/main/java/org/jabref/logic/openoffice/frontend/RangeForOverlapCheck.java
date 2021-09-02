package org.jabref.logic.openoffice.frontend;

import org.jabref.model.openoffice.rangesort.RangeHolder;

import com.sun.star.text.XTextRange;

/**
 * Describe a protected range for overlap checking and reporting.
 *
 * To check that our protected ranges do not overlap, we collect
 * these ranges. To check for overlaps between these, we need the
 * {@code range} itself. To report the results of overlap
 * checking, we need a {@code description} that can be understood
 * by the user.
 *
 * To be able to refer back to more extended data, we might need to
 * identify its {@code kind}, and its index in the corresponding
 * tables or other identifier within its kind ({@code idWithinKind})
 *
 */
public class RangeForOverlapCheck<T> implements RangeHolder {

    public final static int REFERENCE_MARK_KIND = 0;
    public final static int FOOTNOTE_MARK_KIND = 1;
    public final static int CURSOR_MARK_KIND = 2;
    public final static int BIBLIOGRAPHY_MARK_KIND = 3;

    public final XTextRange range;

    public final int kind;
    public final T idWithinKind;
    private final String description;

    public RangeForOverlapCheck(XTextRange range, T idWithinKind, int kind, String description) {
        this.range = range;
        this.kind = kind;
        this.idWithinKind = idWithinKind;
        this.description = description;
    }

    public String format() {
        return description;
    }

    @Override
    public XTextRange getRange() {
        return range;
    }
}
