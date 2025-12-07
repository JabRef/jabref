package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.model.entry.AuthorList;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class PersonNamesPlausibilityComparator implements FieldValuePlausibilityComparator {
    @Override
    public ComparisonResult compare(String left, String right) {
        AuthorList leftAuthors = AuthorList.parse(left);
        AuthorList rightAuthors = AuthorList.parse(right);

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
