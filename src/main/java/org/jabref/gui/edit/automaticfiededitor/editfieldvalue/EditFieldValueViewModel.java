package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class EditFieldValueViewModel extends AbstractViewModel {
    private final BibDatabaseContext databaseContext;
    private final List<BibEntry> selectedEntries;
    private final UndoManager undoManager;

    private final StringProperty fieldValue = new SimpleStringProperty();

    private final StringProperty selectedField = new SimpleStringProperty();

    private final ObservableList<String> allFieldNames = FXCollections.observableArrayList();

    public EditFieldValueViewModel(BibDatabaseContext databaseContext, List<BibEntry> selectedEntries, UndoManager undoManager) {
        this.databaseContext = databaseContext;
        this.selectedEntries = selectedEntries;
        this.undoManager = undoManager;

        allFieldNames.addAll(databaseContext.getDatabase().getAllVisibleFields().stream().map(Field::getName).toList());
    }

    public void clearSelectedField() {
    }

    public void setFieldValue() {
    }

    public void appendToFieldValue() {
    }

    public ObservableList<String> getAllFieldNames() {
        return allFieldNames;
    }
}
