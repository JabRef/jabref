package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

/// 
/// - Names are given in order: von last, jr, first.
/// - First names will be abbreviated.
/// - Individual authors are separated by commas.
/// - The 'and' of a list of three or more authors is preceeded by a comma
/// (Oxford comma)
/// 
public class AuthorLastFirstAbbrOxfordCommas implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorLastNameFirstCommas(fieldText, true, true);
    }
}
