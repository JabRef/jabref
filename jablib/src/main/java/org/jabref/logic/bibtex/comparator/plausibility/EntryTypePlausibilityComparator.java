package org.jabref.logic.bibtex.comparator.plausibility;

import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.model.entry.types.StandardEntryType;
/**
*  If the left entry type is misc then prefer the the right value 
**/
public class EntryTypePlausibilityComparator implements FieldValuePlausibilityComparator {

    // Only the factory may instantiate this
    EntryTypePlausibilityComparator() {
    }

    @Override
    public ComparisonResult compare(String leftValue, String rightValue) {
        if (leftValue.equalsIgnoreCase(StandardEntryType.Misc.getName())) {
            return ComparisonResult.RIGHT_BETTER;
        }
        return ComparisonResult.UNDETERMINED;
    }
}
