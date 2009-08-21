package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.ParamLayoutFormatter;
import net.sf.jabref.export.ExportFormats;

/**
 * Formatter that outputs a sequence number for the current entry. The sequence number is
 * tied to the entry's position in the order, not to the number of calls to this formatter.
 */
public class Number implements ParamLayoutFormatter {
    public void setArgument(String arg) {
        // No effect currently.
    }

    public String format(String fieldText) {
        return String.valueOf(ExportFormats.entryNumber);
    }
}
