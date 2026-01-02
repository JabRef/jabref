package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonthPlausibilityComparatorTest {
    private final MonthPlausibilityComparator comparator = new MonthPlausibilityComparator();

    @ParameterizedTest
    @CsvSource(value = {
            "Jun, #jun#, RIGHT_BETTER",
            "June, #jun#, RIGHT_BETTER",
            "June, 6, RIGHT_BETTER",

            "6, #jun#, RIGHT_BETTER",
            "#jun#, 6, LEFT_BETTER",

            "June, July, UNDETERMINED",
            "#jun#, #jul#, UNDETERMINED",
            "June, #Apr#, UNDETERMINED",

            "NotAMonth, #jun#, RIGHT_BETTER",
            "NotAMonth, June, RIGHT_BETTER",

            ", #jun#, RIGHT_BETTER",
            "#jun#, , LEFT_BETTER"
    }, nullValues = {"null", ""})
    void compare(String left, String right, ComparisonResult expected) {
        assertEquals(expected, comparator.compare(left, right));
    }
}
