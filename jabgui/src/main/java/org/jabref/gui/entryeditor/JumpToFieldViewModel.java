package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.util.strings.StringUtil;
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

    public List<String> getMatchingFieldNames(String userText) {
        String normalizedFieldName = userText.trim().toLowerCase(Locale.ROOT);
        return getFieldNames().stream()
                              .filter(fieldName -> fieldName.toLowerCase(Locale.ROOT).startsWith(normalizedFieldName))
                              .toList();
    }

    /// `true` when jumping to `fieldName` would add a field the entry editor does not offer,
    /// that is: a custom field that does not exist yet.
    public boolean isNewField(String fieldName) {
        if (StringUtil.isBlank(fieldName)) {
            return false;
        }
        return getMatchingFieldNames(fieldName).isEmpty();
    }

    private List<Field> suggestedFields(BibEntry entry) {
        List<Field> suggestedFields = new ArrayList<>();
        suggestedFields.add(InternalField.KEY_FIELD);
        suggestedFields.addAll(entry.getFields());
        suggestedFields.addAll(FieldFactory.getAllFieldsWithOutInternal());
        return suggestedFields;
    }
}
