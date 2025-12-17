package org.jabref.logic.bibtex.comparator.plausibility;

import java.util.Optional;

import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Month;

/**
 * Compares two month values based on their format.
 * Prefers normalized months (e.g. #jun#) and integers over unnormalized strings.
 */
public class MonthPlausibilityComparator implements FieldValuePlausibilityComparator {
    @Override
    public ComparisonResult compare(String leftValue, String rightValue) {
        boolean isLeftBlank = StringUtil.isBlank(leftValue);
        boolean isRightBlank = StringUtil.isBlank(rightValue);

        if (isLeftBlank && !isRightBlank) {
            return ComparisonResult.RIGHT_BETTER;
        } else if (isRightBlank && !isLeftBlank) {
            return ComparisonResult.LEFT_BETTER;
        } else if (isLeftBlank && isRightBlank) {
            return ComparisonResult.UNDETERMINED;
        }

        Optional<Month> leftMonth = Month.parse(leftValue);
        Optional<Month> rightMonth = Month.parse(rightValue);

        if (leftMonth.isPresent() && rightMonth.isEmpty()) {
            return ComparisonResult.LEFT_BETTER;
        } else if (leftMonth.isEmpty() && rightMonth.isPresent()) {
            return ComparisonResult.RIGHT_BETTER;
        }

        boolean isLeftStrict = isStrictFormat(leftValue);
        boolean isRightStrict = isStrictFormat(rightValue);

        if (isLeftStrict && !isRightStrict) {
            return ComparisonResult.LEFT_BETTER;
        } else if (!isLeftStrict && isRightStrict) {
            return ComparisonResult.RIGHT_BETTER;
        }

        return ComparisonResult.UNDETERMINED;
    }

    /**
     * Checks if the value is in a strict BibTeX format.
     * We prefer integers (e.g. "6") or BibTeX strings (e.g. "#jun#") over plain text (e.g. "June").
     */
    private boolean isStrictFormat(String value) {
        String trimmed = value.trim();

        if (trimmed.matches("\\d+")) {
            return true;
        }

        char delimiter = FieldWriter.BIBTEX_STRING_START_END_SYMBOL;
        return trimmed.length() >= 2
                && trimmed.charAt(0) == delimiter
                && trimmed.charAt(trimmed.length() - 1) == delimiter;
    }
}
