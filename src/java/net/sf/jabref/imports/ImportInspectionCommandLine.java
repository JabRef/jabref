/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.imports;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;

public class ImportInspectionCommandLine implements ImportInspector {

    List<BibtexEntry> entries = new LinkedList<BibtexEntry>();

    public void addEntry(BibtexEntry entry) {
        entries.add(entry);
    }

    public void setProgress(int current, int max) {
        status.setStatus(Globals.lang("Progress: %0 of %1", String.valueOf(current), String
            .valueOf(max)));
    }

    OutputPrinter status = new OutputPrinter() {

        public void setStatus(String s) {
            System.out.println(s);
        }

        public void showMessage(Object message, String title, int msgType) {
            System.out.println(title + ": " + message);
        }

        public void showMessage(String message) {
            System.out.println(message);
        }
    };

    public Collection<BibtexEntry> query(String query, EntryFetcher fetcher) {
        entries.clear();
        if (fetcher.processQuery(query, ImportInspectionCommandLine.this, status)) {
            return entries;
        }
        return null;
    }

    public void toFront() {
    }
}
