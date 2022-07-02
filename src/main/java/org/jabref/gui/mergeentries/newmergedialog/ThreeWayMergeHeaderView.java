package org.jabref.gui.mergeentries.newmergedialog;

import javafx.geometry.Insets;
import javafx.scene.control.Control;
import javafx.scene.layout.GridPane;

import org.jabref.gui.mergeentries.newmergedialog.cell.HeaderCell;

/**
 * GridPane was used instead of a Hbox because Hbox allocates more space for cells
 * with longer text, but I wanted all cells to have the same width
 */
public class ThreeWayMergeHeaderView extends GridPane {
    public static final String DEFAULT_STYLE_CLASS = "merge-header";
    private final HeaderCell leftHeaderCell;
    private final HeaderCell rightHeaderCell;

    private final HeaderCell mergedHeaderCell;

    public ThreeWayMergeHeaderView(String leftHeader, String rightHeader) {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        this.leftHeaderCell = new HeaderCell(leftHeader);
        this.rightHeaderCell = new HeaderCell(rightHeader);
        this.mergedHeaderCell = new HeaderCell("Merged Entry");

        addRow(0,
               new HeaderCell(""),
               leftHeaderCell,
               rightHeaderCell,
               mergedHeaderCell
        );

        setPrefHeight(Control.USE_COMPUTED_SIZE);
        setMaxHeight(Control.USE_PREF_SIZE);
        setMinHeight(Control.USE_PREF_SIZE);

        // The fields grid pane is contained within a scroll pane, thus it doesn't allocate the full available width. In
        // fact, it uses the available width minus the size of the scrollbar which is 8. This leads to header columns being
        // always wider than fields columns. This hack should fix it.
        setPadding(new Insets(0, 8, 0, 0));
    }

    public void setLeftHeader(String leftHeader) {
        leftHeaderCell.setText(leftHeader);
    }

    public void setRightHeader(String rightHeader) {
        rightHeaderCell.setText(rightHeader);
    }
}
