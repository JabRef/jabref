/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.mods;

import java.util.Vector;

import net.sf.jabref.export.layout.WSITools;
import net.sf.jabref.export.layout.format.XMLChars;

import net.sf.jabref.AuthorList;

/**
 * @author Michael Wrighton, S M Mahbub Murshed
 *
 * S M Mahbub Murshed : added few functions for convenience. May 15, 2007
 *
 * History
 * Dec 16, 2011 - Changed parseName(String) to export authorname with
 * 				  more than 3 names correctly
 *
 */
public class PersonName {
    protected String givenName = null;
    protected String surname = null;
    protected String middleName = null;

    public PersonName() {
    }

    public PersonName(String name) {
        parseName(name);
    }

    public PersonName(String firstName, String _middleName, String lastName) {
        givenName = firstName;
        middleName = _middleName;
        surname = lastName;
    }

    protected void parseName(String author) {
    		Vector<String> v = new Vector<String>();
            String authorMod = AuthorList.fixAuthor_lastNameFirst(author, false);
             
            //Formating names and replacing escape Char for ',' back to a comma
//            XMLChars xmlChars = new XMLChars();
//            authorMod = xmlChars.format(authorMod).replace("&#44;", ",");
 
            int endOfLastName = authorMod.indexOf(",");

            // Tokenize just the firstName and middleNames as we have the surname
            // before the comma.
            WSITools.tokenize(v, authorMod.substring(endOfLastName+1).trim(), " \n\r");
            if (endOfLastName>=0) // comma is found
            	v.add(authorMod.substring(0, endOfLastName));
            
            int amountOfNames = v.size();

            if (amountOfNames == 1)
                surname = v.get(0);
            else if (amountOfNames == 2) {
                givenName = v.get(0);
                surname = v.get(1);
            }
            else {
                givenName = v.get(0);
                middleName = "";
                for (int i = 1; i < amountOfNames - 1 ; i++)
                	middleName += " " + v.get(i);
                middleName = middleName.trim();
                surname = v.get(amountOfNames-1);
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

    public void setSurname(String lastName)
    {
        surname = lastName;
    }

    public String getFirstname()
    {
        return givenName;
    }

    public void setFirstname(String firstName)
    {
        givenName = firstName;
    }

    public String getMiddlename()
    {
        return middleName;
    }

    public void setMiddlename(String _middleName)
    {
        middleName = _middleName;
    }

    public String getFullname()
    {
    	String fullName = "";
    	if(givenName != null && givenName != "")
    		fullName += givenName + " "; 
    	if(middleName != null && middleName != "")
    		fullName += middleName + " ";
    	if(surname != null && surname != "")
    		fullName += surname;
    	
    	return fullName.trim();
    }

    public String toString() {
        return surname;
    }
}
