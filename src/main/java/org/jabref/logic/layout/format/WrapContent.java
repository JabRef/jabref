package org.jabref.logic.layout.format;

import java.util.List;

import org.jabref.logic.layout.AbstractParamLayoutFormatter;

/**
 * This formatter outputs the input value after adding a prefix and a postfix,
 * as long as the input value is non-empty. If the input value is empty, an
 * empty string is output (the prefix and postfix are not output in this case).
 *
 * The formatter requires an argument containing the prefix and postix separated
 * by a comma. To include a the comma character in either, use an escape sequence
 * (\,).
 */
public class WrapContent extends AbstractParamLayoutFormatter {

    private String before;
    private String after;

    @Override
    public void setArgument(String arg) {
        List<String> parts = AbstractParamLayoutFormatter.parseArgument(arg);
        if (parts.size() < 2) {
            return;
        }
        before = parts.get(0);
        after = parts.get(1);
    }

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        if (before == null) {
            return fieldText;
        }
        if (fieldText.isEmpty()) {
            return "";
        } else {
            return before + fieldText + after;
        }
    }
}
