package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;

public class EditorTextArea extends javafx.scene.control.TextArea implements Initializable {

    public EditorTextArea() {
        this("");
    }

    public EditorTextArea(String text) {
        super(text);

        setMinHeight(1);
        setMinWidth(200);

        // Hide horizontal scrollbar and always wrap text
        setWrapText(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
