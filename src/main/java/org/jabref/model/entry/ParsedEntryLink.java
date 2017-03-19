package org.jabref.model.entry;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

public class ParsedEntryLink {

    private String key;
    private Optional<BibEntry> linkedEntry;
    private BibDatabase dataBase;

    public ParsedEntryLink(String key, BibDatabase dataBase) {
        this.key = key;
        this.linkedEntry = dataBase.getEntryByKey(this.key);
        this.dataBase = dataBase;
    }

    public ParsedEntryLink(BibEntry bibEntry) {
        this.key = bibEntry.getCiteKeyOptional().orElse("");
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
        this.linkedEntry = getDataBase().getEntryByKey(this.key);
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

    public BibDatabase getDataBase() {
        return dataBase;
    }

}
