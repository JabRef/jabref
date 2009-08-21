package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.Globals;

/**
 * Formatter used to signal that a formatter hasn't been found. This can be
 * used for graceful degradation if a layout uses an undefined format.
 */
public class NotFoundFormatter implements LayoutFormatter {
    private String notFound;

    public NotFoundFormatter(String notFound) {

        this.notFound = notFound;
    }

    public String getNotFound() {
        return notFound;
    }

    public String format(String fieldText) {
        return "["+Globals.lang("Formatter not found: %0", notFound)+"] "+fieldText;
    }
}
