package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.entry.AuthorList;

/**
 * <ul>
 * <li>Names are given in order: von last, jr, first.</li>
 * <li>First names will NOT be abbreviated.</li>
 * <li>Individual authors are separated by commas.</li>
 * <li>There is no comma before the 'and' at the end of a list of three or more authors</li>
 * </ul>
 * 
 * @author mkovtun
 * @author Christopher Oezbek <oezi@oezi.de>
 * 
 */
public class AuthorLastFirstCommas implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return AuthorList.fixAuthorLastNameFirstCommas(fieldText, false, false);
    }
}
