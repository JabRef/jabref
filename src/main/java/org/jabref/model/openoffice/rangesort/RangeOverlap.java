package org.jabref.model.openoffice.rangesort;

import java.util.List;

/**
 *  Used in reporting range overlaps.
 */
public class RangeOverlap<V> {
    public final RangeOverlapKind kind;
    public final List<V> valuesForOverlappingRanges;

    public RangeOverlap(RangeOverlapKind kind, List<V> valuesForOverlappingRanges) {
        this.kind = kind;
        this.valuesForOverlappingRanges = valuesForOverlappingRanges;
    }
}
