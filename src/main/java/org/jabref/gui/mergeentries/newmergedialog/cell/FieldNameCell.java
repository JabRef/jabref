package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * A readonly cell used to display the name of some field.
 */
public class FieldNameCell extends ThreeWayMergeCell {
    public static final String DEFAULT_STYLE_CLASS = "field-name";
    protected final HBox actionLayout = new HBox();
    private final Label label = new Label();

    private final HBox labelBox = new HBox(label);

    public FieldNameCell(String text, int rowIndex) {
        super(text, rowIndex);
        initialize();
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        initializeLabel();
        getChildren().addAll(labelBox, actionLayout);
    }

    private void initializeLabel() {
        label.textProperty().bind(textProperty());
        HBox.setHgrow(labelBox, Priority.ALWAYS);
    }

    public void addSideButton(Button sideButton) {
        // TODO: Allow adding more than one side button
        actionLayout.getChildren().clear();
        actionLayout.getChildren().setAll(sideButton);
    }
}
