package org.jabref.logic.sharelatex.events;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.BibEntry;

public class ShareLatexEntryMessageEvent {

    private List<BibEntry> entries = new ArrayList<>();

    private final String database;

    public ShareLatexEntryMessageEvent(List<BibEntry> entries, String database) {
        this.entries = entries;
        this.database = database;
    }

    public List<BibEntry> getEntries() {
        return this.entries;
    }

    public String getNewDatabaseContent() {
        return this.database;
    }

}
