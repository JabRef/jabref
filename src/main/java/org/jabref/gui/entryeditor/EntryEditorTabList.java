package org.jabref.gui.entryeditor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.JabRefPreferences;

/**
 * Class for creating the information about customizable entry editor tabs.
 */
public final class EntryEditorTabList {

    private EntryEditorTabList() {

    }

    public static Map<String, Set<Field>> create(JabRefPreferences preferences) {
        Map<String, Set<Field>> tabs = new LinkedHashMap<>();
        int i = 0;
        String name;
        if (preferences.hasKey(JabRefPreferences.CUSTOM_TAB_NAME + 0)) {
            // The user has modified from the default values:
            while (preferences.hasKey(JabRefPreferences.CUSTOM_TAB_NAME + i)) {
                name = preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + i);
                Set<Field> entry = FieldFactory.parseFieldList(preferences.get(JabRefPreferences.CUSTOM_TAB_FIELDS + i));
                tabs.put(name, entry);
                i++;
            }
        } else {
            // Nothing set, so we use the default values:
            while (preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i) != null) {
                name = preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + "_def" + i);
                Set<Field> entry = FieldFactory.parseFieldList(preferences.get(JabRefPreferences.CUSTOM_TAB_FIELDS + "_def" + i));
                tabs.put(name, entry);
                i++;
            }
        }
        return tabs;
    }
}
