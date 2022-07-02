package org.jabref.gui.mergeentries.newmergedialog;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.mergeentries.newmergedialog.cell.FieldNameCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldValueCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.MergedFieldCell;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.SplitDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.UnifiedDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.model.strings.StringUtil;

import org.fxmisc.richtext.StyleClassedTextArea;

public class FieldRowController {
    private final FieldNameCell fieldNameCell;
    private final FieldValueCell leftValueCell;
    private final FieldValueCell rightValueCell;
    private final MergedFieldCell mergedValueCell;

    private final String leftValue;

    private final String rightValue;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    public FieldRowController(String fieldName, String leftValue, String rightValue, int rowIndex) {
        fieldNameCell = new FieldNameCell(fieldName, rowIndex);
        leftValueCell = new FieldValueCell(leftValue, rowIndex);
        rightValueCell = new FieldValueCell(rightValue, rowIndex);
        mergedValueCell = new MergedFieldCell(StringUtil.isNullOrEmpty(leftValue) ? rightValue : leftValue, rowIndex);

        this.leftValue = leftValue;
        this.rightValue = rightValue;

        toggleGroup.getToggles().addAll(leftValueCell, rightValueCell);
        toggleGroup.selectToggle(StringUtil.isNullOrEmpty(leftValue) ? rightValueCell : leftValueCell);
        toggleGroup.selectedToggleProperty().addListener(invalidated -> {
            if (toggleGroup.getSelectedToggle() != null) {
                mergedValueCell.setText((String) toggleGroup.getSelectedToggle().getUserData());
            }
        });

        mergedValueCell.textProperty().addListener((observable, old, mergedValue) -> {
            if (mergedValue.equals(leftValue)) {
                toggleGroup.selectToggle(leftValueCell);
            } else if (mergedValue.equals(rightValue)) {
                toggleGroup.selectToggle(rightValueCell);
            } else {
                // deselect all toggles because left and right values don't equal the merged value
                toggleGroup.selectToggle(null);
            }
        });

        // Because when both left and right have the same values, then only left value will be shown
        if (hasEqualLeftAndRightValues()) {
            rightValueCell.setDisable(true);
        }
    }

    public void selectLeftValue() {
        toggleGroup.selectToggle(leftValueCell);
    }

    public void selectRightValue() {
        if (rightValueCell.isDisabled()) {
            selectLeftValue();
        } else {
            toggleGroup.selectToggle(rightValueCell);
        }
    }

    public String getMergedValue() {
        return mergedValueProperty().getValue();
    }

    public ReadOnlyStringProperty mergedValueProperty() {
        return mergedValueCell.textProperty();
    }

    public FieldNameCell getFieldNameCell() {
        return fieldNameCell;
    }

    public FieldValueCell getLeftValueCell() {
        return leftValueCell;
    }

    public FieldValueCell getRightValueCell() {
        return rightValueCell;
    }

    public MergedFieldCell getMergedValueCell() {
        return mergedValueCell;
    }

    public boolean hasEqualLeftAndRightValues() {
        return !StringUtil.isNullOrEmpty(leftValueCell.getText()) &&
                !StringUtil.isNullOrEmpty(rightValueCell.getText()) &&
                leftValueCell.getText().equals(rightValueCell.getText());
    }

    public void showDiff(ShowDiffConfig diffConfig) {
        if (leftValueCell.isDisabled() || rightValueCell.isDisabled()) {
            return;
        }

        StyleClassedTextArea leftLabel = leftValueCell.getStyleClassedLabel();
        StyleClassedTextArea rightLabel = rightValueCell.getStyleClassedLabel();
        // Clearing old diff styles based on previous diffConfig
        hideDiff();
        if (diffConfig.diffView() == ThreeWayMergeToolbar.DiffView.UNIFIED) {
            new UnifiedDiffHighlighter(leftLabel, rightLabel, diffConfig.diffMethod()).highlight();
        } else {
            new SplitDiffHighlighter(leftLabel, rightLabel, diffConfig.diffMethod()).highlight();
        }
    }

    public void hideDiff() {
        int leftValueLength = getLeftValueCell().getStyleClassedLabel().getLength();
        getLeftValueCell().getStyleClassedLabel().clearStyle(0, leftValueLength);
        getLeftValueCell().getStyleClassedLabel().replaceText(leftValue);

        int rightValueLength = getRightValueCell().getStyleClassedLabel().getLength();
        getRightValueCell().getStyleClassedLabel().clearStyle(0, rightValueLength);
        getRightValueCell().getStyleClassedLabel().replaceText(rightValue);
    }
}
