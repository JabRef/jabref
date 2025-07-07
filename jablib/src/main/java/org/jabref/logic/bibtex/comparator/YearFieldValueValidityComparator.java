package org.jabref.logic.bibtex.comparator;

import java.time.Year;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.integrity.YearChecker;

public class YearFieldValueValidityComparator extends FieldValueValidityComparator {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");

    /**
     * Compares the validity or appropriateness of two field values.
     *
     * @param leftValue  value from the library (or candidate)
     * @param rightValue value from the fetcher (or existing record)
     * @return 1 if right is better, 0 if undetermined or equal, -1 if right is worse
     */

    @Override
    public int compare(String leftValue, String rightValue) {
        YearChecker checker = new YearChecker();

        boolean leftValid = checker.checkValue(leftValue).isEmpty();

        if (leftValid) {
            Optional<Integer> leftYear = extractYear(leftValue);
            Optional<Integer> rightYear = extractYear(rightValue);

            boolean leftYearInRange = (leftYear.get() >= 1800) && (leftYear.get() <= Year.now().getValue());

            if (leftYearInRange) {
                int diff = Math.abs(leftYear.get() - rightYear.get());
                if (diff > 10) {
                    return Integer.compare(rightYear.get(), leftYear.get());
                }
                return 0; // years are close, undetermined
            }
            return 1;
            }
        return 1;
    }

    private Optional<Integer> extractYear(String value) {
        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }
}
