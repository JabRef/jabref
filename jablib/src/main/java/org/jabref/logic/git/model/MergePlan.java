package org.jabref.logic.git.model;

import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * A data structure representing the result of semantic diffing between base and remote entries.
 *
 * The idea is: library A and then this patch leads to library B (if this patch is calculated between A and B)
 * This is a bit different from {@link org.jabref.logic.bibtex.comparator.BibEntryDiff}, which "just" contains two entries on any field diff, but leaves "computation" on the caller.
 * Thus, the data structure is different, because here, only the patches are contained, not any source or target.
 * "patch" in the sense of "commands" to be applied to the source to get the target
 *
 * @param fieldPatches contains field-level modifications per citation key. citationKey -> field -> newValue (null = delete)
 * @param newEntries entries present in remote but not in base/local
 */
public record MergePlan(
        Map<String, Map<Field, String>> fieldPatches,
        List<BibEntry> newEntries,
        List<String> deletedEntryKeys
) {
    public static MergePlan empty() {
        return new MergePlan(Map.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return fieldPatches.isEmpty() && newEntries.isEmpty() && deletedEntryKeys.isEmpty();
    }
}
