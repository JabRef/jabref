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
 * Wrapper around a {@link BibEntry} offering methods for {@link BibDatabaseMode}-dependent results
 */
public class TypedBibEntry {

    private final BibEntry entry;
    private final Optional<BibDatabase> database;
    private final BibDatabaseMode mode;

    public TypedBibEntry(BibEntry entry, BibDatabaseMode mode) {
        this.entry = Objects.requireNonNull(entry);
        this.database = Optional.empty();
        // mode may be null
        this.mode = mode;
    }

    public TypedBibEntry(BibEntry entry, BibDatabaseContext databaseContext) {
        this.entry = Objects.requireNonNull(entry);
        this.database = Optional.of(databaseContext.getDatabase());
        this.mode = Objects.requireNonNull(databaseContext).getMode();
    }

    /**
     * Checks the fields of the entry whether all required fields are set.
     * In other words: It is checked whether this entry contains all fields it needs to be complete.
     *
     * @return true if all required fields are set, false otherwise
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
