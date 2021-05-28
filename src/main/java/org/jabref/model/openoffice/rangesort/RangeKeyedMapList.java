package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.sun.star.text.XTextRange;

/*
 * Partition by XTextRange.getText() and sort within the partitions a
 * set of XTextRange values, while keeping their associated data
 * recoverable. Allows identical XTextRange values, their data is
 * collected in a list.
 */
public class RangeKeyedMapList<V> {

    private RangeKeyedMap<List<V>> partitions;

    public RangeKeyedMapList() {
        this.partitions = new RangeKeyedMap<>();
    }

    public boolean containsKey(XTextRange range) {
        return partitions.containsKey(range);
    }

    public List<V> get(XTextRange range) {
        return partitions.get(range);
    }

    public void add(XTextRange range, V value) {
        List<V> values = partitions.get(range);
        if (values == null) {
            values = new ArrayList<>();
            values.add(value);
            partitions.put(range, values);
        } else {
            values.add(value);
        }
    }

    /**
     * @return A list of the partitions.
     */
    public List<TreeMap<XTextRange, List<V>>> partitionValues() {
        return this.partitions.partitionValues();
    }

}
