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

import java.util.HashMap;
import java.util.Objects;

import net.sf.jabref.JabRefPreferences;

public class AutoCompletePreferences {

    private final JabRefPreferences preferences;

    private static final String PREF_SHORTEST_TO_COMPLETE = "shortestToComplete";
    private static final String PREF_FIRSTNAME_MODE = "autoCompFirstNameMode";
    private static final String PREF_LAST_FIRST = "autoCompLF";
    private static final String PREF_FIRST_LAST = "autoCompFF";
    private static final String PREF_COMPLETE_FIELDS = "autoCompleteFields";


    public static void putDefaults(HashMap<String, Object> defaults) {
        defaults.put(PREF_SHORTEST_TO_COMPLETE, 1);
        defaults.put(PREF_FIRSTNAME_MODE, AutoCompleteFirstNameMode.BOTH.name());
        defaults.put(PREF_FIRST_LAST, Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put(PREF_LAST_FIRST, Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(PREF_COMPLETE_FIELDS, "author;editor;title;journal;publisher;keywords;crossref");
    }

    public AutoCompletePreferences(JabRefPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public int getShortestLengthToComplete() {
        return preferences.getInt(PREF_SHORTEST_TO_COMPLETE);
    }

    public void setShortestLengthToComplete(Integer value) {
        preferences.putInt(PREF_SHORTEST_TO_COMPLETE, value);
    }

    public AutoCompleteFirstNameMode getFirstnameMode() {
        try {
            return AutoCompleteFirstNameMode.valueOf(preferences.get(PREF_FIRSTNAME_MODE));
        } catch (IllegalArgumentException ex) {
            // Should only occur when preferences are set directly via preferences.put and not via setFirstnameMode
            return AutoCompleteFirstNameMode.BOTH;
        }
    }

    public void setFirstnameMode(AutoCompleteFirstNameMode mode) {
        preferences.put(PREF_FIRSTNAME_MODE, mode.name());
    }

    public boolean getCompleteLastFirst() {
        return preferences.getBoolean(PREF_LAST_FIRST);
    }

    public void setCompleteLastFirst(boolean value) {
        preferences.putBoolean(PREF_LAST_FIRST, value);
    }

    public boolean getCompleteFirstLast() {
        return preferences.getBoolean(PREF_FIRST_LAST);
    }

    public void setCompleteFirstLast(boolean value) {
        preferences.putBoolean(PREF_FIRST_LAST, value);
    }

    public String[] getCompleteNames() {
        return preferences.getStringArray(PREF_COMPLETE_FIELDS);
    }

    public String getCompleteNamesAsString() {
        return preferences.get(PREF_COMPLETE_FIELDS);
    }

    public void setCompleteNames(String value) {
        preferences.put(PREF_COMPLETE_FIELDS, value);
    }
}