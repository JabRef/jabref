/*  Copyright (C) 2003-2012 JabRef contributors.
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
package net.sf.jabref.autocompleter;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 * Interprets the given values as names and stores them in different
 * permutations so we can complete by beginning with last name or first name.
 * 
 * @author kahlert, cordes
 * 
 */
public class NameFieldAutoCompleter extends AbstractAutoCompleter {

	private String[] fieldNames;
    private boolean lastNameOnlyAndSeparationBySpace; // true if only last names should be completed and there is NO separation by " and ", but by " "
    private String prefix = "";
    private boolean autoCompFF, autoCompLF, autoCompFullFirstOnly, autoCompShortFirstOnly;

	/**
	 * @see AutoCompleterFactory
	 */
    protected NameFieldAutoCompleter(String fieldName) {
        this(new String[] {fieldName}, false);

    }

	public NameFieldAutoCompleter(String[] fieldNames, boolean lastNameOnlyAndSeparationBySpace) {
		this.fieldNames = fieldNames;
        this.lastNameOnlyAndSeparationBySpace = lastNameOnlyAndSeparationBySpace;
        if (Globals.prefs.getBoolean("autoCompFF")) {
            autoCompFF = true;
            autoCompLF = false;
        }
        else if (Globals.prefs.getBoolean("autoCompLF")) {
            autoCompFF = false;
            autoCompLF = true;
        }
        else {
            autoCompFF = true;
            autoCompLF = true;
        }
        autoCompShortFirstOnly = Globals.prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR);
        autoCompFullFirstOnly = Globals.prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL);
	}

	public boolean isSingleUnitField() {
		// quick hack
		// when used at entry fields (!this.lastNameOnlyAndSeparationBySpace), this is a single unit field
		// when used at the search form (this.lastNameOnlyAndSeparationBySpace), this is NOT a single unit field
		// reason: search keywords are separated by space. 
		//    This is OK for last names without prefix. "Lastname" works perfectly.
		//    querying for "van der Lastname" can be interpreted as
		//      a) "van" "der" "Lastname"
		//      b) "van der Lastname" (autocompletion lastname)
		return !this.lastNameOnlyAndSeparationBySpace;
	}

	public void addBibtexEntry(BibtexEntry entry) {
        if (entry != null) {
            for (int i=0; i<fieldNames.length; i++) {
                String fieldValue = entry.getField(fieldNames[i]);
                if (fieldValue != null) {
                    AuthorList authorList = AuthorList.getAuthorList(fieldValue);
                    for (int j = 0; j < authorList.size(); j++) {
                        AuthorList.Author author = authorList.getAuthor(j);
                        if (lastNameOnlyAndSeparationBySpace) {
                            addWordToIndex(author.getLastOnly());
                        } else {
                            if (autoCompLF) {
                            	if (autoCompShortFirstOnly) {
                            		addWordToIndex(author.getLastFirst(true));
                            	} else if (autoCompFullFirstOnly) {
                            		addWordToIndex(author.getLastFirst(false));
                            	} else {
                            		// JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH
                            		addWordToIndex(author.getLastFirst(true));
                            		addWordToIndex(author.getLastFirst(false));
                            	}
                            }
                            if (autoCompFF) {
                            	if (autoCompShortFirstOnly) {
                            		addWordToIndex(author.getFirstLast(true));
                            	} else if (autoCompFullFirstOnly) {
                            		addWordToIndex(author.getFirstLast(false));
                            	} else {
                            		// JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH
                                    addWordToIndex(author.getFirstLast(true));
                                    addWordToIndex(author.getFirstLast(false));
                            	}
                            }
                        }
                    }
                }
            }
		}
	}
	
	/**
	 * SIDE EFFECT: sets class variable prefix
	 * Delimiter: " and "
	 * 
	 * @return String without prefix
	 */
	private String determinePrefixAndReturnRemainder_AND(String str) {
        int index = str.toLowerCase().lastIndexOf(" and ");
        if (index >= 0) {
            prefix = str.substring(0, index+5);
            str = str.substring(index+5);
        } else {
        	prefix = "";
        }
        return str;
	}

	/**
	 * SIDE EFFECT: sets class variable prefix
	 * Delimiter: " "
	 * 
	 * @return String without prefix
	 */
	private String determinePrefixAndReturnRemainder_SPACE(String str) {
        int index = str.lastIndexOf(" ");
        if (index >= 0) {
            prefix = str.substring(0, index+1);
            str = str.substring(index+1);
        } else {
        	prefix = "";
        }
        return str;
	}

	public String[] complete(String str) {
		// Normally, one would implement that using 
		// class inheritance. But this seemed to overengineered
		if (this.lastNameOnlyAndSeparationBySpace) {
			str = determinePrefixAndReturnRemainder_SPACE(str);
		} else {
			str = determinePrefixAndReturnRemainder_AND(str);
		}
        String[] res = super.complete(str);
        return res;
	}

	public String getFieldName() {
		return fieldNames[0];
	}

    @Override
    public String getPrefix() {
        return prefix;
    }

}
