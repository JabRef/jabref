package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/// Formatter that returns the first page from the "pages" field, if set.
///
/// For instance, if the pages field is set to "345-360" or "345--360",
/// this formatter will return "345".
public class FirstPage implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        // Replaces fancy characters as '-'
        String normalized = s.trim()
                             .replace('–', '-')
                             .replace('—', '-');

        // Splits the numbers in two parts, shows only first
        String[] pageParts = normalized.split("\\s*-{1,2}\\s*", 2);

        if (pageParts.length == 2) {
            return pageParts[0].trim();
        } else {
            return normalized;
        }
    }
}
