
package org.jabref.gui.mergeentries;

import java.util.LinkedHashSet;
import java.util.Objects;
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
 * Helper class for merging Entries.
 */
public final class MergeEntriesHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeEntriesHelper.class);

    private MergeEntriesHelper() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Merges two BibEntry objects with undo support.
     * Use this method when modifying entries directly in the UI.
     * The original entry will be updated with information from the fetched entry.
     */
    public static void mergeEntries(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound undoManager) {
        Objects.requireNonNull(originalEntry, "Original entry cannot be null");
        Objects.requireNonNull(fetchedEntry, "Fetched entry cannot be null");
        Objects.requireNonNull(undoManager, "Undo manager cannot be null");

        LOGGER.debug("Starting merge of entries. Original type: {}, Fetched type: {}",
                originalEntry.getType(), fetchedEntry.getType());

        updateEntryTypeIfDifferent(originalEntry, fetchedEntry, undoManager);
        updateFieldsWithNewInfo(originalEntry, fetchedEntry, undoManager);
        removeObsoleteFields(originalEntry, fetchedEntry, undoManager);

        LOGGER.debug("Finished merging entries");
    }

    private static void updateEntryTypeIfDifferent(final BibEntry originalEntry,
                                                   final BibEntry fetchedEntry, final NamedCompound undoManager) {
        EntryType oldType = originalEntry.getType();
        EntryType newType = fetchedEntry.getType();

        if (!oldType.equals(newType)) {
            LOGGER.debug("Updating entry type from {} to {}", oldType, newType);
            originalEntry.setType(newType);
            undoManager.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
        }
    }

    private static void updateFieldsWithNewInfo(final BibEntry originalEntry,
                                                final BibEntry fetchedEntry, final NamedCompound undoManager) {
        Set<Field> jointFields = getJointFields(originalEntry, fetchedEntry);
        LOGGER.debug("Processing {} joint fields for updates", jointFields.size());

        for (Field field : jointFields) {
            updateFieldIfNecessaryWithUndo(originalEntry, fetchedEntry, field, undoManager);
        }
    }

    private static void updateFieldIfNecessaryWithUndo(final BibEntry originalEntry,
                                                       final BibEntry fetchedEntry,
                                                       final Field field,
                                                       final NamedCompound undoManager) {
        fetchedEntry.getField(field)
                    .ifPresent(fetchedValue ->
                            originalEntry.getField(field)
                                         .map(originalValue -> fetchedValue.length() > originalValue.length())
                                         .filter(shouldUpdate -> shouldUpdate)
                                         .or(() -> {
                                             updateField(originalEntry, field,
                                                     originalEntry.getField(field).orElse(null),
                                                     fetchedValue,
                                                     undoManager);
                                             return Optional.empty();
                                         }));
    }

    private static void updateField(final BibEntry entry, final Field field,
                                    final String originalValue, final String newValue, final NamedCompound undoManager) {
        LOGGER.debug("Updating field '{}' from '{}' to '{}'", field, originalValue, newValue);
        entry.setField(field, newValue);
        undoManager.addEdit(new UndoableFieldChange(entry, field, originalValue, newValue));
    }

    private static void removeObsoleteFields(final BibEntry originalEntry,
                                             final BibEntry fetchedEntry,
                                             final NamedCompound undoManager) {

        Set<Field> jointFields = getJointFields(originalEntry, fetchedEntry);
        Set<Field> originalFields = getFields(originalEntry);

        LOGGER.debug("Checking {} original fields for obsolete entries", originalFields.size());

        for (Field field : originalFields) {
            if (!jointFields.contains(field) && !FieldFactory.isInternalField(field)) {
                removeFieldWithUndo(originalEntry, field, undoManager);
            }
        }
    }

    private static void removeFieldWithUndo(final BibEntry entry, final Field field,
                                            final NamedCompound undoManager) {
        Optional<String> originalValue = entry.getField(field);
        LOGGER.debug("Removing obsolete field '{}' with value '{}'", field, originalValue.orElse(null));

        entry.clearField(field);
        undoManager.addEdit(new UndoableFieldChange(entry, field, originalValue.orElse(null), null));
    }

    private static Set<Field> getFields(final BibEntry entry) {
        return new LinkedHashSet<>(entry.getFields());
    }

    private static Set<Field> getJointFields(final BibEntry entry1, final BibEntry entry2) {
        Set<Field> fields = new LinkedHashSet<>();
        fields.addAll(getFields(entry1));
        fields.addAll(getFields(entry2));
        return fields;
    }
}
