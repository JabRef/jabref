package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.util.BindingsHelper;

import org.fxmisc.richtext.StyleClassedTextArea;

public class MergedFieldCell extends AbstractCell {
    private static final String DEFAULT_STYLE_CLASS = "merged-field";

    private final StyleClassedTextArea textArea = new StyleClassedTextArea();

    public MergedFieldCell(String text, int rowIndex) {
        super(text, rowIndex);
        initialize();
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        initializeTextArea();
        getChildren().add(textArea);
    }

    private void initializeTextArea() {
        bindTextAreaValue();

        setAlignment(Pos.CENTER);
        textArea.setWrapText(true);
        textArea.setAutoHeight(true);
        textArea.setPadding(new Insets(8));
        HBox.setHgrow(textArea, Priority.ALWAYS);

        textArea.addEventFilter(ScrollEvent.SCROLL, e -> {
            e.consume();
            MergedFieldCell.this.fireEvent(e.copyFor(e.getSource(), MergedFieldCell.this));
        });
    }

    private void bindTextAreaValue() {
        textArea.replaceText(textProperty().get());
        textProperty().addListener(((observable, oldValue, newValue) -> textArea.replaceText(newValue)));
    }
}
