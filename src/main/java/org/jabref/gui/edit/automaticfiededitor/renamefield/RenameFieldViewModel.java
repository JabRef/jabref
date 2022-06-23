package org.jabref.gui.edit.automaticfiededitor.renamefield;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class RenameFieldViewModel extends AbstractViewModel {

    private final StringProperty newFieldName = new SimpleStringProperty();
    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>();

    private final ObservableList<Field> allFields = FXCollections.observableArrayList();
    private final List<BibEntry> selectedEntries;
    private final BibDatabaseContext databaseContext;
    private final NamedCompound dialogEdits;

    public RenameFieldViewModel(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext, NamedCompound dialogEdits) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;
        this.dialogEdits = dialogEdits;

        allFields.addAll(databaseContext.getDatabase().getAllVisibleFields());
    }

    public String getNewFieldName() {
        return newFieldName.get();
    }

    public StringProperty newFieldNameProperty() {
        return newFieldName;
    }

    public Field getSelectedField() {
        return selectedField.get();
    }

    public ObjectProperty<Field> selectedFieldProperty() {
        return selectedField;
    }

    public ObservableList<Field> getAllFields() {
        return allFields;
    }

    public void renameField() {
        NamedCompound renameEdit = new NamedCompound("RENAME_EDIT");

       new MoveFieldValueAction(selectedField.get(),
               FieldFactory.parseField(newFieldName.get()),
               selectedEntries,
               renameEdit).execute();

        if (renameEdit.hasEdits()) {
            renameEdit.end();
            dialogEdits.addEdit(renameEdit);
        }
    }
}
