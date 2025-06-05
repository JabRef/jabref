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
    public static boolean mergeEntries(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        LOGGER.debug("Entry from fetcher: {}", entryFromFetcher);
        LOGGER.debug("Entry from library: {}", entryFromLibrary);

        boolean typeChanged = mergeEntryType(entryFromFetcher, entryFromLibrary, undoManager);
        boolean fieldsChanged = mergeFields(entryFromFetcher, entryFromLibrary, undoManager);
        boolean fieldsRemoved = removeFieldsNotPresentInFetcher(entryFromFetcher, entryFromLibrary, undoManager);

        return typeChanged || fieldsChanged || fieldsRemoved;
    }

    private static boolean mergeEntryType(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        EntryType fetcherType = entryFromFetcher.getType();
        EntryType libraryType = entryFromLibrary.getType();

        if (!libraryType.equals(fetcherType)) {
            LOGGER.debug("Updating type {} -> {}", libraryType, fetcherType);
            entryFromLibrary.setType(fetcherType);
            undoManager.addEdit(new UndoableChangeType(entryFromLibrary, libraryType, fetcherType));
            return true;
        }
        return false;
    }

    private static boolean mergeFields(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        Set<Field> allFields = new LinkedHashSet<>();
        allFields.addAll(entryFromFetcher.getFields());
        allFields.addAll(entryFromLibrary.getFields());

        boolean anyFieldsChanged = false;

        for (Field field : allFields) {
            Optional<String> fetcherValue = entryFromFetcher.getField(field);
            Optional<String> libraryValue = entryFromLibrary.getField(field);

            if (fetcherValue.isPresent() && shouldUpdateField(fetcherValue.get(), libraryValue)) {
                LOGGER.debug("Updating field {}: {} -> {}", field, libraryValue.orElse(null), fetcherValue.get());
                entryFromLibrary.setField(field, fetcherValue.get());
                undoManager.addEdit(new UndoableFieldChange(entryFromLibrary, field, libraryValue.orElse(null), fetcherValue.get()));
                anyFieldsChanged = true;
            }
        }
        return anyFieldsChanged;
    }

    private static boolean removeFieldsNotPresentInFetcher(BibEntry entryFromFetcher, BibEntry entryFromLibrary, NamedCompound undoManager) {
        Set<Field> obsoleteFields = new LinkedHashSet<>(entryFromLibrary.getFields());
        obsoleteFields.removeAll(entryFromFetcher.getFields());

        boolean anyFieldsRemoved = false;

        for (Field field : obsoleteFields) {
            if (FieldFactory.isInternalField(field)) {
                continue;
            }

            Optional<String> value = entryFromLibrary.getField(field);
            if (value.isPresent()) {
                LOGGER.debug("Removing obsolete field {} with value {}", field, value.get());
                entryFromLibrary.clearField(field);
                undoManager.addEdit(new UndoableFieldChange(entryFromLibrary, field, value.get(), null));
                anyFieldsRemoved = true;
            }
        }
        return anyFieldsRemoved;
    }

    private static boolean shouldUpdateField(String fetcherValue, Optional<String> libraryValue) {
        return libraryValue.map(value -> fetcherValue.length() > value.length())
                           .orElse(true);
    }
}
