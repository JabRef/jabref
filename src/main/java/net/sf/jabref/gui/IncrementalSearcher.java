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
package net.sf.jabref.gui;

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.JabRefPreferences;

import java.util.Set;

class IncrementalSearcher {

    private final JabRefPreferences prefs;
    private String hitInField;


    public IncrementalSearcher(JabRefPreferences prefs) {
        this.prefs = prefs;
    }

    public String getField() {
        return hitInField;
    }

    public boolean search(String pattern, BibtexEntry bibtexEntry) {
        hitInField = null;
        return searchFields(bibtexEntry.getAllFields(), bibtexEntry, pattern);
    }

    private boolean searchFields(Set<String> fields, BibtexEntry bibtexEntry,
                                 String searchString) {
        boolean found = false;
        if (fields != null) {

            for (String field : fields) {
                try {
                    /*Globals.logger("Searching field '"+fields[i].toString()
                    	       +"' for '"
                    	       +pattern.toString()+"'.");*/
                    if (bibtexEntry.getField(field) != null) {
                        if (prefs.getBoolean(JabRefPreferences.CASE_SENSITIVE_SEARCH)) {
                            if (bibtexEntry.getField(field).contains(searchString)) {
                                found = true;
                            }
                        } else {
                            if (bibtexEntry.getField(field).toLowerCase().contains(searchString.toLowerCase())) {
                                found = true;
                            }
                        }

                        if (found) {
                            hitInField = field;
                            return true;
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("Searching error: " + t);
                }
            }
        }
        return false;
    }
}
