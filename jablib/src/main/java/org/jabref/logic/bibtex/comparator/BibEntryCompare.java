package org.jabref.logic.bibtex.comparator;

import java.util.Collection;
import java.util.Collections;
import java.util.SequencedSet;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/// This class is a strict entry comparator. See [Result] for possible comparison outcomes.
///
/// Alternative comparison approaches:
///
/// - In package `org.jabref.logic.bibtex.comparator`, one finds multiple comparators implementing `Comparator<BibEntry>`.
///   Example: [org.jabref.logic.bibtex.comparator.EntryComparator]
/// - A whole library can be compared using [org.jabref.logic.bibtex.comparator.BibDatabaseDiff].
/// - Similarity checks can be executed by the duplicate check: [org.jabref.logic.database.DuplicateCheck#isDuplicate]
public class BibEntryCompare {
    public enum Result { SUBSET, EQUAL, SUPERSET, DISJUNCT, DISJUNCT_OR_EQUAL_FIELDS, DIFFERENT }

    /**
     * @return first {Result} second, e.g., if first is a subset of second, then Result.SUBSET is returned.
     */
    public static Result compareEntries(BibEntry first, BibEntry second) {
        if (first.equals(second)) {
            return Result.EQUAL;
        }

        SequencedSet<Field> fieldsFirst = first.getFields();
        SequencedSet<Field> secondFields = second.getFields();

        if (fieldsFirst.containsAll(secondFields)) {
            if (isSubSet(second, first)) {
                return Result.SUPERSET;
            }
            return Result.DIFFERENT;
        }

        if (secondFields.containsAll(fieldsFirst)) {
            if (isSubSet(first, second)) {
                return Result.SUBSET;
            }
            return Result.DIFFERENT;
        }

        if (Collections.disjoint(fieldsFirst, secondFields)) {
            return Result.DISJUNCT;
        }

        fieldsFirst.retainAll(secondFields);
        if (isSubSet(first, second, fieldsFirst)) {
            return Result.DISJUNCT_OR_EQUAL_FIELDS;
        }

        return Result.DIFFERENT;
    }

    private static boolean isSubSet(BibEntry candidateSubSet, BibEntry candidateSuperSet) {
        return isSubSet(candidateSubSet, candidateSuperSet, candidateSubSet.getFields());
    }

    private static boolean isSubSet(BibEntry candidateSubSet, BibEntry candidateSuperSet, Collection<Field> fields) {
        for (Field field : fields) {
            String subValue = candidateSubSet.getField(field).get();
            boolean isEqualValue = candidateSuperSet.getField(field)
                                                    .filter(superValue -> superValue.equals(subValue))
                                                    .isPresent();
            if (!isEqualValue) {
                return false;
            }
        }
        return true;
    }
}
