package org.jabref.gui.mergeentries.newmergedialog;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.mergeentries.newmergedialog.cell.AbstractCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldNameCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldValueCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.MergedFieldCell;
import org.jabref.model.strings.StringUtil;

public class FieldRowController {
    private final FieldNameCell fieldNameCell;
    private final FieldValueCell leftValueCell;
    private final FieldValueCell rightValueCell;
    private final MergedFieldCell mergedValueCell;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    public FieldRowController(String fieldName, String leftValue, String rightValue, AbstractCell.BackgroundTone backgroundTone) {
        fieldNameCell = new FieldNameCell(fieldName, backgroundTone);
        leftValueCell = new FieldValueCell(leftValue, backgroundTone);
        rightValueCell = new FieldValueCell(rightValue, backgroundTone);
        mergedValueCell = new MergedFieldCell(StringUtil.isNullOrEmpty(leftValue) ? rightValue : leftValue, backgroundTone);

        toggleGroup.getToggles().addAll(leftValueCell, rightValueCell);
        toggleGroup.selectToggle(StringUtil.isNullOrEmpty(leftValue) ? rightValueCell : leftValueCell);
        toggleGroup.selectedToggleProperty().addListener(invalidated -> {
            mergedValueCell.setText((String) toggleGroup.getSelectedToggle().getUserData());
        });
    }

    public String getMergedValue() {
        return mergedValueProperty().getValue();
    }

    public ReadOnlyStringProperty mergedValueProperty() {
        return mergedValueCell.textProperty();
    }
}
