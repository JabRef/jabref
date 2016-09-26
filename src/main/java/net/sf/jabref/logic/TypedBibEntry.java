package net.sf.jabref.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.strings.StringUtil;

public class TypedBibEntry {

    private final BibEntry entry;
    private final Optional<BibDatabase> database;
    private final BibDatabaseMode mode;

    public TypedBibEntry(BibEntry entry, BibDatabaseMode mode) {
        this(entry, Optional.empty(), mode);
    }

    private TypedBibEntry(BibEntry entry, Optional<BibDatabase> database, BibDatabaseMode mode) {
        this.entry = Objects.requireNonNull(entry);
        this.database = Objects.requireNonNull(database);
        this.mode = mode;
    }

    public TypedBibEntry(BibEntry entry, BibDatabaseContext databaseContext) {
        this(entry, Optional.of(databaseContext.getDatabase()), databaseContext.getMode());
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
            return StringUtil.capitalizeFirst(entry.getType());
        }
    }

    /**
     * Gets a list of linked files.
     *
     * @return the list of linked files, is never null but can be empty
     */
    public List<ParsedFileField> getFiles() {
        //Extract the path
        Optional<String> oldValue = entry.getField(FieldName.FILE);
        if (!oldValue.isPresent()) {
            return new ArrayList<>();
        }

        return FileField.parse(oldValue.get());
    }

    public Optional<FieldChange> setFiles(List<ParsedFileField> files) {

        Optional<String> oldValue = entry.getField(FieldName.FILE);
        String newValue = FileField.getStringRepresentation(files);

        if(oldValue.isPresent() && oldValue.get().equals(newValue)) {
            return Optional.empty();
        }

        entry.setField(FieldName.FILE, newValue);
        return Optional.of(new FieldChange(entry, FieldName.FILE, oldValue.orElse(""), newValue));
    }
}
