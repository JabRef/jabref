package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

///
/// - Names are given as first name, von and last name.
/// - First names will be abbreviated.
/// - Individual authors separated by comma.
/// - The and of a list of three or more authors is preceeded by a comma
/// (Oxford comma)
///
public class AuthorFirstAbbrLastOxfordCommas implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorFirstNameFirstCommas(fieldText, true, true);
    }
}
