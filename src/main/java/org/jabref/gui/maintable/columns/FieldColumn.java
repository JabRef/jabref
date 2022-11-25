package org.jabref.gui.maintable.columns;

import javafx.beans.value.ObservableValue;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.comparator.NumericFieldComparator;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.UnknownField;

import com.google.common.collect.Iterables;

/**
 * A column that displays the text-value of the field
 */
public class FieldColumn extends MainTableColumn<String> {

    private final OrFields fields;

    public FieldColumn(MainTableColumnModel model) {
        super(model);
        this.fields = FieldFactory.parseOrFields(model.getQualifier());

        setText(getDisplayName());
        setCellValueFactory(param -> getFieldValue(param.getValue()));

        new ValueTableCellFactory<BibEntryTableViewModel, String>()
                .withText(text -> text)
                .install(this);

        if (fields.size() == 1) {
            // comparator can't parse more than one value
            Field field = Iterables.getOnlyElement(fields);

            if (field instanceof UnknownField || field.isNumeric()) {
                this.setComparator(new NumericFieldComparator());
            }
        }

        this.setSortable(true);
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
