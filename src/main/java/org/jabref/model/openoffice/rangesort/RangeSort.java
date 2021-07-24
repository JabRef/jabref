package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.openoffice.uno.UnoCast;
import org.jabref.model.openoffice.uno.UnoTextRange;

import com.sun.star.text.XText;
import com.sun.star.text.XTextRangeCompare;

/**
 * RangeSort provides sorting based on XTextRangeCompare, which only provides comparison
 * between XTextRange values within the same XText.
 */
public class RangeSort {

    private RangeSort() {
        /**/
    }

    /**
     * Compare two RangeHolders (using RangeHolder.getRange()) within an XText.
     *
     * Note: since we only look at the ranges, this comparison is generally not consistent with
     * `equals` on the RangeHolders. Probably should not be used for key comparison in
     * {@code TreeMap<RangeHolder>} or {@code Set<RangeHolder>}
     *
     */
    private static class HolderComparatorWithinPartition implements Comparator<RangeHolder> {

        private final XTextRangeCompare cmp;

        HolderComparatorWithinPartition(XText text) {
            cmp = (UnoCast.cast(XTextRangeCompare.class, text)
                   .orElseThrow(java.lang.IllegalArgumentException::new));
        }

        /**
         * Assumes a and b belong to the same XText as cmp.
         */
        @Override
        public int compare(RangeHolder a, RangeHolder b) {
            return UnoTextRange.compareStartsThenEndsUnsafe(cmp, a.getRange(), b.getRange());
        }
    }

    /**
     * Sort a list of RangeHolder values known to share the same getText().
     *
     * Note: RangeHolder.getRange() is called many times.
     */
    public static <V extends RangeHolder> void sortWithinPartition(List<V> rangeHolders) {
        if (rangeHolders.isEmpty()) {
            return;
        }
        XText text = rangeHolders.get(0).getRange().getText();
        rangeHolders.sort(new HolderComparatorWithinPartition(text));
    }

    /**
     * Represent a partitioning of RangeHolders by XText
     */
    public static class RangePartitions<V extends RangeHolder> {
        private final Map<XText, List<V>> partitions;

        public RangePartitions() {
            this.partitions = new HashMap<>();
        }

        public void add(V holder) {
            XText partitionKey = holder.getRange().getText();
            List<V> partition = partitions.computeIfAbsent(partitionKey, unused -> new ArrayList<>());
            partition.add(holder);
        }

        public List<List<V>> getPartitions() {
            return new ArrayList<>(partitions.values());
        }
    }

    /**
     *  Partition RangeHolders by the corresponding XText.
     */
    public static <V extends RangeHolder> RangePartitions<V> partitionRanges(List<V> holders) {
        RangePartitions<V> result = new RangePartitions<>();
        for (V holder : holders) {
            result.add(holder);
        }
        return result;
    }

    /**
     * Note: RangeHolder.getRange() is called many times.
     */
    public static <V extends RangeHolder> RangePartitions<V> partitionAndSortRanges(List<V> holders) {
        RangePartitions<V> result = partitionRanges(holders);
        for (List<V> partition : result.getPartitions()) {
            sortWithinPartition(partition);
        }
        return result;
    }
}
