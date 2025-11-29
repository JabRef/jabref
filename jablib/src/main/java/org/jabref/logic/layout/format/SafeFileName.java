package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/**
 * Remove all the characters that are not allowed by the OS in file names
 */
public class SafeFileName implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return fieldText.replaceAll("[\\\\/:*?\"<>|]", "");
    }
}
