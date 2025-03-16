package org.jabref.logic.bibtex.comparator;

import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class ConflictResolver {

    public enum ResolveStrategy {
        KEEP_LEFT, KEEP_RIGHT, MERGE_MANUALLY
    }

    public static BibEntry resolveConflicts(BibEntry left, BibEntry right, ResolveStrategy strategy) {
        BibEntry mergedEntry = new BibEntry();

        Map<Field, String> leftFields = left.getFieldMap();
        Map<Field, String> rightFields = right.getFieldMap();

        for (Field field : leftFields.keySet()) {
            String leftValue = leftFields.get(field);
            String rightValue = rightFields.getOrDefault(field, "");

            if (leftValue.equals(rightValue)) {
                mergedEntry.setField(field, leftValue);
            } else {
                switch (strategy) {
                    case KEEP_LEFT:
                        mergedEntry.setField(field, leftValue);
                        break;

                    case KEEP_RIGHT:
                        mergedEntry.setField(field, rightValue);
                        break;

                    case MERGE_MANUALLY:
                        mergedEntry.setField(field, "[CONFLICT] Left: " + leftValue + " | Right: " + rightValue);
                        break;
                }
            }
        }

        for (Field field : rightFields.keySet()) {
            if (!leftFields.containsKey(field)) {
                mergedEntry.setField(field, rightFields.get(field));
            }
        }
        return mergedEntry;
    }
}


