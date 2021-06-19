package org.jabref.model.openoffice.style;

import java.util.Objects;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class CitationLookupResult {

    public final BibEntry entry;
    public final BibDatabase database;

    public CitationLookupResult(BibEntry entry, BibDatabase database) {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(database);
        this.entry = entry;
        this.database = database;
    }

    /**
     * Note: BibEntry overrides Object.equals, but BibDatabase does not.
     *
     *       Consequently, {@code this.database.equals(that.database)} below
     *       is equivalent to {@code this.database == that.database}.
     *
     *       Since within each GUI call we use a fixed list of databases, it is OK.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CitationLookupResult)) {
            return false;
        }
        CitationLookupResult that = (CitationLookupResult) o;
        return this.entry.equals(that.entry) && this.database.equals(that.database);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + entry.hashCode();
        result = prime * result + database.hashCode();
        return result;
    }
}
