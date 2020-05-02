package org.jabref.model.util;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class LinkedHashSetEqualsUtil {

    public static <K> boolean linkedEquals(LinkedHashSet<K> left, LinkedHashSet<K> right) {
        Iterator<K> leftItr = left.iterator();
        Iterator<K> rightItr = right.iterator();

        while (leftItr.hasNext() && rightItr.hasNext()) {
            K leftEntry = leftItr.next();
            K rightEntry = rightItr.next();

            //AbstractList does null checks here but for sets we can assume you never get null entries
            if (!leftEntry.equals(rightEntry)) {
                return false;
            }
        }
        return !(leftItr.hasNext() || rightItr.hasNext());
    }
}
