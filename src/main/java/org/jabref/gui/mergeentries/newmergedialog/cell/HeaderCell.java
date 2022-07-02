package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.geometry.Insets;
import javafx.scene.control.Label;

public class HeaderCell extends AbstractCell {
    public static final String DEFAULT_STYLE_CLASS = "header-cell";
    private final Label label = new Label();

    public HeaderCell(String text) {
        super(text, 1);
        initialize();
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        initializeLabel();
        getChildren().add(label);
        setStyle("-fx-border-width: 0 0 0.8 0; -fx-border-color: #424758");
    }

    private void initializeLabel() {
        label.textProperty().bind(textProperty());
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13");
        label.setPadding(new Insets(1, 0, 1, 0));
    }
}
