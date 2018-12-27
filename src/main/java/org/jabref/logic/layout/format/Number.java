package org.jabref.logic.layout.format;

import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.layout.ParamLayoutFormatter;

/**
 * Formatter that outputs a sequence number for the current entry. The sequence number is
 * tied to the entry's position in the order, not to the number of calls to this formatter.
 */
public class Number implements ParamLayoutFormatter {

    @Override
    public void setArgument(String arg) {
        // No effect currently.
    }

    @Override
    public String format(String fieldText) {
        return String.valueOf(ExporterFactory.entryNumber);
    }
}
