package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

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
