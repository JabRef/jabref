package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.entry.AuthorList;

public class AuthorLastFirst implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorLastNameFirst(fieldText);
    }
}
