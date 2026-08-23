package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;

public class JumpToFieldViewModel extends AbstractViewModel {

    private final StringProperty searchText = new SimpleStringProperty("");
    private final EntryEditor entryEditor;

    public JumpToFieldViewModel(EntryEditor entryEditor) {
        this.entryEditor = entryEditor;
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public List<String> getFieldNames() {
        BibEntry entry = entryEditor.getCurrentlyEditedEntry();
        if (entry == null) {
            return List.of();
        }

        return suggestedFields(entry).stream()
                                     .map(Field::getName)
                                     .distinct()
                                     .sorted()
                                     .toList();
    }

    private List<Field> suggestedFields(BibEntry entry) {
        List<Field> suggestedFields = new ArrayList<>();
        suggestedFields.add(InternalField.KEY_FIELD);
        suggestedFields.addAll(entry.getFields());
        suggestedFields.addAll(FieldFactory.getAllFieldsWithOutInternal());
        return suggestedFields;
    }
}
