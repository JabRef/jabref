package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YearFieldValuePlausibilityComparatorTest {

    private final YearFieldValuePlausibilityComparator comparator = new YearFieldValuePlausibilityComparator();

    @ParameterizedTest
    @CsvSource(textBlock = """
            # Blank Validation
            '', 2020, RIGHT_BETTER
            2020, '', LEFT_BETTER
            '', '', UNDETERMINED

            # Year Format Validation
            2020, Twenty-twenty, LEFT_BETTER
            Twenty-twenty, 2020, RIGHT_BETTER
            Twenty-twenty, Twenty-twenty-one, UNDETERMINED

            # Year Range Validation (1800 <= year <= currentYear + 2)
            2020, 1200, LEFT_BETTER
            1200, 2020, RIGHT_BETTER
            1200, 1300, UNDETERMINED

            # Year Proximity Validation (diff > 10)
            2000, 2020, RIGHT_BETTER
            2020, 2000, LEFT_BETTER
            2020, 2025, UNDETERMINED
            """)
    void compare(String left, String right, ComparisonResult expected) {
        assertEquals(expected, comparator.compare(left, right));
    }
}
