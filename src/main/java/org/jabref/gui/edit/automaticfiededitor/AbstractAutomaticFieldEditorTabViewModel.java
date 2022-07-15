package org.jabref.gui.edit.automaticfiededitor;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;

public abstract class AbstractAutomaticFieldEditorTabViewModel extends AbstractViewModel {

    private final ObservableSet<Field> allFields = FXCollections.observableSet();

    public AbstractAutomaticFieldEditorTabViewModel(BibDatabase bibDatabase) {
        Objects.requireNonNull(bibDatabase);
        Bindings.bindContent(allFields, bibDatabase.getAllVisibleFields());
    }

    public ObservableSet<Field> getAllFields() {
        return allFields;
    }
}
