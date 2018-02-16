package org.jabref.logic.layout;

/**
 * The LayoutFormatter is used for a Filter design-pattern.
 *
 * Implementing classes have to accept a String and returned a formatted version of it.
 *
 * Example:
 *
 *   "John von Neumann" => "von Neumann, John"
 *
 * @version 1.2 - Documentation CO
 */
@FunctionalInterface
public interface LayoutFormatter {

    /**
     * Failure Mode:
     * <p>
     * Formatters should be robust in the sense that they always return some
     * relevant string.
     * <p>
     * If the formatter can detect an invalid input it should return the
     * original string otherwise it may simply return a wrong output.
     *
     * @param fieldText
     *            The text to layout.
     * @return The layouted text.
     */
    String format(String fieldText);
}
