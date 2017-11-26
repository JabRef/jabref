package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Utility class with static methods for javafx {@link Text} objects
 */
public class TextUtil {

    public enum TextType {
        NORMAL, BOLD, ITALIC, MONOSPACED
    }

    public static Text createText(String textString, double size, TextType textType) {
        Text text = new Text(textString);
        Font font = null;
        switch (textType) {
            case NORMAL:
                font = Font.font("System Regular", size);
                break;
            case BOLD:
                font = Font.font("System Regular", FontWeight.BOLD, size);
                break;
            case ITALIC:
                font = Font.font("System Regular", FontPosture.ITALIC, size);
                break;
            case MONOSPACED:
                font = Font.font("Monospaced", size);
                break;
        }
        text.setFont(font);
        return text;
    }

    public static Text createText(String textString, double size) {
        return createText(textString, size, TextType.NORMAL);
    }

    public static String textToHTMLString(Text text) {
        String textString = text.getText();
        textString = textString.replace("\n", "<br>");
        if (text.getFont().getFamily().equals("Monospaced")) {
            textString = String.format("<kbd>%s</kbd>", textString);
        }
        switch (text.getFont().getStyle()) {
            case "Bold":
                return String.format("<b>%s</b>", textString);
            case "Italic":
                return String.format("<i>%s</i>", textString);
            default:
                return textString;
        }
    }


    /**
     * Formats a String to multiple Texts by replacing some parts and adding font characteristics.
     */
    public static List<Text> formatToTexts(String original, double textSize, TextReplacement... replacements) {
        List<Text> textList = new ArrayList<>();
        textList.add(new Text(original));
        for (TextReplacement replacement : replacements) {
            splitReplace(textList, textSize, replacement);
        }

        return textList;
    }

    private static void splitReplace(List<Text> textList, double textSize, TextReplacement replacement) {
        Optional<Text> textContainingReplacement = textList.stream().filter(it -> it.getText().contains(replacement.toReplace)).findFirst();
        if (textContainingReplacement.isPresent()) {
            int index = textList.indexOf(textContainingReplacement.get());
            String original = textContainingReplacement.get().getText();
            textList.remove(index);
            String[] textParts = original.split(replacement.toReplace);
            if (textParts.length == 2) {
                if (textParts[0].equals("")) {
                    textList.add(index, TextUtil.createText(replacement.replacement, textSize, replacement.textType));
                    textList.add(index + 1, TextUtil.createText(textParts[1], textSize, TextUtil.TextType.NORMAL));
                } else {
                    textList.add(index, TextUtil.createText(textParts[0], textSize, TextUtil.TextType.NORMAL));
                    textList.add(index + 1, TextUtil.createText(replacement.replacement, textSize, replacement.textType));
                    textList.add(index + 2, TextUtil.createText(textParts[1], textSize, TextUtil.TextType.NORMAL));
                }
            } else if (textParts.length == 1) {
                textList.add(index, TextUtil.createText(textParts[0], textSize, TextUtil.TextType.NORMAL));
                textList.add(index + 1, TextUtil.createText(replacement.replacement, textSize, replacement.textType));
            } else {
                throw new IllegalStateException("It is not allowed that the toReplace string: '" + replacement.toReplace
                        + "' exists multiple times in the original string");
            }
        } else {
            throw new IllegalStateException("It is not allowed that the toReplace string: '" + replacement.toReplace
                    + "' does not exist in the original string");
        }
    }

    public static class TextReplacement {
        String toReplace;
        String replacement;
        TextUtil.TextType textType;

        public TextReplacement(String toReplace, String replacement, TextUtil.TextType textType) {
            this.toReplace = toReplace;
            this.replacement = replacement;
            this.textType = textType;
        }
    }
}
