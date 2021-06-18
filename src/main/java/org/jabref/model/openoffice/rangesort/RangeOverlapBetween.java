package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.openoffice.uno.UnoCast;
import org.jabref.model.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.util.OOTuple3;

import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

public class RangeOverlapBetween {

    private RangeOverlapBetween() { }

    /**
     * Check for any overlap between two sets of XTextRange values.
     *
     * Assume fewHolders is small (usually a single element, for checking the cursor)
     *
     * Returns on first problem found.
     */
    public static <V extends RangeHolder>
    List<RangeOverlap<V>> findFirst(XTextDocument doc,
                                    List<V> fewHolders,
                                    List<V> manyHolders,
                                    boolean includeTouching) {

        List<RangeOverlap<V>> result = new ArrayList<>();

        if (fewHolders.size() == 0) {
            return result;
        }

        List<OOTuple3<XText, XTextRangeCompare, V>> fewTuples =
            new ArrayList<>(fewHolders.size());

            for (V aHolder : fewHolders) {
                XText aText = aHolder.getRange().getText();
                fewTuples.add(new OOTuple3<>(aText,
                                             UnoCast.cast(XTextRangeCompare.class, aText).get(),
                                             aHolder));
        }

        for (V bHolder : manyHolders) {
            XTextRange bRange = bHolder.getRange();
            XText bText = bRange.getText();
            XTextRange bRangeStart = bRange.getStart();
            XTextRange bRangeEnd = bRange.getEnd();

            for (OOTuple3<XText, XTextRangeCompare, V> tup : fewTuples) {
                XText aText = tup.a;
                XTextRangeCompare cmp = tup.b;
                V aHolder = tup.c;
                XTextRange aRange = aHolder.getRange();
                if (aText != bText) {
                    continue;
                }
                int abEndToStart = -1 * cmp.compareRegionStarts(aRange.getEnd(), bRangeStart);
                if (abEndToStart < 0 || (!includeTouching && (abEndToStart == 0))) {
                    continue;
                }
                int baEndToStart = -1 * cmp.compareRegionStarts(bRangeEnd, aRange.getStart());
                if (baEndToStart < 0 || (!includeTouching && (baEndToStart == 0))) {
                    continue;
                }

                boolean equal = UnoTextRange.compareStartsThenEnds(aRange, bRange) == 0;
                boolean touching = (abEndToStart == 0 || baEndToStart == 0);

                // In case of two equal collapsed ranges there is an ambiguity : TOUCH or
                // EQUAL_RANGE ?
                //
                // We return EQUAL_RANGE
                RangeOverlapKind kind = (equal ? RangeOverlapKind.EQUAL_RANGE
                                         : (touching ? RangeOverlapKind.TOUCH
                                            : RangeOverlapKind.OVERLAP));

                List<V> valuesForOverlappingRanges = new ArrayList<>();
                valuesForOverlappingRanges.add(aHolder);
                valuesForOverlappingRanges.add(bHolder);

                result.add(new RangeOverlap<V>(kind, valuesForOverlappingRanges));
                return result;
            }
        }
        return result;
    }
}
