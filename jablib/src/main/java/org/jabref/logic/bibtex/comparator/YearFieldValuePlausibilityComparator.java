package org.jabref.logic.bibtex.comparator;

import java.time.Year;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.integrity.YearChecker;

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
        YearChecker checker = new YearChecker();

        boolean leftValid = checker.checkValue(leftValue).isEmpty();

        if (leftValid) {
            Optional<Integer> leftYear = extractYear(leftValue);
            Optional<Integer> rightYear = extractYear(rightValue);

           if (leftYear.isEmpty() || rightYear.isEmpty()) {
               return checkEmptyValues(leftYear, rightYear);
           }

            boolean leftYearInRange = (leftYear.get() >= 1800) && (leftYear.get() <= Year.now().getValue() + 2);

            if (leftYearInRange) {
                int diff = Math.abs(leftYear.get() - rightYear.get());
                if (diff > 10) {
                    return rightYear.get() > leftYear.get()
                            ? ComparisonResult.RIGHT_BETTER
                            : ComparisonResult.LEFT_BETTER;
                }
                return ComparisonResult.UNDETERMINED; // years are close, undetermined
            }
            return ComparisonResult.RIGHT_BETTER;
            }
        return ComparisonResult.RIGHT_BETTER;
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

    /**
     * Prevents the years from being empty, so the following logic has non empty values
     *
     * @param leftValue year from the library (or candidate)
     * @param rightValue year from the fetcher (or existing record)
     * @return ComparisonResult depending on which value is empty: RIGHT_BETTER, LEFT_BETTER, or UNDETERMINED
     */
    private ComparisonResult checkEmptyValues(Optional<Integer> leftValue, Optional<Integer> rightValue) {
        if (leftValue.isEmpty() && rightValue.isEmpty()) {
            return ComparisonResult.UNDETERMINED;
        } else if (leftValue.isEmpty()) {
            return ComparisonResult.RIGHT_BETTER;
        } else {
            return ComparisonResult.LEFT_BETTER;
        }
    }
}
