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
package net.sf.jabref.model.entry;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;

import java.util.Objects;
import java.util.Optional;

public class TypedBibEntry {

    private final BibEntry entry;
    private final Optional<BibDatabase> database;
    private final BibDatabaseMode type;

    public TypedBibEntry(BibEntry entry, BibDatabaseMode type) {
        this(entry, Optional.empty(), type);
    }

    public TypedBibEntry(BibEntry entry, Optional<BibDatabase> database, BibDatabaseMode type) {
        this.entry = Objects.requireNonNull(entry);
        this.database = Objects.requireNonNull(database);
        this.type = type;
    }

    /**
     * Returns true if this entry contains the fields it needs to be
     * complete.
     */
    public boolean hasAllRequiredFields() {
        EntryType type = EntryTypes.getType(entry.getType(), this.type);
        return entry.allFieldsPresent(type.getRequiredFields(), database.orElse(null));
    }

    /**
     * Gets the display name for the type of the entry.
     */
    public String getTypeForDisplay() {
        EntryType entryType = EntryTypes.getType(entry.getType(), type);
        if (entryType != null) {
            return entryType.getName();
        } else {
            return EntryUtil.capitalizeFirst(entry.getType());
        }
    }
}
