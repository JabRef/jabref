package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

/**
 * <ul>
 * <li>Names are given as first name, von and last name.</li>
 * <li>First names will not be abbreviated.</li>
 * <li>Individual authors separated by comma.</li>
 * <li>There is no comma before the and of a list of three or more authors.</li>
 * </ul>
 */
public class AuthorFirstLastCommas implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorFirstNameFirstCommas(fieldText, false, false);
    }
}
