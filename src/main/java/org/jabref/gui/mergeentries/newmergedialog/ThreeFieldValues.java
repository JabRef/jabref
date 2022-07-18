package org.jabref.gui.mergeentries.newmergedialog;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import org.jabref.gui.mergeentries.newmergedialog.cell.FieldValueCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.MergedFieldCell;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.SplitDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.UnifiedDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;
import org.fxmisc.richtext.StyleClassedTextArea;

import static org.jabref.gui.mergeentries.newmergedialog.ThreeFieldValuesViewModel.Selection;

/**
 * A controller class to control left, right and merged field values
 */
public class ThreeFieldValues {
    private final FieldValueCell leftValueCell;
    private FieldValueCell rightValueCell;
    private final MergedFieldCell mergedValueCell;

    private final String leftValue;

    private final String rightValue;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    private final ThreeFieldValuesViewModel viewModel;

    public ThreeFieldValues(String leftValue, String rightValue, int rowIndex) {
        leftValueCell = new FieldValueCell(leftValue, rowIndex);
        rightValueCell = new FieldValueCell(rightValue, rowIndex);
        mergedValueCell = new MergedFieldCell(StringUtil.isNullOrEmpty(leftValue) ? rightValue : leftValue, rowIndex);

        viewModel = new ThreeFieldValuesViewModel(leftValue, rightValue);

        this.leftValue = leftValue;
        this.rightValue = rightValue;

        toggleGroup.getToggles().addAll(leftValueCell, rightValueCell);

        mergedValueCell.textProperty().bindBidirectional(viewModel.mergedFieldValueProperty());
        leftValueCell.textProperty().bindBidirectional(viewModel.leftFieldValueProperty());
        rightValueCell.textProperty().bindBidirectional(viewModel.rightFieldValueProperty());

        EasyBind.subscribe(viewModel.selectionProperty(), selection -> {
            if (selection == Selection.LEFT) {
                toggleGroup.selectToggle(leftValueCell);
            } else if (selection == Selection.RIGHT) {
                toggleGroup.selectToggle(rightValueCell);
            } else if (selection == Selection.NONE) {
                toggleGroup.selectToggle(null);
            }
        });

        EasyBind.subscribe(toggleGroup.selectedToggleProperty(), selectedToggle -> {
            if (selectedToggle == leftValueCell) {
                selectLeftValue();
            } else if (selectedToggle == rightValueCell) {
                selectRightValue();
            } else {
                selectNone();
            }
        });

        // Hide rightValueCell and extend leftValueCell to 2 columns when fields are merged
        EasyBind.subscribe(viewModel.isFieldsMergedProperty(), isFieldsMerged -> {
            if (isFieldsMerged) {
                rightValueCell.setVisible(false);
                GridPane.setColumnSpan(leftValueCell, 2);
            } else {
                rightValueCell.setVisible(true);
                GridPane.setColumnSpan(leftValueCell, 1);
            }
        });
    }

    public void selectLeftValue() {
        viewModel.selectLeftValue();
    }

    public void selectRightValue() {
        viewModel.selectRightValue();
    }

    public void selectNone() {
        viewModel.selectNone();
    }

    public String getMergedValue() {
        return mergedValueProperty().getValue();
    }

    public ReadOnlyStringProperty mergedValueProperty() {
        return viewModel.mergedFieldValueProperty();
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
        return viewModel.hasEqualLeftAndRightValues();
    }

    public void showDiff(ShowDiffConfig diffConfig) {
        if (isRightValueCellHidden()) {
            return;
        }

        StyleClassedTextArea leftLabel = leftValueCell.getStyleClassedLabel();
        StyleClassedTextArea rightLabel = rightValueCell.getStyleClassedLabel();
        // Clearing old diff styles based on previous diffConfig
        hideDiff();
        if (diffConfig.diffView() == ThreeWayMergeToolbar.DiffView.UNIFIED) {
            new UnifiedDiffHighlighter(leftLabel, rightLabel, diffConfig.diffHighlightingMethod()).highlight();
        } else {
            new SplitDiffHighlighter(leftLabel, rightLabel, diffConfig.diffHighlightingMethod()).highlight();
        }
    }

    public void hideDiff() {
        if (isRightValueCellHidden()) {
            return;
        }

        int leftValueLength = getLeftValueCell().getStyleClassedLabel().getLength();
        getLeftValueCell().getStyleClassedLabel().clearStyle(0, leftValueLength);
        getLeftValueCell().getStyleClassedLabel().replaceText(leftValue);

        int rightValueLength = getRightValueCell().getStyleClassedLabel().getLength();
        getRightValueCell().getStyleClassedLabel().clearStyle(0, rightValueLength);
        getRightValueCell().getStyleClassedLabel().replaceText(rightValue);
    }

    private boolean isRightValueCellHidden() {
        return rightValueCell == null;
    }

    private ObjectProperty<ThreeFieldValuesViewModel.Selection> selectionProperty() {
        return viewModel.selectionProperty();
    }
}
