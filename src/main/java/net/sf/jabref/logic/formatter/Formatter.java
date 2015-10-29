package net.sf.jabref.logic.formatter;

/**
 * Formatter Interface
 */
public interface Formatter {
    /**
     * Returns a human readable name of the formatter usable for e.g. in the GUI
     *
     * @return the name of the formatter
     */
    String getName();

    /**
     * Formats a field value by with a particular formatter transformation.
     *
     * @param value the input String
     * @return the formatted output String
     */
    String format(String value);
}
