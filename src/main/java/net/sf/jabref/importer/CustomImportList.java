/*
 Copyright (C) 2005-2015 Andreas Rudert, Oscar Gustafsson based on CustomExportList by ??

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
  Copyright (C) 2005-2014 JabRef contributors.

*/
package net.sf.jabref.importer;

import java.util.List;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 * Collection of user defined custom import formats.
 *
 * <p>The collection can be stored and retrieved from Preferences. It is sorted by the default
 * order of {@link ImportFormat}.</p>
 */
public class CustomImportList extends TreeSet<CustomImporter> {

    private final JabRefPreferences prefs;

    private static final Log LOGGER = LogFactory.getLog(CustomImportList.class);


    public CustomImportList(JabRefPreferences prefs) {
        super();
        this.prefs = prefs;
        readPrefs();
    }

    private void readPrefs() {
        int i = 0;
        List<String> s;
        while (!((s = prefs.getStringList(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i)).isEmpty())) {
            try {
                super.add(new CustomImporter(s));
            } catch (Exception e) {
                LOGGER.warn("Could not load " + s.get(0) + " from preferences. Will ignore.", e);
            }
            i++;
        }
    }

    private void addImporter(CustomImporter customImporter) {
        super.add(customImporter);
    }

    /**
     * Adds an importer.
     *
     * <p>If an old one equal to the new one was contained, the old
     * one is replaced.</p>
     *
     * @param customImporter new (version of an) importer
     * @return  if the importer was contained
     */
    public boolean replaceImporter(CustomImporter customImporter) {
        boolean wasContained = this.remove(customImporter);
        this.addImporter(customImporter);
        return wasContained;
    }

    public void store() {
        purgeAll();
        CustomImporter[] importers = this.toArray(new CustomImporter[this.size()]);
        for (int i = 0; i < importers.length; i++) {
            Globals.prefs.putStringList(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i, importers[i].getAsStringList());
        }
    }

    private void purgeAll() {
        for (int i = 0; !(Globals.prefs.getStringList(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i).isEmpty()); i++) {
            Globals.prefs.remove(JabRefPreferences.CUSTOM_IMPORT_FORMAT + i);
        }
    }
}
