package org.jabref.gui.openoffice;

import java.util.ArrayList;
//import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.TreeMap;
import com.sun.star.text.XText;
import com.sun.star.text.XTextRange;

/**
 * Map XTextRange values to some value of type V.
 *
 * Since XTextRange values are only comparable if they share the same
 * r.getText(), we group them by these.
 *
 * Within such groups (partitions) we may define comparison, here
 * based on (r.getStart(),r.getEnd()), thus equality means identical
 * ranges.
 *
 * For finding overlapping ranges we use RangeKeyedMapList, that
 * allows a list of values to be associated with a single XTextRange.
 *
 */
class RangeKeyedMap<V> {
    Map<XText, TreeMap<XTextRange,V>> xxs;

    public
    RangeKeyedMap() {
        this.xxs = new HashMap<>();
    }

    public boolean
    containsKey(XTextRange r) {
        return xxs.containsKey( r.getText() )
            && xxs.get(r).containsKey(r);
    }

    public V
    get(XTextRange r) {
        TreeMap<XTextRange,V> xs = xxs.get(r.getText());
        if ( xs == null ){
            return null;
        }
        return( xs.get(r) );
    }

    private static int
    comparator( XTextRange a, XTextRange b  ) {
        int startOrder = DocumentConnection.javaCompareRegionStarts( a, b );
        if ( startOrder != 0 ){
            return startOrder;
        }
        return DocumentConnection.javaCompareRegionEnds( a, b );
    }

    public V
    put(XTextRange r, V value) {
        TreeMap<XTextRange,V> xs = xxs.get(r.getText());
        if ( xs == null ){
            xs = new TreeMap<>( RangeKeyedMap::comparator );
            xxs.put( r.getText(), xs );
        }
        return xs.put( r, value );
    }

    /**
     * @return A list of the partitions.
     */
    public List<TreeMap<XTextRange,V>>
    partitionValues() {
        return new ArrayList(xxs.values());
    }
}
