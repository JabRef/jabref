package net.sf.jabref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for holding the information about customizable entry editor tabs.
 */
public final class EntryEditorTabList {

    private List<List<String>> list = null;
    private List<String> names = null;

    public EntryEditorTabList() {
        init();
    }

    private void init() {
        list = new ArrayList<List<String>>();
        names = new ArrayList<String>();
        int i = 0;
        String name;
        String[] fields;
        if (Globals.prefs.hasKey(Globals.prefs.CUSTOM_TAB_NAME + 0)) {
            // The user has modified from the default values:
            while (Globals.prefs.hasKey(Globals.prefs.CUSTOM_TAB_NAME + i)) {
                name = Globals.prefs.get(Globals.prefs.CUSTOM_TAB_NAME + i);
                fields = Globals.prefs.get(Globals.prefs.CUSTOM_TAB_FIELDS + i).split(";");
                List<String> entry = Arrays.asList(fields);
                names.add(name);
                list.add(entry);
                i++;
            }
        } else {
            // Nothing set, so we use the default values:
            while (Globals.prefs.get(Globals.prefs.CUSTOM_TAB_NAME + "_def"+i) != null) {
                name = Globals.prefs.get(Globals.prefs.CUSTOM_TAB_NAME + "_def" + i);
                fields = Globals.prefs.get(Globals.prefs.CUSTOM_TAB_FIELDS + "_def" + i).split(";");
                List<String> entry = Arrays.asList(fields);
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
