package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;
import java.util.List;

/**
 * This class represents a list of comparators. The first Comparator takes precedence,
 * and each time a Comparator returns 0, the next one is attempted. If all comparators
 * return 0 the final result will be 0.
 */
public class FieldComparatorStack<T> implements Comparator<T> {

    private final List<? extends Comparator<? super T>> comparators;


    public FieldComparatorStack(List<? extends Comparator<? super T>> comparators) {
        this.comparators = comparators;
    }

    @Override
    public int compare(T o1, T o2) {
        for (Comparator<? super T> comp : comparators) {
            int res = comp.compare(o1, o2);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }
}
