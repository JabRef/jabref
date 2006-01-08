package net.sf.jabref.search;

import java.util.Comparator;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * This Comparator compares two objects based on whether none, one of them, or both
 * match a given Matcher. It is used to "float" group and search hits in the main table.
 */
public class HitOrMissComparator implements Comparator {
    private Matcher hitOrMiss;

    public HitOrMissComparator(Matcher hitOrMiss) {
        this.hitOrMiss = hitOrMiss;
    }

    public int compare(Object o1, Object o2) {
        if (hitOrMiss == null)
            return 0;
        
        boolean
                hit1 = hitOrMiss.matches(o1),
                hit2 = hitOrMiss.matches(o2);
        if (hit1 == hit2)
            return 0;
        else
            return hit1 ? -1 : 1;
    }
}
