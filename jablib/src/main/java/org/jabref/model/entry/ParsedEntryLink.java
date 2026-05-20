package org.jabref.model.entry;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

import org.jspecify.annotations.Nullable;

public class ParsedEntryLink {

    private String key;
    @Nullable private BibEntry linkedEntry;
    private BibDatabase database;

    public ParsedEntryLink(String key, BibDatabase database) {
        this.key = key;
        this.linkedEntry = database.getEntryByCitationKey(this.key).orElse(null);
        this.database = database;
    }

    public ParsedEntryLink(BibEntry bibEntry) {
        this.key = bibEntry.getCitationKey().orElse("");
        this.linkedEntry = bibEntry;
    }

    public String getKey() {
        return key;
    }

    public Optional<BibEntry> getLinkedEntry() {
        return Optional.ofNullable(linkedEntry);
    }

    public void setKey(String newKey) {
        this.key = newKey;
        this.linkedEntry = getDatabase().getEntryByCitationKey(this.key).orElse(null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, linkedEntry);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ParsedEntryLink other)) {
            return false;
        }
        return Objects.equals(key, other.key) && Objects.equals(linkedEntry, other.linkedEntry);
    }

    public BibDatabase getDatabase() {
        return database;
    }
}
