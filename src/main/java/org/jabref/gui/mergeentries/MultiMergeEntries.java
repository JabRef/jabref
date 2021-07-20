package org.jabref.gui.mergeentries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Supplier;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class MultiMergeEntries extends GridPane {

    private HashMap<Field, FieldRow> fieldRows = new HashMap<>();
    private List<Node> fieldEditorColumn = new Vector<>();

    private ColumnConstraints columnConstraints = new ColumnConstraints();

    public MultiMergeEntries() {
        Node fieldEditorHeader = new Label(Localization.lang("Entry"));
        fieldEditorColumn.add(fieldEditorHeader);
        this.addRow(0, new Label(Localization.lang("Supplier:")), fieldEditorHeader);
        columnConstraints.setPercentWidth(100.0 / getColumnCount());
        getColumnConstraints().addAll(columnConstraints, columnConstraints);
    }

    public void addEntry(String title, BibEntry entry) {
        int column = addColumn(title);

        for (Map.Entry<Field, String> fieldEntry : entry.getFieldMap().entrySet()) {
            if (!fieldRows.containsKey(fieldEntry.getKey())) {
                addField(fieldEntry.getKey());
            }
            fieldRows.get(fieldEntry.getKey()).addValue(column, fieldEntry.getValue());
        }
        getColumnConstraints().add(columnConstraints);
    }

    public void addEntry(String title, Supplier<Optional<BibEntry>> entrySupplier) {
    }

    public BibEntry getMergeEntry() {
        BibEntry mergedEntry = new BibEntry();
        for (Map.Entry<Field, FieldRow> fieldRowEntry : fieldRows.entrySet()) {
            mergedEntry.setField(fieldRowEntry.getKey(), ((TextArea) fieldEditorColumn.get(fieldRowEntry.getValue().rowIndex)).getText());
        }
        return mergedEntry;
    }

    private int addColumn(String name) {
        int columnIndex = getColumnCount() - 1;
        // This will replace the fieldEditor column, which has to move 1 to the right.
        int newEntryEditorColumnIndex = getColumnCount();
        for (Node node : fieldEditorColumn) {
            setColumnIndex(node, newEntryEditorColumnIndex);
        }
        Button sourceButton = new Button(name);
        sourceButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                getChildrenUnmodifiable().stream().filter(node -> getColumnIndex(node) == columnIndex).filter(node -> node instanceof ToggleButton).forEach(toggleButton -> ((ToggleButton) toggleButton).setSelected(true));
            }
        });
        add(sourceButton, columnIndex, 0);
        return columnIndex;
    }

    public void addField(Field field) {
        TextArea fieldEditorCell = new TextArea();
        FieldRow newRow = new FieldRow(getRowCount(), fieldEditorCell);
        fieldRows.put(field, newRow);
        int fieldEditorColumnIndex = getColumnCount() - 1;
        add(new Label(field.getDisplayName()), 0, newRow.rowIndex);
        fieldEditorColumn.add(fieldEditorCell);
        add(fieldEditorCell, fieldEditorColumnIndex, newRow.rowIndex);
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
            cellButton.setToggleGroup(toggleGroup);
            add(cellButton, columnIndex, rowIndex);
        }
    }
}
