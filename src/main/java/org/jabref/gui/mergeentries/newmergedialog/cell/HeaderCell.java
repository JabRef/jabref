package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.scene.control.Label;

public class HeaderCell extends AbstractCell {
    public static final String DEFAULT_STYLE_CLASS = "header-cell";
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
    }
}
