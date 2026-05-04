package org.jabref.gui.edit.automaticfiededitor;

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
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;

public abstract class AbstractAutomaticFieldEditorTabViewModel extends AbstractViewModel {
    @NonNull protected final DialogService dialogService;
    @NonNull protected final StateManager stateManager;
    @NonNull private final NamedCompoundEdit compoundEdit;

    private final ObservableList<Field> allFields = FXCollections.observableArrayList();

    public AbstractAutomaticFieldEditorTabViewModel(@NonNull BibDatabase bibDatabase,
                                                    @NonNull NamedCompoundEdit compoundEdit,
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

    protected void addEdit(AutomaticFieldEditorUndoableEdit edits) {
        compoundEdit.addEdit(edits);
        dialogService.notify(new Notifications.UiNotification(
                Localization.lang("Automatic field editor"),
                Localization.lang("%0 / %1 affected entries", edits.getAffectedEntries(), stateManager.getSelectedEntries().size()))
                .withAutoClose(Duration.seconds(5)));
    }
}
