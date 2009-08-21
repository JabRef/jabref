package net.sf.jabref.export.layout.format;

import net.sf.jabref.AuthorList;
import net.sf.jabref.export.layout.LayoutFormatter;

/**
 *
 */
public class AuthorLF_FFAbbr implements LayoutFormatter {

    public String format(String fieldText) {
        AuthorList al = AuthorList.getAuthorList(fieldText);

        return al.getAuthorsLastFirstFirstLastAnds(true);
    }
}
