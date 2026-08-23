package org.jabref.gui.mergeentries;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoManager;
import org.jabref.model.undo.UndoableChangeType;

import org.jspecify.annotations.NullMarked;

/// A class that is desired to update the original entry with the merged entry in the multi-way merge dialog.
@NullMarked
class UpdateOriginalEntry {

    private final BibEntry originalEntry;
    private final Optional<BibEntry> mergedEntry;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final String editName;
    private final String successMessage;

    UpdateOriginalEntry(BibEntry originalEntry,
                        Optional<BibEntry> mergedEntry,
                        DialogService dialogService,
                        UndoManager undoManager,
                        String editName,
                        String successMessage) {
        this.originalEntry = originalEntry;
        this.mergedEntry = mergedEntry;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.editName = editName;
        this.successMessage = successMessage;
    }

    // [impl->req~ux.update-entry-web-info.apply-merge-result~1]
    public void update() {
        mergedEntry.ifPresentOrElse(this::updateOriginalEntry, () -> dialogService.notify(Localization.lang("Canceled merging entries")));
    }

    /// If any differences are found between the original entry and the merged entry, the original entry will be updated with the merged entry's information.
    private void updateOriginalEntry(BibEntry mergedEntry) {
        boolean edited = undoManager.addEdit(editName, edit -> {
            updateEntryType(mergedEntry, edit);
            if (!mergedEntry.getFields().isEmpty()) {
                updateFields(mergedEntry, edit);
            }
        });

        if (edited) {
            dialogService.notify(successMessage);
        } else {
            dialogService.notify(Localization.lang("No information added"));
        }
    }

    private void updateEntryType(BibEntry mergedEntry, CompoundEdit recorder) {
        EntryType oldType = originalEntry.getType();
        EntryType newType = mergedEntry.getType();

        if (oldType.equals(newType)) {
            return;
        }

        originalEntry.setType(newType);
        recorder.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
    }

    private void updateFields(BibEntry mergedEntry, CompoundEdit recorder) {
        Set<Field> mergedFields = new TreeSet<>(Comparator.comparing(Field::getName));
        mergedFields.addAll(mergedEntry.getFields());

        Set<Field> originalFields = new TreeSet<>(Comparator.comparing(Field::getName));
        originalFields.addAll(originalEntry.getFields());

        // This loop is for setting fields
        for (Field field : mergedFields) {
            Optional<String> originalString = originalEntry.getField(field);
            Optional<String> mergedString = mergedEntry.getField(field);

            if (originalString.isEmpty() || !originalString.equals(mergedString)) {
                recorder.addEdit(originalEntry.setField(field, mergedString.orElseThrow()));
            }
        }

        // This one is for clearing fields
        for (Field field : originalFields) {
            if (!mergedFields.contains(field) && !FieldFactory.isInternalField(field)) {
                recorder.addEdit(originalEntry.clearField(field));
            }
        }
    }
}
