package org.jabref.logic.bibtex.comparator.plausibility;

import java.util.Optional;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Date;

public class DateFieldPlausibilityComparator implements FieldValuePlausibilityComparator {

    // Only {@link PlausibilityComparatorFactory} may instantiate this
    DateFieldPlausibilityComparator() {
    }

    /// Compares the plausibility of two field values.
    ///
    /// @param leftValue  value from the library (or candidate)
    /// @param rightValue value from the fetcher (or existing record)
    /// @return ComparisonResult indicating which date is more plausible: RIGHT_BETTER, LEFT_BETTER, or UNDETERMINED

    @Override
    public ComparisonResult compare(String leftValue, String rightValue) {
        boolean isLeftBlank = StringUtil.isBlank(leftValue);
        boolean isRightBlank = StringUtil.isBlank(rightValue);

        if (isLeftBlank && !isRightBlank) {
            return ComparisonResult.RIGHT_BETTER;
        }
        if (!isLeftBlank && isRightBlank) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (isLeftBlank && isRightBlank) {
            return ComparisonResult.UNDETERMINED;
        }

        Optional<Date> leftDate = Date.parse(leftValue);
        Optional<Date> rightDate = Date.parse(rightValue);

        if (leftDate.isPresent() && rightDate.isEmpty()) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (leftDate.isEmpty() && rightDate.isPresent()) {
            return ComparisonResult.RIGHT_BETTER;
        }
        if (leftDate.isEmpty() && rightDate.isEmpty()) {
            return ComparisonResult.UNDETERMINED;
        }

        int leftSpecificity = getSpecificity(leftDate.get());
        int rightSpecificity = getSpecificity(rightDate.get());

        if (leftSpecificity > rightSpecificity) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightSpecificity > leftSpecificity) {
            return ComparisonResult.RIGHT_BETTER;
        }
        return ComparisonResult.UNDETERMINED;
    }

    // Calculates date specificity from parsed date components.
    // Specificity order: year < year-month < year-month-day.
    private int getSpecificity(Date date) {
        if (date.getDay().isPresent()) {
            return 3;
        }
        if (date.getMonth().isPresent()) {
            return 2;
        }
        return 1;
    }
}
