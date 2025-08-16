package org.jabref.logic.bibtex.comparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YearFieldValuePlausibilityComparatorTest {

    private final YearFieldValuePlausibilityComparator comparator = new YearFieldValuePlausibilityComparator();
    @Test
    void compareEmptyValuesTest() {
        String emptyString = "";
        String validYearString = "1999";

        ComparisonResult leftRight = ComparisonResult.LEFT_BETTER;
        ComparisonResult rightRight = ComparisonResult.RIGHT_BETTER;
        ComparisonResult undetermined = ComparisonResult.UNDETERMINED;

        assertEquals(rightRight, comparator.compare(emptyString, validYearString));
        assertEquals(leftRight, comparator.compare(validYearString, emptyString));
        assertEquals(undetermined, comparator.compare(emptyString, emptyString));
    }
}
