package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.model.entry.AuthorList;

public class PersonNamesPlausibilityComparator implements FieldValuePlausibilityComparator {
    @Override
    public ComparisonResult compare(String left, String right) {

        if (left == null || right == null) {
            return ComparisonResult.UNDETERMINED;
        }

        var leftAuthors = AuthorList.parse(left);
        var rightAuthors = AuthorList.parse(right);

        int leftCount = leftAuthors.getNumberOfAuthors();
        int rightCount = rightAuthors.getNumberOfAuthors();

        if (leftCount > rightCount) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightCount > leftCount) {
            return ComparisonResult.RIGHT_BETTER;
        }

        int leftLength = left.length();
        int rightLength = right.length();

        if (leftLength > rightLength) {
            return ComparisonResult.LEFT_BETTER;
        }
        if (rightLength > leftLength) {
            return ComparisonResult.RIGHT_BETTER;
        }

        return ComparisonResult.UNDETERMINED;
    }
}
