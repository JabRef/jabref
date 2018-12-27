package org.jabref.gui.entryeditor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jabref.preferences.JabRefPreferences;

/**
 * Class for creating the information about customizable entry editor tabs.
 */
public final class EntryEditorTabList {

    private EntryEditorTabList() {

    }

    public static Map<String, List<String>> create(JabRefPreferences preferences) {
        Map<String, List<String>> tabs = new TreeMap<>();
        int i = 0;
        String name;
        if (preferences.hasKey(JabRefPreferences.CUSTOM_TAB_NAME + 0)) {
            // The user has modified from the default values:
            while (preferences.hasKey(JabRefPreferences.CUSTOM_TAB_NAME + i)) {
                name = preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + i);
                List<String> entry = Arrays
                        .asList(preferences.get(JabRefPreferences.CUSTOM_TAB_FIELDS + i).split(";"));
                tabs.put(name, entry);
                i++;
            }
        } else {
            // Nothing set, so we use the default values:
            while (preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i) != null) {
                name = preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i);
                List<String> entry = Arrays
                        .asList(preferences.get(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def" + i).split(";"));
                tabs.put(name, entry);
                i++;
            }
        }
        return tabs;
    }
}
