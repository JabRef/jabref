package org.jabref.logic.bibtex.comparator.plausibility;

import java.util.Optional;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.model.entry.Month;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class MonthPlausibilityComparator implements FieldValuePlausibilityComparator {

    @Override
    public ComparisonResult compare(String left, String right) {
        Optional<Month> leftM = Month.parse(left);
        Optional<Month> rightM = Month.parse(right);

        if (leftM.isPresent() && rightM.isEmpty()) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightM.isPresent() && leftM.isEmpty()) {
            return ComparisonResult.RIGHT_BETTER;
        }
        if (leftM.isEmpty() && rightM.isEmpty()) {
            return ComparisonResult.UNDETERMINED;
        }
        if (!leftM.equals(rightM)) {
            return ComparisonResult.UNDETERMINED;
        }

        Month leftMonth = leftM.get();
        Month rightMonth = rightM.get();

        boolean leftIsJabRef = left.equals(leftMonth.getJabRefFormat());
        boolean rightIsJabRef = right.equals(rightMonth.getJabRefFormat());
        if (leftIsJabRef && !rightIsJabRef) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightIsJabRef && !leftIsJabRef) {
            return ComparisonResult.RIGHT_BETTER;
        }

        boolean leftIsShort = left.equalsIgnoreCase(leftMonth.getShortName());
        boolean rightIsShort = right.equalsIgnoreCase(rightMonth.getShortName());
        if (leftIsShort && !rightIsShort) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightIsShort && !leftIsShort) {
            return ComparisonResult.RIGHT_BETTER;
        }

        boolean leftIsSimpleNum;
        try {
            leftIsSimpleNum = Integer.parseInt(left) == leftM.get().getNumber();
        } catch (NumberFormatException e) {
            leftIsSimpleNum = false;
        }
        boolean rightIsSimpleNum;
        try {
            rightIsSimpleNum = Integer.parseInt(right) == rightM.get().getNumber();
        } catch (NumberFormatException e) {
            rightIsSimpleNum = false;
        }
        if (leftIsSimpleNum && !rightIsSimpleNum) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightIsSimpleNum && !leftIsSimpleNum) {
            return ComparisonResult.RIGHT_BETTER;
        }
        if (left.length() < right.length()) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (right.length() < left.length()) {
            return ComparisonResult.RIGHT_BETTER;
        }

        return ComparisonResult.UNDETERMINED;
    }
}
