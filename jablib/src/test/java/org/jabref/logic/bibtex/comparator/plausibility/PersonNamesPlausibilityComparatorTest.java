package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonNamesPlausibilityComparatorTest {
    private final PersonNamesPlausibilityComparator comparator = new PersonNamesPlausibilityComparator();

    @Test
    void rightSideHasMoreAuthors() {
        String left = "A. Author";
        String right = "A. Author and B. Writer";

        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare(left, right));
    }

    @Test
    void leftSideHasMoreAuthors() {
        String left = "A. Author and B. Writer";
        String right = "A. Author";

        assertEquals(ComparisonResult.LEFT_BETTER, comparator.compare(left, right));
    }

    @Test
    void sameCountRightIsLonger() {
        String left = "A. Author";
        String right = "A. Very-Long-Name Author";

        assertEquals(ComparisonResult.RIGHT_BETTER, comparator.compare(left, right));
    }

    @Test
    void sameCountLeftIsLonger() {
        String left = "A. Very-Long-Name Author";
        String right = "A. Author";

        assertEquals(ComparisonResult.LEFT_BETTER, comparator.compare(left, right));
    }

    @Test
    void identicalValuesReturnUndetermined() {
        String left = "A. Author";
        String right = "A. Author";

        assertEquals(ComparisonResult.UNDETERMINED, comparator.compare(left, right));
    }
}
