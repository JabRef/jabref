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
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class MultiMergeEntries extends GridPane {

    private HashMap<Field, FieldRow> fieldRows = new HashMap<>();

    private ScrollPane topScrollPane;
    private ScrollPane leftScrollPane;
    private ScrollPane centerScrollPane;
    private ScrollPane rightScrollPane;

    private HBox supplierHeader;
    private VBox fieldHeader;
    private GridPane optionsGrid;
    private VBox fieldEditor;

    private ColumnConstraints leftColumnConstraints = new ColumnConstraints();
    private ColumnConstraints centerColumnConstraints = new ColumnConstraints();
    private ColumnConstraints rightColumnConstraints = new ColumnConstraints();

    public MultiMergeEntries() {
        init();
    }

    private void init() {

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

        add(new Label(Localization.lang("Supplier:")), 0, 0);
        add(topScrollPane, 1, 0);
        add(new Label(Localization.lang("Entry:")), 2, 0);
        add(leftScrollPane, 0, 1);
        add(centerScrollPane, 1, 1);
        add(rightScrollPane, 2, 1);

        topScrollPane.hvalueProperty().bindBidirectional(centerScrollPane.hvalueProperty());
        leftScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());
        rightScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());

        leftColumnConstraints.setMinWidth(200);
        centerColumnConstraints.setMinWidth(750);
        rightColumnConstraints.setMinWidth(250);
        getColumnConstraints().addAll(leftColumnConstraints, centerColumnConstraints, rightColumnConstraints);

        RowConstraints topRowConstraints = new RowConstraints();
        RowConstraints bottomRowConstraints = new RowConstraints();
        topRowConstraints.setPercentHeight(0.05);
        bottomRowConstraints.setPercentHeight(0.95);
        getRowConstraints().addAll(topRowConstraints, bottomRowConstraints);
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
            mergedEntry.setField(fieldRowEntry.getKey(), ((TextArea) fieldEditor.getChildren().get(fieldRowEntry.getValue().rowIndex)).getText());
        }
        return mergedEntry;
    }

    private int addColumn(String name) {
        int columnIndex = supplierHeader.getChildren().size();
        Button sourceButton = new Button(name);
        sourceButton.setOnAction(event -> optionsGrid.getChildrenUnmodifiable().stream().filter(node -> getColumnIndex(node) == columnIndex).filter(node -> node instanceof ToggleButton).forEach(toggleButton -> ((ToggleButton) toggleButton).setSelected(true)));
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
        TextArea fieldEditorCell = new TextArea();
        fieldEditorCell.setWrapText(true);
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
        public TextArea entryEditorField;

        public FieldRow(int rowIndex, TextArea entryEditorField) {
            this.rowIndex = rowIndex;
            this.entryEditorField = entryEditorField;

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
