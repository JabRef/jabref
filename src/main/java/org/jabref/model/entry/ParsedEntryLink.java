package org.jabref.model.entry;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

public class ParsedEntryLink {

    private String key;
    private Optional<BibEntry> linkedEntry;
    private BibDatabase database;

    public ParsedEntryLink(String key, BibDatabase database) {
        this.key = key;
        this.linkedEntry = database.getEntryByCitationKey(this.key);
        this.database = database;
    }

    public ParsedEntryLink(BibEntry bibEntry) {
        this.key = bibEntry.getCitationKey().orElse("");
        this.linkedEntry = Optional.of(bibEntry);
    }

    public String getKey() {
        return key;
    }

    public Optional<BibEntry> getLinkedEntry() {
        return linkedEntry;
    }

    public void setKey(String newKey) {
        this.key = newKey;
        this.linkedEntry = getDatabase().getEntryByCitationKey(this.key);
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
        if (!(obj instanceof ParsedEntryLink)) {
            return false;
        }
        ParsedEntryLink other = (ParsedEntryLink) obj;
        return Objects.equals(key, other.key) && Objects.equals(linkedEntry, other.linkedEntry);
    }

    public BibDatabase getDatabase() {
        return database;
    }
}
