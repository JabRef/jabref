package org.jabref.logic.bibtex.comparator;

public abstract class FieldValuePlausibilityComparator {
    /**
     * Compares the plausibility of two field values.
     *
     * @param leftValue  value from the library (or candidate)
     * @param rightValue value from the fetcher (or existing record)
     * @return ComparisonResult indicating which field is more plausible: RIGHT_BETTER, LEFT_BETTER, or UNDETERMINED
     */
    public abstract ComparisonResult compare(String leftValue, String rightValue);
}
