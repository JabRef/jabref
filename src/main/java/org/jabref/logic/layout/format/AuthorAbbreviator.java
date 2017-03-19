package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

/**
 * Duplicate of AuthorLastFirstAbbreviator.
 *
 * @see AuthorLastFirstAbbreviator
 *
 * @author Carlos Silla
 */
public class AuthorAbbreviator implements LayoutFormatter {

    /*
     * (non-Javadoc)
     *
     * @see org.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {
        AuthorList list = AuthorList.parse(fieldText);
        return list.getAsLastFirstNamesWithAnd(true);
    }
}
