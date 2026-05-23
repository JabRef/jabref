package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YearFieldValuePlausibilityComparatorTest {

    private final YearFieldValuePlausibilityComparator comparator = new YearFieldValuePlausibilityComparator();

    // Blank Validation
    @Test
    void leftSideIsBlank() {
        String left = "";
        String right = "2020";

        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare(left, right));
    }

    @Test
    void rightSideIsBlank() {
        String left = "2020";
        String right = "";

        assertEquals(ComparisonResult.LEFT_BETTER, comparator.compare(left, right));
    }

    @Test
    void bothSidesAreBlank() {
        String left = "";
        String right = "";

        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare(left, right));
    }

    // Year Format Validation
    @Test
    void leftSideCorrectlyFormatted() {
        String left = "2020";
        String right = "Twenty-twenty";

        assertEquals(ComparisonResult.LEFT_BETTER, comparator.compare(left, right));
    }

    @Test
    void rightSideCorrectlyFormatted() {
        String left = "Twenty-twenty";
        String right = "2020";

        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare(left, right));
    }

    @Test
    void bothSidesIncorrectlyFormatted() {
        String left = "Twenty-twenty";
        String right = "Twenty-twenty-one";

        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare(left, right));
    }

    // Year Range Validation 1800 <= year <= currentYear + 2
    @Test
    void leftSideInYearRange() {
        String left = "2026";
        String right = "1000";

        assertEquals(ComparisonResult.LEFT_BETTER, comparator.compare(left, right));
    }

    @Test
    void rightSideInYearRange() {
        String left = "1000";
        String right = "2026";

        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare(left, right));
    }

    @Test
    void bothSidesOutOfYearRange() {
        String left = "1000";
        String right = "3000";

        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare(left, right));
    }

    // NumberOfYears Validation
    @Test
    void rightSideIsNewerByMoreThanTenYears() {
        String left = "1000";
        String right = "2026";

        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare(left, right));
    }

    @Test
    void leftSideIsNewerByMoreThanTenYears() {
        String left = "2026";
        String right = "1000";

        assertEquals(ComparisonResult.LEFT_BETTER, comparator.compare(left, right));
    }

    @Test
    void rightSideIsNewerByLessThanTenYears() {
        String left = "2020";
        String right = "2026";

        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare(left, right));
    }

    @Test
    void leftSideIsNewerByLessThanTenYears() {
        String left = "2026";
        String right = "2020";

        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare(left, right));
    }

    @Test
    void bothSidesSameYear() {
        String left = "2026";
        String right = "2026";

        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare(left, right));
    }
}
