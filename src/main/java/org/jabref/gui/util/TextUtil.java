package org.jabref.gui.util;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Utility class with static methods for creating javafx {@link Text} objects
 */
public class TextUtil {

    public static Text createText(String textString, double size) {
        Text text = new Text(textString);
        text.setFont(Font.font("System Regular", size));
        return text;
    }

    public static Text createTextBold(String textString, double size) {
        Text text = new Text(textString);
        text.setFont(Font.font("System Regular", FontWeight.BOLD, size));
        return text;
    }

    public static Text createTextItalic(String textString, double size) {
        Text text = new Text(textString);
        text.setFont(Font.font("System Regular", FontPosture.ITALIC, size));
        return text;
    }

    public static Text createTextMonospaced(String textString, double size) {
        Text text = new Text(textString);
        text.setFont(Font.font("Monospaced", size));
        return text;
    }

    public static String textToHTMLString(Text text) {
        String textString = text.getText();
        textString = textString.replace("\n", "<br>");
        if (text.getFont().getFamily().equals("Monospaced")) {
            textString = String.format("<kbd>%s</kbd>",textString);
        }
        switch (text.getFont().getStyle()) {
            case "Bold":
                return String.format("<b>%s</b>",textString);
            case "Italic":
                return String.format("<i>%s</i>",textString);
            default:
                return textString;
        }
    }
}
