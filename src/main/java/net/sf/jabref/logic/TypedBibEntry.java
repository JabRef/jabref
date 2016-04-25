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
package net.sf.jabref.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;

public class TypedBibEntry {

    private final BibEntry entry;
    private final Optional<BibDatabase> database;
    private final BibDatabaseMode mode;

    public TypedBibEntry(BibEntry entry, BibDatabaseMode mode) {
        this(entry, Optional.empty(), mode);
    }

    public TypedBibEntry(BibEntry entry, Optional<BibDatabase> database, BibDatabaseMode mode) {
        this.entry = Objects.requireNonNull(entry);
        this.database = Objects.requireNonNull(database);
        this.mode = mode;
    }

    public TypedBibEntry(BibEntry entry, BibDatabaseContext databaseContext) {
        this(entry, databaseContext.getDatabase(), databaseContext.getMode());
    }

    public TypedBibEntry(BibEntry entry, BibDatabase database, BibDatabaseMode mode) {
        this(entry, Optional.of(database), mode);
    }

    /**
     * Returns true if this entry contains the fields it needs to be
     * complete.
     */
    public boolean hasAllRequiredFields() {
        Optional<EntryType> type = EntryTypes.getType(entry.getType(), this.mode);
        if(type.isPresent()) {
            return entry.allFieldsPresent(type.get().getRequiredFields(), database.orElse(null));
        } else {
            return true;
        }
    }

    /**
     * Gets the display name for the type of the entry.
     */
    public String getTypeForDisplay() {
        Optional<EntryType> entryType = EntryTypes.getType(entry.getType(), mode);
        if (entryType.isPresent()) {
            return entryType.get().getName();
        } else {
            return EntryUtil.capitalizeFirst(entry.getType());
        }
    }

    /**
     * Gets a list of linked files.
     *
     * @return the list of linked files, is never null but can be empty
     */
    public List<ParsedFileField> getFiles() {
        //Extract the path
        Optional<String> oldValue = entry.getFieldOptional(Globals.FILE_FIELD);
        if (!oldValue.isPresent()) {
            return new ArrayList<>();
        }

        return FileField.parse(oldValue.get());
    }

    public Optional<FieldChange> setFiles(List<ParsedFileField> files) {

        Optional<String> oldValue = entry.getFieldOptional(Globals.FILE_FIELD);
        String newValue = FileField.getStringRepresentation(files);

        if(oldValue.isPresent() && oldValue.get().equals(newValue)) {
            return Optional.empty();
        }

        entry.setField(Globals.FILE_FIELD, newValue);
        return Optional.of(new FieldChange(entry, Globals.FILE_FIELD, oldValue.orElse(""), newValue));
    }
}
