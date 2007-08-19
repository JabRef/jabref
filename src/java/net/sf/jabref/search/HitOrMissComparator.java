package net.sf.jabref.search;

import java.util.Comparator;

import net.sf.jabref.BibtexEntry;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * This Comparator compares two objects based on whether none, one of them, or both
 * match a given Matcher. It is used to "float" group and search hits in the main table.
 */
public class HitOrMissComparator implements Comparator<BibtexEntry> {
    private Matcher<BibtexEntry> hitOrMiss;

    public HitOrMissComparator(Matcher<BibtexEntry> hitOrMiss) {
        this.hitOrMiss = hitOrMiss;
    }

    public int compare(BibtexEntry o1, BibtexEntry o2) {
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
