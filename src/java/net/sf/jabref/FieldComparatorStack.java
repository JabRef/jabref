package net.sf.jabref;

import java.util.Comparator;
import java.util.List;
import java.util.Iterator;

/**
 * This class represents a list of comparators. The first Comparator takes precedence,
 * and each time a Comparator returns 0, the next one is attempted. If all comparators
 * return 0 the final result will be 0.
 */
public class FieldComparatorStack implements Comparator {

    List comparators;

    public FieldComparatorStack(List comparators) {
        this.comparators = comparators;
    }

    public int compare(Object o1, Object o2) {
        for (Iterator i=comparators.iterator(); i.hasNext();) {
            int res = ((Comparator)i.next()).compare(o1, o2);
            if (res != 0)
                return res;
        }
        return 0;
    }

}
