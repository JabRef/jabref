package org.jabref.gui.mergeentries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class MultiMergeEntries extends SplitPane {

    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private HashMap<Field, FieldRow> fieldRows = new HashMap<>();

    private VBox labelColumn;
    private VBox optionsColumn;
    private VBox entryColumn;

    private ScrollPane topScrollPane;
    private ScrollPane leftScrollPane;
    private ScrollPane centerScrollPane;
    private ScrollPane rightScrollPane;

    private HBox supplierHeader;
    private VBox fieldHeader;
    private GridPane optionsGrid;
    private VBox fieldEditor;

    public MultiMergeEntries(FieldContentFormatterPreferences fieldContentFormatterPreferences) {
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
        init();
    }

    private void init() {

        labelColumn = new VBox();
        optionsColumn = new VBox();
        entryColumn = new VBox();

        supplierHeader = new HBox();
        fieldHeader = new VBox();
        optionsGrid = new GridPane();
        fieldEditor = new VBox();

        topScrollPane = new ScrollPane(supplierHeader);
        leftScrollPane = new ScrollPane(fieldHeader);
        centerScrollPane = new ScrollPane(optionsGrid);
        rightScrollPane = new ScrollPane(fieldEditor);
        topScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        topScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        topScrollPane.hvalueProperty().bindBidirectional(centerScrollPane.hvalueProperty());
        leftScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());
        rightScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());

        labelColumn.getChildren().addAll(new Label(Localization.lang("Supplier:")), leftScrollPane);
        optionsColumn.getChildren().addAll(topScrollPane, centerScrollPane);
        entryColumn.getChildren().addAll(new Label(Localization.lang("Entry:")), rightScrollPane);

        setResizableWithParent(labelColumn, false);
        setDividerPositions(0.1, 0.6, 0.3);
        getItems().addAll(labelColumn, optionsColumn, entryColumn);
    }

    public void addEntry(String title, BibEntry entry) {
        int column = addColumn(title);

        for (Map.Entry<Field, String> fieldEntry : entry.getFieldMap().entrySet()) {
            if (!fieldRows.containsKey(fieldEntry.getKey())) {
                addField(fieldEntry.getKey());
            }
            fieldRows.get(fieldEntry.getKey()).addValue(column, fieldEntry.getValue());
        }
    }

    public void addEntry(String title, Supplier<Optional<BibEntry>> entrySupplier) {
    }

    public BibEntry getMergeEntry() {
        BibEntry mergedEntry = new BibEntry();
        for (Map.Entry<Field, FieldRow> fieldRowEntry : fieldRows.entrySet()) {
            mergedEntry.setField(fieldRowEntry.getKey(), ((TextInputControl) fieldEditor.getChildren().get(fieldRowEntry.getValue().rowIndex)).getText());
        }
        return mergedEntry;
    }

    private int addColumn(String name) {
        int columnIndex = supplierHeader.getChildren().size();
        Button sourceButton = new Button(name);
        sourceButton.setOnAction(event -> optionsGrid.getChildrenUnmodifiable().stream().filter(node -> GridPane.getColumnIndex(node) == columnIndex).filter(node -> node instanceof ToggleButton).forEach(toggleButton -> ((ToggleButton) toggleButton).setSelected(true)));
        HBox.setHgrow(sourceButton, Priority.ALWAYS);
        supplierHeader.getChildren().add(sourceButton);
        sourceButton.setMinWidth(250);

        optionsGrid.add(new Label(), columnIndex, 0);
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setMinWidth(Control.USE_PREF_SIZE);
        constraint.setMaxWidth(Control.USE_PREF_SIZE);
        constraint.prefWidthProperty().bind(sourceButton.widthProperty());
        optionsGrid.getColumnConstraints().add(constraint);

        return columnIndex;
    }

    public void addField(Field field) {
        boolean isMultiLine = FieldFactory.isMultiLineField(field, fieldContentFormatterPreferences.getNonWrappableFields());
        TextInputControl fieldEditorCell = null;
        if (isMultiLine) {
            fieldEditorCell = new TextArea();
            ((TextArea) fieldEditorCell).setWrapText(true);
        } else {
            fieldEditorCell = new TextField();
        }
        VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);
        FieldRow newRow = new FieldRow(fieldHeader.getChildren().size(), fieldEditorCell);
        fieldRows.put(field, newRow);
        Label fieldHeaderLabel = new Label(field.getDisplayName());
        fieldHeaderLabel.prefHeightProperty().bind(fieldEditorCell.heightProperty());
        fieldHeaderLabel.setMaxWidth(Control.USE_PREF_SIZE);
        fieldHeaderLabel.setMinWidth(Control.USE_PREF_SIZE);
        fieldEditorCell.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);
        fieldHeader.getChildren().add(fieldHeaderLabel);
        fieldEditor.getChildren().add(fieldEditorCell);

        optionsGrid.add(new Label(), 0, newRow.rowIndex);
        RowConstraints constraint = new RowConstraints();
        constraint.setMinHeight(Control.USE_PREF_SIZE);
        constraint.setMaxHeight(Control.USE_PREF_SIZE);
        constraint.prefHeightProperty().bind(fieldEditorCell.heightProperty());
        optionsGrid.getRowConstraints().add(constraint);
    }

    private class FieldRow {
        public int rowIndex;
        public ToggleGroup toggleGroup = new ToggleGroup();
        public TextInputControl entryEditorField;

        public FieldRow(int rowIndex, TextInputControl entryEditorField) {
            this.rowIndex = rowIndex;
            this.entryEditorField = entryEditorField;
            entryEditorField.prefWidthProperty().bind(rightScrollPane.widthProperty());
            entryEditorField.setMinWidth(Control.USE_PREF_SIZE);
            entryEditorField.setMaxWidth(Control.USE_PREF_SIZE);

            ChangeListener textChangeListener = new ChangeListener() {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                    if (toggleGroup.getSelectedToggle() != null) {
                        toggleGroup.getSelectedToggle().setSelected(false);
                        entryEditorField.textProperty().removeListener(this);
                    }
                }
            };

            toggleGroup.selectedToggleProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (observable != null && newValue != null && newValue.isSelected()) {
                            ToggleButton toggle = (ToggleButton) observable.getValue();
                            entryEditorField.setText(toggle.getText());
                            entryEditorField.textProperty().addListener(textChangeListener);
                        } else {
                            entryEditorField.textProperty().removeListener(textChangeListener);
                        }
                    }
            );
        }

        public void addValue(int columnIndex, String value) {
            ToggleButton cellButton = new ToggleButton(value);
            cellButton.setWrapText(true);
            optionsGrid.add(cellButton, columnIndex, rowIndex);
            if (toggleGroup.getSelectedToggle() == null) {
                cellButton.setSelected(true);
            }
            for (Toggle otherToggle : toggleGroup.getToggles()) {
                if (otherToggle instanceof ToggleButton) {
                    ToggleButton otherToggleButton = (ToggleButton) otherToggle;
                    if (otherToggleButton.getText().equals(value)) {
                        cellButton.selectedProperty().bindBidirectional(otherToggleButton.selectedProperty());
                        return;
                    }
                }
            }
            cellButton.setToggleGroup(toggleGroup);
        }
    }
}
