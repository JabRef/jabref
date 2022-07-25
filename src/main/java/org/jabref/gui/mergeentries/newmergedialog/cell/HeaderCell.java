package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.geometry.Insets;
import javafx.scene.control.Label;

/**
 * A readonly cell used to display the header of the ThreeWayMerge UI at the top of the layout.
 * */
public class HeaderCell extends AbstractCell {
    public static final String DEFAULT_STYLE_CLASS = "merge-header-cell";
    private final Label label = new Label();

    public HeaderCell(String text) {
        super(text, AbstractCell.NO_ROW_NUMBER);
        initialize();
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        initializeLabel();
        getChildren().add(label);
    }

    private void initializeLabel() {
        label.textProperty().bind(textProperty());
        label.setPadding(new Insets(getPadding().getTop(), getPadding().getRight(), getPadding().getBottom(), 16));
    }
}
