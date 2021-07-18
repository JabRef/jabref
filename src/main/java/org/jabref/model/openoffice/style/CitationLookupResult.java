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
     *
     *  CitationLookupResult.equals is used in CitedKey.addPath to check the added Citation
     *  refers to the same source as the others. As long as we look up each citation key
     *  only once (in CitationGroups.lookupCitations), the default implementation for equals
     *  would be sufficient (and could also omit hashCode below).
     */
    @Override
    public boolean equals(Object otherObject) {
        if (otherObject == this) {
            return true;
        }
        if (!(otherObject instanceof CitationLookupResult)) {
            return false;
        }
        CitationLookupResult that = (CitationLookupResult) otherObject;
        return Objects.equals(this.entry, that.entry) && Objects.equals(this.database, that.database);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry, database);
    }
}
