/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 * Class for holding the information about customizable entry editor tabs.
 */
public final class EntryEditorTabList {

    private List<List<String>> list;
    private List<String> names;

    public EntryEditorTabList() {
        init();
    }

    private void init() {
        list = new ArrayList<>();
        names = new ArrayList<>();
        int i = 0;
        String name;
        if (Globals.prefs.hasKey(JabRefPreferences.CUSTOM_TAB_NAME + 0)) {
            // The user has modified from the default values:
            while (Globals.prefs.hasKey(JabRefPreferences.CUSTOM_TAB_NAME + i)) {
                name = Globals.prefs.get(JabRefPreferences.CUSTOM_TAB_NAME + i);
                List<String> entry = Arrays
                        .asList(Globals.prefs.get(JabRefPreferences.CUSTOM_TAB_FIELDS + i).split(";"));
                names.add(name);
                list.add(entry);
                i++;
            }
        } else {
            // Nothing set, so we use the default values:
            while (Globals.prefs.get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i) != null) {
                name = Globals.prefs.get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i);
                List<String> entry = Arrays
                        .asList(Globals.prefs.get(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def" + i).split(";"));
                names.add(name);
                list.add(entry);
                i++;
            }
        }
    }

    public int getTabCount() {
        return list.size();
    }

    public String getTabName(int tab) {
        return names.get(tab);
    }

    public List<String> getTabFields(int tab) {
        return list.get(tab);
    }
}
