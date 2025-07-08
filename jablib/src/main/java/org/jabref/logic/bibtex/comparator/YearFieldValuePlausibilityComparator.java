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

            boolean leftYearInRange = (leftYear.get() >= 1800) && (leftYear.get() <= Year.now().getValue() + 2);

            if (leftYearInRange) {
                int diff = Math.abs(leftYear.get() - rightYear.get());
                if (diff > 10) {
                    return ComparisonResult.fromInt(Integer.compare(rightYear.get(), leftYear.get()));
                }
                return ComparisonResult.UNDETERMINED; // years are close, undetermined
            }
            return ComparisonResult.RIGHT_BETTER;
            }
        return ComparisonResult.RIGHT_BETTER;
    }

    private Optional<Integer> extractYear(String value) {
        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }
}
