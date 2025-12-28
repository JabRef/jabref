package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonthPlausibilityComparatorTest {
    private final MonthPlausibilityComparator comparator = new MonthPlausibilityComparator();

    @ParameterizedTest
    @CsvSource(value = {
            "{Jun}, jun, RIGHT_BETTER",
            "Jun, #jun#, RIGHT_BETTER",
            "June, #jun#, RIGHT_BETTER",
            "June, 6, RIGHT_BETTER",
            "June, July, UNDETERMINED",
            "#jun#, #jul#, UNDETERMINED",
            "6, #jun#, RIGHT_BETTER",
            "June, #Apr#, UNDETERMINED",
            "June, July, UNDETERMINED",
            "NotAMonth, #jun#, RIGHT_BETTER",
            "NotAMonth, June, RIGHT_BETTER",
            ", #jun#, RIGHT_BETTER",
            "#jun#, , LEFT_BETTER",
            "06, 6, RIGHT_BETTER",
            "jan, jan, UNDETERMINED",
            "#JAN#, #jan#, RIGHT_BETTER"
    }, nullValues = {"null", "N/A", "EMPTY"})
    void compare(String left, String right, ComparisonResult expected) {
        assertEquals(expected, comparator.compare(left, right));
    }
}
