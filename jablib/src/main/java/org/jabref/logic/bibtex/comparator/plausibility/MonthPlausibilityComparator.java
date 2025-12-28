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

        // 1. Presence Checks
        if (leftM.isPresent() && rightM.isEmpty()) {
            return ComparisonResult.LEFT_BETTER;
        }

        if (rightM.isPresent() && leftM.isEmpty()) {
            return ComparisonResult.RIGHT_BETTER;
        }
        if (leftM.isEmpty() && rightM.isEmpty()) {
            return ComparisonResult.UNDETERMINED;
        }

        // 2. Conflict Check
        if (!leftM.equals(rightM)) {
            return ComparisonResult.UNDETERMINED;
        }

        // 3. Format Prioritization

        // Check for JabRef Format (#jan#)
        boolean leftIsJabRef = left.equals(leftM.get().getJabRefFormat());
        boolean rightIsJabRef = right.equals(rightM.get().getJabRefFormat());

        if (leftIsJabRef && !rightIsJabRef) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightIsJabRef && !leftIsJabRef) {
            return ComparisonResult.RIGHT_BETTER;
        }

        // Check for Standard Short Name (jan, feb...)
        boolean leftIsShort = left.equalsIgnoreCase(leftM.get().getShortName());
        boolean rightIsShort = right.equalsIgnoreCase(rightM.get().getShortName());

        if (leftIsShort && !rightIsShort) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightIsShort && !leftIsShort) {
            return ComparisonResult.RIGHT_BETTER;
        }

        // Check for Simple Number
        boolean leftIsSimpleNum = left.matches("\\d+") && Integer.parseInt(left) == leftM.get().getNumber();
        boolean rightIsSimpleNum = right.matches("\\d+") && Integer.parseInt(right) == rightM.get().getNumber();

        if (leftIsSimpleNum && !rightIsSimpleNum) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightIsSimpleNum && !leftIsSimpleNum) {
            return ComparisonResult.RIGHT_BETTER;
        }

        // Length Fallback
        if (left.length() < right.length()) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (right.length() < left.length()) {
            return ComparisonResult.RIGHT_BETTER;
        }

        return ComparisonResult.UNDETERMINED;
    }
}
