/*
 * Created on Oct 25, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jabref.mods;

import java.util.Vector;
import wsi.ra.tool.WSITools;


import net.sf.jabref.AuthorList;

/**
 * @author Michael Wrighton
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PersonName {
    protected String givenName = null;
    protected String surname = null;
    protected String middleName = null;

    public PersonName(String name) {
        parseName(name);
    }

    protected void parseName(String author) {
            // TODO: replace special characters
            Vector v = new Vector();
            String authorMod = AuthorList.fixAuthor_firstNameFirst(author);

            WSITools.tokenize(v, authorMod, " \n\r");

            if (v.size() == 1)
                surname = (String) v.get(0);
            else if (v.size() == 2) {
                givenName = (String) v.get(0);
                surname = (String) v.get(1);
            }
            else {
                givenName = (String) v.get(0);
                middleName = (String) v.get(1);
                surname = (String) v.get(2);
            }
    }

    public String getGivenNames() {
        String result = "";
        if (givenName != null)
            result += givenName;
        if (middleName != null)
            result += " " + middleName;
        return result;
    }

    public String getSurname()
    {
        return surname;
    }

    public String toString() {
        return surname;
    }
}
