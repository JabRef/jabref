package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

/**
 *
 */
public class AuthorLF_FFAbbr implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        AuthorList al = AuthorList.parse(fieldText);

        return al.getAsLastFirstFirstLastNamesWithAnd(true);
    }
}
