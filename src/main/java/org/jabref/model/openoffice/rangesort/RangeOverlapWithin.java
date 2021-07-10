package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.openoffice.uno.UnoCast;
import org.jabref.model.openoffice.uno.UnoTextRange;

import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

public class RangeOverlapWithin {

    private RangeOverlapWithin() { }

    /**
     * Report identical, overlapping or touching ranges between elements of rangeHolders.
     *
     * For overlapping and touching, only report consecutive ranges and only with a single sample of
     * otherwise identical ranges.
     *
     * @param rangeHolders represent the ranges to be checked.
     *
     *        Note: for each rangeHolder, rangeHolder.getRange() is called multiple times.
     *              To avoid repeated work, they should keep a copy of the range instead of
     *              getting it each time from the document.
     *
     * @param reportAtMost Limit the number of records returned to atMost.
     *        Zero {@code reportAtMost} means no limit.
     *
     * @param includeTouching Should the result contain ranges sharing only a boundary?
     */
    public static <V extends RangeHolder>
    List<RangeOverlap<V>> findOverlappingRanges(XTextDocument doc,
                                                List<V> rangeHolders,
                                                boolean includeTouching,
                                                int reportAtMost) {

        RangeSort.RangePartitions<V> partitions = RangeSort.partitionAndSortRanges(rangeHolders);

        return findOverlappingRanges(partitions, reportAtMost, includeTouching);
    }

    /**
     * Report identical, overlapping or touching ranges.
     *
     * For overlapping and touching, only report consecutive ranges and only with a single sample of
     * otherwise identical ranges.
     *
     * @param atMost Limit the number of records returned to atMost.
     *        Zero {@code atMost} means no limit.
     *
     * @param includeTouching Should the result contain ranges sharing only a boundary?
     */
    public static <V extends RangeHolder>
    List<RangeOverlap<V>> findOverlappingRanges(RangeSort.RangePartitions<V> input,
                                                int atMost,
                                                boolean includeTouching) {
        assert atMost >= 0;

        List<RangeOverlap<V>> result = new ArrayList<>();

        for (List<V> partition : input.getPartitions()) {
            if (partition.isEmpty()) {
                continue;
            }
            XTextRangeCompare cmp = UnoCast.cast(XTextRangeCompare.class,
                                                 partition.get(0).getRange().getText()).get();

            for (int i = 0; i < (partition.size() - 1); i++) {
                V aHolder = partition.get(i);
                V bHolder = partition.get(i + 1);
                XTextRange aRange = aHolder.getRange();
                XTextRange bRange = bHolder.getRange();

                // check equal values
                int cmpResult = UnoTextRange.compareStartsThenEndsUnsafe(cmp, aRange, bRange);
                if (cmpResult == 0) {
                    List<V> aValues = new ArrayList<>();
                    aValues.add(aHolder);
                    // aValues.add(bHolder);
                    // collect those equal
                    while (i < (partition.size() - 1) &&
                           UnoTextRange.compareStartsThenEndsUnsafe(
                               cmp,
                               aRange,
                               partition.get(i + 1).getRange()) == 0) {
                        bHolder = partition.get(i + 1);
                        aValues.add(bHolder);
                        i++;
                    }
                    result.add(new RangeOverlap<V>(RangeOverlapKind.EQUAL_RANGE, aValues));
                    if (atMost > 0 && result.size() >= atMost) {
                        return result;
                    }
                    continue;
                }

                // Not equal, and (a <= b) since sorted.
                // Check if a.end >= b.start
                cmpResult = UnoTextRange.compareStartsUnsafe(cmp, aRange.getEnd(), bRange.getStart());
                if (cmpResult > 0 || (includeTouching && (cmpResult == 0))) {
                    // found overlap or touch
                    List<V> valuesForOverlappingRanges = new ArrayList<>();
                    valuesForOverlappingRanges.add(aHolder);
                    valuesForOverlappingRanges.add(bHolder);
                    result.add(new RangeOverlap<V>((cmpResult == 0)
                                                   ? RangeOverlapKind.TOUCH
                                                   : RangeOverlapKind.OVERLAP,
                                                   valuesForOverlappingRanges));
                }
                if (atMost > 0 && result.size() >= atMost) {
                        return result;
                }
            }
        }
        return result;
    }

}
