/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.model.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EntrySorter {

    private static final Log LOGGER = LogFactory.getLog(EntrySorter.class);

    // guarded by itself
    private final List<BibEntry> entries;
    private final Comparator<BibEntry> comp;

    public EntrySorter(List<BibEntry> entries, Comparator<BibEntry> comparator) {
        this.entries = new ArrayList<>(entries);
        this.comp = comparator;
        Collections.sort(this.entries, comparator);
    }

    public BibEntry getEntryAt(int pos) {
        return entries.get(pos);
    }

    public int getEntryCount() {
        return entries.size();
    }

}
