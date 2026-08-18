package org.jabref.gui.mergeentries;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.undo.BibChangeEdit;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.change.EntryTypeEdit;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;

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
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(editName);
        boolean edited = updateEntryType(mergedEntry, compoundEdit);

        if (!mergedEntry.getFields().isEmpty()) {
            edited = updateFields(mergedEntry, compoundEdit) || edited;
        }

        if (edited) {
            compoundEdit.end();
            undoManager.addEdit(compoundEdit);
            dialogService.notify(successMessage);
        } else {
            dialogService.notify(Localization.lang("No information added"));
        }
    }

    private boolean updateEntryType(BibEntry mergedEntry, NamedCompoundEdit compoundEdit) {
        EntryType oldType = originalEntry.getType();
        EntryType newType = mergedEntry.getType();

        if (oldType.equals(newType)) {
            return false;
        }

        originalEntry.setType(newType);
        compoundEdit.addEdit(new BibChangeEdit(new EntryTypeEdit(originalEntry, oldType, newType)));
        return true;
    }

    private boolean updateFields(BibEntry mergedEntry, NamedCompoundEdit compoundEdit) {
        Set<Field> mergedFields = new TreeSet<>(Comparator.comparing(Field::getName));
        mergedFields.addAll(mergedEntry.getFields());

        Set<Field> originalFields = new TreeSet<>(Comparator.comparing(Field::getName));
        originalFields.addAll(originalEntry.getFields());

        boolean edited = false;

        // This loop is for setting fields
        for (Field field : mergedFields) {
            Optional<String> originalString = originalEntry.getField(field);
            Optional<String> mergedString = mergedEntry.getField(field);

            if (originalString.isEmpty() || !originalString.equals(mergedString)) {
                edited = applyFieldChange(originalEntry.setField(field, mergedString.orElseThrow()), compoundEdit) || edited;
            }
        }

        // This one is for clearing fields
        for (Field field : originalFields) {
            if (!mergedFields.contains(field) && !FieldFactory.isInternalField(field)) {
                edited = applyFieldChange(originalEntry.clearField(field), compoundEdit) || edited;
            }
        }

        return edited;
    }

    private static boolean applyFieldChange(Optional<FieldChange> fieldChange, NamedCompoundEdit compoundEdit) {
        return fieldChange.map(change -> {
            compoundEdit.addEdit(new UndoableFieldChange(change));
            return true;
        }).orElse(false);
    }
}
