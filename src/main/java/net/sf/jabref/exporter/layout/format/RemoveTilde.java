package net.sf.jabref.exporter.layout.format;

import net.sf.jabref.exporter.layout.LayoutFormatter;

/**
 * Replace a non-command tilde ~ by a space.
 * 
 * Useful for formatting Latex code.
 */
public class RemoveTilde implements LayoutFormatter {
    @Override
    public String format(String fieldText) {
        StringBuilder result = new StringBuilder(fieldText.length());

        char[] c = fieldText.toCharArray();

        for (int i = 0; i < c.length; i++) {

            if (c[i] != '~') {
                result.append(c[i]);
                // Skip the next character if the current one is a backslash
                if (c[i] == '\\' && i + 1 < c.length) {
                    i++;
                    result.append(c[i]);
                }
            } else {
                result.append(' ');
            }
        }
        return result.toString();
    }
}
