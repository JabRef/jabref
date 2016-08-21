package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.entry.AuthorList;

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
