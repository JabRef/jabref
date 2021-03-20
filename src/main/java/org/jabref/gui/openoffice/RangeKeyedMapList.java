package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.sun.star.text.XText;
import com.sun.star.text.XTextRange;

class RangeKeyedMapList<V> {
    RangeKeyedMap<List<V>> xxs;

    public
    RangeKeyedMapList() {
        this.xxs = new RangeKeyedMap<>();
    }

    public boolean
    containsKey(XTextRange r) {
        return xxs.containsKey(r);
    }

    public List<V>
    get(XTextRange r) {
        return xxs.get(r);
    }

    public void
    add( XTextRange r, V value) {
        List<V> vs = xxs.get(r);
        if (vs == null){
            vs = new ArrayList<>();
            vs.add(value);
            xxs.put(r,vs);
        } else {
            vs.add(value);
        }
    }

    enum OverlapKind {
        TOUCH,
        OVERLAP,
        EQUAL_RANGE
    }

    /**
     *  Used in reporting range overlaps.
     *
     *  You probably want {@code V} to include information
     *  identifying the ranges.
     */
    class RangeOverlap {
        OverlapKind kind;
        List<V> vs;

        public
        RangeOverlap( OverlapKind kind, List<V> vs ) {
            this.kind = kind;
            this.vs = vs;
        }
    };

    /**
     * @return A list of the partitions.
     */
    public List<TreeMap<XTextRange,List<V>>>
    partitionValues() {
        return this.xxs.partitionValues();
    }

    /**
     * Report identical, overlapping or touching ranges.
     *
     * For overlapping and touching, only report consecutive ranges
     * and only with a single sample of otherwise identical ranges.
     *
     * @param atMost Limit the number of records returneed to atMost.
     *        Zero or negative {@code atMost} means no limit.
     */
    List<RangeOverlap>
    findOverlappingRanges(int atMost, boolean includeTouching) {
        List<RangeOverlap> res = new ArrayList<>();
        for (TreeMap<XTextRange,List<V>> xs : xxs.partitionValues()) {
            List<XTextRange> oxs = new ArrayList<>(xs.keySet());
            for (int i = 0; i < oxs.size(); i++) {
                XTextRange a = oxs.get(i);
                List<V> avs = xs.get(a);
                if (avs.size() > 1) {
                    res.add(
                        new RangeOverlap(
                            OverlapKind.EQUAL_RANGE,
                            avs
                            )
                        );
                    if ( atMost > 0 && res.size() >= atMost ) {
                        return res;
                    }
                }
                if ((i + 1) < oxs.size()) {
                    XTextRange b = oxs.get(i + 1);
                    int cmp = DocumentConnection.javaCompareRegionStarts(a.getEnd(), b.getStart());
                    if (cmp > 0 || (includeTouching && (cmp == 0))) {
                        // found overlap or touch
                        List<V> bvs = xs.get(b);
                        List<V> vs = new ArrayList<>();
                        vs.add( avs.get(0) );
                        vs.add( bvs.get(0) );
                        res.add(
                            new RangeOverlap(
                                (cmp == 0) ? OverlapKind.TOUCH : OverlapKind.OVERLAP,
                                vs
                                )
                            );
                    }
                    if ( atMost > 0 && res.size() >= atMost ) {
                        return res;
                    }
                }
            }
        }
        return res;
    }

}
