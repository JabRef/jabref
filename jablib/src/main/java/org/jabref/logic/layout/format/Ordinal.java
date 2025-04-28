package org.jabref.logic.layout.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.layout.LayoutFormatter;

/**
 * Converts number to ordinal
 */
public class Ordinal implements LayoutFormatter {

    // Detect last digit in number not directly followed by a letter plus the number 11
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(1?\\d\\b)");

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        Matcher m = NUMBER_PATTERN.matcher(fieldText);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String result = m.group(1);
            int value = Integer.parseInt(result);
            // CHECKSTYLE:OFF
            String ordinalString = switch (value) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
            // CHECKSTYLE:ON
            m.appendReplacement(sb, result + ordinalString);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
