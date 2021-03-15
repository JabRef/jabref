package org.jabref.gui.openoffice;

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
 * To be able to refer back to more extended data, we might need
 * to identify its {@code kind}, and index ({@code i}) in the
 * corresponding tables.
 *
 */
class RangeForOverlapCheck {
    final static int REFERENCE_MARK_KIND = 0;
    final static int FOOTNOTE_MARK_KIND = 1;

    XTextRange range;
    int kind;
    int i;
    String description;

    RangeForOverlapCheck(XTextRange range, int i, int kind, String description) {
        this.range = range;
        this.kind = kind;
        this.i = i;
        this.description = description;
    }

    String format() {
        return description;
    }
}
