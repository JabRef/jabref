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
            if (!StringUtil.isNullOrEmpty(mergedValue)) {
                if (mergedValue.equals(leftValue)) {
                    toggleGroup.selectToggle(leftValueCell);
                } else if (mergedValue.equals(rightValue)) {
                    toggleGroup.selectToggle(rightValueCell);
                } else {
                    // deselect all toggles because left and right values don't equal the merged value
                    toggleGroup.selectToggle(null);
                }
            } else {
                // deselect all toggles because empty toggles cannot be selected
                toggleGroup.selectToggle(null);
            }
        });

        // empty toggles are disabled and cannot be selected
        if (StringUtil.isNullOrEmpty(leftValue)) {
            leftValueCell.setDisable(true);
        } else if (StringUtil.isNullOrEmpty(rightValue)) {
            rightValueCell.setDisable(true);
        }
    }

    /**
     * @return True if left value was selected, False otherwise
     */
    public boolean selectLeftValue() {
        if (StringUtil.isNullOrEmpty(leftValue)) {
            toggleGroup.selectToggle(null);
            mergedValueCell.setText("");
            return false;
        } else {
            toggleGroup.selectToggle(leftValueCell);
            return true;
        }
    }

    /**
     * @return True if left value was selected, False otherwise
     */
    public boolean selectRightValue() {
        if (StringUtil.isNullOrEmpty(rightValue)) {
            toggleGroup.selectToggle(null);
            mergedValueCell.setText("");
            return false;
        } else {
            if (hasEqualLeftAndRightValues()) {
                toggleGroup.selectToggle(leftValueCell);
            } else {
                toggleGroup.selectToggle(rightValueCell);
            }
            return true;
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
        // Clearing old diff styles
        hideDiff();
        if (diffConfig.diffView() == ThreeWayMergeToolbar.DiffView.UNIFIED) {
            new UnifiedDiffHighlighter(leftValueCell.getStyleClassedLabel(),
                                       rightValueCell.getStyleClassedLabel(),
                                       diffConfig.diffMethod()).highlight();
        } else {
            new SplitDiffHighlighter(leftValueCell.getStyleClassedLabel(),
                                     rightValueCell.getStyleClassedLabel(),
                                     diffConfig.diffMethod()).highlight();
        }
    }

    public void hideDiff() {
        if (!StringUtil.isNullOrEmpty(leftValue)) {
            int leftValueLength = getLeftValueCell().getStyleClassedLabel().getLength();
            getLeftValueCell().getStyleClassedLabel().clearStyle(0, leftValueLength);
            getLeftValueCell().getStyleClassedLabel().replaceText(leftValue);
        }

        if (!StringUtil.isNullOrEmpty(rightValue)) {
            int rightValueLength = getRightValueCell().getStyleClassedLabel().getLength();
            getRightValueCell().getStyleClassedLabel().clearStyle(0, rightValueLength);
            getRightValueCell().getStyleClassedLabel().replaceText(rightValue);
        }
    }
}
