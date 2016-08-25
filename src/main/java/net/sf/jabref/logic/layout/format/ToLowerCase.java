package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Convert the contents to lower case.
 */
public class ToLowerCase implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        return fieldText.toLowerCase();
    }
}
