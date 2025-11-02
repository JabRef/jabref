package org.jabref.logic.bibtex.comparator;

import java.time.Year;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.integrity.YearChecker;
import org.jabref.logic.util.strings.StringUtil;

public class YearFieldValuePlausibilityComparator extends FieldValuePlausibilityComparator {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");

    /**
     * Compares the plausibility of two field values.
     *
     * @param leftValue  value from the library (or candidate)
     * @param rightValue value from the fetcher (or existing record)
     * @return ComparisonResult indicating which year is more plausible: RIGHT_BETTER, LEFT_BETTER, or UNDETERMINED
     */

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

        // left and right values are not blank.

        boolean leftValueCorrectlyFormatted = YearChecker.isValueCorrectlyFormatted(leftValue);
        boolean rightValueCorrectlyFormatted = YearChecker.isValueCorrectlyFormatted(rightValue);
        if (leftValueCorrectlyFormatted && !rightValueCorrectlyFormatted) {
            return ComparisonResult.LEFT_BETTER;
        } else if (rightValueCorrectlyFormatted && !leftValueCorrectlyFormatted) {
            return ComparisonResult.RIGHT_BETTER;
        } else if (!leftValueCorrectlyFormatted && !rightValueCorrectlyFormatted) {
            return ComparisonResult.UNDETERMINED;
        }

        // left and right values are correctly formatted.

        int leftYear = extractYear(leftValue).get();
        int rightYear = extractYear(rightValue).get();
        boolean leftYearInRange = (leftYear >= 1800) && (leftYear <= Year.now().getValue() + 2);
        boolean rightYearInRange = (rightYear >= 1800) && (rightYear <= Year.now().getValue() + 2);
        if (leftYearInRange && !rightYearInRange) {
            return ComparisonResult.LEFT_BETTER;
        } else if (rightYearInRange && !leftYearInRange) {
            return ComparisonResult.RIGHT_BETTER;
        } else if (!leftYearInRange && !rightYearInRange) {
            return ComparisonResult.UNDETERMINED;
        }

        int diff = Math.abs(leftYear - rightYear);
        if (diff > 10) {
            return rightYear > leftYear
                   ? ComparisonResult.RIGHT_BETTER
                   : ComparisonResult.LEFT_BETTER;
        }

        return ComparisonResult.UNDETERMINED; // years are close, undetermined
    }

    /**
     * Extracts the first 4-digit number found in the string.
     * Used to identify year-like values such as "About 2000" or "Published in 1999".
     *
     * @param value the input string possibly containing a year
     * @return Optional containing the 4-digit year if found, otherwise Optional.empty()
     */
    private Optional<Integer> extractYear(String value) {
        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }
}
