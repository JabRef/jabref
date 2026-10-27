package org.jabref.gui.edit.automaticfieldeditor;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.Notifications;
import org.jabref.gui.StateManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.undo.CompoundEdit;

import org.jspecify.annotations.NonNull;

public abstract class AbstractAutomaticFieldEditorTabViewModel extends AbstractViewModel {
    @NonNull protected final DialogService dialogService;
    @NonNull protected final StateManager stateManager;
    @NonNull private final CompoundEdit compoundEdit;

    private final ObservableList<Field> allFields = FXCollections.observableArrayList();

    public AbstractAutomaticFieldEditorTabViewModel(@NonNull BibDatabase bibDatabase,
                                                    @NonNull CompoundEdit compoundEdit,
                                                    @NonNull DialogService dialogService,
                                                    @NonNull StateManager stateManager) {
        this.compoundEdit = compoundEdit;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        addFields(EnumSet.allOf(StandardField.class));
        addFields(bibDatabase.getAllVisibleFields());
        allFields.sort(Comparator.comparing(Field::getName));
    }

    public ObservableList<Field> getAllFields() {
        return allFields;
    }

    private void addFields(Collection<? extends Field> fields) {
        Set<Field> fieldsSet = new HashSet<>(allFields);
        fieldsSet.addAll(fields);
        allFields.setAll(fieldsSet);
    }

    /// Folds `edits` into the dialog's step and reports how many entries it touched.
    ///
    /// `affectedEntries` is passed rather than carried by `edits`, because it is what this
    /// notification says to the user and not part of the change being recorded. An
    /// `AutomaticFieldEditorUndoableEdit` subclass used to carry it, which cost [CompoundEdit]
    /// its `final` and gave a value type a field the undo model never reads.
    protected void addEdit(CompoundEdit edits, int affectedEntries) {
        compoundEdit.addEdit(edits);
        dialogService.notify(new Notifications.UiNotification(
                Localization.lang("Automatic field editor"),
                Localization.lang("%0 / %1 affected entries", affectedEntries, stateManager.getSelectedEntries().size()))
                .withAutoClose(Duration.seconds(5)));
    }
}
