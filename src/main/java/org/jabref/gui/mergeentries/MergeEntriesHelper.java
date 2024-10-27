
package org.jabref.gui.mergeentries;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for merging bibliography entries with undo support.
 * Source entry data is merged into the library entry, with longer field values preferred
 * and obsolete fields removed.
 */
public final class MergeEntriesHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeEntriesHelper.class);

    private MergeEntriesHelper() {
    }

    /**
     * Merges two BibEntry objects with undo support.
     *
     * @param entryFromFetcher The entry containing new information (source, from the fetcher)
     * @param entryFromLibrary The entry to be updated (target, from the library)
     * @param undoManager Compound edit to collect undo information
     */
    public static void mergeEntries(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        LOGGER.debug("Entry from fetcher: {}", entryFromFetcher);
        LOGGER.debug("Entry from library: {}", entryFromLibrary);

        mergeEntryType(entryFromFetcher, entryFromLibrary, undoManager);
        mergeFields(entryFromFetcher, entryFromLibrary, undoManager);
        removeFieldsNotPresentInFetcher(entryFromFetcher, entryFromLibrary, undoManager);
    }

    private static void mergeEntryType(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        EntryType fetcherType = entryFromFetcher.getType();
        EntryType libraryType = entryFromLibrary.getType();

        if (!libraryType.equals(fetcherType)) {
            LOGGER.debug("Updating type {} -> {}", libraryType, fetcherType);
            entryFromLibrary.setType(fetcherType);
            undoManager.addEdit(new UndoableChangeType(entryFromLibrary, libraryType, fetcherType));
        }
    }

    private static void mergeFields(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        Set<Field> allFields = new LinkedHashSet<>();
        allFields.addAll(entryFromFetcher.getFields());
        allFields.addAll(entryFromLibrary.getFields());

        for (Field field : allFields) {
            Optional<String> fetcherValue = entryFromFetcher.getField(field);
            Optional<String> libraryValue = entryFromLibrary.getField(field);

            fetcherValue.ifPresent(newValue -> {
                if (shouldUpdateField(newValue, libraryValue)) {
                    LOGGER.debug("Updating field {}: {} -> {}", field, libraryValue.orElse(null), newValue);
                    entryFromLibrary.setField(field, newValue);
                    undoManager.addEdit(new UndoableFieldChange(entryFromLibrary, field, libraryValue.orElse(null), newValue));
                }
            });
        }
    }

    private static boolean shouldUpdateField(String fetcherValue, Optional<String> libraryValue) {
        return libraryValue.map(value -> fetcherValue.length() > value.length())
                           .orElse(true);
    }

    /**
     * Removes fields from the library entry that are not present in the fetcher entry.
     * This ensures the merged entry only contains fields that are considered current
     * according to the fetched data.
     */
    private static void removeFieldsNotPresentInFetcher(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        Set<Field> obsoleteFields = new LinkedHashSet<>(entryFromLibrary.getFields());
        obsoleteFields.removeAll(entryFromFetcher.getFields());

        for (Field field : obsoleteFields) {
            if (FieldFactory.isInternalField(field)) {
                continue;
            }

            entryFromLibrary.getField(field).ifPresent(value -> {
                LOGGER.debug("Removing obsolete field {} with value {}", field, value);
                entryFromLibrary.clearField(field);
                undoManager.addEdit(new UndoableFieldChange(entryFromLibrary, field, value, null));
            });
        }
    }
}
