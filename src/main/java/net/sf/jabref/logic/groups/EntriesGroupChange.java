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
package net.sf.jabref.logic.groups;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.entry.BibEntry;

public class EntriesGroupChange {

    private Set<BibEntry> oldEntries;
    private Set<BibEntry> newEntries;
    private List<FieldChange> entryChanges;

    public EntriesGroupChange(Set<BibEntry> oldEntries, Set<BibEntry> newEntries) {
        this(oldEntries, newEntries, Collections.emptyList());
    }

    public EntriesGroupChange(List<FieldChange> entryChanges) {
        this(Collections.emptySet(), Collections.emptySet(), entryChanges);
    }

    public EntriesGroupChange(Set<BibEntry> oldEntries, Set<BibEntry> newEntries,
            List<FieldChange> entryChanges) {
        this.oldEntries = oldEntries;
        this.newEntries = newEntries;
        this.entryChanges = entryChanges;
    }

    public Set<BibEntry> getOldEntries() {
        return oldEntries;
    }

    public Set<BibEntry> getNewEntries() {
        return newEntries;
    }

    public Iterable<FieldChange> getEntryChanges() {
        return entryChanges;
    }

}
