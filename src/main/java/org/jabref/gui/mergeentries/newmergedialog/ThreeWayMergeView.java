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

import static org.jabref.gui.mergeentries.newmergedialog.cell.AbstractCell.BackgroundTone;

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
        // getStylesheets().add("newmergedialog.css");
        viewModel = new ThreeWayMergeViewModel(leftEntry, rightEntry, leftHeader, rightHeader);

        mergeToolbox = new ThreeWayMergeToolbox();
        headerView = new HeaderView();
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
        String leftEntryValue = viewModel.getLeftEntry().getField(field).orElse("");
        String rightEntryValue = viewModel.getRightEntry().getField(field).orElse("");
        BackgroundTone backgroundTone = index % 2 == 0 ? BackgroundTone.DARK : BackgroundTone.LIGHT;

        FieldRowController fieldRow = new FieldRowController(field.getDisplayName(), leftEntryValue, rightEntryValue, backgroundTone);
        mergeGridPane.addRow(index, fieldRow.getFieldNameCell(), fieldRow.getLeftValueCell(), fieldRow.getRightValueCell(), fieldRow.getMergedValueCell());
    }
}
