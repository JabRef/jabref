package net.sf.jabref.logic.util.io;

import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.preferences.JabRefPreferences;

public class FileHistory {

    private final JabRefPreferences prefs;
    private final List<String> history = new LinkedList<>();

    private static final int HISTORY_SIZE = 8;


    public FileHistory(JabRefPreferences prefs) {
        this.prefs = prefs;
        List<String> old = prefs.getStringList(JabRefPreferences.RECENT_FILES);
        if (old != null) {
            history.addAll(old);
        }
    }

    public int size() {
        return history.size();
    }

    /**
     * Adds the filename to the top of the list. If it already is in the list, it is merely moved to the top.
     *
     * @param filename a <code>String</code> value
     */

    public void newFile(String filename) {
        history.remove(filename);
        ((LinkedList<String>) history).addFirst(filename);
        while (history.size() > HISTORY_SIZE) {
            ((LinkedList<String>) history).removeLast();
        }
    }

    public String getFileName(int i) {
        return history.get(i);
    }

    public void removeItem(String filename) {
        history.remove(filename);
    }

    public void storeHistory() {
        if (!history.isEmpty()) {
            prefs.putStringList(JabRefPreferences.RECENT_FILES, history);
        }
    }

}
