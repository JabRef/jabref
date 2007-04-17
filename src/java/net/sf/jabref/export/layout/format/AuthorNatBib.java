package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.AuthorList;

/**
 * Natbib style: Last names only. Two authors are separated by "and",
 * three or more authors are given as "Smith et al."
 *
 * @author Morten O. Alver
 */
public class AuthorNatBib implements LayoutFormatter {


    public String format(String fieldText) {
        return AuthorList.fixAuthor_Natbib(fieldText);
    }
}
