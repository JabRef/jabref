/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 * Returns an autocompleter to a given fieldname.
 * 
 * @author kahlert, cordes
 */
public class AutoCompleterFactory {

    public static int SHORTEST_TO_COMPLETE = Globals.prefs.getInt(JabRefPreferences.SHORTEST_TO_COMPLETE);


    public static AutoCompleter<String> getFor(String fieldName) {
        if ("author".equals(fieldName) || "editor".equals(fieldName)) {
            return new NameFieldAutoCompleter(fieldName);
        } else if ("crossref".equals(fieldName)) {
            return new CrossrefAutoCompleter();
        } else if ("journal".equals(fieldName) || "publisher".equals(fieldName)) {
            return new EntireFieldAutoCompleter(fieldName);
        } else {
            return new DefaultAutoCompleter(fieldName);
        }
    }

    public static AutoCompleter<String> getFor(String fieldName, String secondFieldName) {
        return new NameFieldAutoCompleter(new String[] {fieldName, secondFieldName}, true);
    }

}
