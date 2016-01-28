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

package net.sf.jabref.logic.util.io;

import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.JabRefPreferences;

public class FileHistory {

    private final JabRefPreferences prefs;
    private final List<String> history = new LinkedList<>();

    private final static int HISTORY_SIZE = 8;


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
