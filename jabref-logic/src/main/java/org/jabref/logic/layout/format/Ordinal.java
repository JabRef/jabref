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
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String result = m.group(1);
            int value = Integer.parseInt(result);
            String ordinalString;
            switch (value) {
            case 1:
                ordinalString = "st";
                break;
            case 2:
                ordinalString = "nd";
                break;
            case 3:
                ordinalString = "rd";
                break;
            default:
                ordinalString = "th";
                break;
            }
            m.appendReplacement(sb, result + ordinalString);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
