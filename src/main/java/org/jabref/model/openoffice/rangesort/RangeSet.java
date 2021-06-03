package org.jabref.model.openoffice.rangesort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.jabref.model.openoffice.uno.UnoTextRange;

import com.sun.star.text.XText;
import com.sun.star.text.XTextRange;

public class RangeSet {

    private final Map<XText, TreeSet<XTextRange>> partitions;

    public RangeSet() {
        this.partitions = new HashMap<>();
    }

    public boolean contains(XTextRange range) {
        Objects.requireNonNull(range);
        XText partitionKey = range.getText();
        if (!this.partitions.containsKey(partitionKey)) {
            return false;
        }
        return partitions.get(partitionKey).contains(range);
    }

    /* return false if already contained */
    public boolean add(XTextRange range) {
        TreeSet<XTextRange> partition = partitions.get(range.getText());
        if (partition == null) {
            partition = new TreeSet<>(UnoTextRange::compareStartsThenEnds);
            partitions.put(range.getText(), partition);
        }
        return partition.add(range);
    }

    /**
     * @return A list of the partitions.
     */
    public List<TreeSet<XTextRange>> partitionValues() {
        return new ArrayList<>(partitions.values());
    }
}
