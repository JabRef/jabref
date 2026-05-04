package org.jabref.logic.bibtex.comparator.plausibility;

import java.util.Optional;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.model.entry.Month;

/// Compares two months values based on their format.
/// Prefers normalized months (e.g. `#jun#`) and integers over unnormalized strings.
public class MonthPlausibilityComparator implements FieldValuePlausibilityComparator {
    @Override
    public ComparisonResult compare(String left, String right) {
        Optional<Month> leftMonth = Month.parse(left);
        Optional<Month> rightMonth = Month.parse(right);

        if (leftMonth.isPresent() && rightMonth.isEmpty()) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (leftMonth.isEmpty() && rightMonth.isPresent()) {
            return ComparisonResult.RIGHT_BETTER;
        }

        if (leftMonth.isPresent() && rightMonth.isPresent() && leftMonth.get() == rightMonth.get()) {
            boolean leftStrict = Month.isStrictFormat(left);
            boolean rightStrict = Month.isStrictFormat(right);

            if (leftStrict && !rightStrict) {
                return ComparisonResult.LEFT_BETTER;
            }
            if (!leftStrict && rightStrict) {
                return ComparisonResult.RIGHT_BETTER;
            }

            if (leftStrict && rightStrict) {
                boolean leftIsBibtexConstant = left.trim().startsWith("#");
                boolean rightIsBibtexConstant = right.trim().startsWith("#");

                if (leftIsBibtexConstant != rightIsBibtexConstant) {
                    return leftIsBibtexConstant ? ComparisonResult.LEFT_BETTER : ComparisonResult.RIGHT_BETTER;
                }
            }
        }

        return ComparisonResult.UNDETERMINED;
    }
}
