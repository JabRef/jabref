package org.jabref.logic.cleanup;

/**
 * The Formatter is used for a Filter design-pattern. Extending classes have to accept a String and returned a formatted
 * version of it. Implementations have to reside in the logic package.
 * <p>
 * Example:
 * <p>
 * "John von Neumann" => "von Neumann, John"
 */
public abstract class Formatter {

    /**
     * Returns a human readable name of the formatter usable for e.g. in the GUI
     *
     * @return the name of the formatter, always not null
     */
    public abstract String getName();

    /**
     * Returns a unique key for the formatter that can be used for its identification
     *
     * @return the key of the formatter, always not null
     */
    public abstract String getKey();

    /**
     * Formats a field value by with a particular formatter transformation.
     * <p>
     * Calling this method with a null argument results in a NullPointerException.
     *
     * @param value the input String
     * @return the formatted output String
     */
    public abstract String format(String value);

    /**
     * Returns a description of the formatter.
     *
     * @return the description string, always non empty
     */
    public abstract String getDescription();

    /**
     * Returns an example input string of the formatter. This example is used as input to the formatter to demonstrate
     * its functionality
     *
     * @return the example input string, always non empty
     */
    public abstract String getExampleInput();

    /**
     * Returns a default hashcode of the formatter based on its key.
     *
     * @return the hash of the key of the formatter
     */
    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    /**
     * Indicates whether some other object is the same formatter as this one based on the key.
     *
     * @param obj the object to compare the formatter to
     * @return true if the object is a formatter with the same key
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Formatter) {
            return getKey().equals(((Formatter) obj).getKey());
        } else {
            return false;
        }
    }
}
