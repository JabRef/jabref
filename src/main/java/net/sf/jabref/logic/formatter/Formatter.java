package net.sf.jabref.logic.formatter;

/**
 * The Formatter is used for a Filter design-pattern. Implementing classes have to accept a String and returned a
 * formatted version of it.
 *
 * Example:
 *
 * "John von Neumann" => "von Neumann, John"
 *
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
