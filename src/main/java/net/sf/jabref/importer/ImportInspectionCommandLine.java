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
package net.sf.jabref.importer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.importer.fetcher.EntryFetcher;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.l10n.Localization;

public class ImportInspectionCommandLine implements ImportInspector {

    private final List<BibEntry> entries = new LinkedList<>();


    @Override
    public void addEntry(BibEntry entry) {
        entries.add(entry);
    }

    @Override
    public void setProgress(int current, int max) {
        status.setStatus(Localization.lang("Progress: %0 of %1", String.valueOf(current), String
                .valueOf(max)));
    }


    private final OutputPrinter status = new OutputPrinter() {

        @Override
        public void setStatus(String s) {
            System.out.println(s);
        }

        @Override
        public void showMessage(Object message, String title, int msgType) {
            System.out.println(title + ": " + message);
        }

        @Override
        public void showMessage(String message) {
            System.out.println(message);
        }
    };


    public Collection<BibEntry> query(String query, EntryFetcher fetcher) {
        entries.clear();
        if (fetcher.processQuery(query, ImportInspectionCommandLine.this, status)) {
            return entries;
        }
        return null;
    }

    @Override
    public void toFront() {
        // Nothing
    }
}
