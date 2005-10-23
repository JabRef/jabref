/*
 * AuthorFirstLastCommas.java
 *
 * Created on September 7, 2005, 1:06 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.AuthorList;

/**
 *
 * @author mkovtun
 */
public class AuthorFirstLastCommas implements LayoutFormatter {

    public String format(String fieldText) {
        return AuthorList.fixAuthor_firstNameFirstCommas(fieldText, false);
    }

}