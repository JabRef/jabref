package org.jabref.gui.maintable.columns;

import java.util.Map;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;

/**
 * A column that displays the text-value of the field
 */
public class FieldColumn extends MainTableColumn<Map<ObservableValue<String>, String>> {

    private final OrFields fields;

    public FieldColumn(MainTableColumnModel model) {
        super(model);
        this.fields = FieldFactory.parseOrFields(model.getQualifier());
        setText(getDisplayName());

//        this.setCellValueFactory(cellData -> getFieldValue(cellData.getValue())));
//        new ValueTableCellFactory<BibEntryTableViewModel, String>()
//                .withText(text -> text)
//                .install(this);

        setCellValueFactory(cellData -> new ReadOnlyObjectWrapper(cellData.getValue().getBibPreview(getFieldValue(cellData.getValue()))));
        new ValueTableCellFactory<BibEntryTableViewModel, Map<ObservableValue<String>, String>>()
                .withText(this::extractFieldValue)
                .withTooltip(this::createTooltip)
                .install(this);

        // TODO DETTA BÖR FUNGERA EGENTLIGEN
//        if (fields.hasExactlyOne()) {
//            // comparator can't parse more than one value
//            Field field = fields.getFields().stream().collect(MoreCollectors.onlyElement());
//
//            if ((field instanceof UnknownField) || field.isNumeric()) {
//                this.setComparator(new NumericFieldComparator());
//            }
//        }

//        this.setSortable(true);
    }

    private String extractFieldValue(Map<ObservableValue<String>, String> values) {
        String fieldText = "";
        for (ObservableValue<String> key : values.keySet()) {
            fieldText = key.getValue();
        }
        return fieldText;
    }

    private String createTooltip(Map<ObservableValue<String>, String> values) {
        String tooltipText = "";
        String cellFieldText = "";
        for (ObservableValue<String> key : values.keySet()) {
            cellFieldText = key.getValue();
        }
        tooltipText += cellFieldText + "\n\n";

        for (String preview : values.values()) {
            tooltipText += preview;
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
