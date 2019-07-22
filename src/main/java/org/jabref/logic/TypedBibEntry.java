package org.jabref.logic;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;

/**
 * Wrapper around a {@link BibEntry} offering methods for {@link BibDatabaseMode} dependend results
 */
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
     * @param entryTypesManager
     */
    public boolean hasAllRequiredFields(BibEntryTypesManager entryTypesManager) {
        Optional<BibEntryType> type = entryTypesManager.enrich(entry.getType(), this.mode);
        if (type.isPresent()) {
            return entry.allFieldsPresent(type.get().getRequiredFields(), database.orElse(null));
        } else {
            return true;
        }
    }

    /**
     * Gets the display name for the type of the entry.
     */
    public String getTypeForDisplay() {
        return entry.getType().getDisplayName();
    }
}
