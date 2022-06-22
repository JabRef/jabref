package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.text.Text;

/**
 * Utility class with static methods for javafx {@link Text} objects
 */
public class TooltipTextUtil {

    // (?s) tells Java that "." also matches the newline character
    // (?<...>...) are named groups in Java regular expressions: https://stackoverflow.com/a/415635/873282
    // .*? tells to match non-greedy (see https://stackoverflow.com/q/7124778/873282 for details)
    private static final Pattern TT_TEXT = Pattern.compile("(?s)(?<before>.*?)<tt>(?<in>.*?)</tt>");

    private static final Pattern B_TEXT = Pattern.compile("(?s)(?<before>.*?)<b>(?<in>.*?)</b>");

    public enum TextType {
        NORMAL, BOLD, ITALIC, MONOSPACED
    }

    public static Text createText(String textString, TextType textType) {
        Text text = new Text(textString);
        switch (textType) {
            case BOLD:
                text.getStyleClass().setAll("tooltip-text-bold");
                break;
            case ITALIC:
                text.getStyleClass().setAll("tooltip-text-italic");
                break;
            case MONOSPACED:
                text.getStyleClass().setAll("tooltip-text-monospaced");
                break;
            default:
                break;
        }
        return text;
    }

    public static Text createText(String textString) {
        return createText(textString, TextType.NORMAL);
    }

    /**
     * Creates a list of Text elements respecting <code>tt</code> and <code>b</code> markers.
     * Nesting of these markers is not possible.
     */
    public static List<Text> createTextsFromHtml(String htmlString) {
        List<Text> result = new ArrayList<>();

        Matcher matcher = TT_TEXT.matcher(htmlString);
        int lastMatchPos = 0;
        while (matcher.find()) {
            lastMatchPos = matcher.end();
            String before = matcher.group("before");
            if (!before.isBlank()) {
                result.addAll(convertHtmlBold(before));
            }
            String in = matcher.group("in");
            result.add(TooltipTextUtil.createText(in, TooltipTextUtil.TextType.MONOSPACED));
        }
        if (lastMatchPos < htmlString.length()) {
            String remaining = htmlString.substring(lastMatchPos);
            result.addAll(convertHtmlBold(remaining));
        }

        return result;
    }

    private static List<Text> convertHtmlBold(String htmlString) {
        List<Text> result = new ArrayList<>();

        Matcher matcher = B_TEXT.matcher(htmlString);
        int lastMatchPos = 0;
        while (matcher.find()) {
            lastMatchPos = matcher.end();
            String before = matcher.group("before");
            if (!before.isBlank()) {
                result.add(TooltipTextUtil.createText(before));
            }
            String in = matcher.group("in");
            result.add(TooltipTextUtil.createText(in, TextType.BOLD));
        }
        if (lastMatchPos < htmlString.length()) {
            String remaining = htmlString.substring(lastMatchPos);
            result.add(TooltipTextUtil.createText(remaining));
        }

        return result;
    }

    /**
     * Formats a String to multiple Texts by replacing some parts and adding font characteristics.
     */
    public static List<Text> formatToTexts(String original, TextReplacement... replacements) {
        List<Text> textList = new ArrayList<>();
        textList.add(new Text(original));
        for (TextReplacement replacement : replacements) {
            splitReplace(textList, replacement);
        }

        return textList;
    }

    private static void splitReplace(List<Text> textList, TextReplacement replacement) {
        Optional<Text> textContainingReplacement = textList.stream().filter(it -> it.getText().contains(replacement.toReplace)).findFirst();
        if (textContainingReplacement.isPresent()) {
            int index = textList.indexOf(textContainingReplacement.get());
            String original = textContainingReplacement.get().getText();
            textList.remove(index);
            String[] textParts = original.split(replacement.toReplace);
            if (textParts.length == 2) {
                if (textParts[0].equals("")) {
                    textList.add(index, TooltipTextUtil.createText(replacement.replacement, replacement.textType));
                    textList.add(index + 1, TooltipTextUtil.createText(textParts[1], TooltipTextUtil.TextType.NORMAL));
                } else {
                    textList.add(index, TooltipTextUtil.createText(textParts[0], TooltipTextUtil.TextType.NORMAL));
                    textList.add(index + 1, TooltipTextUtil.createText(replacement.replacement, replacement.textType));
                    textList.add(index + 2, TooltipTextUtil.createText(textParts[1], TooltipTextUtil.TextType.NORMAL));
                }
            } else if (textParts.length == 1) {
                textList.add(index, TooltipTextUtil.createText(textParts[0], TooltipTextUtil.TextType.NORMAL));
                textList.add(index + 1, TooltipTextUtil.createText(replacement.replacement, replacement.textType));
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
        private final String toReplace;
        private final String replacement;
        private final TooltipTextUtil.TextType textType;

        public TextReplacement(String toReplace, String replacement, TooltipTextUtil.TextType textType) {
            this.toReplace = toReplace;
            this.replacement = replacement;
            this.textType = textType;
        }
    }

    public static String textToHtmlString(Text text) {
        String textString = text.getText();
        textString = textString.replace("\n", "<br>");
        if (text.getStyleClass().toString().contains("tooltip-text-monospaced")) {
            textString = String.format("<tt>%s</tt>", textString);
        }
        if (text.getStyleClass().toString().contains("tooltip-text-bold")) {
            textString = String.format("<b>%s</b>", textString);
        }
        if (text.getStyleClass().toString().contains("tooltip-text-italic")) {
            textString = String.format("<i>%s</i>", textString);
        }
        return textString;
    }
}
