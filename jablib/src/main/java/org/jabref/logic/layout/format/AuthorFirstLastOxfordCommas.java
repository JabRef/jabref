package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

/**
 * <ul>
 * <li>Names are given as first name, von and last name.</li>
 * <li>Individual authors separated by comma.</li>
 * <li>The and of a list of three or more authors is preceeded by a comma
 * (Oxford comma)</li>
 * </ul>
 */
public class AuthorFirstLastOxfordCommas implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorFirstNameFirstCommas(fieldText, false, true);
    }
}
