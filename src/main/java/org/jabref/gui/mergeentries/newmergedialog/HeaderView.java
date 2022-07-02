package org.jabref.gui.mergeentries.newmergedialog;

import javafx.geometry.Insets;
import javafx.scene.control.Control;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import org.jabref.gui.mergeentries.newmergedialog.cell.HeaderCell;

/**
 * I used a GridPane instead of a Hbox because Hbox allocates more space for cells
 * with longer text, but I wanted all cells to have the same width
 */
public class HeaderView extends GridPane {
    public static final int GRID_COLUMN_MIN_WIDTH = 250;
    private final String leftHeader;
    private final String rightHeader;
    public HeaderView(String leftHeader, String rightHeader) {
        this.leftHeader = leftHeader;
        this.rightHeader = rightHeader;
        setPrefHeight(Control.USE_COMPUTED_SIZE);

        setMaxHeight(Control.USE_PREF_SIZE);
        setMinHeight(Control.USE_PREF_SIZE);
        addRow(
                0,
                new HeaderCell(""),
                new HeaderCell(leftHeader),
                new HeaderCell(rightHeader),
                new HeaderCell("Merged Entry")
        );

        bindHeaderContainerWidthToFieldGridWidth();

        ColumnConstraints fieldNameColumnConstraints = new ColumnConstraints(150);
        fieldNameColumnConstraints.setHgrow(Priority.NEVER);

        ColumnConstraints leftEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);
        ColumnConstraints rightEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);
        ColumnConstraints mergedEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);

        leftEntryColumnConstraints.setHgrow(Priority.ALWAYS);
        rightEntryColumnConstraints.setHgrow(Priority.ALWAYS);
        mergedEntryColumnConstraints.setHgrow(Priority.ALWAYS);

        getColumnConstraints().setAll(
                fieldNameColumnConstraints,
                leftEntryColumnConstraints,
                rightEntryColumnConstraints,
                mergedEntryColumnConstraints
        );
    }

    /**
     * The fields grid pane is contained within a scroll pane, thus it doesn't allocate the full available width. In
     * fact, it uses the available width minus the size of the scrollbar which is 8. This leads to header columns being
     * always larger than fields columns. This hack should solve this problem.
     */
    private void bindHeaderContainerWidthToFieldGridWidth() {
        setPadding(new Insets(0, 8, 0, 0));
        setStyle("-fx-background-color: #EFEFEF");
    }
}
