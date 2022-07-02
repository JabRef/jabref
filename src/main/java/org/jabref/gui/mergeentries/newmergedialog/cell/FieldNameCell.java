package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.scene.control.Label;

/**
 * A non-editable cell that contains the name of some field
 */
public class FieldNameCell extends AbstractCell {
    public static final String DEFAULT_STYLE_CLASS = "field-name";
    private final Label label = new Label();

    public FieldNameCell(String text, BackgroundTone backgroundTone) {
        super(text, backgroundTone);
        initialize();
    }

    public FieldNameCell(String text) {
        super(text);
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
