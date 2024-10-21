
package org.jabref.logic.importer;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;

public class MergeEntriesHelper {

    private MergeEntriesHelper() {
        // Private constructor to prevent instantiation
    }

    public static void mergeEntries(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound ce) {
        updateEntryTypeIfDifferent(originalEntry, fetchedEntry, ce);
        updateFieldsWithNewInfo(originalEntry, fetchedEntry, ce);
        removeObsoleteFields(originalEntry, fetchedEntry, ce);
    }

    private static void updateEntryTypeIfDifferent(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound ce) {
        EntryType oldType = originalEntry.getType();
        EntryType newType = fetchedEntry.getType();

        if (!oldType.equals(newType)) {
            originalEntry.setType(newType);
            ce.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
        }
    }

    private static void updateFieldsWithNewInfo(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound ce) {
        Set<Field> jointFields = getJointFields(originalEntry, fetchedEntry);
        for (Field field : jointFields) {
            updateFieldIfNecessary(originalEntry, fetchedEntry, field, ce);
        }
    }

    private static void updateFieldIfNecessary(BibEntry originalEntry, BibEntry fetchedEntry, Field field, NamedCompound ce) {
        fetchedEntry.getField(field).ifPresent(fetchedValue -> {
            Optional<String> originalValue = originalEntry.getField(field);
            if (originalValue.isEmpty() || fetchedValue.length() > originalValue.get().length()) {
                originalEntry.setField(field, fetchedValue);
                ce.addEdit(new UndoableFieldChange(originalEntry, field, originalValue.orElse(null), fetchedValue));
            }
        });
    }

    private static void removeObsoleteFields(BibEntry originalEntry, BibEntry fetchedEntry, NamedCompound ce) {
        Set<Field> jointFields = getJointFields(originalEntry, fetchedEntry);
        Set<Field> originalFields = getFields(originalEntry);

        for (Field field : originalFields) {
            if (!jointFields.contains(field) && !FieldFactory.isInternalField(field)) {
                removeField(originalEntry, field, ce);
            }
        }
    }

    private static void removeField(BibEntry entry, Field field, NamedCompound ce) {
        Optional<String> originalValue = entry.getField(field);
        entry.clearField(field);
        ce.addEdit(new UndoableFieldChange(entry, field, originalValue.orElse(null), null));
    }

    private static Set<Field> getFields(BibEntry entry) {
        // Get sorted set of fields for consistent ordering
        return entry.getFields().stream()
                    .sorted(Comparator.comparing(Field::getName))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<Field> getJointFields(BibEntry entry1, BibEntry entry2) {
        Set<Field> fields = new LinkedHashSet<>();
        fields.addAll(getFields(entry1));
        fields.addAll(getFields(entry2));
        return fields;
    }
}
