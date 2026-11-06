package org.jabref.gui.mergeentries.multiwaymerge;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.StandardField;

import com.tobiasdiez.easybind.EasyObservableValue;

public class MultiMergeFieldRow {
    public static final PseudoClass ODD_PSEUDO_CLASS = PseudoClass.getPseudoClass("odd");
    public static final PseudoClass EVEN_PSEUDO_CLASS = PseudoClass.getPseudoClass("even");

    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final TextInputControl fieldEditorCell;
    private final HBox fieldEditorContainer;
    private final HBox fieldHeaderCell;
    private final Label fieldHeaderLabel;
    private final int rowIndex;
    private final Field field;

    @SuppressWarnings("FieldCanBeLocal")
    private EasyObservableValue<String> fieldBinding;

    public MultiMergeFieldRow(Field field,
                             int rowIndex,
                             MultiMergeEntriesViewModel viewModel,
                             ToggleGroup headerToggleGroup,
                             VBox fieldHeader,
                             VBox fieldEditor,
                             GridPane optionsGrid,
                             GuiPreferences preferences) {
        this.field = field;
        this.rowIndex = rowIndex;

        boolean isMultiLine = isMultilineField(field, preferences);
        if (isMultiLine) {
            TextArea textArea = new TextArea();
            textArea.setWrapText(true);
            fieldEditorCell = textArea;
        } else {
            fieldEditorCell = new TextField();
        }
        fieldEditorCell.getStyleClass().add("merged-field-editor");

        fieldEditorContainer = new HBox(fieldEditorCell);
        fieldEditorContainer.getStyleClass().addAll("field-cell", "merged-field");
        fieldEditorContainer.pseudoClassStateChanged(ODD_PSEUDO_CLASS, rowIndex % 2 == 1);
        fieldEditorContainer.pseudoClassStateChanged(EVEN_PSEUDO_CLASS, rowIndex % 2 == 0);
        fieldEditorContainer.setPadding(new Insets(4, 8, 4, 8));
        fieldEditorContainer.setAlignment(Pos.CENTER);
        HBox.setHgrow(fieldEditorCell, Priority.ALWAYS);
        fieldEditorCell.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(fieldEditorContainer, Priority.ALWAYS);

        fieldBinding = viewModel.mergedEntryProperty().get().getFieldBinding(field).asOrdinary();
        BindingsHelper.bindBidirectional(
                fieldEditorCell.textProperty(),
                fieldBinding,
                text -> fieldEditorCell.setText(text == null ? "" : text),
                binding -> viewModel.setMergedFieldValue(field, binding));

        fieldEditor.getChildren().add(fieldEditorContainer);

        // Header label
        fieldHeaderLabel = new Label(FieldTextMapper.getDisplayName(field));
        fieldHeaderCell = new HBox(fieldHeaderLabel);
        fieldHeaderCell.getStyleClass().addAll("field-cell", "field-name");
        fieldHeaderCell.pseudoClassStateChanged(ODD_PSEUDO_CLASS, rowIndex % 2 == 1);
        fieldHeaderCell.pseudoClassStateChanged(EVEN_PSEUDO_CLASS, rowIndex % 2 == 0);
        fieldHeaderCell.setAlignment(Pos.CENTER_LEFT);
        fieldHeaderCell.setPadding(new Insets(4, 8, 4, 12));
        fieldHeaderCell.prefHeightProperty().bind(fieldEditorContainer.heightProperty());
        fieldHeaderCell.setMaxWidth(Control.USE_PREF_SIZE);
        fieldHeaderCell.setMinWidth(Control.USE_PREF_SIZE);
        fieldHeader.getChildren().add(fieldHeaderCell);

        // Row constraints
        RowConstraints constraint = new RowConstraints();
        constraint.setMinHeight(Control.USE_PREF_SIZE);
        constraint.setMaxHeight(Control.USE_PREF_SIZE);
        constraint.prefHeightProperty().bind(fieldEditorContainer.heightProperty());
        optionsGrid.getRowConstraints().add(constraint);

        fieldEditorCell.addEventFilter(KeyEvent.KEY_PRESSED, _ -> toggleGroup.selectToggle(null));

        toggleGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
            if (newValue == null) {
                viewModel.setMergedFieldValue(field, null);
            } else {
                viewModel.setMergedFieldValue(field, ((DiffHighlightingEllipsingTextFlow) ((ToggleButton) newValue).getGraphic()).getFullText());
                headerToggleGroup.selectToggle(null);
            }
        });
    }

    private static boolean isMultilineField(Field field, GuiPreferences preferences) {
        if (field.equals(StandardField.DOI)) {
            return false;
        }
        return FieldFactory.isMultiLineField(field, preferences.getFieldPreferences().getNonWrappableFields());
    }

    public ToggleGroup getToggleGroup() {
        return toggleGroup;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public Field getField() {
        return field;
    }

    public TextInputControl getFieldEditorCell() {
        return fieldEditorCell;
    }

    public HBox getFieldEditorContainer() {
        return fieldEditorContainer;
    }

    public HBox getFieldHeaderCell() {
        return fieldHeaderCell;
    }

    public EasyObservableValue<String> getFieldBinding() {
        return fieldBinding;
    }
}
