package org.jabref.logic.bibtex;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Wrapper around a {@link BibEntry} offering methods for {@link BibDatabaseMode}-dependent results
public class TypedBibEntry {

    private final BibEntry entry;
    @Nullable private final BibDatabaseMode mode;

    public TypedBibEntry(@NonNull BibEntry entry, @Nullable BibDatabaseMode mode) {
        this.entry = entry;
        this.mode = mode;
    }

    /// Checks the fields of the entry whether all required fields are set.
    /// In other words: It is checked whether this entry contains all fields it needs to be complete.
    ///
    /// @return true if all required fields are set, false otherwise
    public boolean hasAllRequiredFields(BibEntryTypesManager entryTypesManager) {
        Optional<BibEntryType> type = entryTypesManager.enrich(entry.getType(), mode);
        return type.map(bibEntryType ->
                           entry.allFieldsPresent(bibEntryType.getRequiredFields(), null))
                   .orElse(true);
    }

    /// Gets the display name for the type of the entry.
    public String getTypeForDisplay() {
        return entry.getType().getDisplayName();
    }
}
