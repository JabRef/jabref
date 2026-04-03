package org.jabref.model.search;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateComparisonUtilsTest {
    @Test
    void testExactYearComparison() {
        // Is 2024 >= 2020? (true)
        assertTrue(DateComparisonUtils.evaluateDateRange(
                "2024", "2020", DateComparisonUtils.DateOperator.GREATER_OR_EQUAL));

        // Is 2015 > 2015? (false)
        assertFalse(DateComparisonUtils.evaluateDateRange(
                "2015", "2015", DateComparisonUtils.DateOperator.GREATER_THAN));

        // Is 2010 <= 2020? (true)
        assertTrue(DateComparisonUtils.evaluateDateRange(
                "2010", "2020", DateComparisonUtils.DateOperator.LESS_OR_EQUAL));

        // Is 2020 <= 2020 (true)
        assertTrue(DateComparisonUtils.evaluateDateRange(
                "2020", "2020", DateComparisonUtils.DateOperator.LESS_OR_EQUAL));

        // Is 2009 >= 2009 (true)
        assertTrue(DateComparisonUtils.evaluateDateRange(
                "2009", "2009", DateComparisonUtils.DateOperator.GREATER_OR_EQUAL));
    }

    @Test
    void testMixedFormatComparison() {
        // Is 2024-05-17 > 2024? (2024 defaults to Jan 1st, so this is true)
        assertTrue(DateComparisonUtils.evaluateDateRange(
                "2024-05-17", "2024", DateComparisonUtils.DateOperator.GREATER_THAN));

        // Is 2021-12 < 2022-01-15? (Should be true)
        assertTrue(DateComparisonUtils.evaluateDateRange(
                "2021-12", "2022-01-15", DateComparisonUtils.DateOperator.LESS_THAN));
    }

    @Test
    void testInvalidGibberishDates() {
        // Should gracefully return false instead of crashing the app
        assertFalse(DateComparisonUtils.evaluateDateRange(
                "Spring 2015", "2020", DateComparisonUtils.DateOperator.LESS_THAN));

        assertFalse(DateComparisonUtils.evaluateDateRange(
                "2020", "Unknown", DateComparisonUtils.DateOperator.GREATER_THAN));
    }
}
