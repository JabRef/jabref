package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

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
        if (entryEditor.getCurrentlyEditedEntry() == null) {
            return List.of();
        }

        return FieldFactory.getAllFieldsWithOutInternal().stream()
                           .map(Field::getName)
                           .distinct()
                           .sorted()
                           .collect(Collectors.toList());
    }
}
