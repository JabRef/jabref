package org.jabref.logic.bibtex.comparator.plausibility;

import java.util.regex.Pattern;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.logic.util.strings.StringUtil;

public class MonthPlausibilityComparator implements FieldValuePlausibilityComparator {
    private static final Pattern MONTH_NORMALIZED = Pattern.compile("#(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)#");

    private static final Pattern MONTH_INTEGER = Pattern.compile("([1-9]|10|11|12)");

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

        boolean isLeftStrict = isStrictlyValid(leftValue);
        boolean isRightStrict = isStrictlyValid(rightValue);

        if (isLeftStrict && !isRightStrict) {
            return ComparisonResult.LEFT_BETTER;
        } else if (!isLeftStrict && isRightStrict) {
            return ComparisonResult.RIGHT_BETTER;
        }

        return ComparisonResult.UNDETERMINED;
    }

    private boolean isStrictlyValid(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();

        return MONTH_NORMALIZED.matcher(trimmed).matches() || MONTH_INTEGER.matcher(trimmed).matches();
    }
}
