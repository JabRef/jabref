package org.jabref.gui.mergeentries;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class MultiMergeEntriesViewModel extends AbstractViewModel {

    private HashMap<Field, FieldRow> fieldRows = new HashMap<>();

    public static class FieldRow {
        public int rowIndex;
        public ToggleGroup toggleGroup = new ToggleGroup();
        private final StringProperty entryEditorText = new SimpleStringProperty();

        private Field field;

        public FieldRow(int rowIndex, Field field) {
            this.rowIndex = rowIndex;
            this.field = field;
        }

        public StringProperty entryEditorTextProperty() {
            return entryEditorText;
        }

        public String getEntryEditorText() {
            return entryEditorText.get();
        }

        public void setEntryEditorText(String newEntryEditorText) {
            entryEditorText.set(newEntryEditorText);
        }
    }

    public HashMap<Field, FieldRow> getFieldRows() {
        return fieldRows;
    }

    public BibEntry getMergedEntry() {
        BibEntry mergedEntry = new BibEntry();
        for (Map.Entry<Field, FieldRow> fieldRowEntry : fieldRows.entrySet()) {
            String fieldValue = fieldRowEntry.getValue().getEntryEditorText();
            if (fieldValue != null) {
                mergedEntry.setField(fieldRowEntry.getKey(), fieldValue);
            }
        }
        return mergedEntry;
    }
}
