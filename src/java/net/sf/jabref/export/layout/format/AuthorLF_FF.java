package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.AuthorList;

/**
 *
 */
public class AuthorLF_FF implements LayoutFormatter {

    public String format(String fieldText) {
        AuthorList al = AuthorList.getAuthorList(fieldText);

        return al.getAuthorsLastFirstFirstLastAnds(false);
    }
}
