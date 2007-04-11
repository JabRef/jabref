package net.sf.jabref.export.layout.format;

import net.sf.jabref.AuthorList;
import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * <ul>
 * <li>Names are given in order: first von last, jr.</li>
 * <li>First names will NOT be abbreviated.</li>
 * <li>Individual authors are separated by commas.</li>
 * <li>There is no comma before the 'and' at the end of a list of three or more authors</li>
 * </ul>
 *
 * @author Morten O. Alver / Christopher Oezbek <oezi@oezi.de>
 *
 */
public class AuthorFirstFirstCommas implements LayoutFormatter {

    public String format(String fieldText) {
        return AuthorList.fixAuthor_firstNameFirstCommas(fieldText, false, false);
    }
}
