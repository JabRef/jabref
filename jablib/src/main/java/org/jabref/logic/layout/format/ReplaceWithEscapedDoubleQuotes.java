package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

public class ReplaceWithEscapedDoubleQuotes implements LayoutFormatter {
    @Override
    public String format(String fieldText) {
        StringBuilder builder = new StringBuilder(fieldText.length());

        for (char c : fieldText.toCharArray()) {
            if (c == '\"') {
                builder.append("\"\"");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
