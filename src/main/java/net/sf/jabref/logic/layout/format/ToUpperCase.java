package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Convert the contents to upper case.
 */
public class ToUpperCase implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }

        return fieldText.toUpperCase();
    }

}
