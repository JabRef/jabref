package org.jabref.gui.edit.automaticfiededitor.renamefield;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel;
import org.jabref.gui.edit.automaticfiededitor.MoveFieldValueAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

public class RenameFieldViewModel extends AbstractAutomaticFieldEditorTabViewModel {

    private final StringProperty newFieldName = new SimpleStringProperty("");
    private final ObjectProperty<Field> selectedField = new SimpleObjectProperty<>(StandardField.AUTHOR);
    private final List<BibEntry> selectedEntries;
    private final NamedCompound dialogEdits;

    public RenameFieldViewModel(List<BibEntry> selectedEntries, BibDatabase database, NamedCompound dialogEdits) {
        super(database);
        this.selectedEntries = selectedEntries;
        this.dialogEdits = dialogEdits;
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

    public void renameField() {
        NamedCompound renameEdit = new NamedCompound("RENAME_EDIT");
        // TODO: find a way to access the affected entries count or fire the event bus
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
