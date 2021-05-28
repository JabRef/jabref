package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.sun.star.text.XText;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.UnoRuntime;

/**
 * Purpose: in order to check overlaps of XTextRange values, sort
 *          them, and allow recovering some corresponding information
 *          (of type V)
 *
 * Since XTextRange values are only comparable if they share the same
 * range.getText(), we group them by these.
 *
 * Within such groups (partitions) we may define comparison, here
 * based on (range.getStart(),range.getEnd()), where equality means identical
 * ranges.
 *
 * For finding overlapping ranges this class proved insufficient,
 * beacause it does not allow multiple values to be associated with a
 * single XTextRange.  The class RangeKeyedMapList solves this.
 *
 */
public class RangeKeyedMap<V> {

    private final Map<XText, TreeMap<XTextRange, V>> partitions;

    public RangeKeyedMap() {
        this.partitions = new HashMap<>();
    }

    public boolean containsKey(XTextRange range) {
        Objects.requireNonNull(range);
        XText partitionKey = range.getText();
        if (!partitions.containsKey(partitionKey)) {
            return false;
        }
        return partitions.get(partitionKey).containsKey(range);
    }

    public V get(XTextRange range) {
        TreeMap<XTextRange, V> partition = partitions.get(range.getText());
        if (partition == null) {
            return null;
        }
        return partition.get(range);
    }

    /*
     * Same as UnoTextRange.compareStartsThenEnds in logic.
     */
    private static int comparator(XTextRange a, XTextRange b) {
        if (a.getText() != b.getText()) {
            throw new RuntimeException("comparator: got incomparable regions");
        }

        final XTextRangeCompare compare =
            UnoRuntime.queryInterface(XTextRangeCompare.class, a.getText());

        int cmpStart = (-1) * compare.compareRegionStarts(a, b);
        if (cmpStart != 0) {
            return cmpStart;
        }
        return (-1) * compare.compareRegionEnds(a, b);
    }

    public V put(XTextRange range, V value) {
        TreeMap<XTextRange, V> partition = partitions.get(range.getText());
        if (partition == null) {
            partition = new TreeMap<>(RangeKeyedMap::comparator);
            partitions.put(range.getText(), partition);
        }
        return partition.put(range, value);
    }

    /**
     * @return A list of the partitions.
     */
    public List<TreeMap<XTextRange, V>> partitionValues() {
        return new ArrayList<>(partitions.values());
    }
}
