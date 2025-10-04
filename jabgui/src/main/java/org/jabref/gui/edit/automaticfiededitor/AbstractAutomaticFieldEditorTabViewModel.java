package org.jabref.gui.edit.automaticfiededitor;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;

public abstract class AbstractAutomaticFieldEditorTabViewModel extends AbstractViewModel {
    protected final StateManager stateManager;

    private final ObservableList<Field> allFields = FXCollections.observableArrayList();

    public AbstractAutomaticFieldEditorTabViewModel(@NonNull BibDatabase bibDatabase,
                                                    @NonNull StateManager stateManager) {
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
}
