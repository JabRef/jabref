package org.jabref.gui.mergeentries;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;

public class UpdateOriginalEntry {
    private final BibEntry originalEntry;
    private final Optional<BibEntry> mergedEntry;
    private final Optional<WebFetcher> fetcher;
    private final DialogService dialogService;
    private final UndoManager undoManager;

    public UpdateOriginalEntry(BibEntry originalEntry, Optional<BibEntry> mergedEntry, Optional<WebFetcher> fetcher, DialogService dialogService, UndoManager undoManager) {
        this.originalEntry = originalEntry;
        this.mergedEntry = mergedEntry;
        this.fetcher = fetcher;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
    }

    public void update() {
        if (mergedEntry.isPresent() && !mergedEntry.get().getFields().isEmpty()) {
            String editName = fetcher
                    .map(value -> Localization.lang("Merge entry with %0 information", value.getName()))
                    .orElseGet(() -> Localization.lang("Merge entry with information"));
            NamedCompoundEdit compoundEdit = new NamedCompoundEdit(editName);

            // Updated the original entry with the new fields
            Set<Field> jointFields = new TreeSet<>(Comparator.comparing(Field::getName));
            jointFields.addAll(mergedEntry.get().getFields());
            Set<Field> originalFields = new TreeSet<>(Comparator.comparing(Field::getName));
            originalFields.addAll(originalEntry.getFields());
            boolean edited = false;

            // entry type
            EntryType oldType = originalEntry.getType();
            EntryType newType = mergedEntry.get().getType();

            if (!oldType.equals(newType)) {
                originalEntry.setType(newType);
                compoundEdit.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
                edited = true;
            }

            // fields
            for (Field field : jointFields) {
                Optional<String> originalString = originalEntry.getField(field);
                Optional<String> mergedString = mergedEntry.get().getField(field);
                if (originalString.isEmpty() || !originalString.equals(mergedString)) {
                    originalEntry.setField(field, mergedString.get()); // mergedString always present
                    compoundEdit.addEdit(new UndoableFieldChange(originalEntry, field, originalString.orElse(null),
                            mergedString.get()));
                    edited = true;
                }
            }

            // Remove fields which are not in the merged entry, unless they are internal fields
            for (Field field : originalFields) {
                if (!jointFields.contains(field) && !FieldFactory.isInternalField(field)) {
                    Optional<String> originalString = originalEntry.getField(field);
                    originalEntry.clearField(field);
                    compoundEdit.addEdit(new UndoableFieldChange(originalEntry, field, originalString.get(), null)); // originalString always present
                    edited = true;
                }
            }

            if (edited) {
                String notificationMessage = fetcher
                        .map(value -> Localization.lang("Updated entry with info from %0", value.getName()))
                        .orElseGet(() -> Localization.lang("Updated entry with merged information from multiple sources"));
                compoundEdit.end();
                undoManager.addEdit(compoundEdit);
                dialogService.notify(notificationMessage);
            } else {
                dialogService.notify(Localization.lang("No information added"));
            }
        } else {
            dialogService.notify(Localization.lang("Canceled merging entries"));
        }
    }
}
