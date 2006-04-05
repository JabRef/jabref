package net.sf.jabref;

import java.util.Comparator;

/**
 * Comparator for sorting BibtexEntry objects based on their ID. This
 * can be used to sort entries back into the order they were created,
 * provided the IDs given to entries are lexically monotonically increasing.
 */
public class IdComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        BibtexEntry one = (BibtexEntry)o1,
                two = (BibtexEntry)o2;
        return one.getId().compareTo(two.getId());
    }
}
