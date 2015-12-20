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
package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibEntry;

public class DatabaseChangeEvent {

    public enum ChangeType {
        ADDED_ENTRY, REMOVED_ENTRY, CHANGED_ENTRY, CHANGING_ENTRY
    }


    private final BibEntry entry;
    private final ChangeType type;
    private final BibDatabase source;


    public DatabaseChangeEvent(BibDatabase source, ChangeType type,
            BibEntry entry) {
        this.source = source;
        this.type = type;
        this.entry = entry;
    }

    public BibDatabase getSource() {
        return source;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public ChangeType getType() {
        return type;
    }
}
