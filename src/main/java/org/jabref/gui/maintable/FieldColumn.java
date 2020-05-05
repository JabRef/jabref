package org.jabref.gui.maintable;

import javafx.beans.value.ObservableValue;

import org.jabref.model.entry.field.OrFields;

/**
 * A column that displays the text-value of the field
 */
public class FieldColumn extends MainTableColumn<String> {

    private final OrFields fields;

    public FieldColumn(MainTableColumnModel model, OrFields fields) {
        super(model);
        this.fields = fields;

        setText(getDisplayName());
        setCellValueFactory(param -> getFieldValue(param.getValue()));
    }

    /**
     * Get the table column name to be displayed in the UI
     *
     * @return name to be displayed. null if field is empty.
     */
    @Override
    public String getDisplayName() {
        return fields.getDisplayName();
    }

    private ObservableValue<String> getFieldValue(BibEntryTableViewModel entry) {
        if (fields.isEmpty()) {
            return null;
        } else {
            return entry.getFields(fields);
        }
    }
}
