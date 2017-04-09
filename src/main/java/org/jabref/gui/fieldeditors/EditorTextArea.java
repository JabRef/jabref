package org.jabref.gui.fieldeditors;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.text.Font;

import org.jabref.gui.GUIGlobals;

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

        if (GUIGlobals.currentFont != null) {
            setFont(Font.font(GUIGlobals.currentFont.getFontName(), GUIGlobals.currentFont.getSize()));

            setStyle(
                    "text-area-background: " + convertToHex(GUIGlobals.validFieldBackgroundColor) + ";"
                            + "text-area-foreground: " + convertToHex(GUIGlobals.editorTextColor) + ";"
                            + "text-area-highlight: " + convertToHex(GUIGlobals.activeBackgroundColor) + ";"
            );
        }

        getStylesheets().add("org/jabref/gui/fieldeditors/EditorTextArea.css");
    }

    private String convertToHex(java.awt.Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
