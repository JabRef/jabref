package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.jabref.model.openoffice.uno.UnoTextRange;

import com.sun.star.text.XTextRange;

public class RangeOverlapFinder {
    /**
     * Report identical, overlapping or touching ranges.
     *
     * For overlapping and touching, only report consecutive ranges
     * and only with a single sample of otherwise identical ranges.
     *
     * @param atMost Limit the number of records returned to atMost.
     *        Zero or negative {@code atMost} means no limit.
     *
     * @param includeTouching Should the result contain ranges
     *                        sharing only a boundary?
     */
    public static <V> List<RangeOverlap<V>> findOverlappingRanges(RangeKeyedMapList<V> input,
                                                                  int atMost,
                                                                  boolean includeTouching) {
        List<RangeOverlap<V>> result = new ArrayList<>();
        for (TreeMap<XTextRange, List<V>> partition : input.partitionValues()) {
            List<XTextRange> orderedRanges = new ArrayList<>(partition.keySet());
            for (int i = 0; i < orderedRanges.size(); i++) {
                XTextRange aRange = orderedRanges.get(i);
                List<V> aValues = partition.get(aRange);
                if (aValues.size() > 1) {
                    result.add(new RangeOverlap<V>(RangeOverlapKind.EQUAL_RANGE, aValues));
                    if (atMost > 0 && result.size() >= atMost) {
                        return result;
                    }
                }
                if ((i + 1) < orderedRanges.size()) {
                    XTextRange bRange = orderedRanges.get(i + 1);
                    int cmp = UnoTextRange.compareStarts(aRange.getEnd(), bRange.getStart());
                    if (cmp > 0 || (includeTouching && (cmp == 0))) {
                        // found overlap or touch
                        List<V> bValues = partition.get(bRange);
                        List<V> valuesForOverlappingRanges = new ArrayList<>();
                        valuesForOverlappingRanges.add(aValues.get(0));
                        valuesForOverlappingRanges.add(bValues.get(0));
                        result.add(new RangeOverlap<V>((cmp == 0)
                                                       ? RangeOverlapKind.TOUCH
                                                       : RangeOverlapKind.OVERLAP,
                                                       valuesForOverlappingRanges));
                    }
                    if (atMost > 0 && result.size() >= atMost) {
                        return result;
                    }
                }
            }
        }
        return result;
    }
}
