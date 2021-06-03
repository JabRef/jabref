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

public class RangeSort {

    /*
     * Sort within a partition
     */

    public static class HolderComparatorWithinPartition implements Comparator<RangeHolder> {

        XTextRangeCompare cmp;

        HolderComparatorWithinPartition(XText text) {
            cmp = UnoCast.unoQI(XTextRangeCompare.class, text);
        }

        /*
         * Assumes a and b belong to the same XText as cmp.
         */
        @Override
        public int compare(RangeHolder a, RangeHolder b) {
            return UnoTextRange.compareStartsThenEndsUnsafe(cmp, a.getRange(), b.getRange());
        }
    }

    public static <V extends RangeHolder>
    void sortWithinPartition(List<V> rangeHolders) {
        if (rangeHolders.isEmpty()) {
            return;
        }
        XText text = rangeHolders.get(0).getRange().getText();
        rangeHolders.sort(new HolderComparatorWithinPartition(text));
    }

    /*
     * Partitioning
     */

    public static class RangePartitions<V extends RangeHolder> {
        private final Map<XText, List<V>> partitions;

        public RangePartitions() {
            this.partitions = new HashMap<>();
        }

        public void add(V holder) {
            XText partitionKey = holder.getRange().getText();
            List<V> partition = partitions.get(partitionKey);
            if (partition == null) {
                partition = new ArrayList<>();
                partitions.put(partitionKey, partition);
            }
            partition.add(holder);
        }

        public List<List<V>> getPartitions() {
            return new ArrayList<>(partitions.values());
        }
    }

    public static <V extends RangeHolder>
    RangePartitions<V> partitionRanges(List<V> holders) {
        RangePartitions<V> result = new RangePartitions<>();
        for (V holder : holders) {
            result.add(holder);
        }
        return result;
    }

    public static <V extends RangeHolder>
    RangePartitions<V> partitionAndSortRanges(List<V> holders) {
        RangePartitions<V> result = partitionRanges(holders);
        for (List<V> partition : result.getPartitions()) {
            sortWithinPartition(partition);
        }
        return result;
    }
}
