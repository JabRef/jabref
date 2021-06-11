package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.text.XFootnote;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

public class UnoTextRange {

    private UnoTextRange() { }

    /**
     *  If original is in a footnote, return a range containing
     *  the corresponding footnote marker.
     *
     *  Returns Optional.empty if not in a footnote.
     */
    public static Optional<XTextRange> getFootnoteMarkRange(XTextRange original) {
        Optional<XFootnote> footer = UnoCast.cast(XFootnote.class, original.getText());
        if (footer.isPresent()) {
            // If we are inside a footnote,
            // find the linking footnote marker:
            // The footnote's anchor gives the correct position in the text:
            return Optional.ofNullable(footer.get().getAnchor());
        }
        return Optional.empty();
    }

    /**
     * Test if two XTextRange values are comparable (i.e. they share
     * the same getText()).
     */
    public static boolean comparables(XTextRange a, XTextRange b) {
        return a.getText() == b.getText();
    }

    /**
     * @return follows java conventions
     *
     * 1 if  (a &gt; b); (-1) if (a &lt; b)
     */
    public static int compareStartsUnsafe(XTextRangeCompare compare, XTextRange a, XTextRange b) {
        return (-1) * compare.compareRegionStarts(a, b);
    }

    public static int compareStarts(XTextRange a, XTextRange b) {
        if (!comparables(a, b)) {
            throw new java.lang.IllegalArgumentException("compareStarts: got incomparable regions");
        }
        final XTextRangeCompare compare = UnoCast.cast(XTextRangeCompare.class, a.getText()).get();
        return compareStartsUnsafe(compare, a, b);
    }

    /**
     * @return follows java conventions
     *
     * 1 if  (a &gt; b); (-1) if (a &lt; b)
     */
    public static int compareEnds(XTextRange a, XTextRange b) {
        if (!comparables(a, b)) {
            throw new java.lang.IllegalArgumentException("compareEnds: got incomparable regions");
        }
        final XTextRangeCompare compare = UnoCast.cast(XTextRangeCompare.class, a.getText()).get();
        return (-1) * compare.compareRegionEnds(a, b);
    }

    /*
     * Assumes a and b belong to the same XText as compare.
     */
    public static int compareStartsThenEndsUnsafe(XTextRangeCompare compare, XTextRange a, XTextRange b) {
        int res = compare.compareRegionStarts(a, b);
        if (res != 0) {
            return (-1) * res;
        }
        return (-1) * compare.compareRegionEnds(a, b);
    }

    public static int compareStartsThenEnds(XTextRange a, XTextRange b) {
        if (!comparables(a, b)) {
            throw new java.lang.IllegalArgumentException("compareStartsThenEnds: got incomparable regions");
        }
        final XTextRangeCompare compare = UnoCast.cast(XTextRangeCompare.class, a.getText()).get();
        return compareStartsThenEndsUnsafe(compare, a, b);
    }
}
