package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/**
 * Uses as input the fields (author or editor) in the LastFirst format.
 * <p>
 * This formater enables to abbreviate the authors name in the following way:
 * <p>
 * Ex: Someone, Van Something will be abbreviated as Someone, V.S.
 */
public class AuthorLastFirstAbbreviator implements LayoutFormatter {

    /**
     * @see org.jabref.logic.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {
        // This formatter is a duplicate of AuthorAbbreviator, so we simply call that one.
        return new AuthorAbbreviator().format(fieldText);
    }
}
