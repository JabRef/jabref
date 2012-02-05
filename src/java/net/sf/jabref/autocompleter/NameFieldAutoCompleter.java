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
    private boolean lastNameOnly;
    private String prefix = "";
    private boolean autoCompFF, autoCompLF;

	/**
	 * @see AutoCompleterFactory
	 */
    protected NameFieldAutoCompleter(String fieldName) {
        this(new String[] {fieldName}, false);

    }

	public NameFieldAutoCompleter(String[] fieldNames, boolean lastNameOnly) {
		this.fieldNames = fieldNames;
        this.lastNameOnly = lastNameOnly;
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

	}

	public boolean isSingleUnitField() {
		return true;
	}

	public void addBibtexEntry(String fieldValue, BibtexEntry entry) {
		addBibtexEntry(entry);
	}

	public void addBibtexEntry(BibtexEntry entry) {
        if (entry != null) {
            for (int i=0; i<fieldNames.length; i++) {
                String fieldValue = entry.getField(fieldNames[i]);
                if (fieldValue != null) {
                    AuthorList authorList = AuthorList.getAuthorList(fieldValue);
                    for (int j = 0; j < authorList.size(); j++) {
                        AuthorList.Author author = authorList.getAuthor(j);
                        if (lastNameOnly) {
                            addWordToIndex(author.getLastOnly());
                        } else {
                            if (autoCompLF) {
                            	if (Globals.prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR)) {
                            		addWordToIndex(author.getLastFirst(true));
                            	} else if (Globals.prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL)) {
                            		addWordToIndex(author.getLastFirst(false));
                            	} else {
                            		// JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH
                            		addWordToIndex(author.getLastFirst(true));
                            		addWordToIndex(author.getLastFirst(false));
                            	}
                            }
                            if (autoCompFF) {
                            	if (Globals.prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_ABBR)) {
                            		addWordToIndex(author.getFirstLast(true));
                            	} else if (Globals.prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE).equals(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_ONLY_FULL)) {
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

	public String[] complete(String str) {
        int index = str.toLowerCase().lastIndexOf(" and ");
        if (index >= 0) {
            prefix = str.substring(0, index+5);
            str = str.substring(index+5);
        }
        else prefix = "";

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
