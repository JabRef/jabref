package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

/**
 * Author First First prints ....
 *
 */
public class AuthorFirstFirst implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorFirstNameFirst(fieldText);
    }
}
