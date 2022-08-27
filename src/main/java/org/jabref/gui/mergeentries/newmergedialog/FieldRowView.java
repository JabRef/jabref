package org.jabref.gui.mergeentries.newmergedialog;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldNameCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.FieldValueCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.MergedFieldCell;
import org.jabref.gui.mergeentries.newmergedialog.cell.sidebuttons.ToggleMergeUnmergeButton;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.SplitDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.UnifiedDiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMerger;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.tobiasdiez.easybind.EasyBind;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.mergeentries.newmergedialog.FieldRowViewModel.Selection;

/**
 * A controller class to control left, right and merged field values
 */
public class FieldRowView {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldRowView.class);

    protected final FieldRowViewModel viewModel;

    protected final BooleanProperty shouldShowDiffs = new SimpleBooleanProperty(true);
    private final FieldNameCell fieldNameCell;
    private final FieldValueCell leftValueCell;
    private final FieldValueCell rightValueCell;
    private final MergedFieldCell mergedValueCell;

    private final ToggleGroup toggleGroup = new ToggleGroup();

    private final CompoundEdit fieldsMergedEdit = new CompoundEdit();

    public FieldRowView(Field field, BibEntry leftEntry, BibEntry rightEntry, BibEntry mergedEntry, FieldMergerFactory fieldMergerFactory, int rowIndex) {
        viewModel = new FieldRowViewModel(field, leftEntry, rightEntry, mergedEntry);

        fieldNameCell = new FieldNameCell(field.getDisplayName(), rowIndex);
        leftValueCell = new FieldValueCell(viewModel.getLeftFieldValue(), rowIndex);
        rightValueCell = new FieldValueCell(viewModel.getRightFieldValue(), rowIndex);
        mergedValueCell = new MergedFieldCell(viewModel.getMergedFieldValue(), rowIndex);

        if (FieldMergerFactory.canMerge(field)) {
            ToggleMergeUnmergeButton toggleMergeUnmergeButton = new ToggleMergeUnmergeButton(field);
            toggleMergeUnmergeButton.setCanMerge(!viewModel.hasEqualLeftAndRightValues());
            fieldNameCell.addSideButton(toggleMergeUnmergeButton);

            EasyBind.listen(toggleMergeUnmergeButton.fieldStateProperty(), ((observableValue, old, fieldState) -> {
                LOGGER.debug("Field merge state is {} for field {}", fieldState, field);
                if (fieldState == ToggleMergeUnmergeButton.FieldState.MERGED) {
                    new MergeCommand(fieldMergerFactory.create(field)).execute();
                } else {
                    new UnmergeCommand().execute();
                }
            }));
        }

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

        EasyBind.listen(viewModel.hasEqualLeftAndRightBinding(), (obs, old, isEqual) -> {
            if (isEqual) {
                LOGGER.debug("Left and right values are equal, LEFT==RIGHT=={}", viewModel.getLeftFieldValue());
                hideDiff();
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

    public void showDiff(ShowDiffConfig diffConfig) {
        if (!rightValueCell.isVisible()) {
            return;
        }
        LOGGER.debug("Showing diffs...");

        StyleClassedTextArea leftLabel = leftValueCell.getStyleClassedLabel();
        StyleClassedTextArea rightLabel = rightValueCell.getStyleClassedLabel();
        // Clearing old diff styles based on previous diffConfig
        hideDiff();
        if (shouldShowDiffs.get()) {
            if (diffConfig.diffView() == ThreeWayMergeToolbar.DiffView.UNIFIED) {
                new UnifiedDiffHighlighter(leftLabel, rightLabel, diffConfig.diffHighlightingMethod()).highlight();
            } else {
                new SplitDiffHighlighter(leftLabel, rightLabel, diffConfig.diffHighlightingMethod()).highlight();
            }
        }
    }

    public void hideDiff() {
        if (!rightValueCell.isVisible()) {
            return;
        }

        LOGGER.debug("Hiding diffs...");

        int leftValueLength = getLeftValueCell().getStyleClassedLabel().getLength();
        getLeftValueCell().getStyleClassedLabel().clearStyle(0, leftValueLength);
        getLeftValueCell().getStyleClassedLabel().replaceText(viewModel.getLeftFieldValue());

        int rightValueLength = getRightValueCell().getStyleClassedLabel().getLength();
        getRightValueCell().getStyleClassedLabel().clearStyle(0, rightValueLength);
        getRightValueCell().getStyleClassedLabel().replaceText(viewModel.getRightFieldValue());
    }

    public class MergeCommand extends SimpleCommand {
        private final FieldMerger fieldMerger;

        public MergeCommand(FieldMerger fieldMerger) {
            this.fieldMerger = fieldMerger;

            this.executable.bind(viewModel.hasEqualLeftAndRightBinding().not());
        }

        @Override
        public void execute() {
            assert !viewModel.getLeftFieldValue().equals(viewModel.getRightFieldValue());

            String oldLeftFieldValue = viewModel.getLeftFieldValue();
            String oldRightFieldValue = viewModel.getRightFieldValue();

            String mergedFields = fieldMerger.merge(viewModel.getLeftFieldValue(), viewModel.getRightFieldValue());
            viewModel.setLeftFieldValue(mergedFields);
            viewModel.setRightFieldValue(mergedFields);

            if (fieldsMergedEdit.canRedo()) {
                fieldsMergedEdit.redo();
            } else {
                fieldsMergedEdit.addEdit(new MergeFieldsUndo(oldLeftFieldValue, oldRightFieldValue, mergedFields));
                fieldsMergedEdit.end();
            }
        }
    }

    public class UnmergeCommand extends SimpleCommand {
        @Override
        public void execute() {
            if (fieldsMergedEdit.canUndo()) {
                fieldsMergedEdit.undo();
            }
        }
    }

    class MergeFieldsUndo extends AbstractUndoableEdit {
        private final String oldLeft;
        private final String oldRight;
        private final String mergedFields;

        MergeFieldsUndo(String oldLeft, String oldRight, String mergedFields) {
            this.oldLeft = oldLeft;
            this.oldRight = oldRight;
            this.mergedFields = mergedFields;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            viewModel.setLeftFieldValue(oldLeft);
            viewModel.setRightFieldValue(oldRight);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            viewModel.setLeftFieldValue(mergedFields);
            viewModel.setRightFieldValue(mergedFields);
        }
    }
}
