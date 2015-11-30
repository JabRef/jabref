package net.sf.jabref.logic.formatter.minifier;

import net.sf.jabref.logic.formatter.Formatter;

/**
 * Replaces three or more authors with and others
 */
public class AuthorsMinifier implements Formatter {
    @Override
    public String getName() {
        return "Minify authors";
    }

    /**
     * Replaces three or more authors with and others.
     *
     * <example> Stefan Kolb -> Stefan Kolb Stefan Kolb and Simon Harrer -> Stefan Kolb and Simon Harrer Stefan Kolb and
     * Simon Harrer and Jörg Lenhard -> Stefan Kolb and others </example>
     */
    @Override
    public String format(String value) {
        // nothing to do
        if ((value == null) || value.isEmpty()) {
            return value;
        }

        return abbreviateAuthor(value);
    }

    private String abbreviateAuthor(String authorField) {
        // single author
        String authorSeparator = " and ";

        if (!authorField.contains(authorSeparator)) {
            return authorField;
        }

        String[] authors = authorField.split(authorSeparator);

        // trim authors
        for (int i = 0; i < authors.length; i++) {
            authors[i] = authors[i].trim();
        }

        // already abbreviated
        if ("others".equals(authors[authors.length - 1]) && (authors.length == 2)) {
            return authorField;
        }

        // abbreviate
        if (authors.length < 3) {
            return authorField;
        }

        return authors[0] + authorSeparator + "others";
    }
}
