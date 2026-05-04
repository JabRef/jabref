package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateFieldPlausibilityComparatorTest {
    private final DateFieldPlausibilityComparator comparator = new DateFieldPlausibilityComparator();

    @ParameterizedTest
    @CsvSource(value = {
            // blank handling
            ", 2017-09, RIGHT_BETTER",
            "2017-09, , LEFT_BETTER",
            ", , UNDETERMINED",

            // invalid vs valid
            "foo-bar-baz, 2017-09, RIGHT_BETTER",
            "2017-09, foo-bar-baz, LEFT_BETTER",
            "foo, bar, UNDETERMINED",

            // specificity: year < year-month < year-month-day
            "2017, 2017-09, RIGHT_BETTER",
            "2017-09, 2017, LEFT_BETTER",
            "2017-09, 2017-09-12, RIGHT_BETTER",
            "2017-09-12, 2017-09, LEFT_BETTER",
            "2017, 2017-09-12, RIGHT_BETTER",
            "2017-09-12, 2017, LEFT_BETTER",

            // equal specificity
            "2017-09, 2018-09, UNDETERMINED",
            "2017-09-12, 2018-09-12, UNDETERMINED",
            "2017, 2018, UNDETERMINED"
    }, nullValues = {"null", ""})
    void compare(String left, String right, ComparisonResult expected) {
        assertEquals(expected, comparator.compare(left, right));
    }
}
