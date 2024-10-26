
package org.jabref.gui.mergeentries;

import java.util.LinkedHashSet;
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
        // Private constructor to prevent instantiation
    }

    /**
     * Merges two BibEntry objects with undo support.
     * Following the typical source -> target pattern, but with domain-specific naming.
     *
     * @param entryFromLibrary The entry to be updated (target, from the library)
     * @param source The entry containing new information (source, from the fetcher)
     * @param undoManager Compound edit to collect undo information
     */
    public static void mergeEntries(BibEntry entryFromLibrary, BibEntry source, NamedCompound undoManager) {
        LOGGER.debug("Entry from library: {}", entryFromLibrary);
        LOGGER.debug("Source entry: {}", source);

        mergeEntryType(source, entryFromLibrary, undoManager);
        mergeFields(source, entryFromLibrary, undoManager);
        removeObsoleteFields(source, entryFromLibrary, undoManager);
    }

    private static void mergeEntryType(BibEntry source, BibEntry entryFromLibrary, NamedCompound undoManager) {
        EntryType libraryType = entryFromLibrary.getType();
        EntryType sourceType = source.getType();

        if (!libraryType.equals(sourceType)) {
            LOGGER.debug("Updating type {} -> {}", libraryType, sourceType);
            entryFromLibrary.setType(sourceType);
            undoManager.addEdit(new UndoableChangeType(entryFromLibrary, libraryType, sourceType));
        }
    }

    private static void mergeFields(BibEntry source, BibEntry entryFromLibrary, NamedCompound undoManager) {
        Set<Field> allFields = new LinkedHashSet<>();
        allFields.addAll(source.getFields());
        allFields.addAll(entryFromLibrary.getFields());

        for (Field field : allFields) {
            String sourceValue = source.getField(field).orElse(null);
            if (sourceValue == null) {
                continue;
            }

            String libraryValue = entryFromLibrary.getField(field).orElse(null);
            if (shouldUpdateField(libraryValue, sourceValue)) {
                LOGGER.debug("Updating field {}: {} -> {}", field, libraryValue, sourceValue);
                entryFromLibrary.setField(field, sourceValue);
                undoManager.addEdit(new UndoableFieldChange(entryFromLibrary, field, libraryValue, sourceValue));
            }
        }
    }

    private static boolean shouldUpdateField(String libraryValue, String sourceValue) {
        if (libraryValue == null) {
            return true;
        }
        return sourceValue.length() > libraryValue.length();
    }

    private static void removeObsoleteFields(BibEntry source, BibEntry entryFromLibrary, NamedCompound undoManager) {
        Set<Field> obsoleteFields = new LinkedHashSet<>(entryFromLibrary.getFields());
        obsoleteFields.removeAll(source.getFields());

        for (Field field : obsoleteFields) {
            if (FieldFactory.isInternalField(field)) {
                continue;
            }

            String value = entryFromLibrary.getField(field).orElse(null);
            LOGGER.debug("Removing obsolete field {} with value {}", field, value);
            entryFromLibrary.clearField(field);
            undoManager.addEdit(new UndoableFieldChange(entryFromLibrary, field, value, null));
        }
    }
}
