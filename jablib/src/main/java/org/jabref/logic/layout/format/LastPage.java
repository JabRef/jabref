package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/// Formatter that returns the last page from the "pages" field, if set.
///
/// For instance, if the pages field is set to "345-360" or "345--360",
/// this formatter will return "360".
public class LastPage implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        //Replaces fancy characters as '-'
        String normalized = s.trim()
                             .replace('–', '-')
                             .replace('—', '-');
        //Splits in two parts

        String[] pageParts = normalized.split("\\s*-{1,2}\\s*", 2);
        //Returns Second part as last page
        if (pageParts.length == 2) {
            return pageParts[1].trim();
        } else if (pageParts.length >= 1) {
            return normalized;
        } else {
            return "";
        }
    }
}
