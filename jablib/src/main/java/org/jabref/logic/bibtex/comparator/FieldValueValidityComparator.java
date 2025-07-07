package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;

public class FieldValueValidityComparator implements Comparator<String> {
    /**
     * Compares the validity or appropriateness of two field values.
     *
     * @param leftValue  value from the fetcher (or candidate)
     * @param rightValue value from the library (or existing record)
     * @return 1 if left is better, 0 if undetermined or equal, -1 if left is worse
     */
    @Override
    public int compare(String leftValue, String rightValue) {
        return 0;
    }
}
