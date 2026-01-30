package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

///
/// - Names are given in order: von last, jr, first.
/// - First names will NOT be abbreviated.
/// - Individual authors are separated by commas.
/// - There is no comma before the 'and' at the end of a list of three or more authors
///
public class AuthorLastFirstCommas implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorLastNameFirstCommas(fieldText, false, false);
    }
}
