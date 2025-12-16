package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MonthPlausibilityComparatorTest {
    private final MonthPlausibilityComparator comparator = new MonthPlausibilityComparator();

    @Test
    void preferNormalizedOverUnnormalized() {
        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare("Jun", "#jun#"));
    }

    @Test
    void preferIntegerOverUnnormalized() {
        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare("June", "6"));
    }

    @Test
    void equalIfBothNormalized() {
        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare("#jun#", "#jul#"));
    }

    @Test
    void equalIfBothInvalid() {
        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare("June", "July"));
    }
}
