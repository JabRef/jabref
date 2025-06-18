package org.jabref.logic.git.util;

import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * A data structure representing the result of semantic diffing between base and remote entries.
 * Currently placed in the logic package as a merge-specific value object since it's not a persistent or user-visible concept.
 * may be moved to model in the future
 *
 * @param fieldPatches contain field-level modifications per citation key. citationKey -> field -> newValue (null = delete)
 * @param newEntries entries present in remote but not in base/local
 */
public record MergePlan(
        Map<String, Map<Field, String>> fieldPatches,
        List<BibEntry> newEntries) { }
