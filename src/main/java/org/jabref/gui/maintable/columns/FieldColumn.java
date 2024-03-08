package org.jabref.gui.maintable.columns;

import java.util.Comparator;
import java.util.Map;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.comparator.NumericFieldComparator;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.UnknownField;

import com.google.common.collect.MoreCollectors;

/**
 * A column that displays the text-value of the field
 */
public class FieldColumn extends MainTableColumn<Map<ObservableValue<String>, String>> {

    private final OrFields fields;

    public FieldColumn(MainTableColumnModel model) {
        super(model);
        this.fields = FieldFactory.parseOrFields(model.getQualifier());
        setText(getDisplayName());

        setCellValueFactory(cellData -> new ReadOnlyObjectWrapper(cellData.getValue().getBibPreview(getFieldValue(cellData.getValue()))));
        new ValueTableCellFactory<BibEntryTableViewModel, Map<ObservableValue<String>, String>>()
                .withText(this::extractFieldValue)
                .withTooltip(this::createTooltip)
                .install(this);

        if (fields.hasExactlyOne()) {
            // comparator can't parse more than one value
            Field field = fields.getFields().stream().collect(MoreCollectors.onlyElement());

            if ((field instanceof UnknownField) || field.isNumeric()) {
                this.setComparator(new Comparator<Map<ObservableValue<String>, String>>() {
                    @Override
                    public int compare(Map<ObservableValue<String>, String> o1, Map<ObservableValue<String>, String> o2) {
                        NumericFieldComparator numericFieldComparator = new NumericFieldComparator();
                        return numericFieldComparator.compare(extractFieldValue(o1), extractFieldValue(o2));
                    }
                });
            }
        }

        this.setSortable(true);
    }

    public String extractFieldValue(Map<ObservableValue<String>, String> values) {
        String fieldText = "";
        for (ObservableValue<String> key : values.keySet()) {
            fieldText = key.getValue();
        }
        return fieldText;
    }

    public String createTooltip(Map<ObservableValue<String>, String> values) {
        String tooltipText = "";
        String cellFieldText = "";
        for (ObservableValue<String> key : values.keySet()) {
            cellFieldText = key.getValue();
        }
        tooltipText += cellFieldText;

        for (String preview : values.values()) {
            tooltipText += "\n\n" + preview;
        }
        return tooltipText;
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
