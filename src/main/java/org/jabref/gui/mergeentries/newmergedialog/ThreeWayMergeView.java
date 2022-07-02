package org.jabref.gui.mergeentries.newmergedialog;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

import org.jabref.gui.mergeentries.newmergedialog.toolbox.ThreeWayMergeToolbox;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.types.EntryTypeFactory;

public class ThreeWayMergeView extends VBox {
    public static final int GRID_COLUMN_MIN_WIDTH = 100;
    public static final String LEFT_DEFAULT_HEADER = "Left Entry";
    public static final String RIGHT_DEFAULT_HEADER = "Right Entry";
    private ThreeWayMergeToolbox mergeToolbox;
    private HeaderView headerView;
    private final ScrollPane scrollPane;
    private final GridPane mergeGridPane;

    private final ThreeWayMergeViewModel viewModel;

    public ThreeWayMergeView(BibEntry leftEntry, BibEntry rightEntry, String leftHeader, String rightHeader) {
        getStylesheets().add(ThreeWayMergeView.class.getResource("ThreeWayMergeView.css").toExternalForm());
        viewModel = new ThreeWayMergeViewModel(leftEntry, rightEntry, leftHeader, rightHeader);

        mergeToolbox = new ThreeWayMergeToolbox();
        headerView = new HeaderView(leftHeader, rightHeader);
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mergeGridPane = new GridPane();
        initializeMergeGridPane();

        scrollPane.setContent(mergeGridPane);
        getChildren().addAll(mergeToolbox, headerView, scrollPane);

        this.setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.78);
        this.setPrefWidth(Screen.getPrimary().getBounds().getWidth() * 0.95);
    }

    public ThreeWayMergeView(BibEntry leftEntry, BibEntry rightEntry) {
        this(leftEntry, rightEntry, LEFT_DEFAULT_HEADER, RIGHT_DEFAULT_HEADER);
    }

    private void initializeMergeGridPane() {
        ColumnConstraints fieldNameColumnConstraints = new ColumnConstraints(150);
        fieldNameColumnConstraints.setHgrow(Priority.NEVER);

        ColumnConstraints leftEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);
        ColumnConstraints rightEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);
        ColumnConstraints mergedEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);

        leftEntryColumnConstraints.setHgrow(Priority.ALWAYS);
        rightEntryColumnConstraints.setHgrow(Priority.ALWAYS);
        mergedEntryColumnConstraints.setHgrow(Priority.ALWAYS);

        mergeGridPane.getColumnConstraints().addAll(fieldNameColumnConstraints, leftEntryColumnConstraints, rightEntryColumnConstraints, mergedEntryColumnConstraints);

        for (int fieldIndex = 0; fieldIndex < viewModel.allFieldsSize(); fieldIndex++) {
            addFieldRow(fieldIndex);
        }
    }

    private void addFieldRow(int index) {
        Field field = viewModel.allFields().get(index);

        String leftEntryValue;
        String rightEntryValue;
        if (field.equals(InternalField.TYPE_HEADER)) {
            leftEntryValue = viewModel.getLeftEntry().getType().getDisplayName();
            rightEntryValue = viewModel.getRightEntry().getType().getDisplayName();
        } else {
            leftEntryValue = viewModel.getLeftEntry().getField(field).orElse("");
            rightEntryValue = viewModel.getRightEntry().getField(field).orElse("");
        }

        FieldRowController fieldRow = new FieldRowController(field.getDisplayName(), leftEntryValue, rightEntryValue, index);
        fieldRow.mergedValueProperty().addListener((observable, old, mergedValue) -> {
            if (field.equals(InternalField.TYPE_HEADER)) {
                getMergedEntry().setType(EntryTypeFactory.parse(mergedValue));
            } else {
                getMergedEntry().setField(field, mergedValue);
            }
        });
        if (field.equals(InternalField.TYPE_HEADER)) {
            getMergedEntry().setType(EntryTypeFactory.parse(fieldRow.getMergedValue()));
        } else {
            getMergedEntry().setField(field, fieldRow.getMergedValue());
        }

        if (fieldRow.hasEqualLeftAndRightValues()) {
            mergeGridPane.add(fieldRow.getFieldNameCell(), 0, index, 1, 1);
            mergeGridPane.add(fieldRow.getLeftValueCell(), 1, index, 2, 1);
            mergeGridPane.add(fieldRow.getMergedValueCell(), 3, index, 1, 1);
        } else {
            mergeGridPane.addRow(index, fieldRow.getFieldNameCell(), fieldRow.getLeftValueCell(), fieldRow.getRightValueCell(), fieldRow.getMergedValueCell());
        }
    }

    public BibEntry getMergedEntry() {
        return viewModel.getMergedEntry();
    }
}
