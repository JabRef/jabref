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
package net.sf.jabref.exporter;

import java.util.Comparator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.BasicEventList;
import net.sf.jabref.logic.l10n.Localization;

/**
 * This class handles user defined custom export formats. They are initially
 * read from Preferences, and kept alphabetically (sorted by name). Formats can
 * be added or removed. When modified, the sort() method must be called to make
 * sure the formats stay properly sorted. When the method store() is called,
 * export formats are written to Preferences.
 */

public class CustomExportList {

    private final EventList<String[]> list;
    private final SortedList<String[]> sorted;
    private final TreeMap<String, ExportFormat> formats = new TreeMap<>();

    private static final Log LOGGER = LogFactory.getLog(CustomExportList.class);


    public CustomExportList(Comparator<String[]> comp) {
        list = new BasicEventList<>();
        sorted = new SortedList<>(list, comp);
    }

    public TreeMap<String, ExportFormat> getCustomExportFormats() {
        formats.clear();
        readPrefs();
        return formats;
    }

    public int size() {
        return list.size();
    }

    public EventList<String[]> getSortedList() {
        return sorted;
    }

    private void readPrefs() {
        formats.clear();
        list.clear();
        int i = 0;
        String[] s;
        while ((s = Globals.prefs.getStringArray("customExportFormat" + i)) != null) {
            ExportFormat format = createFormat(s);
            if (format != null) {
                formats.put(format.getConsoleName(), format);
                list.add(s);
            } else {
                String customExportFormat = Globals.prefs.get("customExportFormat" + i);
                LOGGER.error(Localization.lang("Error initializing custom export format from string '%0'",
                        customExportFormat));
            }
            i++;
        }
    }

    private ExportFormat createFormat(String[] s) {
        if (s.length < 3) {
            return null;
        }
        String lfFileName;
        if (s[1].endsWith(".layout")) {
            lfFileName = s[1].substring(0, s[1].length() - 7);
        } else {
            lfFileName = s[1];
        }
        ExportFormat format = new ExportFormat(s[0], s[0], lfFileName, null,
                s[2]);
        format.setCustomExport(true);
        return format;
    }

    public void addFormat(String[] s) {
        list.add(s);
        ExportFormat format = createFormat(s);
        formats.put(format.getConsoleName(), format);
    }

    public void remove(String[] toRemove) {

        ExportFormat format = createFormat(toRemove);
        formats.remove(format.getConsoleName());
        list.remove(toRemove);

    }

    public void store() {

        if (list.isEmpty()) {
            purge(0);
        } else {
            for (int i = 0; i < list.size(); i++) {
                // System.out.println(i+"..");
                Globals.prefs.putStringArray("customExportFormat" + i,
                        list.get(i));
            }
            purge(list.size());
        }
    }

    private void purge(int from) {
        int i = from;
        while (Globals.prefs.getStringArray("customExportFormat" + i) != null) {
            Globals.prefs.remove("customExportFormat" + i);
            i++;
        }
    }

}
